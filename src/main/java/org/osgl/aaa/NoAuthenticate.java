package org.osgl.aaa;

import java.lang.annotation.*;

/**
 * Indicate that the annotated method is not subject to Secure authentication
 * @deprecated use {@link NoAuthentication} instead
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
@Deprecated
public @interface NoAuthenticate {
}
