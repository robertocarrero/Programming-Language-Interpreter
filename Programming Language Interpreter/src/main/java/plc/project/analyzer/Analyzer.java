package plc.project.analyzer;

import plc.project.parser.Ast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;

public final class Analyzer implements Ast.Visitor<Type, AnalyzeException> {

    private Context context;

    public Analyzer(Scope scope) {
        this.context = new Context(scope, Optional.empty(), new HashSet<>(), false);
    }

    public Context getContext() {
        return context;
    }

    @Override
    public Type visit(Ast.Source ast) throws AnalyzeException {
        Type type = Type.NIL;
        for (var statement : ast.statements()) {
            type = visit(statement);
        }
        return type;
    }

    @Override
    public Type visit(Ast.Stmt.Let ast) throws AnalyzeException {
        Type varType;
        if (ast.type().isPresent()) {
            varType = resolveTypeName(ast.type().get());
        }
        else if (ast.value().isPresent()) {
            varType = visit(ast.value().get());
        }
        else {
            varType = Type.DYNAMIC;
        }
        if (ast.value().isPresent()) {
            Type valueType = visit(ast.value().get());
            if (!valueType.isSubtypeOf(varType)) {
                throw new AnalyzeException("Value type " + valueType + " is not a subtype of variable type " + varType, ast);
            }
        }
        try {
            context.scope().declare(ast.name(), varType);
        } catch (IllegalStateException e) {
            throw new AnalyzeException("Variable is already defined.", ast);
        }

        if (ast.value().isEmpty()) {
            context.uninitialized().add(ast.name());
        } else {
            context.uninitialized().remove(ast.name());
        }
        return varType;
    }

    @Override
    public Type visit(Ast.Stmt.Def ast) throws AnalyzeException {
        List<Type> paramTypes = new ArrayList<>();
        for (int i = 0; i < ast.parameters().size(); i++) {
            Optional<String> typeNameOpt = ast.parameterTypes().get(i);
            if (typeNameOpt.isPresent()) {
                paramTypes.add(resolveTypeName(typeNameOpt.get()));
            }
            else {
                paramTypes.add(Type.DYNAMIC);
            }
        }
        Type returnType = ast.returnType().isPresent() ? resolveTypeName(ast.returnType().get()) : Type.DYNAMIC;
        Type.Function funcType = new Type.Function(paramTypes, returnType);
        try {
            context.scope().declare(ast.name(), funcType);
        }
        catch (IllegalStateException e) {
            throw new AnalyzeException("Function already defined: " + ast.name(), ast);
        }
        Context savedContext = context;
        Context funcContext = new Context(new Scope(context.scope()), Optional.of(funcType), new HashSet<>(context.uninitialized()), false);
        context = funcContext;
        try {
            for (int i = 0; i < ast.parameters().size(); i++) {
                try {
                    context.scope().declare(ast.parameters().get(i), paramTypes.get(i));
                }
                catch (IllegalStateException e) {
                    throw new AnalyzeException("Duplicate parameter name: " + ast.parameters().get(i), ast);
                }
            }
            analyzeStatements(ast.body());
            if (!returnType.equals(Type.DYNAMIC) && !returnType.equals(Type.NIL) && !context.returns()) {
                throw new AnalyzeException("Function '" + ast.name() + "' does not always return value", ast);
            }
        }
        finally {
            context = savedContext;
        }
        return funcType;
    }

    @Override
    public Type visit(Ast.Stmt.If ast) throws AnalyzeException {
        Type condition = visit(ast.condition());
        if (!condition.isSubtypeOf(Type.BOOLEAN)) {
            throw new AnalyzeException("If condition must be Boolean, got " + condition, ast);
        }
        Context savedContext = context;
        Context thenContext = new Context(savedContext);
        context = thenContext;
        analyzeStatements(ast.thenBody());
        Context elseContext = new Context(savedContext);
        context = elseContext;
        analyzeStatements(ast.elseBody());
        context = savedContext;
        context.merge(List.of(thenContext, elseContext));
        return Type.DYNAMIC;
    }

