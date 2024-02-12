package json.sql.udf;

import com.jayway.jsonpath.DocumentContext;

import java.math.BigDecimal;

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
}
