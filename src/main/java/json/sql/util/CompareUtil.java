package json.sql.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class CompareUtil {

    private CompareUtil(){

    }

    /**
     * 比较两个值是否满足比较符
     * @param left 左值
     * @param operator 比较符
     * @param right 右值
     * @return 满足比较符，返回true，否则，返回false
     */
    public static boolean compareValues(Object left, String operator, Object right) {
        if (left == null || right == null) {
            return false;
        }
        switch(operator) {
            case "=":
                Integer flag = compareValue(left, right);
                return flag != null && flag == 0;
            case "<>":
            case "!=":
                Integer flag1 = compareValue(left, right);
                return flag1 != null && flag1 != 0;
            case ">":
                Integer flag2 = compareValue(left, right);
                return flag2 != null && flag2 > 0;
            case ">=":
                Integer flag3 = compareValue(left, right);
                return flag3 != null && flag3 >= 0;
            case "<":
                Integer flag4 = compareValue(left, right);
                return flag4 != null && flag4 < 0;
            case "<=":
                Integer flag5 = compareValue(left, right);
                return flag5 != null && flag5 <= 0;
            default:
                return false;
        }
    }

    /**
     * 比较两个值的大小，按照时间、数字、boolean、字符串的优先级顺序比较
     * @param left 左值
     * @param right 右值
     * @return 左值大 返回正树，相等 返回 0 ,左值小 返回负数,不可比较返回 null
     */
    public static Integer compareValue(Object left, Object right) {
        // 集合类型不可比较
        if(left.getClass().isArray() || Map.class.isAssignableFrom(left.getClass()) || Collection.class.isAssignableFrom(left.getClass())){
            return null;
        }
        if(right.getClass().isArray() || Map.class.isAssignableFrom(right.getClass()) || Collection.class.isAssignableFrom(right.getClass())){
            return null;
        }
        // 先尝试时间比较
        try {
            // 是时间类型或者是字符串类型，才尝试使用时间比较
            if((left instanceof Date || left instanceof CharSequence)
                    && (right instanceof Date || right instanceof CharSequence)){
                Date d1 = Convert.convert(Date.class, left);
                Date d2 = Convert.convert(Date.class, right);
                return d1.compareTo(d2);
            }else{
                // 尝试直接转时间
                Date d1 = Convert.convert(Date.class, left);
                Date d2 = Convert.convert(Date.class, right);
                return d1.compareTo(d2);
            }
        }catch (Exception e){}

        // 时间不能比较，则尝试数字比较
        try {
            // 其中一个是数字，才尝试比较
            if(left instanceof Number || right instanceof Number){
                return compareNumbers(left,right);
            }
            throw new RuntimeException("number类型不正确，不能比较");
        }catch (Exception e){}
        // 数字不能比较，则尝试boolean比较
        try {
            // 两个都能转成boolean，才比较
            Boolean b1 = null;
            Boolean b2 = null;
            if (Boolean.TRUE.toString().equalsIgnoreCase(left.toString()) || Boolean.FALSE.toString().equalsIgnoreCase(left.toString())) {
                b1 = Boolean.parseBoolean(left.toString());
            }
            if (Boolean.TRUE.toString().equalsIgnoreCase(right.toString()) || Boolean.FALSE.toString().equalsIgnoreCase(right.toString())) {
                b2 = Boolean.parseBoolean(right.toString());
            }
            if(ObjectUtil.hasEmpty(b1,b2)){
                throw new RuntimeException("不可比较");
            }
            if(Boolean.TRUE.equals(b1) && Boolean.TRUE.equals(b2)){
                return 0;
            }
            if(Boolean.TRUE.equals(b1) && Boolean.FALSE.equals(b2)){
                return 1;
            }
            if(Boolean.FALSE.equals(b1) && Boolean.TRUE.equals(b2)){
                return -1;
            }
            if(Boolean.FALSE.equals(b1) && Boolean.FALSE.equals(b2)){
                return 0;
            }
        }catch (Exception e){}
        // 还不能比较，按照字符串比较
        try {
            String s1 =  Convert.convert(String.class, left);
            String s2 =  Convert.convert(String.class, right);
            return s1.compareTo(s2);
        }catch (Exception e){}
        return null;
    }

    /**
     * 比较两个数字的大小
     * @param left 左值
     * @param right 右值
     * @return 左值大 返回正树，相等 返回 0 ,左值小 返回负数
     * @throws NumberFormatException NumberFormatException
     */
    public static int compareNumbers(Object left, Object right) throws NumberFormatException {
        if (left instanceof Integer && right instanceof Integer) {
            return ((Integer) left).compareTo((Integer) right);
        }

        if (left instanceof Double && right instanceof Double) {
            return ((Double) left).compareTo((Double) right);
        }

        if (left instanceof Integer && right instanceof Double) {
            return Double.compare((Integer) left, (Double) right);
        }

        if (left instanceof Double && right instanceof Integer) {
            return Double.compare((Double) left, (Integer) right);
        }
        if (left instanceof BigDecimal && right instanceof BigDecimal) {
            BigDecimal v1 = (BigDecimal)left;
            BigDecimal v2 = (BigDecimal)right;
            return v1.compareTo(v2);
        }
        BigDecimal v1 = new BigDecimal(left.toString());
        BigDecimal v2 = new BigDecimal(right.toString());
        return v1.compareTo(v2);
    }
}
