package app.leesh.tratic.auth.controller;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ErrorPageController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public ErrorPageController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public ResponseEntity<ProblemDetail> error(@NonNull HttpServletRequest request) {
        WebRequest webRequest = new ServletWebRequest(request);
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(webRequest,
                ErrorAttributeOptions.defaults());

        int statusCode = Optional.ofNullable(attributes.get("status"))
                .map(Object::toString)
                .flatMap(ErrorPageController::parseInt)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR.value());

        HttpStatus status = HttpStatus.valueOf(statusCode);

        String detail = Optional.ofNullable(attributes.get("message"))
                .map(Object::toString)
                .filter(message -> !message.isBlank())
                .orElse(null);

        String path = Optional.ofNullable(attributes.get("path"))
                .map(Object::toString)
                .orElse(null);

        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(status.getReasonPhrase());
        if (detail != null) {
            problem.setDetail(detail);
        }
        if (path != null && !path.isBlank()) {
            problem.setInstance(URI.create(path));
        }

        String traceId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .filter(value -> !value.isBlank())
                .orElse(null);
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(problem);
    }

    private static Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
