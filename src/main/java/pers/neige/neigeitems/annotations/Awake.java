package pers.neige.neigeitems.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Awake {
    LifeCycle lifeCycle() default LifeCycle.ENABLE;

    enum LifeCycle {
        ENABLE,
        ACTIVE,
        DISABLE
    }
}
