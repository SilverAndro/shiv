package io.github.silverandro.shiv.mixin_util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes this mixin conditionally apply
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IfMatch {
	/**
	 * The match string to check before applying this mixin
	 */
	String matchString();

	/**
	 * The message to log if this mixin will be applied
	 */
	String message();
}
