package com.jianrc.frame.jpa;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *  只能注释给查询条件bean中的public getter方法
 *
 * @author jianrc
 */
@Inherited
@Documented
@Target({METHOD})
@Retention(RUNTIME)
public @interface SiftColumn {

    /**
     * JPQL中对应查询条件的字段名
     * @return
     */
    String jpaname();
    
    String[] keywordNames() default {};

    /**
     *  只用于字段为数字型getter方法
     *
     * @return int
     */
    int least() default 1;

    /**
     *  当值大于0的时候， 字段条件以?1 ?2拼凑， 否则拼在sql语句中
     *
     * @return
     */
    int paramidx() default 0;

    /**
     *  方法的作用是否是用于生成where的JPA SQL语句
     *
     * @return
     */
    boolean gensql() default false;

    /**
     * 条件类型
     * @return
     */
    ColumnShip ship() default ColumnShip.EQUAL;
}
