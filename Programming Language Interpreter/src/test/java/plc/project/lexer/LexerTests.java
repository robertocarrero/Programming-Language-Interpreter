package plc.project.lexer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

public final class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testWhitespace(String test, String input, boolean equals) {
        test(input, List.of(), equals);
    }

    public static Stream<Arguments> testWhitespace() {
        return Stream.of(
            Arguments.of("Space", " ", true),
            Arguments.of("Newline", "\n", true),
            Arguments.of("Multiple", "    \n    ", true),

                // Written by me:
            Arguments.of("Backspace", "\b", true),
            Arguments.of("Tab", "\t", true),
            Arguments.of("Carriage return", "\r", true),
            Arguments.of("Mixed whitespace", "\t\n\r\b", true),
            Arguments.of("Multiple new lines", "\n\n\n", true),
            Arguments.of("Non-whitespace: Letter", "a", false),
            Arguments.of("Non-whitespace: Number", "5", false),
            Arguments.of("Non-whitespace: Operator", "+", false),
            Arguments.of("Non-whitespace: String", "\"hi\"", false),
            Arguments.of("Non-whitespace: Character", "\'h\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testComment(String test, String input, boolean equals) {
        test(input, List.of(), equals);
    }

    public static Stream<Arguments> testComment() {
        return Stream.of(
            Arguments.of("Empty", "//", true),
            Arguments.of("Text", "//comment", true),
            //Written by me:
            Arguments.of("Space after and in between", "// this is a comment", true),
            Arguments.of("Special charactes", "//!@#$%^&*()", true),
            Arguments.of("Multiple slashes", "////comment", true),
            Arguments.of("Until newline", "//comment\n", true),
            Arguments.of("With numbers", "//1234", true),
            Arguments.of("With tabs", "//\t\thi", true),
            Arguments.of("Single slash", "/", false),
            Arguments.of("Missing //", "comment", false),
            Arguments.of("Division", "5/2", false),
            Arguments.of("Whitespace between slashes", "/ /", false),
            Arguments.of("String", "\"//not a comment\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.IDENTIFIER, input)), equals);
    }

    public static Stream<Arguments> testIdentifier() {
        return Stream.of(
            Arguments.of("Alphabetic", "getName", true),
            Arguments.of("Alphanumeric", "thelegend27", true),
            Arguments.of("Leading Hyphen", "-five", false),
            Arguments.of("Leading Digit", "1fish2fish", false),
                //Written by me:
            Arguments.of("One letter", "a", true),
            Arguments.of("Underscore at start", "_user", true),
            Arguments.of("With hyphens", "my-user", true),
            Arguments.of("With underscores", "my_user_name", true),
            Arguments.of("Hyphens and underscores", "user_name-123", true),
            Arguments.of("All caps", "CONSTANT", true),
            Arguments.of("CamelCase", "myVariableName", true),
            Arguments.of("Identifier", "123", false),
            Arguments.of("String", "\"name\"", false),
            Arguments.of("Operator", "+", false),
            Arguments.of("Whitespace", " ", false),
            Arguments.of("Character", "\'a\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.INTEGER, input)), equals);
    }

    public static Stream<Arguments> testInteger() {
        return Stream.of(
            Arguments.of("Single Digit", "1", true),
            Arguments.of("Multiple Digits", "123", true),
            Arguments.of("Exponent", "1e10", true),
            Arguments.of("Missing Exponent Digits", "1e", false),
                //Written by me:
            Arguments.of("Positive sign", "+1", true),
            Arguments.of("Negative sign", "-1", true),
            Arguments.of("Edge case: Large number", "999999999", true),
            Arguments.of("Edge case: Zero", "0", true),
            Arguments.of("Exponent positive", "1e+3", true),
            Arguments.of("Exponent negative", "1e-3", true),
            Arguments.of("Zero Exponent", "1e0", true),
            Arguments.of("Decimal", "1.5", false),
            Arguments.of("Identifier", "abc", false),
            Arguments.of("String number", "\"123\"", false),
            Arguments.of("Character number", "\'1\'", false),
            Arguments.of("Operator", "+", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.DECIMAL, input)), equals);
    }

    public static Stream<Arguments> testDecimal() {
        return Stream.of(
            Arguments.of("Decimal", "1.0", true),
            Arguments.of("Multiple Digits", "123.456", true),
            Arguments.of("Exponent", "1.0e10", true),
            Arguments.of("Trailing Decimal", "1.", false),
                //Written by me:
            Arguments.of("Exponent positive", "1.5e+10", true),
            Arguments.of("Exponent negative", "1.5e-10", true),
            Arguments.of("Many decimal places", "9.9999999999999", true),
            Arguments.of("Small decimal", "0.001", true),
            Arguments.of("Leading zero", "0.5", true),
            Arguments.of("Positive sign", "+1.11", true),
            Arguments.of("Negative sign", "-1.11", true),
            Arguments.of("Zero decimal", "0.0", true),
            Arguments.of("Integer", "123", false),
            Arguments.of("Identifier", "abc", false),
            Arguments.of("String", "\"1.5\"", false),
            Arguments.of("Character number", "\'1\'", false),
            Arguments.of("Operator", ".", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.CHARACTER, input)), equals);
    }

    public static Stream<Arguments> testCharacter() {
        return Stream.of(
            Arguments.of("Alphabetic", "\'c\'", true),
            Arguments.of("Newline Escape", "\'\\n\'", true),
            Arguments.of("Unterminated", "\'u", false),
            Arguments.of("Multiple", "\'abc\'", false),
                //Written by me:
            Arguments.of("Tab escape", "\'\\t\'", true),
            Arguments.of("Backslash Escape", "\'\\\\\'", true),
            Arguments.of("Digit", "\'1\'", true),
            Arguments.of("Space", "\' \'", true),
            Arguments.of("Single quote escape", "\'\\\'\'", true),
            Arguments.of("Double quote escape", "\'\\\"\'", true),
            Arguments.of("Carriage return escape", "\'\\r\'", true),
            Arguments.of("Backspace escape", "\'\\b\'", true),
            Arguments.of("Special character", "\'!\'", true),
            Arguments.of("Empty", "\'\'", false),
            Arguments.of("String", "\"a\"", false),
            Arguments.of("Identifier", "c", false),
            Arguments.of("Integer", "5", false),
            Arguments.of("Operator", "+", false),
            Arguments.of("Whitespace", " ", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.STRING, input)), equals);
    }

    public static Stream<Arguments> testString() {
        return Stream.of(
            Arguments.of("Empty", "\"\"", true),
            Arguments.of("Alphabetic", "\"string\"", true),
            Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
            Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                //Written by me:
            Arguments.of("With spaces", "\"hello world\"", true),
            Arguments.of("With numbers", "\"abc123\"", true),
            Arguments.of("Tab escape", "\"tab\\tpresent\"", true),
            Arguments.of("Multiple escapes", "\"\\n\\t\\r\"", true),
            Arguments.of("Backslash escape", "\"back\\\\slash\\\\escape\"", true),
            Arguments.of("Quote escape", "\"He said \\\"Hello\\\"\"", true),
            Arguments.of("Single quote in string", "\"don't\"", true),
            Arguments.of("Unterminated", "\"unterminated", false),
            Arguments.of("Character", "\'s\'", false),
            Arguments.of("Identifier", "string", false),
            Arguments.of("Integer", "123", false),
            Arguments.of("Operator", "+", false),
            Arguments.of("Whitespace", " ", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.OPERATOR, input)), equals);
    }

    public static Stream<Arguments> testOperator() {
        return Stream.of(
            Arguments.of("Character", "(", true),
            Arguments.of("Comparison", "<=", true),
            Arguments.of("Whitespace", " ", false),
            Arguments.of("Double Quote", "\"", false),
                //Written by me:
            Arguments.of("Plus", "+", true),
            Arguments.of("Minus", "-", true),
            Arguments.of("Multiply", "*", true),
            Arguments.of("Divide", "/", true),
            Arguments.of("Equals", "=", true),
            Arguments.of("Not Equals", "!=", true),
            Arguments.of("Greater Than", ">", true),
            Arguments.of("Greater Equal", ">=", true),
            Arguments.of("Less Than", "<", true),
            Arguments.of("Semicolon", ";", true),
            Arguments.of("Comma", ",", true),
            Arguments.of("Dot", ".", true),
            Arguments.of("Colon", ":", true),
            Arguments.of("Opening Bracket", "[", true),
            Arguments.of("Closing Bracket", "]", true),
            Arguments.of("Opening Brace", "{", true),
            Arguments.of("Closing Brace", "}", true),
            Arguments.of("Identifier", "abc", false),
            Arguments.of("Integer", "123", false),
            Arguments.of("String", "\"op\"", false),
            Arguments.of("Character", "\'c\'", false),
            Arguments.of("Whitespace", "\n", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteraction(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    public static Stream<Arguments> testInteraction() {
        return Stream.of(
            Arguments.of("Whitespace", "first second", List.of(
                new Token(Token.Type.IDENTIFIER, "first"),
                new Token(Token.Type.IDENTIFIER, "second")
            )),
            Arguments.of("Identifier Leading Hyphen", "-five", List.of(
                new Token(Token.Type.OPERATOR, "-"),
                new Token(Token.Type.IDENTIFIER, "five")
            )),
            Arguments.of("Identifier Leading Digit", "1fish2fish", List.of(
                new Token(Token.Type.INTEGER, "1"),
                new Token(Token.Type.IDENTIFIER, "fish2fish")
            )),
            Arguments.of("Integer Missing Exponent Digits", "1e", List.of(
                new Token(Token.Type.INTEGER, "1"),
                new Token(Token.Type.IDENTIFIER, "e")
            )),
            Arguments.of("Decimal Missing Decimal Digits", "1.", List.of(
                new Token(Token.Type.INTEGER, "1"),
                new Token(Token.Type.OPERATOR, ".")
            )),
            Arguments.of("Operator Multiple Operators", "<=>", List.of(
                new Token(Token.Type.OPERATOR, "<="),
                new Token(Token.Type.OPERATOR, ">")
            )),
            //Written by me:
            Arguments.of("Comment after code", "x//comment", List.of(
                    new Token(Token.Type.IDENTIFIER, "x")
            )),
            Arguments.of("No spaces", "123abc", List.of(
                    new Token(Token.Type.INTEGER, "123"),
                    new Token(Token.Type.IDENTIFIER, "abc")
            )),
            Arguments.of("Negative number", "-2", List.of(
                    new Token(Token.Type.INTEGER, "-2")
            )),
            Arguments.of("Multiple Tokens", "x+y", List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.IDENTIFIER, "y")
            )),
            Arguments.of("Positive number", "+2", List.of(
                    new Token(Token.Type.INTEGER, "+2")
            )),
            Arguments.of("Mixed Operators", "!=", List.of(
                    new Token(Token.Type.OPERATOR, "!=")
            ))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testException(String test, String input, int index) {
        var e = Assertions.assertThrows(LexException.class, () -> new Lexer(input).lex());
        Assertions.assertEquals(index, e.getIndex());
    }

    public static Stream<Arguments> testException() {
        return Stream.of(
            Arguments.of("Character Unterminated", "\'u", 2),
            Arguments.of("Character Multiple", "\'abc\'", 2),
            Arguments.of("String invalid escape", "\"invalid\\escape\"", 9),
            Arguments.of("String unterminated", "\"hello", 6),
            Arguments.of("Character newline", "\'\\n", 3),
            Arguments.of("Character empty", "\'\'", 1),
            Arguments.of("String newline literal", "\"hello\n\"", 6),
            Arguments.of("Character carriage return", "\'\r\'", 1),
            Arguments.of("Invalid escape in character", "\'\\a\'", 2)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testProgram(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    public static Stream<Arguments> testProgram() {
        return Stream.of(
            Arguments.of("Variable", "LET x = 5;", List.of(
                new Token(Token.Type.IDENTIFIER, "LET"),
                new Token(Token.Type.IDENTIFIER, "x"),
                new Token(Token.Type.OPERATOR, "="),
                new Token(Token.Type.INTEGER, "5"),
                new Token(Token.Type.OPERATOR, ";")
            )),
            Arguments.of("Print Function", "print(\"Hello, World!\");", List.of(
                new Token(Token.Type.IDENTIFIER, "print"),
                new Token(Token.Type.OPERATOR, "("),
                new Token(Token.Type.STRING, "\"Hello, World!\""),
                new Token(Token.Type.OPERATOR, ")"),
                new Token(Token.Type.OPERATOR, ";")
            )),
            Arguments.of("Arithmetic", "x = 1 + 3 * 4;", List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.INTEGER, "1"),
                    new Token(Token.Type.OPERATOR, "+"),
                    new Token(Token.Type.INTEGER, "3"),
                    new Token(Token.Type.OPERATOR, "*"),
                    new Token(Token.Type.INTEGER, "4"),
                    new Token(Token.Type.OPERATOR, ";")
            )),
            Arguments.of("Function call", "func(a, b, c)", List.of(
                    new Token(Token.Type.IDENTIFIER, "func"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "a"),
                    new Token(Token.Type.OPERATOR, ","),
                    new Token(Token.Type.IDENTIFIER, "b"),
                    new Token(Token.Type.OPERATOR, ","),
                    new Token(Token.Type.IDENTIFIER, "c"),
                    new Token(Token.Type.OPERATOR, ")")
            )),
            Arguments.of("Comparison", "if (x <= 10)", List.of(
                    new Token(Token.Type.IDENTIFIER, "if"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "<="),
                    new Token(Token.Type.INTEGER, "10"),
                    new Token(Token.Type.OPERATOR, ")")
            )),
            Arguments.of("Decimal assignment", "decimal = 1.50;", List.of(
                    new Token(Token.Type.IDENTIFIER, "decimal"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.DECIMAL, "1.50"),
                    new Token(Token.Type.OPERATOR, ";")
            )),
            Arguments.of("Character usage", "c = 'a';", List.of(
                    new Token(Token.Type.IDENTIFIER, "c"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.CHARACTER, "\'a\'"),
                    new Token(Token.Type.OPERATOR, ";")
            )),
            Arguments.of("With comment", "x = 9; //set x to 9", List.of(
                    new Token(Token.Type.IDENTIFIER, "x"),
                    new Token(Token.Type.OPERATOR, "="),
                    new Token(Token.Type.INTEGER, "9"),
                    new Token(Token.Type.OPERATOR, ";")
            ))
        );
    }

    private static void test(String input, List<Token> expected, boolean equals) {
        if (equals) {
            //Expect the result to exactly match expected.
            var tokens = Assertions.assertDoesNotThrow(() -> new Lexer(input).lex());
            Assertions.assertEquals(expected, tokens);
        } else {
            //Expect either an exception or different result, used for tests
            //verifying input is not lexed as a specific token type.
            try {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            } catch (LexException ignored) {}
        }
    }

}
