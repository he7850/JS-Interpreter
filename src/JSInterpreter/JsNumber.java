package JSInterpreter;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class JsNumber extends JsObject{
    public int value;
    public JsNumber(int v)
    {
        value = v;
    }
    public String toString(){
        return String.valueOf(value);
    }
    public JsString toJsString(){
        return new JsString(this);
    }
}
