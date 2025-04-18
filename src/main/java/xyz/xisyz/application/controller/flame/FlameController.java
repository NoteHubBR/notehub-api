package xyz.xisyz.application.controller.flame;

import com.auth0.jwt.JWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.xisyz.application.dto.response.flame.DetailFlameRES;
import xyz.xisyz.application.dto.response.page.PageRES;
import xyz.xisyz.domain.flame.Flame;
import xyz.xisyz.domain.flame.FlameService;

import java.util.UUID;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "https://xisyz.xyz"})
@RequestMapping("/api/v1/flames")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Flame Controller", description = "Endpoints for managing user flames")
@RequiredArgsConstructor
public class FlameController {

    private final FlameService service;

    private UUID getSubject(String bearerToken) {
        if (bearerToken == null) return null;
        String idFromToken = JWT.decode(bearerToken.replace("Bearer ", "")).getSubject();
        return UUID.fromString(idFromToken);
    }

    @Operation(summary = "Register a new flame", description = "Inflames a note.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Flame created successfully."),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "Note note found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "409", description = "Only one flame per note."),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PostMapping("/{id}")
    @Transactional
    public ResponseEntity<DetailFlameRES> inflameNote(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath
    ) {
        UUID idFromToken = getSubject(accessToken);
        Flame flame = service.inflame(idFromToken, idFromPath);
        return ResponseEntity.status(HttpStatus.CREATED).body(new DetailFlameRES(flame));
    }

    @Operation(summary = "Delete flame", description = "Deflames a note.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Flame deleted successfully."),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "Note not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deflameNote(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.deflame(idFromToken, idFromPath);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Get user inflamed notes", description = "Retrieves a page of flames.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page results retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/{username}")
    public ResponseEntity<PageRES<DetailFlameRES>> getFlames(
            @Parameter(hidden = true) @RequestHeader(required = false, value = "Authorization") String accessToken,
            @ParameterObject @PageableDefault(page = 0, size = 25, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pageable,
            @PathVariable("username") String username,
            @RequestParam(required = false) String q
    ) {
        UUID idFromToken = getSubject(accessToken);
        Page<DetailFlameRES> page = service.getUserFlames(idFromToken, pageable, username, q).map(DetailFlameRES::new);
        return ResponseEntity.status(HttpStatus.OK).body(new PageRES<>(page));
    }

}