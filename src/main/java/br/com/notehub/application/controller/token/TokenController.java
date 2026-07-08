package br.com.notehub.application.controller.token;

import br.com.notehub.adapter.producer.MailProducer;
import br.com.notehub.application.dto.request.token.AuthChangeREQ;
import br.com.notehub.application.dto.request.token.AuthSessionsREQ;
import br.com.notehub.application.dto.response.auth.AuthRES;
import br.com.notehub.application.dto.response.auth.SessionRES;
import br.com.notehub.domain.token.Token;
import br.com.notehub.domain.token.TokenService;
import com.auth0.jwt.JWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tokens")
@Tag(name = "Token Controller", description = "Endpoints for authorization")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService service;
    private final MailProducer producer;

    @Operation(summary = "Refresh token", description = "Generates a new access token using a refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "New access token created."),
            @ApiResponse(responseCode = "400", description = "Missing or invalid X-Device-Id or X-Refresh-Token.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Token not found, please log in again.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @Parameters(value = {
            @Parameter(name = "X-Device-Id", in = ParameterIn.HEADER, required = true, schema = @Schema(format = "uuid")),
            @Parameter(name = "X-Refresh-Token", in = ParameterIn.HEADER, required = true, schema = @Schema(format = "uuid"))
    })
    @GetMapping("/refresh")
    public ResponseEntity<AuthRES> refreshToken(
            HttpServletRequest request
    ) {
        AuthRES token = service.recreateSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @Operation(summary = "Request user password change", description = "Generates a token for password change and sends it via email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Email sent successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Account not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PostMapping("/change-password")
    public ResponseEntity<Void> requestPasswordChange(
            @Valid @RequestBody AuthChangeREQ dto
    ) {
        String jwt = service.generatePasswordChangeToken(dto.email());
        producer.publishAccountPasswordChangeMessage(dto.email(), jwt);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Request user email change", description = "Generates a token for email change and sends it via email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Email sent successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Account not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PostMapping("/change-email")
    public ResponseEntity<Void> requestEmailChange(
            @Valid @RequestBody AuthChangeREQ dto
    ) {
        String jwt = service.generateEmailChangeToken(dto.email());
        producer.publishAccountEmailChangeMessage(dto.email(), jwt);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(
            summary = "Fetch all account sessions",
            description = "Get a full description of all connections across differents devices.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sessions retrieves successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid token.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Null token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PostMapping("/sessions")
    public ResponseEntity<List<SessionRES>> findAllSessions(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody AuthSessionsREQ dto
    ) {
        UUID idFromToken = accessToken != null
                ? UUID.fromString(JWT.decode(accessToken.replace("Bearer ", "")).getSubject())
                : null;
        List<Token> tokens = service.getAllSessions(idFromToken, dto.password());
        return ResponseEntity.status(HttpStatus.OK).body(tokens.stream().map(SessionRES::new).toList());
    }

    @Operation(
            summary = "Disconnect session",
            description = "Disconnect a session via token id. The session ID is only exposed after a password-verified fetch via POST /sessions, ensuring that only authenticated users with knowledge of their own sessions can perform this action."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Session disconnected successfully."),
            @ApiResponse(responseCode = "404", description = "Session not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @DeleteMapping("/session/{id}")
    public ResponseEntity<Void> disconnectSession(
            @PathVariable("id") UUID id
    ) {
        service.disconnect(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}