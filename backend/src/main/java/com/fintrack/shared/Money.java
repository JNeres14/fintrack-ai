package com.fintrack.shared;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Digits;

/**
 * Valor monetário em DTOs: BigDecimal com no máximo 2 casas decimais e 12 inteiras (cabe em NUMERIC(14,2)).
 * O sinal fica a cargo de cada campo (por exemplo, {@code @Positive} no valor da transação).
 */
@Digits(integer = 12, fraction = 2)
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@Target({ FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface Money {

	String message() default "valor monetário inválido (use no máximo 2 casas decimais)";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
