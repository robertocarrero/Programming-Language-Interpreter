package plc.project.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import plc.project.lexer.Lexer;
import plc.project.lexer.Token;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

final class ParserTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, Object input, Object expected) {
        test("source", input, expected);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
            Arguments.of("Single",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "stmt"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Source(List.of(
                    new Ast.Stmt.Expression(new Ast.Expr.Variable("stmt"))
                ))
            ),
            Arguments.of("Multiple",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "first"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "second"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "third"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Source(List.of(
                    new Ast.Stmt.Expression(new Ast.Expr.Variable("first")),
                    new Ast.Stmt.Expression(new Ast.Expr.Variable("second")),
                    new Ast.Stmt.Expression(new Ast.Expr.Variable("third"))
                ))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLetStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testLetStmt() {
        return Stream.of(
            Arguments.of("Declaration",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "name"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("name", Optional.empty())
            ),
            Arguments.of("Initialization",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "name"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "expr"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("name", Optional.of(new Ast.Expr.Variable("expr")))
            ),
            Arguments.of("Missing Semicolon",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "name"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "expr")
                ),
                new ParseException("", Optional.empty())
            ),
            //Written by me:
            Arguments.of("Integer Initialization",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.INTEGER, "42"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("x", Optional.of(new Ast.Expr.Literal(new BigInteger("42"))))
            ),
            Arguments.of("Binary Expression Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "result"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("result", Optional.of(
                    new Ast.Expr.Binary("+", new Ast.Expr.Variable("a"), new Ast.Expr.Variable("b"))
                ))
            ),
            Arguments.of("String Initialization",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "msg"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.STRING, "\"hello\""),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("msg", Optional.of(new Ast.Expr.Literal("hello")))
            ),
            Arguments.of("Nil Initialization",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "NIL"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("x", Optional.of(new Ast.Expr.Literal(null)))
            ),

            Arguments.of("Function Call Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "val"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("val", Optional.of(new Ast.Expr.Function("f", List.of())))
            ),

            Arguments.of("Missing Name",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),

            Arguments.of("Missing Value After Equals",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),

            Arguments.of("Only LET Keyword",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET")
                ),
                new ParseException("", Optional.empty())
            ),

            Arguments.of("Declaration Missing Semicolon",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "name")
                ),
                new ParseException("", Optional.empty())
            ),

            Arguments.of("Operator As Name",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, "+")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDefStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testDefStmt() {
        return Stream.of(
            Arguments.of("Def",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "DEF"),
                    new Token(Token.Type.IDENTIFIER, "name"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.Def("name", List.of(), List.of())
            ),
            Arguments.of("Parameter",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "DEF"),
                    new Token(Token.Type.IDENTIFIER, "name"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "parameter"),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.Def("name", List.of("parameter"), List.of())
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIfStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testIfStmt() {
        return Stream.of(
            Arguments.of("If",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "IF"),
                    new Token(Token.Type.IDENTIFIER, "cond"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "then"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.If(
                    new Ast.Expr.Variable("cond"),
                    List.of(new Ast.Stmt.Expression(new Ast.Expr.Variable("then"))),
                    List.of()
                )
            ),
            Arguments.of("Else",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "IF"),
                    new Token(Token.Type.IDENTIFIER, "cond"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "then"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "ELSE"),
                    new Token(Token.Type.IDENTIFIER, "else"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.If(
                    new Ast.Expr.Variable("cond"),
                    List.of(new Ast.Stmt.Expression(new Ast.Expr.Variable("then"))),
                    List.of(new Ast.Stmt.Expression(new Ast.Expr.Variable("else")))
                )
            ),
            //written by me:
            Arguments.of("Integer Initialization",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.INTEGER, "42"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("x", Optional.of(new Ast.Expr.Literal(new BigInteger("42"))))
            ),
            Arguments.of("Binary Expression Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "result"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("result", Optional.of(
                    new Ast.Expr.Binary("+", new Ast.Expr.Variable("a"), new Ast.Expr.Variable("b"))
                ))
            ),
            Arguments.of("String Initialization",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "msg"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.STRING, "\"hello\""),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("msg", Optional.of(new Ast.Expr.Literal("hello")))
            ),
            Arguments.of("Nil Initialization",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "NIL"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("x", Optional.of(new Ast.Expr.Literal(null)))
            ),
            Arguments.of("Function Call Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "val"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Let("val", Optional.of(new Ast.Expr.Function("f", List.of())))
            ),
            Arguments.of("Missing Name",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),

            Arguments.of("Missing Value After Equals",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),
            Arguments.of("Only LET Keyword",
                List.of(
                        new Token(Token.Type.IDENTIFIER, "LET")
                ),
                new ParseException("", Optional.empty())
            ),

            Arguments.of("Declaration Missing Semicolon",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "name")
                ),
                new ParseException("", Optional.empty())
            ),

            Arguments.of("Operator As Name",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, "+")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testForStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testForStmt() {
        return Stream.of(
            Arguments.of("For",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.IDENTIFIER, "name"),
                    new Token(Token.Type.IDENTIFIER, "IN"),
                    new Token(Token.Type.IDENTIFIER, "expr"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "stmt"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.For(
                    "name",
                    new Ast.Expr.Variable("expr"),
                    List.of(new Ast.Stmt.Expression(new Ast.Expr.Variable("stmt")))
                )
            ),
            Arguments.of("Missing In",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.IDENTIFIER, "name"),
                    new Token(Token.Type.IDENTIFIER, "expr"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "stmt"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.IDENTIFIER, "expr")))
            ),
            //Written by me:
            Arguments.of("Empty Body",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.IDENTIFIER, "IN"),
                    new Token(Token.Type.IDENTIFIER, "list"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.For("x", new Ast.Expr.Variable("list"), List.of())
            ),

            Arguments.of("Multiple Body Statements",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.IDENTIFIER, "i"),
                    new Token(Token.Type.IDENTIFIER, "IN"),
                    new Token(Token.Type.IDENTIFIER, "items"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.For("i", new Ast.Expr.Variable("items"), List.of(
                    new Ast.Stmt.Expression(new Ast.Expr.Variable("a")),
                    new Ast.Stmt.Expression(new Ast.Expr.Variable("b"))
                ))
            ),

            Arguments.of("Function Call Expression",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.IDENTIFIER, "IN"),
                    new Token(Token.Type.IDENTIFIER, "getList"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "stmt"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.For("x", new Ast.Expr.Function("getList", List.of()), List.of(
                    new Ast.Stmt.Expression(new Ast.Expr.Variable("stmt"))
                ))
            ),
            Arguments.of("Let In Body",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.IDENTIFIER, "i"),
                    new Token(Token.Type.IDENTIFIER, "IN"),
                    new Token(Token.Type.IDENTIFIER, "col"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "i"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.For("i", new Ast.Expr.Variable("col"), List.of(
                    new Ast.Stmt.Let("x", Optional.of(new Ast.Expr.Variable("i")))
                ))
            ),
            Arguments.of("Property As Expression",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.IDENTIFIER, "item"),
                    new Token(Token.Type.IDENTIFIER, "IN"),
                    new Token(Token.Type.IDENTIFIER, "obj"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "list"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "stmt"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.For("item",
                    new Ast.Expr.Property(new Ast.Expr.Variable("obj"), "list"),
                    List.of(new Ast.Stmt.Expression(new Ast.Expr.Variable("stmt")))
                )
            ),
            Arguments.of("Missing DO",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.IDENTIFIER, "IN"),
                    new Token(Token.Type.IDENTIFIER, "list"),
                    new Token(Token.Type.IDENTIFIER, "stmt"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.IDENTIFIER, "stmt")))
            ),
            Arguments.of("Missing END",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.IDENTIFIER, "IN"),
                    new Token(Token.Type.IDENTIFIER, "list"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "stmt"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.empty())
            ),

            Arguments.of("Only FOR Keyword",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR")
                ),
                new ParseException("", Optional.empty())
            ),

            Arguments.of("Operator As Iterator",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "IN"),
                    new Token(Token.Type.IDENTIFIER, "list"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, "+")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testReturnStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testReturnStmt() {
        return Stream.of(
            Arguments.of("Return Expr",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN"),
                    new Token(Token.Type.IDENTIFIER, "expr"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Return(Optional.of(new Ast.Expr.Variable("expr")))
            ),
            Arguments.of("Return If",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN"),
                    new Token(Token.Type.IDENTIFIER, "IF"),
                    new Token(Token.Type.IDENTIFIER, "cond"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.If(
                    new Ast.Expr.Variable("cond"),
                    List.of(new Ast.Stmt.Return(Optional.empty())),
                    List.of()
                )
            ),
            //written by me:
            Arguments.of("Return Empty",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Return(Optional.empty())
            ),
            Arguments.of("Return Integer",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN"),
                    new Token(Token.Type.INTEGER, "42"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Return(Optional.of(new Ast.Expr.Literal(new BigInteger("42"))))
            ),
            Arguments.of("Return Binary",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN"),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Return(Optional.of(
                    new Ast.Expr.Binary("+", new Ast.Expr.Variable("a"), new Ast.Expr.Variable("b"))
                ))
            ),

            Arguments.of("Return Function Call",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN"),
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Return(Optional.of(new Ast.Expr.Function("f", List.of())))
            ),

            Arguments.of("Return If Comparison",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN"),
                    new Token(Token.Type.IDENTIFIER, "IF"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, ">"),
                    new Token(Token.Type.INTEGER, "0"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.If(
                    new Ast.Expr.Binary(">", new Ast.Expr.Variable("x"), new Ast.Expr.Literal(new BigInteger("0"))),
                    List.of(new Ast.Stmt.Return(Optional.empty())),
                    List.of()
                )
            ),

            Arguments.of("Missing Semicolon",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN"),
                    new Token(Token.Type.IDENTIFIER, "expr")
                ),
                new ParseException("", Optional.empty())
            ),

            Arguments.of("Return If Missing Condition",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN"),
                    new Token(Token.Type.IDENTIFIER, "IF"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),
            Arguments.of("Return If Missing Semicolon",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN"),
                    new Token(Token.Type.IDENTIFIER, "IF"),
                    new Token(Token.Type.IDENTIFIER, "cond")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Operator As Return Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, "+")))
            ),
            Arguments.of("Only RETURN Keyword",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "RETURN")
                ),
                new ParseException("", Optional.empty())
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExpressionStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testExpressionStmt() {
        return Stream.of(
            Arguments.of("Variable",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "variable"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Expression(new Ast.Expr.Variable("variable"))
            ),
            Arguments.of("Function",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "function"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Expression(new Ast.Expr.Function("function", List.of()))
            ),
            //Written by me:
            Arguments.of("Method Call",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "obj"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "method"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Expression(
                    new Ast.Expr.Method(new Ast.Expr.Variable("obj"), "method", List.of())
                )
            ),
            Arguments.of("Binary Expression",
                    List.of(
                        new Token(Token.Type.IDENTIFIER, "a"),
                        new Token(Token.Type.OPERATOR, "+"),
                        new Token(Token.Type.IDENTIFIER, "b"),
                        new Token(Token.Type.OPERATOR, ";")
                    ),
                    new Ast.Stmt.Expression(
                        new Ast.Expr.Binary("+", new Ast.Expr.Variable("a"), new Ast.Expr.Variable("b"))
                    )
            ),
            Arguments.of("Function With Argument",
                    List.of(
                        new Token(Token.Type.IDENTIFIER, "print"),
                        new Token(Token.Type.OPERATOR, "("),
                        new Token(Token.Type.IDENTIFIER, "x"),
                        new Token(Token.Type.OPERATOR, ")"),
                        new Token(Token.Type.OPERATOR, ";")
                    ),
                    new Ast.Stmt.Expression(
                        new Ast.Expr.Function("print", List.of(new Ast.Expr.Variable("x")))
                    )
            ),
            Arguments.of("Literal Expression",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "NIL"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Expression(new Ast.Expr.Literal(null))
            ),
            Arguments.of("Property Access",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "obj"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "field"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Expression(
                    new Ast.Expr.Property(new Ast.Expr.Variable("obj"), "field")
                )
            ),
            Arguments.of("Missing Semicolon",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "variable")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Leading Operator",
                List.of(
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, "+")))
            ),
            Arguments.of("Function Missing Paren And Semicolon",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "x")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Only Semicolon",
                List.of(
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),
            Arguments.of("Double Semicolon",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "variable"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testAssignmentStmt() {
        return Stream.of(
            Arguments.of("Variable",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "variable"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "value"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Assignment(
                    new Ast.Expr.Variable("variable"),
                    new Ast.Expr.Variable("value")
                )
            ),
            Arguments.of("Missing Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "object"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "property"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),
            //Written by me:
            Arguments.of("Property Assignment",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "obj"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "field"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "value"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Assignment(
                    new Ast.Expr.Property(new Ast.Expr.Variable("obj"), "field"),
                    new Ast.Expr.Variable("value")
                )
            ),
            Arguments.of("Integer Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.INTEGER, "0"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Assignment(
                    new Ast.Expr.Variable("x"),
                    new Ast.Expr.Literal(new BigInteger("0"))
                )
            ),
            Arguments.of("Binary Expression Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Assignment(
                    new Ast.Expr.Variable("x"),
                    new Ast.Expr.Binary("+", new Ast.Expr.Variable("a"), new Ast.Expr.Variable("b"))
                )
            ),
            Arguments.of("Function Call Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Assignment(
                    new Ast.Expr.Variable("x"),
                    new Ast.Expr.Function("f", List.of())
                )
            ),
            Arguments.of("Nil Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "NIL"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Stmt.Assignment(
                    new Ast.Expr.Variable("x"),
                    new Ast.Expr.Literal(null)
                )
            ),

            Arguments.of("Missing Semicolon",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.IDENTIFIER, "value")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Missing Value And Semicolon",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "=")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Operator As Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, "+")))
            ),
            Arguments.of("Property Missing Value",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "obj"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "field"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),
            Arguments.of("Only Equals",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "=")
                ),
                new ParseException("", Optional.empty())
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testLiteralExpr() {
        return Stream.of(
            Arguments.of("Nil",
                List.of(new Token(Token.Type.IDENTIFIER, "NIL")),
                new Ast.Expr.Literal(null)
            ),
            Arguments.of("Boolean",
                List.of(new Token(Token.Type.IDENTIFIER, "TRUE")),
                new Ast.Expr.Literal(true)
            ),
            Arguments.of("Integer",
                List.of(new Token(Token.Type.INTEGER, "1")),
                new Ast.Expr.Literal(new BigInteger("1"))
            ),
            Arguments.of("Decimal",
                List.of(new Token(Token.Type.DECIMAL, "1.0")),
                new Ast.Expr.Literal(new BigDecimal("1.0"))
            ),
            Arguments.of("Character",
                List.of(new Token(Token.Type.CHARACTER, "\'c\'")),
                new Ast.Expr.Literal('c')
            ),
            Arguments.of("String",
                List.of(new Token(Token.Type.STRING, "\"string\"")),
                new Ast.Expr.Literal("string")
            ),
            Arguments.of("String Newline Escape",
                List.of(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"")),
                new Ast.Expr.Literal("Hello,\nWorld!")
            ),
            //Written by me:
            Arguments.of("False Literal",
                List.of(new Token(Token.Type.IDENTIFIER, "FALSE")),
                new Ast.Expr.Literal(false)
            ),
            Arguments.of("Large Integer",
                List.of(new Token(Token.Type.INTEGER, "100000000000")),
                new Ast.Expr.Literal(new BigInteger("100000000000"))
            ),
            Arguments.of("Decimal With Exponent",
                List.of(new Token(Token.Type.DECIMAL, "1.5e10")),
                new Ast.Expr.Literal(new BigDecimal("1.5e10"))
            ),
            Arguments.of("Character Tab Escape",
                List.of(new Token(Token.Type.CHARACTER, "'\\t'")),
                new Ast.Expr.Literal('\t')
            ),

            Arguments.of("String Escaped Quote",
                List.of(new Token(Token.Type.STRING, "\"say \\\"hi\\\"\"")),
                new Ast.Expr.Literal("say \"hi\"")
            ),

            Arguments.of("Operator Is Not Literal",
                List.of(new Token(Token.Type.OPERATOR, "+")),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, "+")))
            ),
            Arguments.of("Empty Literal",
                List.of(),
                new ParseException("", Optional.empty())
            ),

            Arguments.of("Integer Then Extra Token",
                List.of(
                    new Token(Token.Type.INTEGER, "1"),
                    new Token(Token.Type.INTEGER, "2")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.INTEGER, "2")))
            ),

            Arguments.of("Decimal Then Extra Token",
                List.of(
                    new Token(Token.Type.DECIMAL, "3.14"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),
            Arguments.of("Decimal With Exponent",
                List.of(new Token(Token.Type.DECIMAL, "1.5e10")),
                new Ast.Expr.Literal(new BigDecimal("1.5e10"))
            ),
            Arguments.of("Integer Exponent",
                List.of(new Token(Token.Type.INTEGER, "1e9")),
                new Ast.Expr.Literal(new BigInteger("1000000000"))
            ),
            Arguments.of("Integer Large Exponent",
                List.of(new Token(Token.Type.INTEGER, "2e3")),
                new Ast.Expr.Literal(new BigInteger("2000"))
            ),
            Arguments.of("Integer Uppercase Exponent",
                List.of(new Token(Token.Type.INTEGER, "5E2")),
                new Ast.Expr.Literal(new BigInteger("500"))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testGroupExpr() {
        return Stream.of(
            Arguments.of("Group",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "expr"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Group(new Ast.Expr.Variable("expr"))
            ),
            Arguments.of("Missing Expression",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ")")))
            ),
            // Written by me:
            Arguments.of("Grouped Integer",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.INTEGER, "42"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Group(new Ast.Expr.Literal(new BigInteger("42")))
            ),
            Arguments.of("Nested Groups",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Group(new Ast.Expr.Group(new Ast.Expr.Variable("x")))
            ),

            Arguments.of("Grouped Binary",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Group(
                    new Ast.Expr.Binary("+", new Ast.Expr.Variable("a"), new Ast.Expr.Variable("b"))
                )
            ),

            Arguments.of("Grouped Nil",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "NIL"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Group(new Ast.Expr.Literal(null))
            ),

            Arguments.of("Group As Left Operand",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, "*"),
                    new Token(Token.Type.IDENTIFIER, "c")
                ),
                new Ast.Expr.Binary(
                    "*",
                    new Ast.Expr.Group(
                        new Ast.Expr.Binary("+", new Ast.Expr.Variable("a"), new Ast.Expr.Variable("b"))
                    ),
                    new Ast.Expr.Variable("c")
                )
            ),
            Arguments.of("Missing Closing Parenthesis",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "expr")
                ),
                new ParseException("", Optional.empty())
            ),

            Arguments.of("Only Open Paren",
                List.of(
                    new Token(Token.Type.OPERATOR, "(")
                ),
                new ParseException("", Optional.empty())
            ),

            Arguments.of("Group Then Dangling Plus",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, "+")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Unbalanced Nested Parens",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Operator Inside Group",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, "*"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, "*")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testBinaryExpr() {
        return Stream.of(
            Arguments.of("Addition",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "left"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "right")
                ),
                new Ast.Expr.Binary(
                    "+",
                    new Ast.Expr.Variable("left"),
                    new Ast.Expr.Variable("right")
                )
            ),
            Arguments.of("Multiplication",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "left"),
                    new Token(Token.Type.OPERATOR, "*"),
                    new Token(Token.Type.IDENTIFIER, "right")
                ),
                new Ast.Expr.Binary(
                    "*",
                    new Ast.Expr.Variable("left"),
                    new Ast.Expr.Variable("right")
                )
            ),
            Arguments.of("Equal Precedence",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "first"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "second"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "third")
                ),
                new Ast.Expr.Binary(
                    "+",
                    new Ast.Expr.Binary(
                        "+",
                        new Ast.Expr.Variable("first"),
                        new Ast.Expr.Variable("second")
                    ),
                    new Ast.Expr.Variable("third")
                )
            ),
            Arguments.of("Lower Precedence",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "first"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "second"),
                    new Token(Token.Type.OPERATOR, "*"),
                    new Token(Token.Type.IDENTIFIER, "third")
                ),
                new Ast.Expr.Binary(
                    "+",
                    new Ast.Expr.Variable("first"),
                    new Ast.Expr.Binary(
                        "*",
                        new Ast.Expr.Variable("second"),
                        new Ast.Expr.Variable("third")
                    )
                )
            ),
            Arguments.of("Higher Precedence",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "first"),
                    new Token(Token.Type.OPERATOR, "*"),
                    new Token(Token.Type.IDENTIFIER, "second"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "third")
                ),
                new Ast.Expr.Binary(
                    "+",
                    new Ast.Expr.Binary(
                        "*",
                        new Ast.Expr.Variable("first"),
                        new Ast.Expr.Variable("second")
                    ),
                    new Ast.Expr.Variable("third")
                )
            ),

            // Written by me:

            Arguments.of("Subtraction",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "-"),
                    new Token(Token.Type.IDENTIFIER, "b")
                ),
                new Ast.Expr.Binary(
                "-",
                    new Ast.Expr.Variable("a"),
                    new Ast.Expr.Variable("b")
                )
            ),

            Arguments.of("Division",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "/"),
                    new Token(Token.Type.IDENTIFIER, "b")
                ),
                new Ast.Expr.Binary(
                    "/",
                    new Ast.Expr.Variable("a"),
                    new Ast.Expr.Variable("b")
                )
            ),
            Arguments.of("Logical OR",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.IDENTIFIER, "OR"),
                    new Token(Token.Type.IDENTIFIER, "y")
                ),
                new Ast.Expr.Binary(
                "OR",
                    new Ast.Expr.Variable("x"),
                    new Ast.Expr.Variable("y")
                )
            ),
            Arguments.of("Not Equal",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "!="),
                    new Token(Token.Type.IDENTIFIER, "b")
                ),
                new Ast.Expr.Binary(
                    "!=",
                    new Ast.Expr.Variable("a"),
                    new Ast.Expr.Variable("b")
                )
            ),
            Arguments.of("Logical AND Of Comparisons",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "=="),
                    new Token(Token.Type.INTEGER, "1"),
                    new Token(Token.Type.IDENTIFIER, "AND"),
                    new Token(Token.Type.IDENTIFIER, "y"),
                    new Token(Token.Type.OPERATOR, "=="),
                    new Token(Token.Type.INTEGER, "2")
                ),
                new Ast.Expr.Binary(
                    "AND",
                    new Ast.Expr.Binary("==", new Ast.Expr.Variable("x"), new Ast.Expr.Literal(new BigInteger("1"))),
                    new Ast.Expr.Binary("==", new Ast.Expr.Variable("y"), new Ast.Expr.Literal(new BigInteger("2")))
                )
            ),
            Arguments.of("Missing Right Operand Plus",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "+")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Missing Right Operand Multiply",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "*")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Missing Right Operand AND",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.IDENTIFIER, "AND")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Missing Right Operand Less Than",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "<")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Leading Operator No Left Operand",
                List.of(
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "b")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, "+")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testVariableExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testVariableExpr() {
        return Stream.of(
            Arguments.of("Variable",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "variable")
                ),
                new Ast.Expr.Variable("variable")
            ),
            //Written by me:
            Arguments.of("Single Letter Variable",
                List.of(new Token(Token.Type.IDENTIFIER, "x")),
                new Ast.Expr.Variable("x")
            ),
            Arguments.of("Non-Keyword Identifier",
                List.of(new Token(Token.Type.IDENTIFIER, "nothing")),
                new Ast.Expr.Variable("nothing")
            ),
            Arguments.of("Variable In Binary Expression",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "counter"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.INTEGER, "1")
                ),
                new Ast.Expr.Binary("+",
                    new Ast.Expr.Variable("counter"),
                    new Ast.Expr.Literal(new BigInteger("1"))
                )
            ),
            Arguments.of("Underscore Variable",
                List.of(new Token(Token.Type.IDENTIFIER, "my_Var")),
                new Ast.Expr.Variable("my_Var")
            ),

            Arguments.of("Variable As Receiver",
                List.of(
                        new Token(Token.Type.IDENTIFIER, "obj"),
                        new Token(Token.Type.OPERATOR, "."),
                        new Token(Token.Type.IDENTIFIER, "field")
                ),
                new Ast.Expr.Property(new Ast.Expr.Variable("obj"), "field")
            ),

            Arguments.of("Empty Variable",
                List.of(),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Operator Not Variable",
                List.of(new Token(Token.Type.OPERATOR, "=")),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, "=")))
            ),
            Arguments.of("Two Consecutive Variables",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.IDENTIFIER, "b")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.IDENTIFIER, "b")))
            ),
            Arguments.of("Dot With No Receiver",
                List.of(new Token(Token.Type.OPERATOR, ".")),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ".")))
            ),
            Arguments.of("Variable Then Trailing Integer",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "myVar"),
                    new Token(Token.Type.INTEGER, "42")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.INTEGER, "42")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testPropertyExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testPropertyExpr() {
        return Stream.of(
            Arguments.of("Property",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "receiver"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "property")
                ),
                new Ast.Expr.Property(
                    new Ast.Expr.Variable("receiver"),
                    "property"
                )
            ),
            Arguments.of("Missing Name",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "receiver"),
                    new Token(Token.Type.OPERATOR, ".")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Chained Properties",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "c")
                    ),
                new Ast.Expr.Property(new Ast.Expr.Property(new Ast.Expr.Variable("a"), "b"), "c"
                )
            ),
            Arguments.of("Property On Group",
                List.of(
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "obj"),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "field")
                ),
                new Ast.Expr.Property(
                    new Ast.Expr.Group(new Ast.Expr.Variable("obj")),
                    "field"
                )
            ),
            Arguments.of("Property In Addition",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "obj"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.INTEGER, "1")
                ),
                new Ast.Expr.Binary(
                    "+",
                    new Ast.Expr.Property(new Ast.Expr.Variable("obj"), "x"),
                    new Ast.Expr.Literal(new BigInteger("1"))
                )
            ),
            Arguments.of("Triple Chained Property",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "c"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "d")
                ),
                new Ast.Expr.Property(
                    new Ast.Expr.Property(
                        new Ast.Expr.Property(new Ast.Expr.Variable("a"), "b"),
                        "c"
                    ),
                    "d"
                )
            ),
            Arguments.of("Property Named Like Keyword",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "obj"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "length")
                ),
                new Ast.Expr.Property(new Ast.Expr.Variable("obj"), "length")
            ),
            Arguments.of("Property Name Is Integer",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "receiver"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.INTEGER, "42")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.INTEGER, "42")))
            ),
            Arguments.of("Double Dot",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.OPERATOR, ".")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ".")))
            ),
            Arguments.of("Dot At Start",
                List.of(
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "field")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ".")))
            ),
            Arguments.of("Chained Property Missing Final Name",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, ".")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Property Name Is Operator",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "obj"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.OPERATOR, "+")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, "+")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testFunctionExpr() {
        return Stream.of(
            Arguments.of("Function",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "function"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Function("function", List.of())
            ),
            Arguments.of("Argument",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "function"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "argument"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Function("function", List.of(
                    new Ast.Expr.Variable("argument")
                ))
            ),
            Arguments.of("Two Arguments",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, ","),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, ")")
                    ),
                new Ast.Expr.Function("f", List.of(
                    new Ast.Expr.Variable("a"),
                    new Ast.Expr.Variable("b")
                    ))
            ),
            Arguments.of("Literal Argument",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "print"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.INTEGER, "42"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Function("print", List.of(
                    new Ast.Expr.Literal(new BigInteger("42"))
                ))
            ),
            Arguments.of("Nested Function Call",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "g"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Function("f", List.of(
                    new Ast.Expr.Function("g", List.of())
                ))
            ),
            Arguments.of("Function In Binary",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.INTEGER, "1")
                ),
                new Ast.Expr.Binary(
                    "+",
                    new Ast.Expr.Function("f", List.of()),
                    new Ast.Expr.Literal(new BigInteger("1"))
                )
            ),
            Arguments.of("Three Arguments",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "add"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, ","),
                    new Token(Token.Type.IDENTIFIER, "y"),
                    new Token(Token.Type.OPERATOR, ","),
                    new Token(Token.Type.IDENTIFIER, "z"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Function("add", List.of(
                    new Ast.Expr.Variable("x"),
                    new Ast.Expr.Variable("y"),
                    new Ast.Expr.Variable("z")
                ))
            ),
            Arguments.of("Missing Closing Paren",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "a")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Trailing Comma",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, ","),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ")")))
            ),
            Arguments.of("Only Open Paren No Args",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "(")
                ),
                new ParseException("", Optional.empty())
            ),
            Arguments.of("Leading Comma In Args",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ","),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ",")))
            ),
            Arguments.of("Function Name Only Then Plus",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "+")
                ),
                new ParseException("", Optional.empty())
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMethodExpr(String test, Object input, Ast.Expr.Method expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testMethodExpr() {
        return Stream.of(
            Arguments.of("Method",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "receiver"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "method"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Method(
                    new Ast.Expr.Variable("receiver"),
                    "method",
                    List.of()
                )
            ),
            Arguments.of("Arguments",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "receiver"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "method"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "first"),
                    new Token(Token.Type.OPERATOR, ","),
                    new Token(Token.Type.IDENTIFIER, "second"),
                    new Token(Token.Type.OPERATOR, ","),
                    new Token(Token.Type.IDENTIFIER, "third"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Method(
                    new Ast.Expr.Variable("receiver"),
                    "method",
                    List.of(
                        new Ast.Expr.Variable("first"),
                        new Ast.Expr.Variable("second"),
                        new Ast.Expr.Variable("third")
                    )
                )
            ),
            Arguments.of("Binary Arguments (1+2, 3*4)",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "receiver"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "method"),
                    new Token(Token.Type.OPERATOR, "("),

                    new Token(Token.Type.INTEGER, "1"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.INTEGER, "2"),

                    new Token(Token.Type.OPERATOR, ","),

                    new Token(Token.Type.INTEGER, "3"),
                    new Token(Token.Type.OPERATOR, "*"),
                    new Token(Token.Type.INTEGER, "4"),

                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Method(
                    new Ast.Expr.Variable("receiver"),
                    "method",
                List.of(
                    new Ast.Expr.Binary("+",
                            new Ast.Expr.Literal(new java.math.BigInteger("1")),
                            new Ast.Expr.Literal(new java.math.BigInteger("2"))
                    ),
                    new Ast.Expr.Binary("*",
                            new Ast.Expr.Literal(new java.math.BigInteger("3")),
                            new Ast.Expr.Literal(new java.math.BigInteger("4"))
                    )
                )
                )
            ),
            Arguments.of("Chained Receiver Property Then Method (receiver.prop.method())",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "receiver"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "prop"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "method"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Method(
                    new Ast.Expr.Property(
                            new Ast.Expr.Variable("receiver"),
                            "prop"
                    ),
                    "method",
                    List.of()
                )
            ),

            Arguments.of("Method Used As Receiver (receiver.first().second())",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "receiver"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "first"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),

                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "second"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Method(
                    new Ast.Expr.Method(
                            new Ast.Expr.Variable("receiver"),
                            "first",
                            List.of()
                    ),
                    "second",
                    List.of()
                )
            ),

            Arguments.of("Grouped Argument (receiver.method((first)))",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "receiver"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "method"),
                    new Token(Token.Type.OPERATOR, "("),

                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "first"),
                    new Token(Token.Type.OPERATOR, ")"),

                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Method(
                        new Ast.Expr.Variable("receiver"),
                        "method",
                        List.of(
                                new Ast.Expr.Group(new Ast.Expr.Variable("first"))
                        )
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testObjectExpr(String test, Object input, Ast.Expr.ObjectExpr expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testObjectExpr() {
        return Stream.of(
            Arguments.of("Field",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "OBJECT"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "field"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Expr.ObjectExpr(
                    Optional.empty(),
                    List.of(new Ast.Stmt.Let("field", Optional.empty())),
                    List.of()
                )
            ),
            Arguments.of("Method",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "OBJECT"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "DEF"),
                    new Token(Token.Type.IDENTIFIER, "method"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "END"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Expr.ObjectExpr(
                    Optional.empty(),
                    List.of(),
                    List.of(new Ast.Stmt.Def("method", List.of(), List.of()))
                )
            ),
            //written by me:
            Arguments.of("Named Object",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "OBJECT"),
                    new Token(Token.Type.IDENTIFIER, "MyObj"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Expr.ObjectExpr(Optional.of("MyObj"), List.of(), List.of())
            ),
            Arguments.of("Empty Object",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "OBJECT"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Expr.ObjectExpr(Optional.empty(), List.of(), List.of())
            ),
            Arguments.of("Multiple Fields",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "OBJECT"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Expr.ObjectExpr(
                    Optional.empty(),
                    List.of(
                            new Ast.Stmt.Let("a", Optional.empty()),
                            new Ast.Stmt.Let("b", Optional.empty())
                    ),
                    List.of()
                )
            ),
            Arguments.of("Field And Method",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "OBJECT"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "DEF"),
                    new Token(Token.Type.IDENTIFIER, "f"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "END"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Expr.ObjectExpr(
                    Optional.empty(),
                    List.of(new Ast.Stmt.Let("x", Optional.empty())),
                    List.of(new Ast.Stmt.Def("f", List.of(), List.of()))
                )
            ),
            Arguments.of("Initialized Field",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "OBJECT"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.INTEGER, "1"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Expr.ObjectExpr(
                    Optional.empty(),
                    List.of(new Ast.Stmt.Let("x", Optional.of(new Ast.Expr.Literal(new BigInteger("1"))))),
                    List.of()
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testProgram(String test, Object input, Object expected) {
        test("source", input, expected);
    }

    public static Stream<Arguments> testProgram() {
        return Stream.of(
            Arguments.of("Hello World",
                """
                DEF main() DO
                    print("Hello, World!");
                END
                """,
                new Ast.Source(List.of(
                    new Ast.Stmt.Def("main", List.of(), List.of(
                        new Ast.Stmt.Expression(new Ast.Expr.Function(
                            "print",
                            List.of(new Ast.Expr.Literal("Hello, World!"))
                        ))
                    ))
                ))
            )
        );
    }

    interface ParserMethod<T extends Ast> {
        T invoke(Parser parser) throws ParseException;
    }

    private static void test(String rule, Object input, Object expected) {
        var tokens = switch (input) {
            case List<?> list -> (List<Token>) list;
            case String program -> Assertions.assertDoesNotThrow(() -> new Lexer(program).lex());
            default -> throw new AssertionError(input);
        };
        Parser parser = new Parser(tokens);
        switch (expected) {
            case Ast ast -> {
                var received = Assertions.assertDoesNotThrow(() -> parser.parse(rule));
                Assertions.assertEquals(ast, received);
            }
            case ParseException e -> {
                var received = Assertions.assertThrows(ParseException.class, () -> parser.parse(rule));
                Assertions.assertEquals(e.getToken(), received.getToken());
            }
            default -> throw new AssertionError(input);
        }
    }

}
