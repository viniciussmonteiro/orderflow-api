package dev.vinicius.orderflow.shared;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail notFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({BusinessConflictException.class, DataIntegrityViolationException.class})
    public ProblemDetail conflict(Exception ex) {
        String detail = ex instanceof BusinessConflictException
                ? ex.getMessage() : "Conflito de integridade nos dados";
        return problem(HttpStatus.CONFLICT, detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Dados inválidos; verifique os campos enviados");
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(status.getReasonPhrase());
        return p;
    }
}
