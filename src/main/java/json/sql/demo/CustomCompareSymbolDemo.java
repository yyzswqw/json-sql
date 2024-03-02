package json.sql.demo;


import java.util.List;

public class CustomCompareSymbolDemo {

    public static Boolean a(int a, List<String> b){
        return a > Integer.parseInt(b.get(0));
    }

}
