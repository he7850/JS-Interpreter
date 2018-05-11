package com.hubinstudio.jsinterpreter;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class Token {
    public TokenType type;
    public String name;
    public Token(TokenType t, String n) {
        type = t;
        name = n;
    }
}
