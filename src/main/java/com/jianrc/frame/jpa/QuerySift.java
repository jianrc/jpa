package com.jianrc.frame.jpa;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * @author jianrc
 */
@Inherited
@Documented
@Target({TYPE})
@Retention(RUNTIME)
public @interface QuerySift {

    QuerySiftColumn[] value();

    boolean and() default true;

    /**
     *  排序字段只对 public <T> List<T> queryList(final Class<T> entityClass, final int index, final Object... params) 起作用.
     *
     * @return 排序字段名
     */
    String orderby() default "";
}
