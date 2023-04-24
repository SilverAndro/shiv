package io.github.silverandro.shiv.mixin_util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows replacing a mixin injection
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MixinReplace {
	/**
	 * The fully qualified name of the mixin the injection to replace is from
	 */
	String origin();

	/**
	 * The name of the injection to replace
	 */
	String name();
}
