package com.newbank.microservices.genai;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * #11: RFC 7807 problem details for every failure mode. Validation, auth,
 * rate-limit, guardrail and downstream errors map to a proper status —
 * never a 200 with an error string (the old {@code exchange} fallback is gone).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        pd.setDetail(ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b).orElse("invalid request"));
        return pd;
    }

    @ExceptionHandler(NewBankChatClient.RateLimitedException.class)
    ProblemDetail rateLimited(NewBankChatClient.RateLimitedException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        pd.setTitle("Rate limit exceeded");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(Guardrails.GuardrailException.class)
    ProblemDetail guardrail(Guardrails.GuardrailException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setTitle("Blocked by policy");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(BankTools.ConfirmationRequiredException.class)
    ProblemDetail confirmation(BankTools.ConfirmationRequiredException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Confirmation required");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(AIDataProvider.DownstreamUnavailableException.class)
    ProblemDetail downstream(AIDataProvider.DownstreamUnavailableException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setTitle("Downstream service unavailable");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail denied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Forbidden");
        pd.setDetail("Insufficient role for this operation.");
        return pd;
    }
}
