package JSInterpreter;

/**
 * Created by HUBIN on 2016/1/8.
 */
public class JsString extends JsObject {
    String value;

    public JsString(String value) {
        this.value = value;
    }

    public JsString(JsNumber jsNumber) {
        this.value = jsNumber.toString();
    }

    public String toString() {
        return value;
    }

    public JsString toJsString(){
        return this;
    }
}
