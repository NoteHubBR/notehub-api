package br.com.notehub.application.controller.feed;

import br.com.notehub.application.dto.response.feed.FeedEventRES;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.domain.feed.FeedEvent;
import br.com.notehub.domain.feed.FeedService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feed")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Feed Controller", description = "Endpoints for reading the authenticated user's feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService service;

    private UUID getSubject(String bearerToken) {
        if (bearerToken == null) return null;
        String idFromToken = JWT.decode(bearerToken.replace("Bearer ", "")).getSubject();
        return UUID.fromString(idFromToken);
    }

    @Operation(
            summary = "Get authenticated user's feed",
            description = "Retrieves a paginated list of feed events for the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feed retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria or unknown event type.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Access token is invalid or missing.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping
    public ResponseEntity<PageRES<FeedEventRES>> getFeed(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @RequestParam(required = false) List<FeedEvent> events,
            @ParameterObject @PageableDefault(page = 0, size = 25, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID idFromToken = getSubject(accessToken);
        PageRES<FeedEventRES> feed = service.getFeed(pageable, idFromToken, events);
        return ResponseEntity.status(HttpStatus.OK).body(feed);
    }

}