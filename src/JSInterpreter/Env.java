package JSInterpreter;

import java.util.HashMap;
import java.util.function.BiFunction;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class Env {
    public Env outer;                       //外层环境
    public HashMap<String, JsObject> dict;  //全局变量集合
    public static HashMap<String, BiFunction<JsExpression[], Env, JsObject>> builtins
            = new HashMap<String, BiFunction<JsExpression[], Env, JsObject>>(); //内建函数

    public Env(Env outer)
    {
        this.outer = outer;
        this.dict = new HashMap<>();
    }
    //变量（定义）赋值
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
    //在域内寻找变量
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
            //从当前域向外搜索
            if (env.dict.containsKey(key))
            {
                return env.dict.get(key);
            }
            env = env.outer;
        }
        throw new MyException(key + " is not defined");
    }
    //新增内建函数
    public Env Builtin(String name, BiFunction<JsExpression[], Env, JsObject> lambda)
    {
        builtins.put(name, lambda);
        return this;
    }

}
