package io.github.silverandro.fiwb.mixin_util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Totally mixin trust trust
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ArbitraryTransform {
}
