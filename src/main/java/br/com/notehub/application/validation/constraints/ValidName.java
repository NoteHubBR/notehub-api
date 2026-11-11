package br.com.notehub.application.validation.constraints;

import br.com.notehub.application.validation.NameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NameValidator.class)
public @interface ValidName {

    String message() default "Nome inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}