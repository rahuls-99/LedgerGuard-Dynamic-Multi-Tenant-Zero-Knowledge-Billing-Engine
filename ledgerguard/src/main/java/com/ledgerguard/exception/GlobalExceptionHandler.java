package com.ledgerguard.exception;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Object> handleApiException(ApiException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("code", ex.getCode());
    body.put("message", ex.getMessage());
    return ResponseEntity.status(ex.getStatus()).body(body);
  }
  @ExceptionHandler(InvalidIdempotencyKeyException.class)
  public ResponseEntity<Object> handleInvalidKey(InvalidIdempotencyKeyException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("error", "INVALID_IDEMPOTENCY_KEY");
    body.put("message", ex.getMessage() != null ? ex.getMessage() : "Invalid Idempotency-Key header.");
    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }
  @ExceptionHandler({DuplicateRequestInProgressException.class, DataIntegrityViolationException.class})
  public ResponseEntity<Object> handleDuplicateRequest(Exception ex) {
    String message = (ex instanceof DuplicateRequestInProgressException && ex.getMessage() != null) ? ex.getMessage() : "Duplicate request detected or currently in progress.";
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("error", "DUPLICATE_REQUEST");
    body.put("message", message);
    return new ResponseEntity<>(body, HttpStatus.CONFLICT);
  }
}