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
public @interface UniqueSifts {

    UniqueSift[] value();
}
