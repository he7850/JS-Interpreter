package com.hubinstudio.jsinterpreter;

import com.hubinstudio.jsinterpreter.types.*;

import java.util.ArrayList;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class JsExpression {
    public String value;
    public JsExpression parent;
    public ArrayList<JsExpression> child;

    public JsExpression(String token, JsExpression parent) {
        this.value = token;
        this.parent = parent;
        this.child = new ArrayList<>();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("value="+value+", child=");
        for (JsExpression item : child) {
            sb.append(item.toString());
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    //表达式的执行
    public JsObject evaluate(Env env) throws StrException {
        //根节点value为空值，使用它的唯一子节点
        JsExpression x;
        if (value.equals("")) {
            if (child.size() == 0)  //空表达式
                return new JsObject();
            else                    //root节点
                x = child.get(0);
        } else {
            x = this;
        }
        if (x.child.size() == 0) {  //表达式已计算至顶
            try {
                Integer num = Integer.parseInt(x.value);
                return new JsNumber(num);
            } catch (Exception e) { //不是数，则是变量
                if (x.value.equals(""))  //空表达式
                    return new JsObject();
                else
                    return env.Find(x.value);
            }
        } else {
            if (x.value.equals("if")) { //if格式：cond+exp 或 cond+exp1+exp2
                JsBool cond = (JsBool) (x.child.get(0).evaluate(env));  //计算条件真假
                if (x.child.size() == 3) {
                    return cond.value ? x.child.get(1).evaluate(env) : x.child.get(2).evaluate(env);
                } else {
                    return cond.value ? x.child.get(1).evaluate(env) : new JsBool(false);
                }
            } else if (x.value.equals("while")) {   //while格式：cond+exp
                JsObject res = new JsObject();
                while (((JsBool) x.child.get(0).evaluate(env)).value) {
                    res = x.child.get(1).evaluate(env);
                }
                return res;
            } else if (x.value.equals("for")) {   //while格式：cond+exp
                x.child.get(0).evaluate(env);
                JsObject res = new JsObject();
                while (((JsBool) x.child.get(1).evaluate(env)).value) {
                    res = x.child.get(3).evaluate(env);
                    x.child.get(2).evaluate(env);
                }
                return res;
            } else if (x.value.equals("return")) {  //函数返回值
                return x.child.get(0).evaluate(env);
            } else if (x.value.equals("var")) {
                JsObject res = new JsObject();
                for (JsExpression assExp : x.child) {
                    res = env.assignVariable(assExp.child.get(0).value, assExp.child.get(1).evaluate(env));
                }
                return res;
            } else if (x.value.equals("=")) {   //给变量节点赋表达式节点的值
                Env currentEnv = env;
                while (currentEnv.outer != null) {
                    if (currentEnv.findInScope(x.child.get(0).value) != null)   //在当前域内找到
                        return currentEnv.assignVariable(x.child.get(0).value, x.child.get(1).evaluate(env));
                    currentEnv = currentEnv.outer;  //从当前层向外拓展
                }
                return currentEnv.assignVariable(x.child.get(0).value, x.child.get(1).evaluate(env));  //所有域都没找到，则直接在当前域新声明一个变量
            } else if (x.value.equals("function")) {    //函数定义节点
                if (x.child.size() == 3) {  //函数名+参数列表+函数体
                    ArrayList<String> args = x.child.get(1).child.stream().map(i -> i.value).collect(
                            ArrayList::new,
                            ArrayList::add,
                            ArrayList::addAll); //获得参数列表集合
                    JsFunction func = new JsFunction(args, x.child.get(2), null);   //创建函数
                    return env.assignVariable(x.child.get(0).value, func);                  //函数作为变量存入Env
                } else {    //匿名函数:参数列表+函数体
                    ArrayList<String> args = x.child.get(0).child.stream().map(i -> i.value).collect(
                            ArrayList::new,
                            ArrayList::add,
                            ArrayList::addAll);
                    return new JsFunction(args, x.child.get(1), null);
                }
            } else if (x.value.equals("{")) { //表达式集合，子节点依次运行
                JsObject val = null;
                for (JsExpression exp : x.child) {
                    val = exp.evaluate(env);
                }
                return val;
            } else if (Env.builtins.containsKey(x.value)) { //内建函数
                JsExpression[] args = new JsExpression[x.child.size()];
                x.child.toArray(args);
                return Env.builtins.get(x.value).apply(args, env);
            } else if (x.value.equals("string")) {
                return new JsString(x.child.get(0).value);
            } else {    //自定义函数调用
                //获得参数
                JsObject[] arguments = x.child.stream().skip(1).map(item -> {   //第一项为"args"，需要skip
                    try {
                        return item.evaluate(env);  //计算参数表达式
                    } catch (StrException e) {
                        return new JsObject();
                    }
                }).toArray(JsObject[]::new);
                //执行函数调用
                if (x.value.equals("print")) {  //print(string)为内建控制台输出函数
                    if (arguments.length != 1) {
                        throw new StrException("Wrong argument number");
                    } else {
                        System.out.println("" + arguments[0].toString());
                        return new JsObject();
                    }
                } else {
                    JsFunction func = (JsFunction) env.Find(x.value);
                    return func.UpdateArgsAndEnv(arguments, env).evaluate();
                }
            }
        }
    }
}
