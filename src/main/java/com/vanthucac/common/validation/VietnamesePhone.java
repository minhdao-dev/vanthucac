package com.vanthucac.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VietnamesePhoneValidator.class)
public @interface VietnamesePhone {
    String message() default "Invalid Vietnamese phone number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
