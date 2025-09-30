package com.example.payments.adapters.api.subscriptions;

import com.example.payments.application.services.SubscriptionService;
import com.example.payments.domain.billing.Subscription;
import com.example.payments.domain.billing.SubscriptionBillingCycle;
import com.example.payments.domain.billing.SubscriptionStatus;
import com.example.payments.domain.shared.CorrelationId;
import com.example.payments.domain.shared.IdempotencyKey;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubscriptionController.class)
@Import(com.example.payments.testsupport.TestSecurityConfiguration.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionService subscriptionService;

    private UUID subscriptionId;
    private UUID customerId;
    private Subscription subscription;
    private OffsetDateTime now;
    private String idempotencyKey;
    private String correlationId;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        now = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1); // ensure future first billing

        idempotencyKey = UUID.randomUUID().toString();
        correlationId = UUID.randomUUID().toString();
        subscription = Subscription.create(customerId, "PLAN_BASIC", SubscriptionBillingCycle.MONTHLY, 30,
                new BigDecimal("49.99"), "USD", "tok_123", "client-ref-1", now.plusDays(14), now, 4, now.minusDays(1));
    }

    @Test
    @DisplayName("createSubscription returns 201 on success")
    void createSubscription_success() throws Exception {
        when(subscriptionService.createSubscription(eq(customerId), eq("PLAN_BASIC"),
                eq("client-ref-1"), eq(new BigDecimal("49.99")), eq("USD"), eq(SubscriptionBillingCycle.MONTHLY),
                any(OffsetDateTime.class), any(OffsetDateTime.class), any(IdempotencyKey.class),
                any(CorrelationId.class), eq("tok_123"), eq(4), eq(30))).thenReturn(subscription);

        OffsetDateTime trialEnd = now.plusDays(14);
        OffsetDateTime firstBilling = now;
        String payload = """
                {
                  "customerId":"%s",
                  "planCode":"PLAN_BASIC",
                  "clientReference":"client-ref-1",
                  "billingCycle":"MONTHLY",
                  "intervalDays":30,
                  "amount":49.99,
                  "currency":"USD",
                  "paymentMethodToken":"tok_123",
                  "trialEnd":"%s",
                  "firstBillingAt":"%s",
                  "maxRetryAttempts":4,
                  "idempotencyKey":"%s",
                  "correlationId":"%s"
                }
                """.formatted(customerId, trialEnd, firstBilling, idempotencyKey, correlationId);

        mockMvc.perform(post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-Correlation-Id", correlationId)
                .with(user("writer").roles("SUBSCRIPTIONS_WRITE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planCode").value("PLAN_BASIC"))
                .andExpect(jsonPath("$.amount").value("49.99"));
    }

    @Test
    @DisplayName("createSubscription requires SUBSCRIPTIONS_WRITE role")
    void createSubscription_forbiddenWithoutRole() throws Exception {
        OffsetDateTime trialEnd = now.plusDays(14);
        OffsetDateTime firstBilling = now;
        String payload = """
                {
                  "customerId":"%s",
                  "planCode":"PLAN_BASIC",
                  "clientReference":"client-ref-1",
                  "billingCycle":"MONTHLY",
                  "intervalDays":30,
                  "amount":49.99,
                  "currency":"USD",
                  "paymentMethodToken":"tok_123",
                  "trialEnd":"%s",
                  "firstBillingAt":"%s",
                  "maxRetryAttempts":4,
                  "idempotencyKey":"%s",
                  "correlationId":"%s"
                }
                """.formatted(customerId, trialEnd, firstBilling, idempotencyKey, correlationId);

        mockMvc.perform(post("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-Correlation-Id", correlationId)
                .with(user("reader").roles("PAYMENTS_READ")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getSubscription returns subscription payload")
    void getSubscription_success() throws Exception {
        when(subscriptionService.getSubscription(subscriptionId)).thenReturn(subscription);

        mockMvc.perform(get("/api/v1/subscriptions/{subscriptionId}", subscriptionId)
                .with(user("reader").roles("SUBSCRIPTIONS_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(subscription.getId().toString()))
                .andExpect(jsonPath("$.status").value(SubscriptionStatus.ACTIVE.name()));
    }

    @Test
    @DisplayName("updateSubscription passes optional fields to service")
    void updateSubscription_success() throws Exception {
        when(subscriptionService.updateSubscription(eq(subscriptionId), any(), any(), any(), any(), any(), any(), any(),
                any(OffsetDateTime.class))).thenReturn(subscription);

        String payload = "{" +
                "\"planCode\":\"PLAN_PRO\"," +
                "\"amount\":79.99," +
                "\"currency\":\"USD\"," +
                "\"paymentMethodToken\":\"tok_999\"," +
                "\"maxRetryAttempts\":6," +
                "\"intervalDays\":20," +
                "\"nextBillingAt\":\"" + now.plusDays(5) + "\"" +
                "}";

        mockMvc.perform(put("/api/v1/subscriptions/{subscriptionId}", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("Idempotency-Key", idempotencyKey)
                .with(user("writer").roles("SUBSCRIPTIONS_WRITE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("PLAN_BASIC"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Optional<String>> planCaptor = ArgumentCaptor.forClass((Class) Optional.class);
        verify(subscriptionService).updateSubscription(eq(subscriptionId), planCaptor.capture(), any(), any(), any(), any(), any(), any(),
                any(OffsetDateTime.class));
        assertThat(planCaptor.getValue()).contains("PLAN_PRO");
    }

    @Test
    @DisplayName("listSchedules returns schedule response")
    void listSchedules_success() throws Exception {
        when(subscriptionService.getSchedules(subscriptionId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/subscriptions/{subscriptionId}/schedules", subscriptionId)
                .with(user("reader").roles("SUBSCRIPTIONS_READ")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("resumeSubscription defaults nextBilling when absent")
    void resumeSubscription_defaultsNextBilling() throws Exception {
        when(subscriptionService.resumeSubscription(eq(subscriptionId), any(OffsetDateTime.class)))
                .thenReturn(subscription);

        String payload = "{}";
        OffsetDateTime before = OffsetDateTime.now().minusMinutes(1);

        mockMvc.perform(post("/api/v1/subscriptions/{subscriptionId}/resume", subscriptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .with(user("writer").roles("SUBSCRIPTIONS_WRITE")))
                .andExpect(status().isOk());

        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(subscriptionService).resumeSubscription(eq(subscriptionId), captor.capture());
        assertThat(captor.getValue()).isAfter(before);
    }

    @Test
    @DisplayName("listSubscriptions returns list")
    void listSubscriptions_missingMerchantHeader() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions")
                .with(user("reader").roles("SUBSCRIPTIONS_READ")))
                .andExpect(status().isOk());
    }
}
