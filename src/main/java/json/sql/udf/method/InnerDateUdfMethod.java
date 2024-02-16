package json.sql.udf.method;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import json.sql.CurContextProxy;
import json.sql.annotation.*;
import json.sql.constant.Constants;
import json.sql.entity.UdfFunctionDescInfo;
import json.sql.enums.MacroEnum;
import json.sql.util.MacroParamArgsContext;
import net.minidev.json.JSONArray;

import java.util.*;
import java.util.regex.Pattern;

@UdfClass(ignoreSourceClass = true)
public class InnerDateUdfMethod {

    @UdfMethod(functionName = "toDate",desc = "将数据转换为日期")
    public static Date toDate(@UdfParam(desc = "可转换为java.util.Date的数据") Object date){
        if(ObjectUtil.isNull(date)){
            return null;
        }
        try {
            return Convert.convert(Date.class, date);
        }catch (Exception e){
            return null;
        }
    }

    @UdfMethod(functionName = "date2Ms",desc = "将日期转换为毫秒值")
    public static Long date2Ms(@UdfParam(desc = "可转换为java.util.Date的数据") Object date){
        if(ObjectUtil.isNull(date)){
            return null;
        }
        try {
            Date convert = Convert.convert(Date.class, date);
            return convert.getTime();
        }catch (Exception e){
            return null;
        }
    }

    @UdfMethod(functionName = "dateOfSecond",desc = "获取日期的秒数部分")
    public static Integer dateOfSecond(@UdfParam(desc = "可转换为java.util.Date的数据") Object date){
        if(ObjectUtil.isNull(date)){
            return null;
        }
        try {
            Date convert = Convert.convert(Date.class, date);
            return DateUtil.second(convert);
        }catch (Exception e){
            return null;
        }
    }

    @UdfMethod(functionName = "dateOfMinute",desc = "获取日期的分钟部分")
    public static Integer dateOfMinute(@UdfParam(desc = "可转换为java.util.Date的数据") Object date){
        if(ObjectUtil.isNull(date)){
            return null;
        }
        try {
            Date convert = Convert.convert(Date.class, date);
            return DateUtil.minute(convert);
        }catch (Exception e){
            return null;
        }
    }

    @UdfMethod(functionName = "dateOfHour",desc = "获取日期的日部分")
    public static Integer dateOfHour(@UdfParam(desc = "可转换为java.util.Date的数据") Object date,
                                    @UdfParam(desc = "是否24小时制") Boolean is24HourClock){
        if(ObjectUtil.isNull(date)){
            return null;
        }
        if(ObjectUtil.isNull(is24HourClock)){
            is24HourClock = true;
        }
        try {
            Date convert = Convert.convert(Date.class, date);
            return DateUtil.hour(convert,is24HourClock);
        }catch (Exception e){
            return null;
        }
    }

    @UdfMethod(functionName = "dateOfDay",desc = "获取日期的日部分")
    public static Integer dateOfDay(@UdfParam(desc = "可转换为java.util.Date的数据") Object date){
        if(ObjectUtil.isNull(date)){
            return null;
        }
        try {
            Date convert = Convert.convert(Date.class, date);
            return DateUtil.dayOfMonth(convert);
        }catch (Exception e){
            return null;
        }
    }

    @UdfMethod(functionName = "dateOfMonth",desc = "获取日期的月份部分")
    public static Integer dateOfMonth(@UdfParam(desc = "可转换为java.util.Date的数据") Object date){
        if(ObjectUtil.isNull(date)){
            return null;
        }
        try {
            Date convert = Convert.convert(Date.class, date);
            return DateUtil.month(convert);
        }catch (Exception e){
            return null;
        }
    }

    @UdfMethod(functionName = "dateOfYear",desc = "获取日期的年份部分")
    public static Integer dateOfYear(@UdfParam(desc = "可转换为java.util.Date的数据") Object date){
        if(ObjectUtil.isNull(date)){
            return null;
        }
        try {
            Date convert = Convert.convert(Date.class, date);
            return DateUtil.year(convert);
        }catch (Exception e){
            return null;
        }
    }

}