    @Override
    public Type visit(Ast.Stmt.For ast) throws AnalyzeException {
        Type iterType = visit(ast.expression());
        if (!iterType.isSubtypeOf(Type.ITERABLE)) {
            throw new AnalyzeException("For expression must be Iterable, got " + iterType, ast);
        }
        Context savedContext = context;
        Context loopContext = new Context(savedContext);
        context = loopContext;
        try {
            context.scope().declare(ast.name(), Type.INTEGER);
            analyzeStatements(ast.body());
        }
        finally {
            context = savedContext;
        }
        if (loopContext.returns()) {
            context.returns(true);
        }
        return Type.NIL;
    }

    @Override
    public Type visit(Ast.Stmt.Return ast) throws AnalyzeException {
        if (context.function().isEmpty()) {
            throw new AnalyzeException("RETURN statement outside of function", ast);
        }
        Type returnType = context.function().get().returns();
        Type valueType = ast.value().isPresent() ? visit(ast.value().get()) : Type.NIL;
        if (!valueType.isSubtypeOf(returnType)) {
            throw new AnalyzeException("Return value type " + valueType + " is not a subtype of function return type " + returnType, ast);
        }
        context.returns(true);
        return Type.NIL;
    }

    @Override
    public Type visit(Ast.Stmt.Expression ast) throws AnalyzeException {
        return visit(ast.expression());
    }

    @Override
    public Type visit(Ast.Stmt.Assignment ast) throws AnalyzeException {
        if (ast.expression() instanceof Ast.Expr.Variable varExpr) {
            Optional<Type> varTypeOpt = context.scope().resolve(varExpr.name());
            if (varTypeOpt.isEmpty()) {
                throw new AnalyzeException("Undefined variable: " + varExpr.name(), ast);
            }
            Type varType = varTypeOpt.get();
            Type valueType = visit(ast.value());
            if (!valueType.isSubtypeOf(varType)) {
                throw new AnalyzeException("Value type " + valueType + " is not a subtype of variable type " + varType, ast);
            }
            context.uninitialized().remove(varExpr.name());
            return varType;
        }
        else if (ast.expression() instanceof Ast.Expr.Property propExpr) {
            Type receiverType = visit(propExpr.receiver());
            Type propType = resolveProperty(receiverType, propExpr.name(), ast);
            Type valueType = visit(ast.value());
            if (!valueType.isSubtypeOf(propType)) {
                throw new AnalyzeException("Value type " + valueType + " is not a subtype of property type " + propType, ast);
            }
            if (propType.equals(Type.DYNAMIC)) {
                return valueType;
            }
            return propType;
        }
        else {
            throw new AnalyzeException("Invalid assignment receiver", ast);
        }
    }

    @Override
    public Type visit(Ast.Expr.Literal ast) throws AnalyzeException {
        return switch (ast.value()) {
            case null -> Type.NIL;
            case Boolean _ -> Type.BOOLEAN;
            case BigInteger _ -> Type.INTEGER;
            case BigDecimal _ -> Type.DECIMAL;
            case Character _ -> Type.CHARACTER;
            case String _ -> Type.STRING;
            default -> throw new AssertionError(ast.value().getClass());
        };
    }

    @Override
    public Type visit(Ast.Expr.Group ast) throws AnalyzeException {
        return visit(ast.expression());
    }

