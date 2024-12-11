# Decaf Compiler
Hello reader!

This is a comipler project based on the Decaf grammar, which is a bit similar to Java.


### Compiler Phases
1. Lexical Analysis
    * Scan input text file for tokens. These tokens are used for syntax analysis.
2. Syntax Analysis
    * Takes the input tokens from previous steps and produces Abstract Syntax Tree (AST).
3. Semantic Analysis
    * Check semantic rules of grammar.
4. Intermediate Code Generation
    * Takes AST and produces assembly for handful of scenarios provided to us for testing.
