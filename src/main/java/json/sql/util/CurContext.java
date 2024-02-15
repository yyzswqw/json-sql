package json.sql.util;

import cn.hutool.core.util.ObjectUtil;
import json.sql.CurContextProxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CurContext {

    private static final ThreadLocal<CurContextProxy> curContextProxyThreadLocal = new ThreadLocal<>();


    public static void set(CurContextProxy curContextProxy){
        curContextProxyThreadLocal.set(curContextProxy);
    }

    public static CurContextProxy getCurContext(){
        return curContextProxyThreadLocal.get();
    }

    public static void remove(){
        curContextProxyThreadLocal.remove();
    }


}
