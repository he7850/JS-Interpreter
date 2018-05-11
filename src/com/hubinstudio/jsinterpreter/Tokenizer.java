package com.hubinstudio.jsinterpreter;

import java.util.ArrayList;
import java.util.HashMap;


public class Tokenizer {
    public static void initTokenizer(){
        for (int i = 0; i < keywords.length; i++) {
            Tokenizer.keyword.put(keywords[i], tokenType[i]);
        }
    }
    public static final TokenType[] tokenType = {TokenType.FOR, TokenType.WHILE, TokenType.FUNCTION, TokenType.IF, TokenType.ELSE, TokenType.VAR, TokenType.RETURN, TokenType.NEW};
    public static final String[] keywords = {"for", "while", "function", "if", "else", "var", "return", "new"};
    public static HashMap<String, TokenType> keyword = new HashMap<>();

    public static TokenType getToken(String identify) {
        return keyword.get(identify);
    }

    public static String Join(String sep, Iterable<String> tokens) {
        return String.join(sep, tokens);
    }

    public static boolean isKeyword(String identify) {
        for (String item : keywords) {
            if (identify.equals(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDelimiter(char ch) {
        char[] delimiters = {',', '(', ')', '[', ']', ';', ':', '=', '<', '>', '+', '-', '*', '/', '%', '&', '{', '}'};
        for (char item : delimiters) {
            if (ch == item)
                return true;
        }
        return false;
    }

    //语法词法分析
    public static ArrayList<Token> Tokenize(String code) throws StrException {
        ArrayList<Token> tokenList = new ArrayList<>();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        State state = State.START;
        label:
        while (count < code.length()) {
            boolean isLexeme = false;
            TokenType currentType = null;
            switch (state) {
                //START, END, NUM, STRING, BOOLEAN, IDENTIFY
                case START:     //起始状态
                    if ((code.charAt(count) == ' ' || code.charAt(count) == '\t')) {  //空格在起始状态下忽略
                        count++;
                        continue label;
                    } else if (Character.isLetter(code.charAt(count)) || code.charAt(count) == '_') {
                        state = State.IDENTIFY; //标识量
                    } else if (Character.isDigit(code.charAt(count))) {
                        state = State.NUM;      //数字
                    } else if (code.charAt(count) == '\"') {
                        state = State.STRING;   //字符串
                        count++;
                        continue;
                    } else {    //其他符号，之后必然是下一个词位
                        switch (code.charAt(count)) {
                            case '+':
                                currentType = TokenType.ADD;
                                break;
                            case '-':
                                currentType = TokenType.SUB;
                                break;
                            case '*':
                                currentType = TokenType.MUL;
                                break;
                            case '/':
                                currentType = TokenType.DIV;
                                break;
                            case '%':
                                currentType = TokenType.MOD;
                                break;
                            case '{':
                                currentType = TokenType.OpenBrace;
                                break;
                            case '}':
                                currentType = TokenType.CloseBrace;
                                break;
                            case '[':
                                currentType = TokenType.OpenBracket;
                                break;
                            case ']':
                                currentType = TokenType.CloseBracket;
                                break;
                            case '(':
                                currentType = TokenType.OpenParenthese;
                                break;
                            case ')':
                                currentType = TokenType.CloseParenthese;
                                break;
                            case ':':
                                currentType = TokenType.COLON;
                                break;
                            case ';':
                                currentType = TokenType.SemiColon;
                                break;
                            case '=':
                                if (code.charAt(count + 1) != '=')
                                    currentType = TokenType.BIND;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = TokenType.EQ;
                                }
                                break;
                            case '>':
                                if (code.charAt(count + 1) != '=')
                                    currentType = TokenType.GE;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = TokenType.GT;
                                }
                                break;
                            case '<':
                                if (code.charAt(count + 1) != '=')
                                    currentType = TokenType.LE;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = TokenType.LT;
                                }
                                break;
                            case '&':
                                if (code.charAt(count + 1) != '&')
                                    currentType = TokenType.AND;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = TokenType.AND;
                                }
                                break;
                            case '!':
                                if (code.charAt(count + 1) != '=')
                                    currentType = TokenType.NOT;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = TokenType.UNEQ;
                                }
                                break;
                            case '|':
                                if (code.charAt(count + 1) != '|')
                                    currentType = TokenType.OR;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = TokenType.OR;
                                }
                                break;
                            case ',':
                                currentType = TokenType.COMMA;
                                break;
                            default:
                                throw new StrException("Syntax error, expect a legal identifier");
                        }
                        isLexeme = true;
                    }
                    sb.append(code.charAt(count));
                    count++;
                    break;
                case NUM:   //若当前字母不是数字，cut
                    if (Character.isDigit(code.charAt(count))) {
                        sb.append(code.charAt(count));
                        count++;
                    } else if (isDelimiter(code.charAt(count)) || code.charAt(count) == ' ') {
                        isLexeme = true;
                    }
                    break;
                case IDENTIFY:  //若当前字母不是Aa-Zz或0-9或_,cut
                    if (Character.isLetterOrDigit(code.charAt(count)) || code.charAt(count) == '_') {
                        sb.append(code.charAt(count));
                        count++;
                    } else if (isDelimiter(code.charAt(count)) || (code.charAt(count) == ' ')) {
                        isLexeme = true;
                    }
                    break;
                case STRING:    //若当前字母是',则字符串已结束，cut并count++
                    if (code.charAt(count) != '\"') {
                        sb.append(code.charAt(count));
                        count++;
                    } else {
                        isLexeme = true;
                        count++;
                    }
                    break;
            }
            //如果cut标志为真，将已读到的string作为一个token存入tokenList
            if (isLexeme) {
                switch (state) {
                    case START:
                        break;
                    case NUM:
                        currentType = TokenType.NUMBER;
                        break;
                    case STRING:
                        currentType = TokenType.STRING;
                        break;
                    case IDENTIFY:
                        if (isKeyword(sb.toString())) {
                            currentType = getToken(sb.toString());
                        } else
                            currentType = TokenType.IDENTIFY;
                        break;
                    default:
                        break;
                }
                tokenList.add(new Token(currentType, sb.toString()));
                sb.setLength(0);
                state = State.START;
            }
        }
        return tokenList;
    }
}
