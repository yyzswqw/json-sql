package json.sql.enums;

public enum CalculateOperatorSymbolLevel {
    /**
     * 同乘除优先级
     */
    MUL_DIV,
    /**
     * 同加减优先级
     */
    ADD_SUB,
    /**
     * 所有优先级都注册
     */
    BOTH,
    /**
     * 所有优先级都不注册
     */
    NONE
}
