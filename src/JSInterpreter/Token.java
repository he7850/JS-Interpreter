package JSInterpreter;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class Token {
    enum TokenType {    //����token�����п�������
        NUMBER, STRING, IDENTIFY,  //��������
        OpenBracket, CloseBracket, /*[*/
        OpenBrace, CloseBrace, /*{*/
        OpenParenthese, CloseParenthese, /*(*/
        FOR, WHILE, FUNCTION, IF, ELSE, VAR, RETURN, NEW, //�߼�����
        ADD, SUB, MUL, DIV, MOD,//�Ӽ��˳�
        COLON/*:*/, EQ/*==*/, BIND/*=*/, SemiColon/*;*/, COMMA/*;*/,
        GT, LT, GE, LE, //�ȽϷ���
        AND, OR, UNEQ, NOT //�߼������&|=!
    }

    public TokenType type;
    public String name;

    public Token(TokenType t, String n) {
        type = t;
        name = n;
    }
}
