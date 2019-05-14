package com.jianrc.frame.jpa;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;

/**
 *
 * @author jianrc
 */
@Inherited
@Documented
public @interface QuerySiftColumn {

    String value();

    ColumnShip ship() default ColumnShip.EQUAL;
}
