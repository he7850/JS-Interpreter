package com.hubinstudio.jsinterpreter.types;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class JsBool extends JsObject {
    public boolean value;
    public static JsBool True = new JsBool(true);
    public static JsBool False = new JsBool(false);
    public JsBool(boolean v)
    {
        this.value = v;
    }
    public String toString()
    {
        return String.valueOf(value);
    }
}
