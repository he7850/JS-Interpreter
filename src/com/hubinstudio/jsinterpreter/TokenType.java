package com.hubinstudio.jsinterpreter;

public enum TokenType {    //单个token的所有可能类型
    NUMBER, STRING, IDENTIFY,  //变量类型
    OpenBracket, CloseBracket, /*[*/
    OpenBrace, CloseBrace, /*{*/
    OpenParenthese, CloseParenthese, /*(*/
    FOR, WHILE, FUNCTION, IF, ELSE, VAR, RETURN, NEW, //逻辑符号
    ADD, SUB, MUL, DIV, MOD,//加减乘除
    COLON/*:*/, EQ/*==*/, BIND/*=*/, SemiColon/*;*/, COMMA/*;*/,
    GT, LT, GE, LE, //比较符号
    AND, OR, UNEQ, NOT //逻辑运算符&|=!
}