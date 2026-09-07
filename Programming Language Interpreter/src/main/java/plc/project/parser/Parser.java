package plc.project.parser;

import com.google.common.base.Preconditions;
import plc.project.lexer.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * This style of parser is called <em>recursive descent</em>. Each rule in our
 * grammar has dedicated function, and references to other rules correspond to
 * calling that function. Recursive rules are therefore supported by actual
 * recursive calls, while operator precedence is encoded via the grammar.
 *
 * <p>The parser has a similar architecture to the lexer, just with
 * {@link Token}s instead of characters. As before, {@link TokenStream#peek} and
 * {@link TokenStream#match} help with traversing the token stream. Instead of
 * emitting tokens, you will instead need to extract the literal value via
 * {@link TokenStream#get} to be added to the relevant AST.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    public Ast parse(String rule) throws ParseException {
        var ast = switch (rule) {
            case "source" -> parseSource();
            case "stmt" -> parseStmt();
            case "expr" -> parseExpr();
            default -> throw new AssertionError(rule);
        };
        if (tokens.has(0)) {
            throw new ParseException("Expected end of input.", tokens.getNext());
        }
        return ast;
    }

    private Ast.Source parseSource() throws ParseException {
        var statements = new ArrayList<Ast.Stmt>();
        while (tokens.has(0)) {
            statements.add(parseStmt());
        }
        return new Ast.Source(statements);
    }

    private Ast.Stmt parseStmt() throws ParseException {
        if (tokens.peek("LET")) {
            return parseLetStmt();
        }
        else if (tokens.peek("DEF")) {
            return parseDefStmt();
        }
        else if (tokens.peek("IF")) {
            return parseIfStmt();
        }
        else if (tokens.peek("FOR")) {
            return parseForStmt();
        }
        else if (tokens.peek("RETURN")) {
            return parseReturnStmt();
        }
        else {
            var expr = parseExpr();
            if (tokens.match("=")) {
                var value = parseExpr();
                if (!tokens.match(";")) {
                    throw new ParseException("Missing semicolon", tokens.getNext());
                }
                return new Ast.Stmt.Assignment(expr, value);
            }
            if (!tokens.match(";")) {
                throw new ParseException("Missing semicolon", tokens.getNext());
            }
            return new Ast.Stmt.Expression(expr);
        }
    }

    private Ast.Stmt parseLetStmt() throws ParseException {
        Preconditions.checkState(tokens.match("LET"));
        if (!tokens.match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Missing name", tokens.getNext());
        }
        var name = tokens.get(-1).literal();
        Optional<String> type = Optional.empty();
        if (tokens.match(":")) {
            if (!tokens.match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Missing type", tokens.getNext());
            }
            type = Optional.of(tokens.get(-1).literal());
        }
        Optional<Ast.Expr> value = Optional.empty();
        if (tokens.match("=")) {
            value = Optional.of(parseExpr());
        }
        if (!tokens.match(";")) {
            throw new ParseException("Missing semicolon", tokens.getNext());
        }
        return new Ast.Stmt.Let(name, type, value);
    }

    private Ast.Stmt parseDefStmt() throws ParseException {
        Preconditions.checkState(tokens.match("DEF"));
        if (!tokens.match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Missing name", tokens.getNext());
        }
        var name = tokens.get(-1).literal();
        if (!tokens.match("(")) {
            throw new ParseException("Missing open parenthesis", tokens.getNext());
        }
        var parameters = new ArrayList<String>();
        var parameterTypes = new ArrayList<Optional<String>>();
        if (!tokens.peek(")")) {
            do {
                if (!tokens.match(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Missing parameter name", tokens.getNext());
                }
                parameters.add(tokens.get(-1).literal());
                Optional<String> parameterType = Optional.empty();
                if (tokens.match(":")) {
                    if (!tokens.match(Token.Type.IDENTIFIER)) {
                        throw new ParseException("Missing parameter type", tokens.getNext());
                    }
                    parameterType = Optional.of(tokens.get(-1).literal());
                }
                parameterTypes.add(parameterType);
            } while (tokens.match(","));
        }
        if (!tokens.match(")")) {
            throw new ParseException("Missing closing parenthesis", tokens.getNext());
        }
        Optional<String> returnType = Optional.empty();
        if (tokens.match(":")) {
            if (!tokens.match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Missing return type", tokens.getNext());
            }
            returnType = Optional.of(tokens.get(-1).literal());
        }
        if (!tokens.match("DO")) {
            throw new ParseException("Missing DO", tokens.getNext());
        }
        var body = new ArrayList<Ast.Stmt>();
        while (!tokens.peek("END") && tokens.has(0)) {
            body.add(parseStmt());
        }
        if (!tokens.match("END")) {
            throw new ParseException("Missing END", tokens.getNext());
        }
        return new Ast.Stmt.Def(name, parameters, parameterTypes, returnType, body);
    }

    private Ast.Stmt parseIfStmt() throws ParseException {
        Preconditions.checkState(tokens.match("IF"));
        var condition = parseExpr();
        if (!tokens.match("DO")) {
            throw new ParseException("Missing DO", tokens.getNext());
        }
        var thenBody = new ArrayList<Ast.Stmt>();
        while (!tokens.peek("ELSE") && !tokens.peek("END") && tokens.has(0)) {
            thenBody.add(parseStmt());
        }
        var elseBody = new ArrayList<Ast.Stmt>();
        if (tokens.match("ELSE")) {
            while (!tokens.peek("END") && tokens.has(0)) {
                elseBody.add(parseStmt());
            }
        }
        if (!tokens.match("END")) {
            throw new ParseException("Missing END", tokens.getNext());
        }
        return new Ast.Stmt.If(condition, thenBody, elseBody);
    }

    private Ast.Stmt parseForStmt() throws ParseException {
        Preconditions.checkState(tokens.match("FOR"));
        if (!tokens.match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Missing name", tokens.getNext());
        }
        var name = tokens.get(-1).literal();
        if (!tokens.match("IN")) {
            throw new ParseException("Missing IN", tokens.getNext());
        }
        var expr = parseExpr();
        if (!tokens.match("DO")) {
            throw new ParseException("Missing DO", tokens.getNext());
        }
        var body = new ArrayList<Ast.Stmt>();
        while (!tokens.peek("END") && tokens.has(0)) {
            body.add(parseStmt());
        }
        if (!tokens.match("END")) {
            throw new ParseException("Missing END", tokens.getNext());
        }
        return new Ast.Stmt.For(name, expr, body);
    }

    private Ast.Stmt parseReturnStmt() throws ParseException {
        Preconditions.checkState(tokens.match("RETURN"));
        if (tokens.match("IF")) {
            var condition = parseExpr();
            Optional<Ast.Expr> value = Optional.empty();
            if (!tokens.peek(";")) {
                value = Optional.of(parseExpr());
            }

            if (!tokens.match(";")) {
                throw new ParseException("Missing semicolon", tokens.getNext());
            }

            return new Ast.Stmt.If(
                    condition,
                    List.of(new Ast.Stmt.Return(value)),
                    List.of()
            );
        }
        Optional<Ast.Expr> value = Optional.empty();
        if (!tokens.peek(";")) {
            value = Optional.of(parseExpr());
        }

        if (!tokens.match(";")) {
            throw new ParseException("Missing semicolon", tokens.getNext());
        }
        return new Ast.Stmt.Return(value);
    }

    private Ast.Stmt parseExpressionOrAssignmentStmt() throws ParseException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parseExpr() throws ParseException {
        return parseLogicalExpr();
    }

    private Ast.Expr parseLogicalExpr() throws ParseException {
        var expr = parseComparisonExpr();
        while (tokens.peek("AND") || tokens.peek("OR")) {
            tokens.match(Token.Type.IDENTIFIER);
            var operator = tokens.get(-1).literal();
            var right = parseComparisonExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseComparisonExpr() throws ParseException {
        var expr = parseAdditiveExpr();
        while (tokens.peek("<") || tokens.peek("<=") || tokens.peek(">") || tokens.peek(">=") || tokens.peek("==") || tokens.peek("!=")) {
            tokens.match(Token.Type.OPERATOR);
            var operator = tokens.get(-1).literal();
            var right = parseAdditiveExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseAdditiveExpr() throws ParseException {
        var expr = parseMultiplicativeExpr();
        while (tokens.peek("+") || tokens.peek("-")) {
            tokens.match(Token.Type.OPERATOR);
            var operator = tokens.get(-1).literal();
            var right = parseMultiplicativeExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseMultiplicativeExpr() throws ParseException {
        var expr = parseSecondaryExpr();
        while (tokens.peek("*") || tokens.peek("/")) {
            tokens.match(Token.Type.OPERATOR);
            var operator = tokens.get(-1).literal();
            var right = parseSecondaryExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseSecondaryExpr() throws ParseException {
        var expr = parsePrimaryExpr();
        while (tokens.peek(".")) {
            expr = parsePropertyOrMethod(expr);
        }
        return expr;
    }

    private Ast.Expr parsePropertyOrMethod(Ast.Expr receiver) throws ParseException {
        Preconditions.checkState(tokens.match("."));
        if (!tokens.peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier after '.'", tokens.getNext());
        }
        tokens.match(Token.Type.IDENTIFIER);
        var name = tokens.get(-1).literal();
        if (tokens.match("(")) {
            var args = new ArrayList<Ast.Expr>();
            if (!tokens.peek(")")) {
                args.add(parseExpr());
                while (tokens.match(",")) {
                    args.add(parseExpr());
                }
            }
            if (!tokens.match(")")) {
                throw new ParseException("Expected ')' after method arguments.", tokens.getNext());
            }
            return new Ast.Expr.Method(receiver, name, args);
        }
        return new Ast.Expr.Property(receiver, name);
    }

    private Ast.Expr parsePrimaryExpr() throws ParseException {
        if (tokens.peek("NIL") || tokens.peek("TRUE") || tokens.peek("FALSE") || tokens.peek(Token.Type.INTEGER) || tokens.peek(Token.Type.DECIMAL) || tokens.peek(Token.Type.CHARACTER) || tokens.peek(Token.Type.STRING)) {
            return parseLiteralExpr();
        }
        else if (tokens.peek("(")) {
            return parseGroupExpr();
        }
        else if (tokens.peek("OBJECT")) {
            return parseObjectExpr();
        }
        else if (tokens.peek(Token.Type.IDENTIFIER)) {
            return parseVariableOrFunctionExpr();
        }
        else {
            throw new ParseException("Expected a primary expression.", tokens.getNext());
        }
    }

    private Ast.Expr parseLiteralExpr() throws ParseException {
        if (tokens.match("NIL")) {
            return new Ast.Expr.Literal(null);
        }
        else if (tokens.match("TRUE")) {
            return new Ast.Expr.Literal(true);
        }
        else if (tokens.match("FALSE")) {
            return new Ast.Expr.Literal(false);
        }
        else if (tokens.match(Token.Type.INTEGER)) {
            var literal = tokens.get(-1).literal();
            if (literal.contains("e") || literal.contains("E")) {
                try {
                    return new Ast.Expr.Literal(new BigDecimal(literal).toBigIntegerExact());
                }
                catch (ArithmeticException e) {
                    throw new ParseException("Integer literal is a decimal", Optional.of(tokens.get(-1)));
                }
            }
            return new Ast.Expr.Literal(new BigInteger(literal));
        }
        else if (tokens.match(Token.Type.DECIMAL)) {
            var literal = tokens.get(-1).literal();
            return new Ast.Expr.Literal(new BigDecimal(literal));
        }
        else if (tokens.match(Token.Type.CHARACTER)) {
            var literal = tokens.get(-1).literal();
            var inside = literal.substring(1, literal.length() - 1);
            var translated = processSequence(inside);
            return new Ast.Expr.Literal(translated.charAt(0));
        }
        else if (tokens.match(Token.Type.STRING)) {
            var literal = tokens.get(-1).literal();
            var inside = literal.substring(1, literal.length() - 1);
            return new Ast.Expr.Literal(processSequence(inside));
        }
        else {
            throw new ParseException("Expected literal expression", tokens.getNext());
        }
    }

    private Ast.Expr parseGroupExpr() throws ParseException {
        Preconditions.checkState(tokens.match("("));
        if (tokens.peek(")")) {
            throw new ParseException("Expression should be inside group", tokens.getNext());
        }
        var expr = parseExpr();
        if (!tokens.match(")")) {
            throw new ParseException("Closing parenthesis missing", tokens.getNext());
        }
        return new Ast.Expr.Group(expr);
    }

    private Ast.Expr parseObjectExpr() throws ParseException {
        Preconditions.checkState(tokens.match("OBJECT"));
        Optional<String> name = Optional.empty();
        if (!tokens.peek("DO") && tokens.match(Token.Type.IDENTIFIER)) {
            name = Optional.of(tokens.get(-1).literal());
        }
        if (!tokens.match("DO")) {
            throw new ParseException("Missing DO", tokens.getNext());
        }
        var fields = new ArrayList<Ast.Stmt.Let>();
        while (tokens.peek("LET")) {
            fields.add((Ast.Stmt.Let) parseLetStmt());
        }
        var methods = new ArrayList<Ast.Stmt.Def>();
        while (tokens.peek("DEF")) {
            methods.add((Ast.Stmt.Def) parseDefStmt());
        }
        if (!tokens.match("END")) {
            throw new ParseException("Missing END", tokens.getNext());
        }
        return new Ast.Expr.ObjectExpr(name, fields, methods);
    }

    private Ast.Expr parseVariableOrFunctionExpr() throws ParseException {
        Preconditions.checkState(tokens.match(Token.Type.IDENTIFIER));
        var name = tokens.get(-1).literal();
        if (tokens.match("(")) {
            var args = new ArrayList<Ast.Expr>();
            if (!tokens.peek(")")) {
                args.add(parseExpr());
                while (tokens.match(",")) {
                    args.add(parseExpr());
                }
            }
            if (!tokens.match(")")) {
                throw new ParseException("Expected closing parenthesis", tokens.getNext());
            }
            return new Ast.Expr.Function(name, args);
        }
        return new Ast.Expr.Variable(name);
    }

    private String processSequence(String s) throws ParseException {
        try {
            return s.translateEscapes();
        }
        catch (IllegalArgumentException e) {
            throw new ParseException("Invalid escape sequence", tokens.getNext());
        }
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at (index + offset).
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Returns the token at (index + offset).
         */
        public Token get(int offset) {
            Preconditions.checkState(has(offset));
            return tokens.get(index + offset);
        }

        /**
         * Returns the next token, if present.
         */
        public Optional<Token> getNext() {
            return index < tokens.size() ? Optional.of(tokens.get(index)) : Optional.empty();
        }

        /**
         * Returns true if the next characters match their corresponding
         * pattern. Each pattern is either a {@link Token.Type}, matching tokens
         * of that type, or a {@link String}, matching tokens with that literal.
         * In effect, {@code new Token(Token.Type.IDENTIFIER, "literal")} is
         * matched by both {@code peek(Token.Type.IDENTIFIER)} and
         * {@code peek("literal")}.
         */
        public boolean peek(Object... patterns) {
            if (!has(patterns.length - 1)) {
                return false;
            }
            for (int offset = 0; offset < patterns.length; offset++) {
                var token = tokens.get(index + offset);
                var pattern = patterns[offset];
                Preconditions.checkState(pattern instanceof Token.Type || pattern instanceof String, pattern);
                if (!token.type().equals(pattern) && !token.literal().equals(pattern)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Equivalent to peek, but also advances the token stream.
         */
        public boolean match(Object... patterns) {
            var peek = peek(patterns);
            if (peek) {
                index += patterns.length;
            }
            return peek;
        }

    }

}
