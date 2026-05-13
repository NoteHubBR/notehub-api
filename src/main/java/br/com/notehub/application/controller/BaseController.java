package br.com.notehub.application.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Hidden
public class BaseController {

    @Value("${api.server.host}")
    private String host;

    @GetMapping
    public RedirectView redirectToDocs() {
        return new RedirectView(String.format("%s/docs", host));
    }

    @GetMapping("/docs")
    public RedirectView redirecToSwaggerUI() {
        return new RedirectView(String.format("%s/swagger-ui.html", host));
    }

    @GetMapping("/api/v1/health")
    public ResponseEntity<Void> checkHealth() {
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}