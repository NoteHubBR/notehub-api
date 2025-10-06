package br.com.notehub.infra.exception;

import java.util.UUID;

public class CustomExceptions {

    public static abstract class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
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

}