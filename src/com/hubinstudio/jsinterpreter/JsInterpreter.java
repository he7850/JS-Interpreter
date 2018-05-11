package com.hubinstudio.jsinterpreter;

import com.hubinstudio.jsinterpreter.types.JsBool;
import com.hubinstudio.jsinterpreter.types.JsNumber;
import com.hubinstudio.jsinterpreter.types.JsObject;
import com.hubinstudio.jsinterpreter.types.JsString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.IntFunction;


/**
 * Created by HUBIN on 2016/1/6.
 */
public class JsInterpreter {

    //将代码解析为表达式树
    public static JsExpression Parse(String code) throws StrException {
        List<Token> tokenList = Tokenizer.Tokenize(code);    //句词分解
        JsExpression program = SyntaxTreeParser.ParseProgram(tokenList);
        return program;
    }

    //判断表达式的bool值
    public static JsBool BoolEval(JsExpression[] args, Env env, BiFunction<JsNumber, JsNumber, Boolean> rel) throws StrException {
        StrException.TrueOrThrows((args.length > 1), "Too less arguments are in relation expressions");
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
    public static void startConsole(Env env) {
        System.out.println("----JsInterpreter Interpreter----");
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
                    } catch (StrException e) {
                        //e.printStackTrace();
                        System.out.println(e.errorMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void initEnv(Env env){
        env.Builtin("+", (JsExpression[] argArray, Env scope) -> {
            ArrayList<JsExpression> argList = new ArrayList<>();
            Collections.addAll(argList, argArray);
            final boolean[] stringFlag = {false};
            JsObject[] objs = argList.stream().map(obj -> {
                try {
                    JsObject result = obj.evaluate(scope);
                    if (result.getClass().getSimpleName().equals(JsString.class.getSimpleName()))
                        stringFlag[0] = true;
                    return result;
                } catch (StrException e) {
                    e.printStackTrace();
                    return null;
                }
            }).toArray((IntFunction<JsObject[]>) JsObject[]::new);
            if (!stringFlag[0]) {
                JsNumber sum = new JsNumber(0);
                for (JsObject item : objs) {
                    sum.value += ((JsNumber) item).value;
                }
                return sum;
            } else {
                JsString sum = new JsString("");
                for (JsObject item : objs) {
                    sum.value += (item.toJsString()).value;
                }
                return sum;
            }
        });
        env.Builtin("-", (argArray, scope) -> {
            if (!argArray[0].getClass().getSimpleName().equals(JsNumber.class.getSimpleName())
                    && argArray[1].getClass().getSimpleName().equals(JsNumber.class.getSimpleName())) {
                try {
                    throw new StrException("String type cannot be used in '-'");
                } catch (StrException e) {
                    System.out.println(e.errorMessage);
                }
            }
            ArrayList<JsExpression> argList = new ArrayList<>();
            Collections.addAll(argList, argArray);
            JsNumber[] numbers = argList.stream().map(obj -> {
                try {
                    return obj.evaluate(scope);
                } catch (StrException e) {
                    System.out.println(e.errorMessage);
                    return null;
                }
            }).toArray((IntFunction<JsNumber[]>) JsNumber[]::new);
            if (numbers.length == 1) {
                return new JsNumber(-numbers[0].value);
            } else {
                int res = numbers[0].value;
                for (int i = 1; i < numbers.length; i++) {
                    res -= numbers[i].value;
                }
                return new JsNumber(res);
            }
        });
        env.Builtin("*", (argArray, scope) -> {
            ArrayList<JsExpression> argList = new ArrayList<>();
            Collections.addAll(argList, argArray);
            JsNumber[] numbers = argList.stream().map(obj -> {
                try {
                    return obj.evaluate(scope);
                } catch (StrException e) {
                    e.printStackTrace();
                    return null;
                }
            }).toArray(JsNumber[]::new);
            int result = 1;
            for (JsNumber num : numbers) {
                result = result * num.value;
            }
            return new JsNumber(result);
        });
        env.Builtin("/", (argArray, scope) -> {
            // convert array to ArrayList
            List<JsExpression> argList = Arrays.asList(argArray);
//            List<JsExpression> argList = new ArrayList<>();
//            Collections.addAll(argList, argArray);
            // map JsExpression to evaluated result
            JsNumber[] numbers = argList.stream().map(obj -> {
                try {
                    return obj.evaluate(scope);
                } catch (StrException e) {
                    e.printStackTrace();
                    return null;
                }
            }).toArray((IntFunction<JsNumber[]>) JsNumber[]::new);
            int result = numbers[0].value;
            for (int i = 1; i < numbers.length; i++) {
                BiFunction<Integer, Integer, Integer> divide = (a, b) -> (a / b);
                result = divide.apply(result, numbers[i].value);
            }
            return new JsNumber(result);
        });
        env.Builtin("%", (argArray, scope) -> {
            ArrayList<JsExpression> argList = new ArrayList<>();
            Collections.addAll(argList, argArray);
            JsNumber[] numbers = argList.stream().map(obj -> {
                try {
                    return obj.evaluate(scope);
                } catch (StrException e) {
                    e.printStackTrace();
                    return null;
                }
            }).toArray((IntFunction<JsNumber[]>) JsNumber[]::new);
            int result = numbers[0].value;
            for (int i = 1; i < numbers.length; i++) {
                result = result % numbers[i].value;
            }
            return new JsNumber(result);
        });
        env.Builtin("==", (argArray, scope) ->
        {
            try {
                return BoolEval(argArray, scope, (s1, s2) -> s1.value == s2.value);
            } catch (StrException e) {
                e.printStackTrace();
                return null;
            }
        });
        env.Builtin(">", (argArray, scope) ->
        {
            try {
                return BoolEval(argArray, scope, (s1, s2) -> s1.value > s2.value);
            } catch (StrException e) {
                e.printStackTrace();
                return null;
            }
        });
        env.Builtin("<", (argArray, scope) ->
        {
            try {
                return BoolEval(argArray, scope, (s1, s2) -> s1.value < s2.value);
            } catch (StrException e) {
                e.printStackTrace();
                return null;
            }
        });
        env.Builtin(">=", (argArray, scope) ->
        {
            try {
                return BoolEval(argArray, scope, (s1, s2) -> s1.value >= s2.value);
            } catch (StrException e) {
                e.printStackTrace();
                return null;
            }
        });
        env.Builtin("<=", (argArray, scope) ->
        {
            try {
                return BoolEval(argArray, scope, (s1, s2) -> s1.value <= s2.value);
            } catch (StrException e) {
                e.printStackTrace();
                return null;
            }
        });
        env.Builtin("!=", (argArray, scope) ->
        {
            try {
                return BoolEval(argArray, scope, (s1, s2) -> s1.value != s2.value);
            } catch (StrException e) {
                e.printStackTrace();
                return null;
            }
        });
        env.Builtin("Object", (argArray, scope) ->
                new JsObject());
    }

    public static void main(String[] args) {
        //String code = "function(a){\n\ra=10;\nvar b=3*(2+5)+6;\nif(a==10)\n{\t  alert('a');var c=function(){return null;};}return b;}";

        Tokenizer.initTokenizer();
        Env root = new Env(null);
        initEnv(root);
        startConsole(root);
    }
}
