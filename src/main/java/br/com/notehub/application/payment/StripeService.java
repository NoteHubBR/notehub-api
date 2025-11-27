package br.com.notehub.application.payment;

import br.com.notehub.application.dto.payment.SponsorshipRES;
import br.com.notehub.application.dto.payment.SponsorshipStatusRES;
import br.com.notehub.domain.user.UserService;
import br.com.notehub.infra.exception.CustomExceptions;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${payment.stripe.secret.key}")
    private String secret;

    @Value("${api.client.host}")
    private String client;

    private final UserService userService;

    private static final String SPONSORSHIP_NAME = "Patrocínio";
    private static final Long SPONSORSHIP_QUANTITY = 1L;

    private void validateAccess(@Nullable String idFromToken, String idFromSession) {
        if (Objects.equals(idFromToken, idFromSession)) return;
        throw new AccessDeniedException("Usuário sem permissão.");
    }

    public SponsorshipRES sponsorshipCheckout(String idFromToken, String currency, Long amount) {

        Stripe.apiKey = secret;

        try {
            var productData = SessionCreateParams.LineItem.PriceData.ProductData.builder()
                    .setName(SPONSORSHIP_NAME)
                    .build();

            var priceData = SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency(currency)
                    .setUnitAmount(amount)
                    .setProductData(productData)
                    .build();

            var lineItem = SessionCreateParams.LineItem.builder()
                    .setQuantity(SPONSORSHIP_QUANTITY)
                    .setPriceData(priceData)
                    .build();

            var params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(String.format("%s/sponsorship/success?session_id={CHECKOUT_SESSION_ID}", client))
                    .setCancelUrl(String.format("%s/sponsorship/cancel", client))
                    .addLineItem(lineItem)
                    .putMetadata("purchaseType", "sponsorship")
                    .putMetadata("uId", idFromToken)
                    .build();

            Session session = Session.create(params);
            return new SponsorshipRES(
                    "SUCCESS",
                    "Payment session created",
                    session.getId(),
                    session.getUrl(),
                    idFromToken
            );

        } catch (StripeException e) {
            throw new CustomExceptions.CustomStripeException(e);
        }
    }

    public SponsorshipStatusRES verifySession(String sessionId, String uIdFromToken) {
        try {
            Stripe.apiKey = secret;
            Session session = Session.retrieve(sessionId);
            String uIdFromSession = session.getMetadata().get("uId");
            validateAccess(uIdFromToken, uIdFromSession);
            return new SponsorshipStatusRES(
                    session.getId(),
                    session.getPaymentStatus(),
                    session.getStatus(),
                    session.getAmountTotal()
            );
        } catch (StripeException e) {
            throw new CustomExceptions.CustomStripeException(e);
        }
    }

    @Transactional
    public void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException("Failed to deserialize session"));
        UUID uId = UUID.fromString(session.getMetadata().get("uId"));
        if ("paid".equals(session.getPaymentStatus())) userService.promote(uId);
    }

}