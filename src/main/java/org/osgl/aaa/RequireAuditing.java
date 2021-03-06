package org.osgl.aaa;

import java.lang.annotation.*;

/**
 * Indicates that a method invocation shall be logged
 * @author greenlaw110@gmail.com
 * @version 1.0 23/12/2010
 */

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.METHOD)
public @interface RequireAuditing {
    /**
     * Indicates the message to be logged
     *
     * @return the auditing messsage
     */
    String value() default "";
    /**
     * Set the time of auditing log, usually it should be true
     * But in rare case when it needs to log after the method executed (e.g. constructor)
     * then it could be set to false;
     * @return `true` if it shall audit before authorization or `false` otherwise
     */
    boolean before() default true;
}
