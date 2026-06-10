package licenta.mihai.aicontractriskanalyzerbackend.exceptions;

import jakarta.validation.ConstraintViolationException;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.BackendErrorDto;
import licenta.mihai.aicontractriskanalyzerbackend.exceptions.BadRequestException;
import licenta.mihai.aicontractriskanalyzerbackend.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<BackendErrorDto> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new BackendErrorDto("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler({BadRequestException.class, ConstraintViolationException.class})
    public ResponseEntity<BackendErrorDto> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new BackendErrorDto("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BackendErrorDto> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new BackendErrorDto("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BackendErrorDto> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null ? "Validation failed" : fieldError.getField() + " " + fieldError.getDefaultMessage();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new BackendErrorDto("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BackendErrorDto> handleAny(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new BackendErrorDto("INTERNAL_ERROR", "Unexpected server error"));
    }
}


