package br.com.notehub.infra.exception;

import com.stripe.exception.StripeException;
import lombok.Getter;

import java.util.UUID;

public class CustomExceptions {

    public static abstract class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }

    public static class CustomStripeException extends BusinessException {

        private static String customizeStripeExceptionMessage(StripeException e) {
            String code = e.getCode();
            String message = e.getMessage();
            if ("amount_too_small".equals(code)) return "Minimum value not reached.";
            if (message.contains("currency")) return "Currency not available.";
            return "Error while processing payment. Please, try again later or contact support.";
        }

        public CustomStripeException(StripeException e) {
            super(customizeStripeExceptionMessage(e));
        }

    }

    public static class InvalidSecretException extends BusinessException {
        public InvalidSecretException() {
            super("Segredo incorreto.");
        }
    }

    public static class MissingDeviceException extends BusinessException {
        public MissingDeviceException() {
            super(String.format("X-Device-Id faltando, use %s no lugar.", UUID.randomUUID()));
        }
    }

    public static class InvalidDeviceException extends BusinessException {
        public InvalidDeviceException() {
            super(String.format("X-Device-Id errado, use %s no lugar.", UUID.randomUUID()));
        }
    }

    public static class MissingRefreshToken extends BusinessException {
        public MissingRefreshToken() {
            super("X-Refresh-Token faltando.");
        }
    }

    public static class InvalidRefreshTokenException extends BusinessException {
        public InvalidRefreshTokenException() {
            super("X-Refresh-Token errado.");
        }
    }

    public static class ScopeNotAllowedException extends BusinessException {
        public ScopeNotAllowedException() {
            super("Escopo não autorizado.");
        }
    }

    public static class SamePasswordException extends BusinessException {
        public SamePasswordException() {
            super("Senha atual.");
        }
    }

    public static class SameEmailExpection extends BusinessException {
        public SameEmailExpection() {
            super("Email atual.");
        }
    }

    public static class SelfFollowException extends BusinessException {
        public SelfFollowException() {
            super("Carência é foda.");
        }
    }

    public static class AlreadyFollowingException extends BusinessException {
        public AlreadyFollowingException() {
            super("Você já segue.");
        }
    }

    public static class NotFollowingException extends BusinessException {
        public NotFollowingException() {
            super("Você já não segue.");
        }
    }

    public static class HostNotAllowedException extends BusinessException {
        public HostNotAllowedException() {
            super("Host não autorizado.");
        }
    }

    public static class UserBlockedException extends BusinessException {
        public UserBlockedException(String message) {
            super(message);
        }
    }

    public static class SubscriptionException extends BusinessException {
        public SubscriptionException(String message) {
            super(message);
        }
    }

    @Getter
    public static class GifNotAllowedException extends BusinessException {

        private final String field;

        public GifNotAllowedException(String field, String message) {
            super(message);
            this.field = field;
        }

    }

}