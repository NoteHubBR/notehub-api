package br.com.notehub.application.controller.payment;

import br.com.notehub.application.dto.payment.SponsorshipREQ;
import br.com.notehub.application.dto.payment.SponsorshipRES;
import br.com.notehub.application.dto.payment.SponsorshipStatusRES;
import br.com.notehub.application.payment.StripeService;
import com.auth0.jwt.JWT;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {"https://notehub.com.br"})
@RequestMapping("/api/v1/payment")
@Tag(name = "Payment Controller", description = "Endpoints for managing payments")
@RequiredArgsConstructor
public class PaymentController {

    @Value("${payment.stripe.webhook.key}")
    private String secret;

    private final StripeService stripeService;

    private String getSubject(String bearerToken) {
        if (bearerToken == null) return null;
        return JWT.decode(bearerToken.replace("Bearer ", "")).getSubject();
    }

    @Operation(
            summary = "Initiates the Stripe sponsorship checkout process",
            description = "Creates a Stripe Checkout Session for a sponsorship with the specified currency and amount."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Checkout session created successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PostMapping("/stripe/sponsorship")
    public ResponseEntity<SponsorshipRES> sponsorshipCheckout(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody SponsorshipREQ dto
    ) {
        String idFromToken = getSubject(accessToken);
        SponsorshipRES res = stripeService.sponsorshipCheckout(idFromToken, dto.locale(), dto.currency(), dto.amount());
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @Operation(
            summary = "Verifies the status of a Stripe sponsorship checkout session",
            description = "Queries Stripe to determine if the payment for the specified session has been successfully completed."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session status returned successfully."),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/stripe/sponsorship/verify/{sessionId}")
    public ResponseEntity<SponsorshipStatusRES> verifySession(
            @PathVariable String sessionId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken
    ) {
        String uIdFromToken = getSubject(accessToken);
        return ResponseEntity.status(HttpStatus.OK).body(stripeService.verifySession(sessionId, uIdFromToken));
    }

    @Hidden
    @PostMapping("/stripe/sponsorship/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, secret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if ("checkout.session.completed".equals(event.getType())) {
            stripeService.handleCheckoutSessionCompleted(event);
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

}