package JSInterpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.BiFunction;

import static JSInterpreter.Token.TokenType.*;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class JavaScript {
    enum State {
        START, NUM, STRING, IDENTIFY
    }

    public static HashMap<String, Token.TokenType> keyword = new HashMap<>();
    public static String[] keywords = {"for", "while", "function", "if", "else", "var", "return", "new"};
    public static Token.TokenType[] tokenType = {FOR, WHILE, FUNCTION, IF, ELSE, VAR, RETURN, NEW};
    public static int index = 0;                //当前
    public static JsExpression current = null;  //当前表达式,用于构建表达式树

    public static Token.TokenType getToken(String identify) {
        return keyword.get(identify);
    }

    public static void TrueOrThrows(Boolean condition, String message) throws MyException {
        if (!condition) {
            throw new MyException(message == null ? "nothing" : message);
        }
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
    public static ArrayList<Token> Tokenizer(String code) throws MyException {
        ArrayList<Token> tokenList = new ArrayList<>();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        State state = State.START;
        label:
        while (count < code.length()) {
            boolean isLexeme = false;
            Token.TokenType currentType = null;
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
                                currentType = ADD;
                                break;
                            case '-':
                                currentType = SUB;
                                break;
                            case '*':
                                currentType = MUL;
                                break;
                            case '/':
                                currentType = DIV;
                                break;
                            case '%':
                                currentType = MOD;
                                break;
                            case '{':
                                currentType = OpenBrace;
                                break;
                            case '}':
                                currentType = CloseBrace;
                                break;
                            case '[':
                                currentType = OpenBracket;
                                break;
                            case ']':
                                currentType = CloseBracket;
                                break;
                            case '(':
                                currentType = OpenParenthese;
                                break;
                            case ')':
                                currentType = CloseParenthese;
                                break;
                            case ':':
                                currentType = COLON;
                                break;
                            case ';':
                                currentType = SemiColon;
                                break;
                            case '=':
                                if (code.charAt(count + 1) != '=')
                                    currentType = BIND;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = EQ;
                                }
                                break;
                            case '>':
                                if (code.charAt(count + 1) != '=')
                                    currentType = GE;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = GT;
                                }
                                break;
                            case '<':
                                if (code.charAt(count + 1) != '=')
                                    currentType = LE;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = LT;
                                }
                                break;
                            case '&':
                                if (code.charAt(count + 1) != '&')
                                    currentType = AND;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = AND;
                                }
                                break;
                            case '!':
                                if (code.charAt(count + 1) != '=')
                                    currentType = NOT;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = UNEQ;
                                }
                                break;
                            case '|':
                                if (code.charAt(count + 1) != '|')
                                    currentType = OR;
                                else {
                                    count++;
                                    sb.append(code.charAt(count));
                                    currentType = OR;
                                }
                                break;
                            case ',':
                                currentType = COMMA;
                                break;
                            default:
                                throw new MyException("Syntax error, expect a legal identifier");
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
                        currentType = Token.TokenType.NUMBER;
                        break;
                    case STRING:
                        currentType = Token.TokenType.STRING;
                        break;
                    case IDENTIFY:
                        if (isKeyword(sb.toString())) {
                            currentType = getToken(sb.toString());
                        } else
                            currentType = Token.TokenType.IDENTIFY;
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

    //格式：var i=(num|identify|function|new identify);
    //parse变量声明：声明后必须赋值。执行之后index指向';'
    public static JsExpression ParseVar(List<Token> list) throws MyException {
        JsExpression varExp = new JsExpression("var",current);
        current.child.add(varExp);
        index++;    //跳过var
        current = varExp;
        current.child.add(ParseAssign(list));    //var声明变量后必须赋值
        current = current.parent;
        return varExp;
    }

    //parse 数字：number直接作为叶子挂到current树下
    public static JsExpression ParseNumber(List<Token> list) {
        return new JsExpression(list.get(index).name, current);
    }

    //parse identify：变量已经是不可再分的单位，直接挂在current下
    public static JsExpression ParseIdentify(List<Token> list) {
        return new JsExpression(list.get(index).name, current);
    }

    //parse string：将string节点挂在current下
    public static JsExpression ParseString(List<Token> list) {
        JsExpression stringNode = new JsExpression("string", current);
        stringNode.child.add(new JsExpression(list.get(index).name, stringNode));
        return stringNode;
    }

    //PARSE 函数声明，格式：function [name] ( [^var[,var]*] ) { [expression;]* }
    public static JsExpression ParseFunction(List<Token> list) throws MyException {
        JsExpression funcExp = new JsExpression("function", current);
        current = funcExp;
        index++;    //match 函数名

        if (list.get(index).type == Token.TokenType.IDENTIFY) { //match function name
            funcExp.child.add(ParseIdentify(list));
            index++;    //match (
            TrueOrThrows((list.get(index).type == OpenParenthese), list.get(index - 1).name + "∧ expect '('");
            index++;    //match args
        } else if (list.get(index).type == OpenParenthese) { //anonymous function
            index++;    //match args
        } else {    //error
            throw new MyException("Expect an identifier or a '('");
        }

        //读取函数参数列表
        JsExpression argsExp = new JsExpression("args", current);
        while (list.get(index).type != CloseParenthese)//add args
        {
            argsExp.child.add(ParseIdentify(list));
            index++;
            if (list.get(index).type == CloseParenthese) {
                break;
            }
            TrueOrThrows((list.get(index).type == COMMA), list.get(index - 1).name + "∧ expect ','");  //参数由','间隔
            index++;
        }
        funcExp.child.add(argsExp);
        index++;    //函数参数接收完毕, match {
        TrueOrThrows((list.get(index).type == OpenBrace), list.get(index - 1).name + "∧ expect '{'");
        funcExp.child.add(ParseExpList(list));  //解析函数体语句
        TrueOrThrows((list.get(index).type == CloseBrace), list.get(index - 1).name + "∧ expect '}'");
        current = funcExp.parent;
        return funcExp;
    }

    //parse对象声明，格式：NEW CLASSA(..)
    public static JsExpression ParseNewExpression(List<Token> list) throws MyException {
        JsExpression newExp = new JsExpression("new", current);
        current = newExp;
        index++;    //match 对象类名
        newExp.child.add(ParseFunctionCall(list));
        current = newExp.parent;
        return newExp;
    }

    //annoymous function just like lambda
    public static JsExpression ParseLambdaCall(List<Token> list) throws MyException {
        current = new JsExpression("(", current);
        index++;    //match (;annoymous function
        current.child.add(ParseFunction(list));
        index = index + 3;//match )(,then new word
        while (list.get(index).type != CloseParenthese) {
            current.child.add(new JsExpression(list.get(index).name, current));
            index++;
            if (list.get(index).type == CloseParenthese) {
                break;
            }
            //match ,
            index++;//next new word
        }
        index++;//match ;
        return current;
    }

    //解析语句块，以'{'开头，以'}'结尾
    public static JsExpression ParseExpList(List<Token> list) throws MyException {
        JsExpression expList = new JsExpression("{", current);
        current = expList;
        index++;    //skip '{'
        while (list.get(index).type != CloseBrace) {
            Token item = list.get(index);
            switch (item.type) {    //语句块内的语句类型只可能为——赋值、IF WHILE FOR、RETURN、圆括号、变量
                case VAR:           //变量声明
                    expList.child.add(ParseVar(list));
                    index += 2;    //skip ';'
                    break;
                case FUNCTION:      //function:函数定义
                    throw new MyException("Function must be defined in global scope");
                    //expList.child.add(ParseFunction(list));break;
                case OpenParenthese:    // (
                    expList.child.add(ParseRelation(list));
                    index += 2;    //skip ';'
                    break;
                case IDENTIFY:      //已定义变量
                    switch (list.get(index + 1).type) {
                        case BIND:  //赋值
                            expList.child.add(ParseAssign(list));
                            index += 2;    //skip ';'
                            break;
                        case OpenParenthese:    //函数调用
                            expList.child.add(ParseRelation(list));
                            index += 2;    //skip ';'
                            break;
                        default:
                            throw new MyException("Syntax error");
                    }
                    break;
                case IF:    //parse IF 语句
                    expList.child.add(ParseIf(list));
                    index++;    //skip '}'
                    break;
                case WHILE: //parse WHILE 语句
                    expList.child.add(ParseWhile(list));
                    index++;    //skip '}'
                    break;
                case RETURN://parse RETURN 语句
                    expList.child.add(ParseReturn(list));
                    index++;    //skip ';'
                    break;
                case STRING:
                case NUMBER:
                    expList.child.add(ParseRelation(list));
                    index += 2;    //skip ';'
                    break;
                default:
                    throw new MyException("Undefine identifier");
            }
        }
        current = expList.parent;
        return expList;
    }

    //parse 返回语句，格式：RETURN ...(可计算表达式)
    public static JsExpression ParseReturn(List<Token> list) throws MyException {
        JsExpression retExp = new JsExpression("return", current);
        current = retExp;
        index++;
        retExp.child.add(ParseRelation(list));
        index++;
        TrueOrThrows((list.get(index).type == SemiColon), list.get(index - 1).name + "∧ expect ';'");
        current = retExp.parent;
        return retExp;
    }

    //parse 条件判断表达式(condition expression)，表达式的结果一般是T/F，但这里没有检查
    public static JsExpression ParseBool(List<Token> list) throws MyException {
        JsExpression result = ParseRelation(list);  //解析可计算式子
        index++;    //match比较符
        Token.TokenType[] relArray = {
                EQ, GT, LT, GE,
                LE, AND, OR, UNEQ,
                NOT
        };
        for (Token.TokenType item : relArray) {
            if (list.get(index).type == item) { //如果后面有比较符
                JsExpression tmp = new JsExpression(list.get(index).name, current);
                current = tmp;
                index++;    //match比较符后的表达式
                JsExpression e1 = ParseRelation(list);
                result.parent = tmp;
                tmp.child.add(result);
                tmp.child.add(e1);
                result = tmp;
            }
        }
        return result;
    }

    //Parse 可计算表达式，格式：[(+|-)Term]*
    public static JsExpression ParseRelation(List<Token> list) throws MyException {
        JsExpression result = ParseTerm(list);  //解析优先级更高的乘除法及括号
        index++;    //match下个元素，只可能是+或-
        JsExpression tmp;
        while (list.get(index).type == ADD || list.get(index).type == SUB) {
            tmp = new JsExpression(list.get(index).name, current);
            current = tmp;    //分叉节点，将result节点用tmp替换
            index++;    //match下个元素
            JsExpression e2 = ParseTerm(list);    //e2的parent为current，也即tmp
            result.parent = tmp;
            tmp.child.add(result);
            tmp.child.add(e2);
            result = tmp;   //将tmp更新为result
            current = result;
            index++;    //match下个元素，只可能是+-
        }
        index--;    //表达式解析完毕时(如2+3;)，index指向';'，为使index回到';'前，index--
        current = result.parent;
        return result;
    }

    //parse 乘除表达式，格式：[(*|/|%)Factor]*
    public static JsExpression ParseTerm(List<Token> list) throws MyException {
        JsExpression result = ParseFactor(list);    //解析(..)或num或var，得到结果
        index++;    //match下个元素，只可能是*/%
        JsExpression tmp;
        while (list.get(index).type == MUL || list.get(index).type == DIV || list.get(index).type == MOD) {
            tmp = new JsExpression(list.get(index).name, current);
            result.parent = tmp;    //遇到优先级高的符号，分叉节点，将result节点用tmp替换
            current = tmp;
            index++;    //match下个元素
            JsExpression e2 = ParseFactor(list);    //e2的parent为current，也即tmp
            tmp.child.add(result);
            tmp.child.add(e2);
            result = tmp;   //将tmp更新为result
            current = result;
            index++;    //match下个元素，只可能是*/%
        }
        index--;
        current = result.parent;
        return result;
    }

    //parse 单个可计算因子，格式：Num|Identify|'(Exp)'|func(..)|string
    public static JsExpression ParseFactor(List<Token> list) throws MyException {
        if (list.get(index).type == NUMBER) {
            return ParseNumber(list);
        } else if (list.get(index).type == IDENTIFY) {
            if (list.get(index + 1).type == OpenParenthese) {
                return ParseFunctionCall(list);
            } else {
                return ParseIdentify(list);
            }
        } else if (list.get(index).type == OpenParenthese) {
            index++;    //match '(' 后的部分
            JsExpression exp = ParseRelation(list);
            index++;    //match )
            TrueOrThrows(list.get(index).type == CloseParenthese, list.get(index - 1).name + "∧ expect ')'");
            return exp;
        } else if (list.get(index).type == STRING) {
            return ParseString(list);
        }
        throw new MyException(list.get(index - 1).name + "∧ expect an identifier");
    }

    //parse 赋值语句，格式：var=num|function|'('|identify,执行之后index指向';'
    public static JsExpression ParseAssign(List<Token> list) throws MyException {
        JsExpression assignExp = new JsExpression("=", current);
        assignExp.child.add(ParseIdentify(list));   //接收var
        index++;    //match =
        TrueOrThrows((list.get(index).type == BIND), list.get(index - 1).name + "∧ expect '='");
        index++;    //match num|function|'('|identify|new identify
        assignExp.child.add(ParseRelation(list));
        current = assignExp.parent;
        while (list.get(index + 1).type == COMMA) {
            JsExpression tmp = new JsExpression("=", current);
            index += 2;    //skip ','
            tmp.child.add(ParseAssign(list));
        }
        return assignExp;
    }

    //parse IF 语句，格式：if(cond){} (else{})
    public static JsExpression ParseIf(List<Token> list) throws MyException {
        JsExpression ifExp = new JsExpression("if", current);
        current = ifExp;
        index++;    //match (
        TrueOrThrows((list.get(index).type == OpenParenthese), list.get(index - 1).name + "∧ expect '('");
        index++;    //match condition expression
        ifExp.child.add(ParseBool(list));//parse bool expression
        index++;    //match )
        TrueOrThrows((list.get(index).type == CloseParenthese), list.get(index - 1).name + "∧ expect ')'");
        index++;    //match {
        TrueOrThrows((list.get(index).type == OpenBrace), list.get(index - 1).name + "∧ expect '{'");
        ifExp.child.add(ParseExpList(list));    //parse expression list
        TrueOrThrows((list.get(index).type == CloseBrace), list.get(index - 1).name + "∧ expect '}'");
        if (list.get(index + 1).type == ELSE) { //check else
            index += 2;    //skip ELSE
            TrueOrThrows((list.get(index).type == OpenBrace), list.get(index - 1).name + "∧ expect '{'");
            ifExp.child.add(ParseExpList(list));
            TrueOrThrows((list.get(index).type == CloseBrace), list.get(index - 1).name + "∧ expect '}'");
        }
        current = ifExp.parent;
        return ifExp;
    }

    //parse WHILE 语句，格式：while(cond){..}
    public static JsExpression ParseWhile(List<Token> list) throws MyException {
        JsExpression whExp = new JsExpression("while", current);
        current = whExp;
        index++;    //match (
        TrueOrThrows((list.get(index).type == OpenParenthese), list.get(index - 1).name + "∧ expect '('");
        index++;    //match condition
        whExp.child.add(ParseBool(list));   //parse bool expression
        index++;    //match )
        TrueOrThrows((list.get(index).type == CloseParenthese), list.get(index - 1).name + "∧ expect ')'");
        index++;    //match {
        TrueOrThrows((list.get(index).type == OpenBrace), list.get(index - 1).name + "∧ expect '{'");
        whExp.child.add(ParseExpList(list));
        TrueOrThrows((list.get(index).type == CloseBrace), list.get(index - 1).name + "∧ expect '}'");
        current = whExp.parent;
        return whExp;
    }

    //parse WHILE 语句，格式：for(exp;cond;exp){..}
    public static JsExpression ParseFor(List<Token> list) throws MyException {
        JsExpression forExp = new JsExpression("for", current);
        current = forExp;
        index++;    //match (
        TrueOrThrows((list.get(index).type == OpenParenthese), list.get(index - 1).name + "∧ expect '('");
        index++;    //match condition
        switch (list.get(index).type) {
            case VAR:
                forExp.child.add(ParseVar(list));
                index++;    //match ';'
                break;
            case IDENTIFY:  //assign expression or relation(include function)
                if (list.get(index + 1).type == BIND) {
                    forExp.child.add(ParseAssign(list));
                } else {
                    forExp.child.add(ParseRelation(list));
                }
                index++;    //match ';'
                break;
            case SemiColon: //no initial expression
                forExp.child.add(new JsExpression("", current));
                break;
            default:
                forExp.child.add(ParseRelation(list));
                index++;    //match ';'
                break;
        }
        TrueOrThrows(list.get(index).type == SemiColon, list.get(index - 1).name + "∧ expect ';'");
        index++;    //skip ';'
        forExp.child.add(ParseBool(list));   //parse bool expression
        index++;    //match second ';'
        TrueOrThrows(list.get(index).type == SemiColon, list.get(index - 1).name + "∧ expect ';'");
        index++;    //skip second ';'
        if (list.get(index + 1).type == BIND) {
            forExp.child.add(ParseAssign(list));
        } else {
            forExp.child.add(ParseRelation(list));
        }
        index++;    //match )
        TrueOrThrows((list.get(index).type == CloseParenthese), list.get(index - 1).name + "∧ expect ')'");
        index++;    //match {
        TrueOrThrows((list.get(index).type == OpenBrace), list.get(index - 1).name + "∧ expect '{'");
        forExp.child.add(ParseExpList(list));
        TrueOrThrows((list.get(index).type == CloseBrace), list.get(index - 1).name + "∧ expect '}'");
        current = forExp.parent;
        return forExp;
    }

    //parse 函数调用，格式：FUNC(...)
    public static JsExpression ParseFunctionCall(List<Token> list) throws MyException {
        JsExpression funcCall = new JsExpression(list.get(index).name, current);
        current = funcCall;
        index++;    //match (
        funcCall.child.add(new JsExpression("args", current));
        index++;    //skip (
        while (list.get(index).type != CloseParenthese) {
            funcCall.child.add(ParseRelation(list));
            index++;    //match ','或')'
            if (list.get(index).type == CloseParenthese) {
                break;
            }
            TrueOrThrows((list.get(index).type == COMMA), list.get(index - 1).name + "∧ expect ','");
            index++;    //match下一个参数
        }
        //此时已经match )
        current = funcCall.parent;
        return funcCall;
    }

    //已经分好单位，开始解析程序。代码的类型有
    // var -- 变量声明赋值 var a=...;
    // function -- 函数定义 function A(var argList..){ ... };
    // functionCall -- 执行某个函数 functionCall(argList..);
    // while,if,for语句块
    public static JsExpression ParseProgram(List<Token> list) throws MyException {
        JsExpression program = new JsExpression("", null);  //program为代码树根节点
        current = program;
        index = 0;
        while (index < list.size()) {
            Token item = list.get(index);
            switch (item.type) {
                case VAR:   //赋值语句
                    program.child.add(ParseVar(list)); //解析得到子树,并挂到program下
                    TrueOrThrows((list.get(index + 1).type == SemiColon), list.get(index).name + "∧ expect ';'");
                    index += 2;    //skip ';'
                    break;
                case FUNCTION:  //函数定义
                    program.child.add(ParseFunction(list));
                    index++;    //skip '}'
                    break;
                case OpenParenthese:    //圆括号
                case STRING:    //字符串
                case NUMBER:    //数字
                    program.child.add(ParseRelation(list));
                    TrueOrThrows((list.get(index + 1).type == SemiColon), list.get(index).name + "∧ expect ';'");
                    index += 2;    //skip ';'
                    break;
                case IDENTIFY:  //变量标识符，格式：var=..;或fun(..);或var;
                    switch (list.get(index + 1).type) {
                        case BIND:      //=
                            program.child.add(ParseAssign(list));
                            break;
                        default:        //不是赋值，即是运算或函数调用
                            program.child.add(ParseRelation(list));
                            break;
                    }
                    TrueOrThrows((list.get(index + 1).type == SemiColon), list.get(index).name + "∧ expect ';'");
                    index += 2;    //skip ';'
                    break;
                case WHILE:
                    program.child.add(ParseWhile(list));
                    index++;    //skip '}'
                    break;
                case IF:
                    program.child.add(ParseIf(list));
                    index++;    //skip '}'
                    break;
                case FOR:
                    program.child.add(ParseFor(list));
                    index++;    //skip '}'
                    break;
                case OpenBrace:
                    program.child.add(ParseExpList(list));
                    index++;    //skip '}'
                    break;
                default:    //null
                    TrueOrThrows((list.get(index).type == SemiColon), "Syntax error, expect a legal identifier");
                    program.child.add(new JsExpression("", current));
                    index++;
                    break;
            }
            current = program;
        }
        return program;
    }

    //将代码解析为表达式树
    public static JsExpression Parse(String code) throws MyException {
        List<Token> tokenList = Tokenizer(code);    //句词分解
        JsExpression program = ParseProgram(tokenList);
        return program;
    }

    //判断表达式的bool值
    public static JsBool BoolEval(JsExpression[] args, Env env, BiFunction<JsNumber, JsNumber, Boolean> rel) throws MyException {
        TrueOrThrows((args.length > 1), "Too less arguments are in relation expressions");
        JsNumber curr = (JsNumber) args[0].evaluate(env);

        for (int i = 1; i < args.length; i++) {
            JsNumber next = (JsNumber) args[i].evaluate(env);
            if (rel.apply(curr, next)) {
                curr = next;
            } else {
                return JsBool.False;
            }
        }
        return JsBool.True;
    }

    //控制台
    public static void GetSchemeConsole(Env env) {
        while (true) {
            int bracketCount = 0, braceCount = 0;// '(' '{'
            try {
                System.out.print(">>>");
                StringBuilder sb = new StringBuilder();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                String input = bufferedReader.readLine();
                //if String doesn't end with ';' or , then continue reading
                while (true) {
                    sb.append(input);
                    for (char c : input.toCharArray()) {
                        if (c == '(') {
                            bracketCount++;
                        } else if (c == ')') {
                            bracketCount--;
                        } else if (c == '{') {
                            braceCount++;
                        } else if (c == '}') {
                            braceCount--;
                        }
                    }
                    int length = input.length();
                    if (input.charAt(length - 1) == ';' || input.charAt(0) == '}') {
                        if (braceCount == 0 && bracketCount == 0)
                            break;
                    }
                    System.out.print("...");
                    input = bufferedReader.readLine();
                }
                input = sb.toString();
                if (input.equals("exit")) {
                    System.out.println("Goodbye!");
                    break;
                }
                if (!input.equals(" ")) {
                    try {
                        Parse(input).evaluate(env);
                    } catch (MyException e) {
                        //e.printStackTrace();
                        System.out.println(e.errorMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) {
        //String code = "function(a){\n\ra=10;\nvar b=3*(2+5)+6;\nif(a==10)\n{\t  alert('a');var c=function(){return null;};}return b;}";
        for (int i = 0; i < keywords.length; i++) {
            keyword.put(keywords[i], tokenType[i]);
        }
        System.out.println("----JavaScript Interpreter----");
        Env root = new Env(null);
        root
                .Builtin("+", (argArray, scope) -> {
                    ArrayList<JsExpression> argList = new ArrayList<>();
                    Collections.addAll(argList, argArray);
                    final boolean[] stringFlag = {false};
                    JsObject[] objs = argList.stream().map(obj -> {
                        try {
                            JsObject result = obj.evaluate(scope);
                            if (result.getClass().getSimpleName().equals(JsString.class.getSimpleName()))
                                stringFlag[0] = true;
                            return result;
                        } catch (MyException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).toArray(JsObject[]::new);
                    if (!stringFlag[0]){
                        JsNumber sum = new JsNumber(0);
                        for (JsObject item : objs) {
                            sum.value += ((JsNumber)item).value;
                        }
                        return sum;
                    } else {
                        JsString sum = new JsString("");
                        for (JsObject item : objs) {
                            sum.value += (item.toJsString()).value;
                        }
                        return sum;
                    }
                })
                .Builtin("-", (argArray, scope) -> {
                    if (!argArray[0].getClass().getSimpleName().equals(JsNumber.class.getSimpleName())
                            && argArray[1].getClass().getSimpleName().equals(JsNumber.class.getSimpleName())) {
                        try {
                            throw new MyException("String type cannot be used in '-'");
                        } catch (MyException e) {
                            System.out.println(e.errorMessage);
                        }
                    }
                    ArrayList<JsExpression> argList = new ArrayList<>();
                    Collections.addAll(argList, argArray);
                    JsNumber[] numbers = argList.stream().map(obj -> {
                        try {
                            return obj.evaluate(scope);
                        } catch (MyException e) {
                            System.out.println(e.errorMessage);
                            return null;
                        }
                    }).toArray(JsNumber[]::new);
                    if (numbers.length == 1) {
                        return new JsNumber(-numbers[0].value);
                    } else {
                        int res = numbers[0].value;
                        for (int i = 1; i < numbers.length; i++) {
                            res -= numbers[i].value;
                        }
                        return new JsNumber(res);
                    }
                })
                .Builtin("*", (argArray, scope) -> {
                    ArrayList<JsExpression> argList = new ArrayList<>();
                    Collections.addAll(argList, argArray);
                    JsNumber[] numbers = argList.stream().map(obj -> {
                        try {
                            return obj.evaluate(scope);
                        } catch (MyException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).toArray(JsNumber[]::new);
                    int result = 1;
                    for (JsNumber num : numbers) {
                        result = result * num.value;
                    }
                    return new JsNumber(result);
                })
                .Builtin("/", (argArray, scope) -> {
                    ArrayList<JsExpression> argList = new ArrayList<>();
                    Collections.addAll(argList, argArray);
                    JsNumber[] numbers = argList.stream().map(obj -> {
                        try {
                            return obj.evaluate(scope);
                        } catch (MyException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).toArray(JsNumber[]::new);
                    int result = numbers[0].value;
                    for (int i = 1; i < numbers.length; i++) {
                        result = result / numbers[i].value;
                    }
                    return new JsNumber(result);
                })
                .Builtin("%", (argArray, scope) -> {
                    ArrayList<JsExpression> argList = new ArrayList<>();
                    Collections.addAll(argList, argArray);
                    JsNumber[] numbers = argList.stream().map(obj -> {
                        try {
                            return obj.evaluate(scope);
                        } catch (MyException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).toArray(JsNumber[]::new);
                    int result = numbers[0].value;
                    for (int i = 1; i < numbers.length; i++) {
                        result = result % numbers[i].value;
                    }
                    return new JsNumber(result);
                })
                .Builtin("==", (argArray, scope) ->
                {
                    try {
                        return BoolEval(argArray, scope, (s1, s2) -> s1.value == s2.value);
                    } catch (MyException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .Builtin(">", (argArray, scope) ->
                {
                    try {
                        return BoolEval(argArray, scope, (s1, s2) -> s1.value > s2.value);
                    } catch (MyException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .Builtin("<", (argArray, scope) ->
                {
                    try {
                        return BoolEval(argArray, scope, (s1, s2) -> s1.value < s2.value);
                    } catch (MyException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .Builtin(">=", (argArray, scope) ->
                {
                    try {
                        return BoolEval(argArray, scope, (s1, s2) -> s1.value >= s2.value);
                    } catch (MyException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .Builtin("<=", (argArray, scope) ->
                {
                    try {
                        return BoolEval(argArray, scope, (s1, s2) -> s1.value <= s2.value);
                    } catch (MyException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .Builtin("!=", (argArray, scope) ->
                {
                    try {
                        return BoolEval(argArray, scope, (s1, s2) -> s1.value != s2.value);
                    } catch (MyException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .Builtin("Object", (argArray, scope) ->
                        new JsObject());
        GetSchemeConsole(root);
    }
}