    @Override
    public Type visit(Ast.Expr.Binary ast) throws AnalyzeException {
        Type left = visit(ast.left());
        Type right = visit(ast.right());
        return switch (ast.operator()) {
            case "+" -> {
                if (left.equals(Type.DYNAMIC) && right.equals(Type.DYNAMIC)) {
                    yield Type.DYNAMIC;
                }
                if (left.equals(Type.DYNAMIC) && right.equals(Type.INTEGER)) {
                    yield Type.INTEGER;
                }
                if (right.equals(Type.DYNAMIC) && left.equals(Type.INTEGER)) {
                    yield Type.INTEGER;
                }
                if (left.equals(Type.DYNAMIC) && right.equals(Type.DECIMAL)) {
                    yield Type.DECIMAL;
                }
                if (right.equals(Type.DYNAMIC) && left.equals(Type.DECIMAL)) {
                    yield Type.DECIMAL;
                }
                if (left.equals(Type.STRING) || right.equals(Type.STRING)) {
                    yield Type.STRING;
                }
                if (left.equals(Type.INTEGER) && right.equals(Type.INTEGER)) {
                    yield Type.INTEGER;
                }
                if (left.equals(Type.DECIMAL) && right.equals(Type.DECIMAL)) {
                    yield Type.DECIMAL;
                }
                throw new AnalyzeException("Invalid operands for +", ast);
            }
            case "-", "*", "/" -> {
                if (left.equals(Type.DYNAMIC) && right.equals(Type.DYNAMIC)) {
                    yield Type.DYNAMIC;
                }
                if (left.equals(Type.DYNAMIC) && right.equals(Type.INTEGER)) {
                    yield Type.INTEGER;
                }
                if (right.equals(Type.DYNAMIC) && left.equals(Type.INTEGER)) {
                    yield Type.INTEGER;
                }
                if (left.equals(Type.DYNAMIC) && right.equals(Type.DECIMAL)) {
                    yield Type.DECIMAL;
                }
                if (right.equals(Type.DYNAMIC) && left.equals(Type.DECIMAL)) {
                    yield Type.DECIMAL;
                }
                if (left.equals(Type.INTEGER) && right.equals(Type.INTEGER)) {
                    yield Type.INTEGER;
                }
                if (left.equals(Type.DECIMAL) && right.equals(Type.DECIMAL)) {
                    yield Type.DECIMAL;
                }
                throw new AnalyzeException("Invalid operands for " + ast.operator(), ast);
            }
            case "<", ">", "<=", ">=" -> {
                if (!left.isSubtypeOf(Type.COMPARABLE) || !right.isSubtypeOf(Type.COMPARABLE)) {
                    throw new AnalyzeException("Operands must be Comparable", ast);
                }
                if (!left.isSubtypeOf(right) && !right.isSubtypeOf(left)) {
                    throw new AnalyzeException("Comparable operands must be compatible", ast);
                }
                yield Type.BOOLEAN;
            }
            case "==", "!=" -> {
                if (!left.isSubtypeOf(right) && !right.isSubtypeOf(left)) {
                    throw new AnalyzeException("Operands must be compatible", ast);
                }
                yield Type.BOOLEAN;
            }
            case "AND", "OR" -> {
                if (!left.isSubtypeOf(Type.BOOLEAN) || !right.isSubtypeOf(Type.BOOLEAN)) {
                    throw new AnalyzeException("Operands must be Boolean", ast);
                }
                yield Type.BOOLEAN;
            }
            default -> throw new AnalyzeException("Unknown binary operator: " + ast.operator(), ast);
        };
    }

    @Override
    public Type visit(Ast.Expr.Variable ast) throws AnalyzeException {
        Optional<Type> typeOpt = context.scope().resolve(ast.name());
        if (typeOpt.isEmpty()) {
            throw new AnalyzeException("Undefined variable: " + ast.name(), ast);
        }
        if (context.uninitialized().contains(ast.name())) {
            throw new AnalyzeException("Variable " + ast.name() + " is uninitialized", ast);
        }
        return typeOpt.get();
    }

    @Override
    public Type visit(Ast.Expr.Property ast) throws AnalyzeException {
        Type receiverType = visit(ast.receiver());
        return resolveProperty(receiverType, ast.name(), ast);
    }

    @Override
    public Type visit(Ast.Expr.Function ast) throws AnalyzeException {
        Optional<Type> typeOpt = context.scope().resolve(ast.name());
        if (typeOpt.isEmpty()) {
            throw new AnalyzeException("Undefined function: " + ast.name(), ast);
        }
        Type type = typeOpt.get();
        if (!(type instanceof Type.Function funcType)) {
            throw new AnalyzeException("Variable " + ast.name() + " is not a function", ast);
        }
        checkArgs(funcType, ast.arguments(), ast);
        return funcType.returns();
    }

