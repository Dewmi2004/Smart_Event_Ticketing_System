package lk.ijse.event_ticketingback_end.exception;

import lk.ijse.event_ticketingback_end.util.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

//    @ExceptionHandler(ItemNotFoundException.class)
//    public ResponseEntity<APIResponse<String>> handleItemNotFoundException(ItemNotFoundException e) {
//        return new ResponseEntity<>(
//                new APIResponse<>(HttpStatus.NOT_FOUND.value(), "Item Not Found", e.getMessage()),
//                HttpStatus.NOT_FOUND
//        );
//    }
//
//    @ExceptionHandler(OrderNotFoundException.class)
//    public ResponseEntity<APIResponse<String>> handleOrderNotFoundException(OrderNotFoundException e) {
//        return new ResponseEntity<>(
//                new APIResponse<>(HttpStatus.NOT_FOUND.value(), "Order Not Found", e.getMessage()),
//                HttpStatus.NOT_FOUND
//        );
//    }
//
//    // Handles not enough stock when placing an order
//    @ExceptionHandler(InsufficientStockException.class)
//    public ResponseEntity<APIResponse<String>> handleInsufficientStockException(InsufficientStockException e) {
//        return new ResponseEntity<>(
//                new APIResponse<>(HttpStatus.CONFLICT.value(), "Insufficient Stock", e.getMessage()),
//                HttpStatus.CONFLICT
//        );
//    }

    // Catch-all for any unexpected exception not handled above
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<String>> handleGenericException(Exception e) {
        return new ResponseEntity<>(
                new APIResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong", e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}