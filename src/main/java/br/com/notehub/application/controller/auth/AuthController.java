package br.com.notehub.application.controller.auth;

import br.com.notehub.adapter.producer.MailProducer;
import br.com.notehub.application.dto.request.token.AuthChangeREQ;
import br.com.notehub.application.dto.request.token.AuthREQ;
import br.com.notehub.application.dto.request.token.OAuth2GoogleREQ;
import br.com.notehub.application.dto.request.token.OAuthGitHubREQ;
import br.com.notehub.application.dto.response.token.AuthRES;
import br.com.notehub.domain.token.TokenService;
import br.com.notehub.domain.user.UserService;
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

@RestController
@CrossOrigin(origins = {"https://notehub.com.br"})
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth Controller", description = "Endpoints for authentication and authorization")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService service;
    private final UserService userService;
    private final MailProducer producer;

    @Operation(summary = "Login user", description = "Authenticates a user with username and password.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User authenticated successfully."),
            @ApiResponse(responseCode = "401", description = "Invalid password.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Missing or invalid X-Device-Id.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Email not confirmed.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @Parameter(name = "X-Device-Id", in = ParameterIn.HEADER, required = true, schema = @Schema(format = "uuid"))
    @PostMapping("/login")
    public ResponseEntity<AuthRES> loginUser(
            HttpServletRequest request,
            @Valid @RequestBody AuthREQ dto
    ) {
        AuthRES token = service.auth(request, dto.username(), dto.password());
        return ResponseEntity.status(HttpStatus.OK).body(token);
    }

    @Operation(summary = "Login with Google", description = "Authenticates a user using Google OAuth2 token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User authenticated successfully."),
            @ApiResponse(responseCode = "400", description = "Missing or invalid X-Device-Id or Google token.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "406", description = "Email already exists.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @Parameter(name = "X-Device-Id", in = ParameterIn.HEADER, required = true, schema = @Schema(format = "uuid"))
    @PostMapping("/login/google")
    public ResponseEntity<AuthRES> loginGoogleUser(
            HttpServletRequest request,
            @Valid @RequestBody OAuth2GoogleREQ dto
    ) {
        AuthRES token = service.authWithGoogleAcc(request, dto.token());
        return ResponseEntity.status(HttpStatus.OK).body(token);
    }

    @Operation(summary = "Login with GitHub", description = "Authenticates a user using GitHub OAuth token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User authenticated successfully."),
            @ApiResponse(responseCode = "400", description = "Missing or invalid X-Device-Id or input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "406", description = "Email already exists.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @Parameter(name = "X-Device-Id", in = ParameterIn.HEADER, required = true, schema = @Schema(format = "uuid"))
    @PostMapping("/login/github")
    public ResponseEntity<AuthRES> loginGitHubUser(
            HttpServletRequest request,
            @Valid @RequestBody OAuthGitHubREQ dto
    ) {
        AuthRES token = service.authWithGitHubAcc(request, dto.code());
        return ResponseEntity.status(HttpStatus.OK).body(token);
    }

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
        AuthRES token = service.recreateToken(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @Operation(summary = "Logout user", description = "Logs out the user by invalidating the refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User logged out successfully."),
            @ApiResponse(responseCode = "400", description = "Missing or invalid X-Refresh-Token.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "Token not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @Parameter(name = "X-Refresh-Token", in = ParameterIn.HEADER, required = true, schema = @Schema(format = "uuid"))
    @DeleteMapping("/logout")
    public ResponseEntity<Void> logoutUser(
            HttpServletRequest request
    ) {
        service.logout(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Request secret key", description = "Generates a secret key for user deletion and sends it via email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Email sent successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Account not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PostMapping("/secret-key")
    public ResponseEntity<Void> requestSecretKey(
            @Valid @RequestBody AuthChangeREQ dto
    ) {
        String secretKey = service.generateSecretKey(dto.email());
        userService.changePassword(dto.email(), secretKey);
        producer.publishAccountSecretKeyGenerationMessage(dto.email(), secretKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
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

}