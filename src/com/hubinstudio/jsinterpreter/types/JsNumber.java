package com.hubinstudio.jsinterpreter.types;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class JsNumber extends JsObject {
    public static int INT=0,DOUBLE=1;
    public int type=INT;
    public int value;
    public double doubleValue;

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
