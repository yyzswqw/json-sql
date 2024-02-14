package json.sql.util;

import cn.hutool.core.util.ObjectUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MacroParamArgsContext {

    private static final ThreadLocal<List<Object>> macroParamArgsThreadLocal = new ThreadLocal<>();


    public static void setMacroParamArgs(List<Object> paramArgs){
        macroParamArgsThreadLocal.set(paramArgs);
    }

    public static List<Object> getMacroParamArgs(){
        return macroParamArgsThreadLocal.get();
    }

    public static void addMacroParamArgs(Collection<?> paramArgsList){
        if(ObjectUtil.isNotEmpty(paramArgsList)){

            List<Object> macroParamArgs = getMacroParamArgs();
            if(ObjectUtil.isEmpty(macroParamArgs)){
                setMacroParamArgs(new ArrayList<>());
            }

            for (Object paramArg : paramArgsList) {
                getMacroParamArgs().add(paramArg);
            }
        }
    }

    /**
     * 一旦设置了值，务必调用该方法删除
     */
    public static void remove(){
        macroParamArgsThreadLocal.remove();
    }


}
