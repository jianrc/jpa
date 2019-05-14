package com.jianrc.frame.jpa;

/**
 *
 * @author jianrc
 */
public enum ColumnShip {

    /** = */
    EQUAL("="),
    /** != */
    UNEQUAL("<>"),
    /** > */
    GREATER(">"),
    
    MORE(">"),
    
    /** < */
    LESS("<"),
    /** >= */
    GREATER_EQUAL(">="),
    
    MORE_EQUAL(">="),
    /** <= */
    LESS_EQUAL("<="),
    /** LIKE */
    LIKE("LIKE"),
    /** LIKE */
    NOTLIKE("NOT LIKE"),
    /** in */
    IN("IN"),
    /** not in */
    NOTIN("NOT IN"),

    KEYWORD("LIKE");
    
    private String sign;

    private ColumnShip(String sign) {
        this.sign = sign;
    }

    public String sign() {
        return this.sign;
    }
}
