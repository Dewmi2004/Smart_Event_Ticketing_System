package lk.ijse.event_ticketingback_end.exception;

import io.jsonwebtoken.ExpiredJwtException;
import lk.ijse.event_ticketingback_end.util.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles @Valid / @Validated failures on DTOs (e.g. @NotBlank, @NotNull, @Size)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return new ResponseEntity<>(
                new APIResponse<>(HttpStatus.BAD_REQUEST.value(), "Validation Failed", errors),
                HttpStatus.BAD_REQUEST
        );
    }

    // Handles null values passed where not expected
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<APIResponse<String>> handleNullPointerException(NullPointerException e) {
        return new ResponseEntity<>(
                new APIResponse<>(HttpStatus.BAD_REQUEST.value(), "Null values are not allowed", e.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    // Handles invalid arguments (e.g. wrong types, illegal values)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIResponse<String>> handleIllegalArgumentException(IllegalArgumentException e) {
        return new ResponseEntity<>(
                new APIResponse<>(HttpStatus.BAD_REQUEST.value(), "Invalid argument provided", e.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    // Handles price/quantity String → number parse failures in OrderServiceImpl
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<APIResponse<String>> handleNumberFormatException(NumberFormatException e) {
        return new ResponseEntity<>(
                new APIResponse<>(HttpStatus.BAD_REQUEST.value(), "Invalid number format in data", e.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    // Handles Customer / Item / Order not found (thrown as RuntimeException in services)
    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<APIResponse<String>> handleCustomerNotFoundException(EventNotFoundException e) {
        return new ResponseEntity<>(
                new APIResponse<>(HttpStatus.NOT_FOUND.value(), "Customer Not Found", e.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public APIResponse handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return new APIResponse(HttpStatus.NOT_FOUND.value(), "Username Not Found", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public APIResponse handleBadCredentialsException(BadCredentialsException ex) {
        return new APIResponse(HttpStatus.UNAUTHORIZED.value(),
                "Username or Password is incorrect", ex.getMessage());
    }

    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public APIResponse handleExpiredJwtException(ExpiredJwtException ex) {
        return new APIResponse(HttpStatus.UNAUTHORIZED.value(), "Expired Token", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public APIResponse handleRuntimeException(RuntimeException ex) {
        return new APIResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error Occurred", ex.getMessage());
    }

    // Catch-all for any unexpected exception not handled above
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<String>> handleGenericException(Exception e) {
        return new ResponseEntity<>(
                new APIResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong", e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}