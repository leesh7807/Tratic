package app.leesh.tratic.shared.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class SpaForwardController {

    @GetMapping({
            "/{path:[^.]*}",
            "/**/{path:[^.]*}"
    })
    public String forward(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (isExcluded(uri)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "forward:/index.html";
    }

    private boolean isExcluded(String uri) {
        return uri.startsWith("/api/")
                || uri.equals("/api")
                || uri.startsWith("/oauth2/")
                || uri.equals("/logout")
                || uri.equals("/error");
    }
}
