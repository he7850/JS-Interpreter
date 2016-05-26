package JSInterpreter;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class MyException extends Exception {

    String errorMessage;

    public MyException(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String toString() {
        return errorMessage;
    }

    public String getMessage() {
        return errorMessage;
    }

}