    @Override
    public Type visit(Ast.Expr.Method ast) throws AnalyzeException {
        Type receiverType = visit(ast.receiver());
        Type methodType = resolveProperty(receiverType, ast.name(), ast);
        if (methodType.equals(Type.DYNAMIC)) {
            for (var argument : ast.arguments()) {
                visit(argument);
            }
            return Type.DYNAMIC;
        }
        if (!(methodType instanceof Type.Function funcType)) {
            throw new AnalyzeException("Property '" + ast.name() + "' is not a method", ast);
        }
        checkArgs(funcType, ast.arguments(), ast);
        return funcType.returns();
    }

    @Override
    public Type visit(Ast.Expr.ObjectExpr ast) throws AnalyzeException {
        Type.ObjectType objectType = new Type.ObjectType(ast.name(), new Scope(null));
        for (var field : ast.fields()) {
            Type fieldType;
            if (field.type().isPresent()) {
                fieldType = resolveTypeName(field.type().get());
            }
            else if (field.value().isPresent()) {
                fieldType = visit(field.value().get());
            }
            else {
                fieldType = Type.DYNAMIC;
            }
            if (field.value().isPresent()) {
                Type valueType = visit(field.value().get());
                if (!valueType.isSubtypeOf(fieldType)) {
                    throw new AnalyzeException("Field value type " + valueType + " is not a subtype of field type " + fieldType, field);
                }
            }
            try {
                objectType.scope().declare(field.name(), fieldType);
            }
            catch (IllegalStateException e) {
                throw new AnalyzeException("Field already defined: " + field.name(), field);
            }
        }
        List<Type.Function> methodTypes = new ArrayList<>();
        for (var method : ast.methods()) {
            List<Type> paramTypes = new ArrayList<>();
            for (int i = 0; i < method.parameters().size(); i++) {
                Optional<String> typeNameOpt = method.parameterTypes().get(i);
                if (typeNameOpt.isPresent()) {
                    paramTypes.add(resolveTypeName(typeNameOpt.get()));
                } else {
                    paramTypes.add(Type.DYNAMIC);
                }
            }
            Type returnType = method.returnType().isPresent() ? resolveTypeName(method.returnType().get()) : Type.DYNAMIC;
            Type.Function methodFuncType = new Type.Function(paramTypes, returnType);
            methodTypes.add(methodFuncType);
            try {
                objectType.scope().declare(method.name(), methodFuncType);
            }
            catch (IllegalStateException e) {
                throw new AnalyzeException("Method already defined: " + method.name(), method);
            }
        }
        for (int methodIndex = 0; methodIndex < ast.methods().size(); methodIndex++) {
            var method = ast.methods().get(methodIndex);
            var methodFuncType = methodTypes.get(methodIndex);
            Context savedContext = context;
            Context methodContext = new Context(new Scope(savedContext.scope()), Optional.of(methodFuncType), new HashSet<>(savedContext.uninitialized()), false);
            context = methodContext;
            try {
                context.scope().declare("this", objectType);

                for (int i = 0; i < method.parameters().size(); i++) {
                    try {
                        context.scope().declare(method.parameters().get(i), methodFuncType.parameters().get(i));
                    }
                    catch (IllegalStateException e) {
                        throw new AnalyzeException("Duplicate parameter name: " + method.parameters().get(i), method);
                    }
                }
                analyzeStatements(method.body());
                if (!methodFuncType.returns().equals(Type.DYNAMIC)
                        && !methodFuncType.returns().equals(Type.NIL)
                        && !context.returns()) {
                    throw new AnalyzeException("Method '" + method.name() + "' does not always return value", method);
                }
            }
            finally {
                context = savedContext;
            }
        }
        return objectType;
    }

    private Type resolveTypeName(String name) throws AnalyzeException {
        Type t = Environment.TYPES.get(name);
        if (t != null) return t;
        Optional<Type> scopeType = context.scope().resolve(name);
        if (scopeType.isPresent()) return scopeType.get();
        throw new AnalyzeException("Unknown type: " + name);
    }

