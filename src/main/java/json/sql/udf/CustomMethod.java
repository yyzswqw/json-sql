package json.sql.udf;

import java.math.BigDecimal;

public class CustomMethod {

    public static int a(Number a,Object b){
        return a.intValue();
    }

    public static int b(BigDecimal a, BigDecimal b){
        return a.add(b).intValue();
    }

}
