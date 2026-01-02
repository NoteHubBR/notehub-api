package br.com.notehub.application.controller.reply;

import br.com.notehub.application.dto.request.reply.CreateReplyREQ;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.application.dto.response.reply.CreateReplyRES;
import br.com.notehub.application.dto.response.reply.DetailReplyRES;
import br.com.notehub.domain.reply.ReplyService;
import com.auth0.jwt.JWT;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notes/comments")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Reply Controller", description = "Endpoints for managing replies")
@RequiredArgsConstructor
public class ReplyController {

    private final ReplyService service;

    private UUID getSubject(String bearerToken) {
        if (bearerToken == null) return null;
        String idFromToken = JWT.decode(bearerToken.replace("Bearer ", "")).getSubject();
        return UUID.fromString(idFromToken);
    }

    @Operation(summary = "Register a new reply to a comment", description = "Creates a new reply.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reply registered successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token or Note is closed.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "Comment note found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PostMapping("/{id}/replies/new")
    public ResponseEntity<CreateReplyRES> createReply(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath,
            @RequestBody @Valid CreateReplyREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        CreateReplyRES reply = service.create(idFromToken, idFromPath, false, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }

    @Operation(summary = "Register a new reply to a another reply", description = "Creates a new reply.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reply registered successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token or Note is closed.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "Reply note found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PostMapping("/replies/{id}/new")
    public ResponseEntity<CreateReplyRES> createSelfReferenceReply(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath,
            @RequestBody @Valid CreateReplyREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        CreateReplyRES reply = service.create(idFromToken, idFromPath, true, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }

    @Operation(summary = "Change reply text", description = "Updates reply text field.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reply text updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token or Note is closed.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "Reply note found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PatchMapping("/replies/{id}/edit")
    public ResponseEntity<Void> editReply(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath,
            @RequestBody @Valid CreateReplyREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.edit(idFromToken, idFromPath, dto.text());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Delete reply", description = "Removes a reply permanently.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reply deleted successfully."),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "Reply not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @DeleteMapping("/replies/{id}/delete")
    public ResponseEntity<Void> deleteReply(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.delete(idFromToken, idFromPath);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Get replies", description = "Retrieves a replies page.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Replies retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/{id}/replies")
    public ResponseEntity<PageRES<DetailReplyRES>> getReplies(
            @Parameter(hidden = true) @RequestHeader(required = false, value = "Authorization") String accessToken,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = {"createdAt"}, direction = Sort.Direction.ASC) Pageable pageable,
            @PathVariable("id") UUID idFromPath
    ) {
        UUID idFromToken = getSubject(accessToken);
        PageRES<DetailReplyRES> page = service.getReplies(pageable, idFromToken, idFromPath);
        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

}