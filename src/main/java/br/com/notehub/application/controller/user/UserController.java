package br.com.notehub.application.controller.user;

import br.com.notehub.adapter.producer.MailProducer;
import br.com.notehub.application.dto.request.user.*;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.application.dto.response.user.CreateUserRES;
import br.com.notehub.application.dto.response.user.DetailUserRES;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserService;
import br.com.notehub.infra.exception.CustomExceptions;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RestController
@CrossOrigin(origins = {"https://notehub.com.br"})
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "User Controller", description = "Endpoints for managing users")
@RequiredArgsConstructor
public class UserController {

    @Value("${api.client.host}")
    private String client;

    private final UserService service;
    private final MailProducer producer;

    private UUID getSubject(String bearerToken) {
        if (bearerToken == null) return null;
        String idFromToken = JWT.decode(bearerToken.replace("Bearer ", "")).getSubject();
        return UUID.fromString(idFromToken);
    }

    private String getSubject(String bearerToken, String expectedScope) {
        DecodedJWT decoded = JWT.decode(bearerToken.replace("Bearer ", ""));
        String emailFromToken = decoded.getSubject();
        String scope = decoded.getClaim("scope").asString();
        if (Objects.equals(expectedScope, scope)) return emailFromToken;
        throw new CustomExceptions.ScopeNotAllowedException();
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account and sends an activation email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "User registered successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "406", description = "Input data already exists.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PostMapping("/register")
    public ResponseEntity<CreateUserRES> createUser(
            @Valid @RequestBody CreateUserREQ dto
    ) {
        User user = service.create(dto.toUser());
        String jwt = service.generateActivationToken(user);
        producer.publishAccountActivationMessage(jwt, user);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new CreateUserRES(user));
    }

