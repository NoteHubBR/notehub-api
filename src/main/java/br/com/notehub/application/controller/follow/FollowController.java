package br.com.notehub.application.controller.follow;

import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.application.dto.response.user.DetailUserRES;
import br.com.notehub.domain.follow.FollowService;
import com.auth0.jwt.JWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Follow System Controller", description = "Endpoints for managing follow service")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService service;

    private UUID getSubject(String bearerToken) {
        if (bearerToken == null) return null;
        String idFromToken = JWT.decode(bearerToken.replace("Bearer ", "")).getSubject();
        return UUID.fromString(idFromToken);
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

}