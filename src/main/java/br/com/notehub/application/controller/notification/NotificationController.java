package br.com.notehub.application.controller.notification;

import br.com.notehub.application.dto.response.notification.DetailNotificationRES;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.domain.notification.NotificationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Notification Controller", description = "Endpoints for managing user notifications, including retrieving and marking notifications as read.")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @Operation(
            summary = "Retrieve notifications",
            description = "Fetch a paginated list of user notifications, ordered by unread notifications first and sorted by creation date."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping
    public ResponseEntity<PageRES<DetailNotificationRES>> readNotification(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID idFromToken = UUID.fromString(JWT.decode(accessToken.replace("Bearer ", "")).getSubject());
        PageRES<DetailNotificationRES> page = service.getNotifications(pageable, idFromToken);
        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

}