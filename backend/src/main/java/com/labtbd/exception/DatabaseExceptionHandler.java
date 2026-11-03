package com.labtbd.exception;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DatabaseExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(DatabaseExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<DatabaseError> handleIntegrityViolation(
            DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new DatabaseError("La operación viola una restricción de integridad"));
    }

    @ExceptionHandler(UncategorizedSQLException.class)
    public ResponseEntity<DatabaseError> handle(UncategorizedSQLException exception) {
        SQLException sqlException = exception.getSQLException();
        if ("P0001".equals(sqlException.getSQLState())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new DatabaseError(sqlException.getMessage()));
        }

        log.error("Unexpected database error (SQLState {})", sqlException.getSQLState(), exception);
        return ResponseEntity.internalServerError()
                .body(new DatabaseError("Error interno al procesar la operación"));
    }

    public record DatabaseError(String message) {}
}
