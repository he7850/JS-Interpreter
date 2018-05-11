package com.hubinstudio.jsinterpreter;

import java.util.List;

public class SyntaxTreeParser {
    public static int index = 0;                //当前
    public static JsExpression current = null;  //当前表达式,用于构建表达式树

    //格式：var i=(num|identify|function|new identify);
    //parse变量声明：声明后必须赋值。执行之后index指向';'
    public static JsExpression ParseVar(List<Token> list) throws StrException {
        JsExpression varExp = new JsExpression("var", current);
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
    public static JsExpression ParseFunction(List<Token> list) throws StrException {
        JsExpression funcExp = new JsExpression("function", current);
        current = funcExp;
        index++;    //match 函数名

        if (list.get(index).type == TokenType.IDENTIFY) { //match function name
            funcExp.child.add(ParseIdentify(list));
            index++;    //match (
            StrException.TrueOrThrows((list.get(index).type == TokenType.OpenParenthese), list.get(index - 1).name + "∧ expect '('");
            index++;    //match args
        } else if (list.get(index).type == TokenType.OpenParenthese) { //anonymous function
            index++;    //match args
        } else {    //error
            throw new StrException("Expect an identifier or a '('");
        }

        //读取函数参数列表
        JsExpression argsExp = new JsExpression("args", current);
        while (list.get(index).type != TokenType.CloseParenthese)//add args
        {
            argsExp.child.add(ParseIdentify(list));
            index++;
            if (list.get(index).type == TokenType.CloseParenthese) {
                break;
            }
            StrException.TrueOrThrows((list.get(index).type == TokenType.COMMA), list.get(index - 1).name + "∧ expect ','");  //参数由','间隔
            index++;
        }
        funcExp.child.add(argsExp);
        index++;    //函数参数接收完毕, match {
        StrException.TrueOrThrows((list.get(index).type == TokenType.OpenBrace), list.get(index - 1).name + "∧ expect '{'");
        funcExp.child.add(ParseExpList(list));  //解析函数体语句
        StrException.TrueOrThrows((list.get(index).type == TokenType.CloseBrace), list.get(index - 1).name + "∧ expect '}'");
        current = funcExp.parent;
        return funcExp;
    }

    //parse对象声明，格式：NEW CLASSA(..)
    public static JsExpression ParseNewExpression(List<Token> list) throws StrException {
        JsExpression newExp = new JsExpression("new", current);
        current = newExp;
        index++;    //match 对象类名
        newExp.child.add(ParseFunctionCall(list));
        current = newExp.parent;
        return newExp;
    }

    //annoymous function just like lambda
    public static JsExpression ParseLambdaCall(List<Token> list) throws StrException {
        current = new JsExpression("(", current);
        index++;    //match (;annoymous function
        current.child.add(ParseFunction(list));
        index = index + 3;//match )(,then new word
        while (list.get(index).type != TokenType.CloseParenthese) {
            current.child.add(new JsExpression(list.get(index).name, current));
            index++;
            if (list.get(index).type == TokenType.CloseParenthese) {
                break;
            }
            //match ,
            index++;//next new word
        }
        index++;//match ;
        return current;
    }

    //解析语句块，以'{'开头，以'}'结尾
    public static JsExpression ParseExpList(List<Token> list) throws StrException {
        JsExpression expList = new JsExpression("{", current);
        current = expList;
        index++;    //skip '{'
        while (list.get(index).type != TokenType.CloseBrace) {
            Token item = list.get(index);
            switch (item.type) {    //语句块内的语句类型只可能为——赋值、IF WHILE FOR、RETURN、圆括号、变量
                case VAR:           //变量声明
                    expList.child.add(ParseVar(list));
                    index += 2;    //skip ';'
                    break;
                case FUNCTION:      //function:函数定义
                    throw new StrException("Function must be defined in global scope");
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
                            throw new StrException("Syntax error");
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
                    throw new StrException("Undefine identifier");
            }
        }
        current = expList.parent;
        return expList;
    }

    //parse 返回语句，格式：RETURN ...(可计算表达式)
    public static JsExpression ParseReturn(List<Token> list) throws StrException {
        JsExpression retExp = new JsExpression("return", current);
        current = retExp;
        index++;
        retExp.child.add(ParseRelation(list));
        index++;
        StrException.TrueOrThrows((list.get(index).type == TokenType.SemiColon), list.get(index - 1).name + "∧ expect ';'");
        current = retExp.parent;
        return retExp;
    }

    //parse 条件判断表达式(condition expression)，表达式的结果一般是T/F，但这里没有检查
    public static JsExpression ParseBool(List<Token> list) throws StrException {
        JsExpression result = ParseRelation(list);  //解析可计算式子
        index++;    //match比较符
        TokenType[] relArray = {
                TokenType.EQ, TokenType.GT, TokenType.LT, TokenType.GE,
                TokenType.LE, TokenType.AND, TokenType.OR, TokenType.UNEQ,
                TokenType.NOT
        };
        for (TokenType item : relArray) {
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
    public static JsExpression ParseRelation(List<Token> list) throws StrException {
        JsExpression result = ParseTerm(list);  //解析优先级更高的乘除法及括号
        index++;    //match下个元素，只可能是+或-
        JsExpression tmp;
        while (list.get(index).type == TokenType.ADD || list.get(index).type == TokenType.SUB) {
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
    public static JsExpression ParseTerm(List<Token> list) throws StrException {
        JsExpression result = ParseFactor(list);    //解析(..)或num或var，得到结果
        index++;    //match下个元素，只可能是*/%
        JsExpression tmp;
        while (list.get(index).type == TokenType.MUL || list.get(index).type == TokenType.DIV || list.get(index).type == TokenType.MOD) {
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
    public static JsExpression ParseFactor(List<Token> list) throws StrException {
        if (list.get(index).type == TokenType.NUMBER) {
            return ParseNumber(list);
        } else if (list.get(index).type == TokenType.IDENTIFY) {
            if (list.get(index + 1).type == TokenType.OpenParenthese) {
                return ParseFunctionCall(list);
            } else {
                return ParseIdentify(list);
            }
        } else if (list.get(index).type == TokenType.OpenParenthese) {
            index++;    //match '(' 后的部分
            JsExpression exp = ParseRelation(list);
            index++;    //match )
            StrException.TrueOrThrows(list.get(index).type == TokenType.CloseParenthese, list.get(index - 1).name + "∧ expect ')'");
            return exp;
        } else if (list.get(index).type == TokenType.STRING) {
            return ParseString(list);
        }
        throw new StrException(list.get(index - 1).name + "∧ expect an identifier");
    }

    //parse 赋值语句，格式：var=num|function|'('|identify,执行之后index指向';'
    public static JsExpression ParseAssign(List<Token> list) throws StrException {
        JsExpression assignExp = new JsExpression("=", current);
        assignExp.child.add(ParseIdentify(list));   //接收var
        index++;    //match =
        StrException.TrueOrThrows((list.get(index).type == TokenType.BIND), list.get(index - 1).name + "∧ expect '='");
        index++;    //match num|function|'('|identify|new identify
        assignExp.child.add(ParseRelation(list));
        current = assignExp.parent;
        while (list.get(index + 1).type == TokenType.COMMA) {
            JsExpression tmp = new JsExpression("=", current);
            index += 2;    //skip ','
            tmp.child.add(ParseAssign(list));
        }
        return assignExp;
    }

    //parse IF 语句，格式：if(cond){} (else{})
    public static JsExpression ParseIf(List<Token> list) throws StrException {
        JsExpression ifExp = new JsExpression("if", current);
        current = ifExp;
        index++;    //match (
        StrException.TrueOrThrows((list.get(index).type == TokenType.OpenParenthese), list.get(index - 1).name + "∧ expect '('");
        index++;    //match condition expression
        ifExp.child.add(ParseBool(list));//parse bool expression
        index++;    //match )
        StrException.TrueOrThrows((list.get(index).type == TokenType.CloseParenthese), list.get(index - 1).name + "∧ expect ')'");
        index++;    //match {
        StrException.TrueOrThrows((list.get(index).type == TokenType.OpenBrace), list.get(index - 1).name + "∧ expect '{'");
        ifExp.child.add(ParseExpList(list));    //parse expression list
        StrException.TrueOrThrows((list.get(index).type == TokenType.CloseBrace), list.get(index - 1).name + "∧ expect '}'");
        if (list.get(index + 1).type == TokenType.ELSE) { //check else
            index += 2;    //skip ELSE
            StrException.TrueOrThrows((list.get(index).type == TokenType.OpenBrace), list.get(index - 1).name + "∧ expect '{'");
            ifExp.child.add(ParseExpList(list));
            StrException.TrueOrThrows((list.get(index).type == TokenType.CloseBrace), list.get(index - 1).name + "∧ expect '}'");
        }
        current = ifExp.parent;
        return ifExp;
    }

    //parse WHILE 语句，格式：while(cond){..}
    public static JsExpression ParseWhile(List<Token> list) throws StrException {
        JsExpression whExp = new JsExpression("while", current);
        current = whExp;
        index++;    //match (
        StrException.TrueOrThrows((list.get(index).type == TokenType.OpenParenthese), list.get(index - 1).name + "∧ expect '('");
        index++;    //match condition
        whExp.child.add(ParseBool(list));   //parse bool expression
        index++;    //match )
        StrException.TrueOrThrows((list.get(index).type == TokenType.CloseParenthese), list.get(index - 1).name + "∧ expect ')'");
        index++;    //match {
        StrException.TrueOrThrows((list.get(index).type == TokenType.OpenBrace), list.get(index - 1).name + "∧ expect '{'");
        whExp.child.add(ParseExpList(list));
        StrException.TrueOrThrows((list.get(index).type == TokenType.CloseBrace), list.get(index - 1).name + "∧ expect '}'");
        current = whExp.parent;
        return whExp;
    }

    //parse WHILE 语句，格式：for(exp;cond;exp){..}
    public static JsExpression ParseFor(List<Token> list) throws StrException {
        JsExpression forExp = new JsExpression("for", current);
        current = forExp;
        index++;    //match (
        StrException.TrueOrThrows((list.get(index).type == TokenType.OpenParenthese), list.get(index - 1).name + "∧ expect '('");
        index++;    //match condition
        switch (list.get(index).type) {
            case VAR:
                forExp.child.add(ParseVar(list));
                index++;    //match ';'
                break;
            case IDENTIFY:  //assign expression or relation(include function)
                if (list.get(index + 1).type == TokenType.BIND) {
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
        StrException.TrueOrThrows(list.get(index).type == TokenType.SemiColon, list.get(index - 1).name + "∧ expect ';'");
        index++;    //skip ';'
        forExp.child.add(ParseBool(list));   //parse bool expression
        index++;    //match second ';'
        StrException.TrueOrThrows(list.get(index).type == TokenType.SemiColon, list.get(index - 1).name + "∧ expect ';'");
        index++;    //skip second ';'
        if (list.get(index + 1).type == TokenType.BIND) {
            forExp.child.add(ParseAssign(list));
        } else {
            forExp.child.add(ParseRelation(list));
        }
        index++;    //match )
        StrException.TrueOrThrows((list.get(index).type == TokenType.CloseParenthese), list.get(index - 1).name + "∧ expect ')'");
        index++;    //match {
        StrException.TrueOrThrows((list.get(index).type == TokenType.OpenBrace), list.get(index - 1).name + "∧ expect '{'");
        forExp.child.add(ParseExpList(list));
        StrException.TrueOrThrows((list.get(index).type == TokenType.CloseBrace), list.get(index - 1).name + "∧ expect '}'");
        current = forExp.parent;
        return forExp;
    }

    //parse 函数调用，格式：FUNC(...)
    public static JsExpression ParseFunctionCall(List<Token> list) throws StrException {
        JsExpression funcCall = new JsExpression(list.get(index).name, current);
        current = funcCall;
        index++;    //match (
        funcCall.child.add(new JsExpression("args", current));
        index++;    //skip (
        while (list.get(index).type != TokenType.CloseParenthese) {
            funcCall.child.add(ParseRelation(list));
            index++;    //match ','或')'
            if (list.get(index).type == TokenType.CloseParenthese) {
                break;
            }
            StrException.TrueOrThrows((list.get(index).type == TokenType.COMMA), list.get(index - 1).name + "∧ expect ','");
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
    public static JsExpression ParseProgram(List<Token> list) throws StrException {
        JsExpression program = new JsExpression("", null);  //program为代码树根节点
        current = program;
        index = 0;
        while (index < list.size()) {
            Token item = list.get(index);
            switch (item.type) {
                case VAR:   //赋值语句
                    program.child.add(ParseVar(list)); //解析得到子树,并挂到program下
                    StrException.TrueOrThrows((list.get(index + 1).type == TokenType.SemiColon), list.get(index).name + "∧ expect ';'");
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
                    StrException.TrueOrThrows((list.get(index + 1).type == TokenType.SemiColon), list.get(index).name + "∧ expect ';'");
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
                    StrException.TrueOrThrows((list.get(index + 1).type == TokenType.SemiColon), list.get(index).name + "∧ expect ';'");
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
                    StrException.TrueOrThrows((list.get(index).type == TokenType.SemiColon), "Syntax error, expect a legal identifier");
                    program.child.add(new JsExpression("", current));
                    index++;
                    break;
            }
            current = program;
        }
        return program;
    }
}