    @Hidden
    @GetMapping("/activate")
    public ResponseEntity<Void> activeUser(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.activate(idFromToken);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Reset user password", description = "Resets the user's password using the provided token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User's password updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid or same password."),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/change-password")
    public ResponseEntity<Void> patchPassword(
            @Parameter(hidden = true) @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordREQ dto
    ) {
        String emailFromToken = getSubject(token, "password");
        service.changePassword(emailFromToken, dto.password());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Update email", description = "Updates the user's email address.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Email updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid or same email."),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "406", description = "Email already exists."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/change-email")
    public ResponseEntity<Void> patchEmail(
            @Parameter(hidden = true) @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangeEmailREQ dto
    ) {
        String emailFromToken = getSubject(token, "email");
        service.changeEmail(emailFromToken, dto.email());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Update user profile", description = "Updates the user's profile information based on the provided data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "406", description = "Input data conflicts with existing information.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PutMapping("/profile")
    public ResponseEntity<DetailUserRES> editUser(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody ChangeUserREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        User user = service.edit(idFromToken, dto.toUser());
        return ResponseEntity.status(HttpStatus.OK).body(new DetailUserRES(user));
    }

    @Operation(summary = "Change profile visibility", description = "Changes the visibility of the user's profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Profile visibility changed successfully."),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/profile/visibility")
    public ResponseEntity<Void> changeProfileVisibility(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeProfileVisibility(idFromToken);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Update username", description = "Updates the user's username.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Username updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid username."),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "406", description = "Username already exists."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/profile/username")
    public ResponseEntity<Void> patchUsername(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody ChangeUsernameREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeUsername(idFromToken, dto.username());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Update display name", description = "Updates the user's display name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Dispaly name updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid display name."),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/profile/display-name")
    public ResponseEntity<Void> patchDisplayName(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody ChangeDisplayNameREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeDisplayName(idFromToken, dto.displayName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Update profile picture", description = "Updates the user's profile picture.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Profile picture updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid profile picture."),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/profile/avatar")
    public ResponseEntity<Void> patchAvatar(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody ChangeAvatarREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeAvatar(idFromToken, dto.avatar());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Update profile banner", description = "Updates the user's profile banner.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Profile banner updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid banner."),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/profile/banner")
    public ResponseEntity<Void> patchBanner(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody ChangeBannerREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeBanner(idFromToken, dto.banner());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Update user message", description = "Updates the user's message.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User's message updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid message.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PatchMapping("/profile/message")
    public ResponseEntity<Void> patchMessage(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody ChangeMessageREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeMessage(idFromToken, dto.message());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Follow a user.", description = "Allows the requesting user to follow the specified user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User followed successfully."),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PostMapping("/{username}/follow")
    public ResponseEntity<Void> followUser(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("username") String userToFollow
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.follow(idFromToken, userToFollow);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Unfollow a user.", description = "Allows the requesting user to unfollow the specified user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User unfollowed successfully."),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @DeleteMapping("/{username}/unfollow")
    public ResponseEntity<Void> unfollowUser(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("username") String userToFollow
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.unfollow(idFromToken, userToFollow);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Delete user account", description = "Deletes the user's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User account deleted successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Invalid password.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUser(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody DeleteUserREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.delete(idFromToken, dto.password());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/subscriptions/{subscription}")
    public ResponseEntity<Void> enableSubscription(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("subscription") String subscription
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.allowSubscription(idFromToken, subscription);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/subscriptions/{subscription}")
    public ResponseEntity<Void> disableSubscription(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("subscription") String subscription
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.disallowSubscription(idFromToken, subscription);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Find users", description = "Retrieves a paginated list of all active users by username or display name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping
    public ResponseEntity<PageRES<DetailUserRES>> findUsers(
            @ParameterObject @PageableDefault(page = 0, size = 25, sort = {"followersCount"}, direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String q
    ) {
        PageRES<DetailUserRES> page = new PageRES<>(service.findAll(pageable, q).map(DetailUserRES::new));
        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

    @Operation(summary = "Get user details by username", description = "Retrieves detailed information about a user by their username.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details retrieved successfully."),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/{username}")
    public ResponseEntity<DetailUserRES> getUser(
            @PathVariable("username") String username
    ) {
        User user = service.getUser(username);
        return ResponseEntity.status(HttpStatus.OK).body(new DetailUserRES(user));
    }

    @Operation(
            summary = "Fetches a paginated list of users that the specified user is following.",
            description = """
                    Retrieves a page of users a specified user is following,
                    The requesting user must have permission to view the requested user's following list
                    (for private accounts, the requested user must follow the requesting user)."""
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Following users page retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Access denied due to invalid token or insufficient permissions.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/{username}/following")
    public ResponseEntity<PageRES<DetailUserRES>> getFollowing(
            @Parameter(hidden = true) @RequestHeader(required = false, value = "Authorization") String accessToken,
            @ParameterObject @PageableDefault(page = 0, size = 25, sort = {"followersCount"}, direction = Sort.Direction.DESC) Pageable pageable,
            @PathVariable("username") String username,
            @RequestParam(required = false) String q
    ) {
        UUID idFromToken = getSubject(accessToken);
        Page<DetailUserRES> page = service.getUserFollowing(pageable, q, idFromToken, username).map(DetailUserRES::new);
        return ResponseEntity.status(HttpStatus.OK).body(new PageRES<>(page));
    }

    @Operation(
            summary = "Fetches a paginated list of users that are following the specified user.",
            description = """
                    Retrieve a page of users following a specified user.
                    The requesting user must have permission to view the requested user's followers list
                    (for private accounts, the requested user must follow the requesting user)."""
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Followers page retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Access denied due to invalid token or insufficient permissions.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/{username}/followers")
    public ResponseEntity<PageRES<DetailUserRES>> getFollowers(
            @Parameter(hidden = true) @RequestHeader(required = false, value = "Authorization") String accessToken,
            @ParameterObject @PageableDefault(page = 0, size = 25, sort = {"followersCount"}, direction = Sort.Direction.DESC) Pageable pageable,
            @PathVariable("username") String username,
            @RequestParam(required = false) String q
    ) {
        UUID idFromToken = getSubject(accessToken);
        Page<DetailUserRES> page = service.getUserFollowers(pageable, q, idFromToken, username).map(DetailUserRES::new);
        return ResponseEntity.status(HttpStatus.OK).body(new PageRES<>(page));
    }

    @Operation(
            summary = "Get user mutual connections",
            description = """
                    Retrieves a list of usernames that has mutual following with current user."
                    This includes users that the current user follows and who also follow the current user."""
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User mutual connections retrieved successfully."),
            @ApiResponse(responseCode = "403", description = "Access denied due to invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/mutuals")
    public ResponseEntity<Set<String>> getMutualConnections(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken
    ) {
        UUID idFromToken = getSubject(accessToken);
        Set<String> mutuals = service.getUserMutualConnections(idFromToken);
        return ResponseEntity.status(HttpStatus.OK).body(mutuals);
    }

    @Operation(summary = "Get user display name history", description = "Retrieves the history of display names for a user by their username.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User display name history retrieved successfully."),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/{username}/display-names")
    public ResponseEntity<List<String>> getUserDisplayNameHistory(
            @PathVariable("username") String username
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(service.getUserDisplayNameHistory(username));
    }

}