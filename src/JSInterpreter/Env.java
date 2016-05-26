package JSInterpreter;

import java.util.HashMap;
import java.util.function.BiFunction;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class Env {
    public Env outer;                       //��㻷��
    public HashMap<String, JsObject> dict;  //ȫ�ֱ�������
    public static HashMap<String, BiFunction<JsExpression[], Env, JsObject>> builtins
            = new HashMap<String, BiFunction<JsExpression[], Env, JsObject>>(); //�ڽ�����

    public Env(Env outer)
    {
        this.outer = outer;
        this.dict = new HashMap<>();
    }
    //���������壩��ֵ
    public JsObject assignVariable(String name, JsObject obj)
    {
        if(dict.containsKey(name))
        {
            dict.replace(name,obj);
        }
        else
        {
            dict.put(name, obj);
        }
        return obj;
    }
    //������Ѱ�ұ���
    public JsObject findInScope(String key)
    {
        if (dict.containsKey(key))
        {
            return dict.get(key);
        }
        return null;
    }

    public JsObject Find(String key) throws MyException {
        Env env = this;
        while (env != null)
        {
            //�ӵ�ǰ����������
            if (env.dict.containsKey(key))
            {
                return env.dict.get(key);
            }
            env = env.outer;
        }
        throw new MyException(key + " is not defined");
    }
    //�����ڽ�����
    public Env Builtin(String name, BiFunction<JsExpression[], Env, JsObject> lambda)
    {
        builtins.put(name, lambda);
        return this;
    }

}
