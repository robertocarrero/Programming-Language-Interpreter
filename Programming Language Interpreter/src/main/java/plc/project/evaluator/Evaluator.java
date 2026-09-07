package plc.project.evaluator;

import plc.project.parser.Ast;

import java.math.MathContext;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

public final class Evaluator implements Ast.Visitor<RuntimeValue, EvaluateException> {

    private Scope scope;

    public Evaluator(Scope scope) {
        this.scope = scope;
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public RuntimeValue visit(Ast.Source ast) throws EvaluateException {
        RuntimeValue value = new RuntimeValue.Primitive(null);
        for (var stmt : ast.statements()) {
            try {
                value = visit(stmt);
            }
            catch (ReturnException e) {
                throw new EvaluateException("RETURN called outside of a function.");
            }
        }
        return value;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Let ast) throws EvaluateException {
        if (scope.get(ast.name()).isPresent()) {
            throw new EvaluateException("Variable is already declared!", ast);
        }
        var value = ast.value().isPresent() ? visit(ast.value().get()) : new RuntimeValue.Primitive(null);
        scope.define(ast.name(), value);
        return value;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Def ast) throws EvaluateException {
        if (scope.get(ast.name()).isPresent()) {
            throw new EvaluateException("Variable identifier is already declared in the current scope.", ast);
        }
        long uniqueCount = ast.parameters().stream().distinct().count();
        if (uniqueCount != ast.parameters().size()) {
            throw new EvaluateException("Duplicate parameter names in function '" + ast.name() + "'.", ast);
        }
        var definingScope = this.scope;
        var function = new RuntimeValue.Function(ast.name(), arguments -> {
            if (arguments.size() != ast.parameters().size()) {
                throw new EvaluateException("Expected " + ast.parameters().size() + " arguments but received " + arguments.size() + ".");
            }
            var savedScope = this.scope;
            this.scope = new Scope(definingScope);
            try {
                for (int i = 0; i < ast.parameters().size(); i++) {
                    this.scope.define(ast.parameters().get(i), arguments.get(i));
                }
                var paramScope = this.scope;
                this.scope = new Scope(paramScope);
                try {
                    for (var stmt : ast.body()) {
                        visit(stmt);
                    }
                    return new RuntimeValue.Primitive(null);
                }
                catch (ReturnException e) {
                    return e.getValue();
                }
                finally {
                    this.scope = paramScope;
                }
            }
            finally {
                this.scope = savedScope;
            }
        });
        scope.define(ast.name(), function);
        return function;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.If ast) throws EvaluateException {
        var conditionVal = visit(ast.condition());
        Boolean condition = requireType(conditionVal, Boolean.class)
                .orElseThrow(() -> new EvaluateException("IF condition must be a boolean.", ast.condition()));
        List<Ast.Stmt> body = condition ? ast.thenBody() : ast.elseBody();
        Scope saved = this.scope;
        this.scope = new Scope(saved);
        try {
            RuntimeValue result = new RuntimeValue.Primitive(null);
            for (var stmt : body) {
                result = visit(stmt);
            }
            return result;
        }
        finally {
            this.scope = saved;
        }
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.For ast) throws EvaluateException {
        var exprValue = visit(ast.expression());
        Iterable<?> iterable = requireType(exprValue, Iterable.class)
                .orElseThrow(() -> new EvaluateException("FOR expression must be a primitive Iterable.", ast.expression()));
        for (Object element : iterable) {
            if (!(element instanceof RuntimeValue elemValue)) {
                throw new EvaluateException("FOR iterable elements must be RuntimeValues.", ast.expression());
            }
            Scope savedScope = this.scope;
            this.scope = new Scope(savedScope);
            try {
                this.scope.define(ast.name(), elemValue);

                Scope iterScope = this.scope;
                this.scope = new Scope(iterScope);
                try {
                    for (var stmt : ast.body()) {
                        visit(stmt);
                    }
                }
                finally {
                    this.scope = iterScope;
                }
            }
            finally {
                this.scope = savedScope;
            }
        }
        return new RuntimeValue.Primitive(null);
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Return ast) throws EvaluateException {
        var value = ast.value().isPresent() ? visit(ast.value().get()) : new RuntimeValue.Primitive(null);
        throw new ReturnException(value);
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Expression ast) throws EvaluateException {
        return visit(ast.expression());
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Assignment ast) throws EvaluateException {
        if (ast.expression() instanceof Ast.Expr.Variable varExpr) {
            if (scope.resolve(varExpr.name()).isEmpty()) {
                throw new EvaluateException("Variable '" + varExpr.name() + "' is not defined.", varExpr);
            }
            var value = visit(ast.value());
            scope.assign(varExpr.name(), value);
            return value;
        }
        else if (ast.expression() instanceof Ast.Expr.Property propExpr) {
            var receiver = visit(propExpr.receiver());
            RuntimeValue.ObjectValue object = requireType(receiver, RuntimeValue.ObjectValue.class)
                    .orElseThrow(() -> new EvaluateException("Property receiver must be an object.", propExpr.receiver()));
            if (lookupProperty(object, propExpr.name()).isEmpty()) {
                throw new EvaluateException("Property '" + propExpr.name() + "' is not defined.", propExpr);
            }
            var value = visit(ast.value());
            if (!assignProperty(object, propExpr.name(), value)) {
                throw new EvaluateException("Property '" + propExpr.name() + "' is not defined.", propExpr);
            }
            return value;
        }
        else {
            throw new EvaluateException("Invalid assignment target.", ast.expression());
        }
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Literal ast) throws EvaluateException {
        return new RuntimeValue.Primitive(ast.value());
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Group ast) throws EvaluateException {
        return visit(ast.expression());
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Binary ast) throws EvaluateException {
        if (ast.operator().equals("AND")) {
            var left = visit(ast.left());
            Boolean leftBool = requireType(left, Boolean.class)
                    .orElseThrow(() -> new EvaluateException("AND left operand must be a Boolean.", ast.left()));
            if (!leftBool) {
                return new RuntimeValue.Primitive(false);
            }
            var right = visit(ast.right());
            Boolean rightBool = requireType(right, Boolean.class)
                    .orElseThrow(() -> new EvaluateException("AND right operand must be a Booolean.", ast.right()));
            return new RuntimeValue.Primitive(rightBool);
        }
        if (ast.operator().equals("OR")) {
            var left = visit(ast.left());
            Boolean leftBool = requireType(left, Boolean.class)
                    .orElseThrow(() -> new EvaluateException("OR left operand must be a Boolean.", ast.left()));
            if (leftBool) {
                return new RuntimeValue.Primitive(true);
            }
            var right = visit(ast.right());
            Boolean rightBool = requireType(right, Boolean.class)
                    .orElseThrow(() -> new EvaluateException("OR right operand must be a Boolean.", ast.right()));
            return new RuntimeValue.Primitive(rightBool);
        }
        return switch (ast.operator()) {
            case "+" -> {
                var left = visit(ast.left());
                var right = visit(ast.right());
                if (left instanceof RuntimeValue.Primitive(String leftStr)) {
                    yield new RuntimeValue.Primitive(leftStr + right.print());
                }
                else if (right instanceof RuntimeValue.Primitive(String rightStr)) {
                    yield new RuntimeValue.Primitive(left.print() + rightStr);
                }
                else if (left instanceof RuntimeValue.Primitive(BigInteger leftInt)) {
                    var rightInt = requireType(right, BigInteger.class)
                            .orElseThrow(() -> new EvaluateException("Invalid Op+ right (" + right + ").", ast.right()));
                    yield new RuntimeValue.Primitive(leftInt.add(rightInt));
                }
                else if (left instanceof RuntimeValue.Primitive(BigDecimal leftDec)) {
                    var rightDec = requireType(right, BigDecimal.class)
                            .orElseThrow(() -> new EvaluateException("Invalid Op+ right (" + right + ").", ast.right()));
                    yield new RuntimeValue.Primitive(leftDec.add(rightDec));
                }
                else if (left instanceof RuntimeValue.Function || left instanceof RuntimeValue.ObjectValue || right instanceof RuntimeValue.Function || right instanceof RuntimeValue.ObjectValue) {
                    yield new RuntimeValue.Primitive(left.print() + right.print());
                }
                else {
                    throw new EvaluateException("Invalid Op+ left (" + left + ").", ast.left());
                }
            }
            case "-" -> {
                var left = visit(ast.left());
                if (left instanceof RuntimeValue.Primitive(BigInteger leftInt)) {
                    var right = visit(ast.right());
                    var rightInt = requireType(right, BigInteger.class)
                            .orElseThrow(() -> new EvaluateException("Invalid Op- right (" + right + ").", ast.right()));
                    yield new RuntimeValue.Primitive(leftInt.subtract(rightInt));
                }
                else if (left instanceof RuntimeValue.Primitive(BigDecimal leftDec)) {
                    var right = visit(ast.right());
                    var rightDec = requireType(right, BigDecimal.class)
                            .orElseThrow(() -> new EvaluateException("Invalid Op- right (" + right + ").", ast.right()));
                    yield new RuntimeValue.Primitive(leftDec.subtract(rightDec));
                }
                else {
                    throw new EvaluateException ("Invalid Op- left (" + left + ").", ast.left());
                }
            }
            case "*" -> {
                var left = visit(ast.left());
                if (left instanceof RuntimeValue.Primitive(BigInteger leftInt)) {
                    var right = visit(ast.right());
                    var rightInt = requireType(right, BigInteger.class)
                            .orElseThrow(() -> new EvaluateException("Invalid Op* right (" + right + ").", ast.right()));
                    yield new RuntimeValue.Primitive(leftInt.multiply(rightInt));
                }
                else if (left instanceof RuntimeValue.Primitive(BigDecimal leftDec)) {
                    var right = visit(ast.right());
                    var rightDec = requireType(right, BigDecimal.class)
                            .orElseThrow(() -> new EvaluateException("Invalid Op* right (" + right + ").", ast.right()));
                    yield new RuntimeValue.Primitive(leftDec.multiply(rightDec));
                }
                else {
                    throw new EvaluateException("Invalid Op* left (" + left + ").", ast.left());
                }
            }
            case "/" -> {
                var left = visit(ast.left());
                if (left instanceof RuntimeValue.Primitive(BigInteger leftInt)) {
                    var right = visit(ast.right());
                    var rightInt = requireType(right, BigInteger.class)
                            .orElseThrow(() -> new EvaluateException("Invalid Op/ right (" + right + ").", ast.right()));
                    if (rightInt.equals(BigInteger.ZERO)) {
                        throw new EvaluateException("Division by zero.", ast.right());
                    }
                    yield new RuntimeValue.Primitive(leftInt.divide(rightInt));
                }
                else if (left instanceof RuntimeValue.Primitive(BigDecimal leftDec)) {
                    var right = visit(ast.right());
                    var rightDec = requireType(right, BigDecimal.class)
                            .orElseThrow(() -> new EvaluateException("Invalid Op/ right (" + right + ").", ast.right()));
                    if (rightDec.compareTo(BigDecimal.ZERO) == 0) {
                        throw new EvaluateException("Division by zero.", ast.right());
                    }
                    yield new RuntimeValue.Primitive(leftDec.divide(rightDec, RoundingMode.HALF_EVEN));
                }
                else {
                    throw new EvaluateException("Invalid Op/ left (" + left + ").", ast.left());
                }
            }
            case "==" -> {
                var left = visit(ast.left());
                var right = visit(ast.right());
                yield new RuntimeValue.Primitive(Objects.equals(requireType(left, RuntimeValue.Primitive.class).map(RuntimeValue.Primitive::value).orElse(left), requireType(right, RuntimeValue.Primitive.class).map(RuntimeValue.Primitive::value).orElse(right)));
            }
            case "!=" -> {
                var left = visit(ast.left());
                var right = visit(ast.right());
                yield new RuntimeValue.Primitive(!Objects.equals(requireType(left, RuntimeValue.Primitive.class).map(RuntimeValue.Primitive::value).orElse(left), requireType(right, RuntimeValue.Primitive.class).map(RuntimeValue.Primitive::value).orElse(right)));
            }
            case "<" -> {
                var left = visit(ast.left());
                var leftComparable = requireType(left, Comparable.class)
                        .orElseThrow(() -> new EvaluateException("Invalid < left (" + left + ").", ast.left()));
                var right = visit(ast.right());
                var rightValue = requireType(right, RuntimeValue.Primitive.class)
                        .orElseThrow(() -> new EvaluateException("Invalid < right (" + right + ").", ast.right()))
                        .value();
                try {
                    yield new RuntimeValue.Primitive(leftComparable.compareTo(rightValue) < 0);
                } catch (ClassCastException e) {
                    throw new EvaluateException("Invalid comparison types.", ast);
                }
            }
            case "<=" -> {
                var left = visit(ast.left());
                var leftComparable = requireType(left, Comparable.class)
                        .orElseThrow(() -> new EvaluateException("Invalid <= left (" + left + ").", ast.left()));
                var right = visit(ast.right());
                var rightValue = requireType(right, RuntimeValue.Primitive.class)
                        .orElseThrow(() -> new EvaluateException("Invalid <= right (" + right + ").", ast.right())).value();
                try {
                    yield new RuntimeValue.Primitive(leftComparable.compareTo(rightValue) <= 0);
                } catch (ClassCastException e) {
                    throw new EvaluateException("Invalid comparison types.", ast);
                }
            }
            case ">" -> {
                var left = visit(ast.left());
                var leftComparable = requireType(left, Comparable.class)
                        .orElseThrow(() -> new EvaluateException("Invalid > left (" + left + ").", ast.left()));
                var right = visit(ast.right());
                var rightValue = requireType(right, RuntimeValue.Primitive.class)
                        .orElseThrow(() -> new EvaluateException("Invalid > right (" + right + ").", ast.right()))
                        .value();
                try {
                    yield new RuntimeValue.Primitive(leftComparable.compareTo(rightValue) > 0);
                } catch (ClassCastException e) {
                    throw new EvaluateException("Invalid comparison types.", ast);
                }
            }
            case ">=" -> {
                var left = visit(ast.left());
                var leftComparable = requireType(left, Comparable.class)
                        .orElseThrow(() -> new EvaluateException("Invalid >= left (" + left + ").", ast.left()));
                var right = visit(ast.right());
                var rightValue = requireType(right, RuntimeValue.Primitive.class)
                        .orElseThrow(() -> new EvaluateException("Invalid >= right (" + right + ").", ast.right())).value();
                try {
                    yield new RuntimeValue.Primitive(leftComparable.compareTo(rightValue) >= 0);
                } catch (ClassCastException e) {
                    throw new EvaluateException("Invalid comparison types.", ast);
                }
            }
            default -> throw new EvaluateException("Unknown operator: " + ast.operator(), ast);
        };
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Variable ast) throws EvaluateException {
        var value = scope.resolve(ast.name())
                .orElseThrow(() -> new EvaluateException("Undefined variable " + ast.name(), ast));
        return value;
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Property ast) throws EvaluateException {
        var receiver = visit(ast.receiver());
        RuntimeValue.ObjectValue object = requireType(receiver, RuntimeValue.ObjectValue.class)
                .orElseThrow(() -> new EvaluateException("Property receiver must be an object.", ast.receiver()));
        return lookupProperty(object, ast.name())
                .orElseThrow(() -> new EvaluateException("Property '" + ast.name() + "' is not defined.", ast));
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Function ast) throws EvaluateException {
        var value = scope.resolve(ast.name())
                .orElseThrow(() -> new EvaluateException("Undefined function " + ast.name(), ast));
        var function = requireType(value, RuntimeValue.Function.class)
                .orElseThrow(() -> new EvaluateException("'" + ast.name() + "' is not a function.", ast));
        var arguments = new ArrayList<RuntimeValue>();
        for (var arg : ast.arguments()) {
            arguments.add(visit(arg));
        }
        return function.definition().invoke(arguments);
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Method ast) throws EvaluateException {
        var receiver = visit(ast.receiver());
        RuntimeValue.ObjectValue object = requireType(receiver, RuntimeValue.ObjectValue.class)
                .orElseThrow(() -> new EvaluateException("Method receiver must be an object.", ast.receiver()));
        var funcValue = lookupProperty(object, ast.name())
                .orElseThrow(() -> new EvaluateException("Method '" + ast.name() + "' is not defined.", ast));
        RuntimeValue.Function function = requireType(funcValue, RuntimeValue.Function.class)
                .orElseThrow(() -> new EvaluateException("'" + ast.name() + "' is not a function.", ast));
        List<RuntimeValue> arguments = new ArrayList<>();
        arguments.add(object);
        for (var arg : ast.arguments()) {
            arguments.add(visit(arg));
        }
        return function.definition().invoke(arguments);
    }

    @Override
    public RuntimeValue visit(Ast.Expr.ObjectExpr ast) throws EvaluateException {
        Scope objectScope = new Scope(null);
        RuntimeValue.ObjectValue object = new RuntimeValue.ObjectValue(ast.name(), objectScope);
        Scope savedScope = this.scope;
        for (var field : ast.fields()) {
            if (objectScope.get(field.name()).isPresent()) {
                throw new EvaluateException("Field '" + field.name() + "' is already defined.", field);
            }
            RuntimeValue value = field.value().isPresent() ? visit(field.value().get()) : new RuntimeValue.Primitive(null);
            objectScope.define(field.name(), value);
        }
        for (var method : ast.methods()) {
            if (objectScope.get(method.name()).isPresent()) {
                throw new EvaluateException("Method '" + method.name() + "' is already defined.", method);
            }
            if (method.parameters().contains("this")) {
                throw new EvaluateException("'this' cannot be a parameter name.", method);
            }
            long uniqueCount = method.parameters().stream().distinct().count();
            if (uniqueCount != method.parameters().size()) {
                throw new EvaluateException("Duplicate parameter names in method '" + method.name() + "'.", method);
            }
            Scope definingScope = savedScope;
            RuntimeValue.Function function = new RuntimeValue.Function(method.name(), arguments -> {
                if (arguments.size() - 1 != method.parameters().size()) {
                    throw new EvaluateException("Expected " + method.parameters().size() + " arguments but received " + (arguments.size() - 1) + ".");
                }
                Scope methodSavedScope = this.scope;
                this.scope = new Scope(definingScope);
                try {
                    this.scope.define("this", arguments.get(0));

                    for (int i = 0; i < method.parameters().size(); i++) {
                        this.scope.define(method.parameters().get(i), arguments.get(i + 1));
                    }
                    Scope bodyParentScope = this.scope;
                    this.scope = new Scope(bodyParentScope);
                    try {
                        for (var stmt : method.body()) {
                            visit(stmt);
                        }
                        return new RuntimeValue.Primitive(null);
                    }
                    catch (ReturnException e) {
                        return e.getValue();
                    }
                    finally {
                        this.scope = bodyParentScope;
                    }
                }
                finally {
                    this.scope = methodSavedScope;
                }
            });

            objectScope.define(method.name(), function);
        }
        return object;
    }

    private Optional<RuntimeValue> lookupProperty(RuntimeValue.ObjectValue object, String name) {
        Optional<RuntimeValue> value = object.scope().resolve(name);
        if (value.isPresent()) {
            return value;
        }
        Optional<RuntimeValue> prototypeValue = object.scope().resolve("prototype");
        if (prototypeValue.isPresent()) {
            Optional<RuntimeValue.ObjectValue> prototype = requireType(prototypeValue.get(), RuntimeValue.ObjectValue.class);
            if (prototype.isPresent()) {
                return lookupProperty(prototype.get(), name);
            }
        }
        return Optional.empty();
    }
    private boolean assignProperty(RuntimeValue.ObjectValue object, String name, RuntimeValue value) {
        if (object.scope().get(name).isPresent()) {
            object.scope().assign(name, value);
            return true;
        }
        Optional<RuntimeValue> prototypeValue = object.scope().get("prototype");
        if (prototypeValue.isPresent()) {
            Optional<RuntimeValue.ObjectValue> prototype = requireType(prototypeValue.get(), RuntimeValue.ObjectValue.class);
            if (prototype.isPresent()) {
                return assignProperty(prototype.get(), name, value);
            }
        }
        return false;
    }

    /**
     * Helper function for extracting RuntimeValues of specific types. If type
     * is a subclass of {@link RuntimeValue} the check applies to the value
     * itself, otherwise the value must be a {@link RuntimeValue.Primitive} and
     * the check applies to the primitive value.
     */
    private static <T> Optional<T> requireType(RuntimeValue value, Class<T> type) {
        //To be discussed in lecture
        Optional<Object> unwrapped = RuntimeValue.class.isAssignableFrom(type)
            ? Optional.of(value)
            : requireType(value, RuntimeValue.Primitive.class).map(RuntimeValue.Primitive::value);
        return (Optional<T>) unwrapped.filter(type::isInstance); //cast checked by isInstance
    }

    public static class Environment {

        public static RuntimeValue sqrt(List<RuntimeValue> arguments) throws EvaluateException {
            if (arguments.size() != 1) {
                throw new EvaluateException("sqrt() expects exactly 1 argument.");
            }
            RuntimeValue arg = arguments.getFirst();
            Optional<BigInteger> intVal = requireType(arg, BigInteger.class);
            if (intVal.isPresent()) {
                return new RuntimeValue.Primitive(intVal.get().sqrt());
            }
            Optional<BigDecimal> decVal = requireType(arg, BigDecimal.class);
            if (decVal.isPresent()) {
                return new RuntimeValue.Primitive(decVal.get().sqrt(MathContext.DECIMAL64));
            }
            throw new EvaluateException("sqrt() argument must be an integer or Decimal.");
        }

        public static RuntimeValue range(List<RuntimeValue> arguments) throws EvaluateException {
            if (arguments.size() != 2) {
                throw new EvaluateException("range() expects exactly 2 arguments.");
            }
            BigInteger start = requireType(arguments.get(0), BigInteger.class)
                    .orElseThrow(() -> new EvaluateException ("range() start must be an integer."));
            BigInteger end = requireType(arguments.get(1), BigInteger.class)
                    .orElseThrow(() -> new EvaluateException("range() end must be an integer."));
            if (start.compareTo(end) > 0) {
                throw new EvaluateException("range() requires start <= end.");
            }
            List<RuntimeValue> result = new ArrayList<>();
            for (BigInteger i = start; i.compareTo(end) < 0; i = i.add(BigInteger.ONE)) {
                result.add(new RuntimeValue.Primitive(i));
            }
            return new RuntimeValue.Primitive(result);
        }

        private static <T> Optional<T> requireType(RuntimeValue value, Class<T> type) {
            Optional<Object> unwrapped = RuntimeValue.class.isAssignableFrom(type)
                    ? Optional.of(value)
                    : requireType(value, RuntimeValue.Primitive.class).map(RuntimeValue.Primitive::value);
            return (Optional<T>) unwrapped.filter(type::isInstance);
        }
    }
}
