# JS-Interpreter

A simple js interpreter.  
本项目为简易版 JavaScript 解释器，并没有支持全部 JavaScript 语句。  
目前支持运算符号 + - * / % ，支持关系运算符 \> < == >= <= != ，支持赋值符号 = 。    
支持的逻辑语句包括 if-else 判断语句、 for 循环语句、 while 循环语句，但结果代码段必须用 “{}” 包括进来。    
支持递归函数的定义， 内建打印函数 print()。      

e.g.  
----JsInterpreter Interpreter----  
\>\>\>a=1;  
\>\>\>print("a="+a);  
a=1  
\>\>\>a="JSInterpreter";  
\>\>\>print(a);  
JSInterpreter  
\>\>\>var i = 0;  
\>\>\>while(i<10){  
...    print(i);  
...    i=i+1;  
...}  
0  
1  
2  
3  
4  
5  
6  
7  
8  
9  
\>\>\>if(i==10){  
...    print("i equals 10.");  
...}else{  
...    print("i is not equal to 10.");  
...}  
i equals 10.  
\>\>\>function f(x){  
...    if(x>0){  
...        return x*f(x-1);  
...    }else{  
...        return 1;  
...    }  
...}  
\>\>\>for(i=0;i<10;i=i+1){  
...    print(i+"!=="+f(i));  
...}  
0!=1  
1!=1  
2!=2  
3!=6  
4!=24  
5!=120  
6!=720  
7!=5040  
8!=40320  
9!=362880  
\>\>\>a=;  
=∧ expect an identifier  
\>\>\>  
  
