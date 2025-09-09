package br.com.notehub.infra.exception;

import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class ControllerAdvice {

    private record CustomResponse(String field, String message) {
        public CustomResponse(FieldError error) {
            this(error.getField(), error.getDefaultMessage());
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    private ResponseEntity<List<CustomResponse>> handleMethdArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getFieldErrors();
        List<CustomResponse> response = errors.stream().map(CustomResponse::new).toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(CustomExceptions.MissingDeviceException.class)
    private ResponseEntity<List<CustomResponse>> handleMissingDeviceException(CustomExceptions.MissingDeviceException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("Headers", "Headers", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(CustomExceptions.InvalidDeviceException.class)
    private ResponseEntity<List<CustomResponse>> handleInvalidDeviceException(CustomExceptions.InvalidDeviceException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("Headers", "Headers", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(CustomExceptions.MissingRefreshToken.class)
    private ResponseEntity<List<CustomResponse>> handleMissingRefreshTokenException(CustomExceptions.MissingRefreshToken ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("Headers", "Headers", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(CustomExceptions.InvalidRefreshTokenException.class)
    private ResponseEntity<List<CustomResponse>> handleInvalidRefreshTokenException(CustomExceptions.InvalidRefreshTokenException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("Headers", "Headers", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    private ResponseEntity<List<CustomResponse>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("parameter", "parameter", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    private ResponseEntity<List<CustomResponse>> handlePropertyReferenceException(PropertyReferenceException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("pageable", "criteria", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    private ResponseEntity<List<CustomResponse>> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("parameter", "parameter", "Parâmetro 'q' vazio."));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    private ResponseEntity<List<CustomResponse>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("tag", "tags", "Não é uma lista."));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    private ResponseEntity<List<CustomResponse>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        List<FieldError> errors = new ArrayList<>();
        switch (ex.getMessage()) {
            case "both":
                errors.add(new FieldError("user", "email", "Email já existe."));
                errors.add(new FieldError("user", "username", "Nome já existe."));
                break;
            case "email":
                errors.add(new FieldError("user", "email", "Email já existe."));
                break;
            case "username":
                errors.add(new FieldError("user", "username", "Nome já existe."));
                break;
        }
        List<CustomResponse> response = errors.stream().map(CustomResponse::new).toList();
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    private ResponseEntity<HttpStatus> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(EntityExistsException.class)
    private ResponseEntity<HttpStatus> handleEntityExistsException(EntityExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @ExceptionHandler(BadCredentialsException.class)
    private ResponseEntity<List<CustomResponse>> handleBadCredentialsException(BadCredentialsException ex) {
        List<FieldError> errors = new ArrayList<>();
        return switch (ex.getMessage()) {
            case "username" -> {
                errors.add(new FieldError("user", "username", "Nome não existe."));
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors.stream().map(CustomResponse::new).toList());
            }
            case "password" -> {
                errors.add(new FieldError("user", "password", "Senha incorreta."));
                yield ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors.stream().map(CustomResponse::new).toList());
            }
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        };
    }

    @ExceptionHandler(DisabledException.class)
    private ResponseEntity<List<CustomResponse>> handleDisabledException(DisabledException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("user", "username", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(AccessDeniedException.class)
    private ResponseEntity<List<CustomResponse>> handleAccessDeniedException(AccessDeniedException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("authorization", "access_token", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(IllegalStateException.class)
    private ResponseEntity<List<CustomResponse>> handleIllegalStateException(IllegalStateException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("note", "closed", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(JWTCreationException.class)
    private ResponseEntity<List<CustomResponse>> handleJWTCreationException(JWTCreationException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("token", "token", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(JWTDecodeException.class)
    private ResponseEntity<List<CustomResponse>> handleJWTDecodeException(JWTDecodeException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("token", "oauth2_token", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(TokenExpiredException.class)
    private ResponseEntity<List<CustomResponse>> handleTokenExpiredException(TokenExpiredException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("token", "refresh_token", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(CustomExceptions.SamePasswordException.class)
    private ResponseEntity<List<CustomResponse>> handleSamePasswordException(CustomExceptions.SamePasswordException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("user", "password", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(CustomExceptions.SameEmailExpection.class)
    private ResponseEntity<List<CustomResponse>> handleSameEmailExpection(CustomExceptions.SameEmailExpection ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("user", "email", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(CustomExceptions.SelfFollowException.class)
    private ResponseEntity<List<CustomResponse>> handleSelfFollowException(CustomExceptions.SelfFollowException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("user", "user", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(CustomExceptions.AlreadyFollowingException.class)
    private ResponseEntity<List<CustomResponse>> handleAlreadyFollowingException(CustomExceptions.AlreadyFollowingException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("user", "user", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(CustomExceptions.NotFollowingException.class)
    private ResponseEntity<List<CustomResponse>> handleNotFollowingException(CustomExceptions.NotFollowingException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("user", "user", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errors.stream().map(CustomResponse::new).toList());
    }

    @ExceptionHandler(CustomExceptions.HostNotAllowedException.class)
    private ResponseEntity<List<CustomResponse>> handleHostNotAllowedException(CustomExceptions.HostNotAllowedException ex) {
        List<FieldError> errors = new ArrayList<>();
        errors.add(new FieldError("user", "host", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors.stream().map(CustomResponse::new).toList());
    }

}