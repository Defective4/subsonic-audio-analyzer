package io.github.defective4.audioanalyzer.app.option;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
public @interface EnvironmentVariable {
    boolean sensitive() default false;
    String value();
}
