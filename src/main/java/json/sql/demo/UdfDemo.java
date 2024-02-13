package json.sql.demo;

import com.jayway.jsonpath.DocumentContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class UdfDemo {
    public static int a(Number a,Object b){
        return a.intValue();
    }

    public static int b(BigDecimal a, BigDecimal b){
        return a.add(b).intValue();
    }

    public static int c(Object a1,Object a2,Object a3,Object a4,Object a5,BigDecimal a, BigDecimal b){
        return a.add(b).intValue();
    }

    public static int d(Object a1, Object a2, Object a3, Object a4, DocumentContext a5){
        return 2;
    }

    public static int e(Object a1,Object a2,String ... s1){
        return 1;
    }

    public static int e1(Object a1,String[] s1,Object a2){
        return 1;
    }

    public static int e2(String[] s1,Object a1,Object a2){
        return 1;
    }

    public static int f(Object a1, Object a2, List<String> list){
        return 1;
    }

    public static int f1(Object a1, List<String> list, Object a2){
        return 1;
    }

    public static int f2( List<String> list,Object a1, Object a2){
        return 1;
    }

    public static int g(Object a1, Object a2, Map<String,String> map){
        return 1;
    }

    public static int g1(Object a1, Map<String,String> map, Object a2){
        return 1;
    }

    public static int g2( Map<String,String> map,Object a1, Object a2){
        return 1;
    }
}
