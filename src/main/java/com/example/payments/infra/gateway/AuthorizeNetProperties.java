package com.example.payments.infra.gateway;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "authorize-net")
public class AuthorizeNetProperties {

    @NotBlank
    private String apiLoginId;

    @NotBlank
    private String transactionKey;

    @NotBlank
    private String endpoint;

    @NotBlank
    private String webhookSignatureKey;

    private String acceptPaymentDescriptor = "COMMON.ACCEPT.INAPP.PAYMENT";
    private String defaultCurrency = "USD";

    public String getApiLoginId() {
        return apiLoginId;
    }

    public void setApiLoginId(String apiLoginId) {
        this.apiLoginId = apiLoginId;
    }

    public String getTransactionKey() {
        return transactionKey;
    }

    public void setTransactionKey(String transactionKey) {
        this.transactionKey = transactionKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getWebhookSignatureKey() {
        return webhookSignatureKey;
    }

    public void setWebhookSignatureKey(String webhookSignatureKey) {
        this.webhookSignatureKey = webhookSignatureKey;
    }

    public String getAcceptPaymentDescriptor() {
        return acceptPaymentDescriptor;
    }

    public void setAcceptPaymentDescriptor(String acceptPaymentDescriptor) {
        this.acceptPaymentDescriptor = acceptPaymentDescriptor;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }
}
