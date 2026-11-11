package br.com.notehub.application.validation;

import br.com.notehub.application.validation.constraints.ValidName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NameValidator implements ConstraintValidator<ValidName, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        String message = null;
        if (value.matches(".*\\s.*")) message = "Não use espaços.";
        else if (!value.matches("[a-zA-Z0-9_.-]+")) message = "Apenas letras, números, ., _ ou -.";
        else if (!value.matches("^[a-zA-Z0-9].*[a-zA-Z0-9]$|^[a-zA-Z0-9]$")) message = "Sem símbolos no início ou fim.";
        else if (value.matches(".*[._-]{2,}.*")) message = "Não use símbolos consecutivos.";
        if (message != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
            return false;
        }
        return true;
    }

}