    private Type resolveProperty(Type receiverType, String name, Ast ast) throws AnalyzeException {
        if (receiverType.equals(Type.DYNAMIC)) {
            return Type.DYNAMIC;
        }
        if (receiverType instanceof Type.ObjectType objectType) {
            Optional<Type> prop = objectType.scope().get(name);
            if (prop.isPresent()) {
                return prop.get();
            }
            Optional<Type> protoOpt = objectType.scope().get("prototype");
            if (protoOpt.isPresent()) {
                return resolveProperty(protoOpt.get(), name, ast);
            }
            throw new AnalyzeException("Property " + name + " not found on object", ast);
        }
        throw new AnalyzeException("Cannot access property " + name + " on type " + receiverType, ast);
    }

    private void checkArgs(Type.Function funcType, List<Ast.Expr> arguments, Ast ast) throws AnalyzeException {
        if (funcType.parameters().size() != arguments.size()) {
            throw new AnalyzeException("Arity mismatch: expected " + funcType.parameters().size() + " arguments, got " + arguments.size(), ast);
        }
        for (int i = 0; i < arguments.size(); i++) {
            Type argType = visit(arguments.get(i));
            Type paramType = funcType.parameters().get(i);
            if (!argType.isSubtypeOf(paramType)) {
                throw new AnalyzeException("Argument " + i + " type " + argType + " is not a subtype of parameter type " + paramType, ast);
            }
        }
    }

    private void analyzeStatements(List<Ast.Stmt> statements) throws AnalyzeException {
        for (var stmt : statements) {
            if (context.returns()) {
                throw new AnalyzeException("Unreachable statement", stmt);
            }
            visit(stmt);
        }
    }

    public static final class ContextHooks {

        public static Set<String> mergeUninitialized(List<Set<String>> children) {
            Set<String> result = new HashSet<>();
            for (Set<String> child : children) {
                result.addAll(child);
            }
            return result;
        }

        public static boolean mergeReturns(List<Boolean> children) {
            if (children.isEmpty()) {
                return false;
            }

            for (Boolean child : children) {
                if (!child) {
                    return false;
                }
            }
            return true;
        }
    }

    public static final class EnvironmentHooks {

        public static final Type SQRT = new Type.Function(List.of(Type.DECIMAL), Type.DECIMAL);
        public static final Type RANGE = new Type.Function(List.of(Type.INTEGER, Type.INTEGER), Type.ITERABLE);

    }

    public static final class TypeHooks {

        public static boolean isSubtypeOf(Type subtype, Type supertype) {
            if (subtype.equals(supertype)) {
                return true;
            }
            if (supertype.equals(Type.ANY)) {
                return true;
            }
            if (subtype.equals(Type.DYNAMIC) || supertype.equals(Type.DYNAMIC)) {
                return true;
            }
            if (supertype.equals(Type.EQUATABLE)) {
                return subtype.equals(Type.NIL)
                        || subtype.equals(Type.BOOLEAN)
                        || subtype.equals(Type.INTEGER)
                        || subtype.equals(Type.DECIMAL)
                        || subtype.equals(Type.CHARACTER)
                        || subtype.equals(Type.STRING)
                        || subtype.equals(Type.COMPARABLE)
                        || subtype.equals(Type.ITERABLE)
                        || subtype.equals(Type.EQUATABLE);
            }
            if (supertype.equals(Type.COMPARABLE)) {
                return subtype.equals(Type.BOOLEAN)
                        || subtype.equals(Type.INTEGER)
                        || subtype.equals(Type.DECIMAL)
                        || subtype.equals(Type.CHARACTER)
                        || subtype.equals(Type.STRING)
                        || subtype.equals(Type.COMPARABLE);
            }
            if (subtype instanceof Type.ObjectType objectSubtype && supertype instanceof Type.ObjectType) {
                Optional<Type> protoOpt = objectSubtype.scope().get("prototype");
                if (protoOpt.isPresent()) {
                    return isSubtypeOf(protoOpt.get(), supertype);
                }
            }
            return false;
        }
    }

}
