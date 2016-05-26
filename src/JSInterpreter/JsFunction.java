package JSInterpreter;

import java.util.ArrayList;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class JsFunction extends JsObject {
    public ArrayList<String> args;  //�����б�
    public JsExpression body;       //������
    public Env env;                 //�������л���
    public boolean hasReturnValue;  //�Ƿ��з���ֵ

    public JsFunction(ArrayList<String> args, JsExpression body, Env env) {
        this.args = args;
        this.body = body;
        this.env = env;
        hasReturnValue = checkReturn(body);
    }

    public static boolean checkReturn(JsExpression body) {
        if (body.value.equals("return"))
            return true;
        for (JsExpression subExpression : body.child) {
            if (checkReturn(subExpression))
                return true;
        }
        return false;
    }

    public JsFunction UpdateArgsAndEnv(JsObject[] parameters, Env env) throws MyException {
        if (args.size() != parameters.length)
            throw new MyException("Function arguments do not match");
        Env newEnv = new Env(env);
        for (int i = 0; i < parameters.length; i++) {
            newEnv.assignVariable(this.args.get(i), parameters[i]);
        }
        return new JsFunction(args, body, newEnv);
    }

    //������ִ��
    public JsObject evaluate() throws MyException {
        if (hasReturnValue)
            return this.body.evaluate(env);
        else
            this.body.evaluate(env);
            return new JsObject();
    }

    public String toString() {
        StringBuilder arguments = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            arguments.append(args.get(i));
            if (i < args.size() - 1) {
                arguments.append(",");
            }
        }
        return "function (" + arguments.toString() + "){..}";
    }
}
