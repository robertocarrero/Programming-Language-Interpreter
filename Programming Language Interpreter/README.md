# Programming Language Interpreter

A Java-based programming language interpreter implementing the major stages of language processing, including lexical analysis, parsing, evaluation, and static semantic analysis.

## Features

* **Lexical Analysis** – Tokenizes source code into identifiers, literals, operators, and other language tokens.
* **Recursive-Descent Parsing** – Converts tokens into an Abstract Syntax Tree (AST) according to the language grammar.
* **Expression & Statement Evaluation** – Executes expressions, variable declarations, control flow, functions, and other language constructs.
* **Scoped Environments** – Supports nested lexical scopes and variable/function resolution.
* **Functions & Closures** – Implements function calls, parameters, return behavior, and captured scopes.
* **Static Analysis** – Performs semantic validation and type checking before program execution.
* **Type System** – Supports type relationships, subtype validation, and inference for supported expressions.
* **Automated Testing** – Tested using JUnit across lexer, parser, evaluator, and analyzer components.

## Technologies

* Java
* Gradle
* JUnit 5
* Guava

## Project Structure

```text
src/main/java/plc/project/
├── lexer/       # Lexical analysis and tokenization
├── parser/      # Recursive-descent parser and AST
├── evaluator/   # Runtime evaluation and scoped environments
└── analyzer/    # Static semantic and type analysis
```

## About

This project was developed as part of the Programming Language Concepts course at the University of Florida.

The project framework, specifications, and testing infrastructure were provided as part of the course. My implementation focused on the language-processing components, including lexical analysis, recursive-descent parsing, evaluation, scope and function semantics, and static/type analysis.
