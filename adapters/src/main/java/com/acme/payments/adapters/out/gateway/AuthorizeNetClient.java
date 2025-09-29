package com.acme.payments.adapters.out.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class AuthorizeNetClient {
    private final String apiLoginId;
    private final String transactionKey;
    private final boolean sandbox;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final MeterRegistry meterRegistry;

    public AuthorizeNetClient(
            @Value("${authorizenet.apiLoginId:}") String apiLoginId,
            @Value("${authorizenet.transactionKey:}") String transactionKey,
            @Value("${authorizenet.sandbox:true}") boolean sandbox,
            MeterRegistry meterRegistry) {
        this.apiLoginId = apiLoginId;
        this.transactionKey = transactionKey;
        this.sandbox = sandbox;
        this.meterRegistry = meterRegistry;
    }

    private String baseUrl() { return sandbox ? "https://apitest.authorize.net/xml/v1/request.api" : "https://api2.authorize.net/xml/v1/request.api"; }

    public GatewayResponse purchase(String token, long amountMinor, String currency) {
        return transact("authCaptureTransaction", token, null, amountMinor);
    }
    public GatewayResponse authorize(String token, long amountMinor, String currency) {
        return transact("authOnlyTransaction", token, null, amountMinor);
    }
    public GatewayResponse capture(String authRef, long amountMinor) {
        return transact("priorAuthCaptureTransaction", null, authRef, amountMinor);
    }
    public GatewayResponse voidAuth(String ref) { return transact("voidTransaction", null, ref, 0); }
    public GatewayResponse refund(String ref, long amountMinor) { return transact("refundTransaction", null, ref, amountMinor); }

    private GatewayResponse transact(String type, String token, String ref, long amountMinor) {
        try {
            BigDecimal amount = new BigDecimal(amountMinor).movePointLeft(2);
            String body = buildRequest(type, token, ref, amount);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            long start = System.nanoTime();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            long duration = System.nanoTime() - start;
            Timer.builder("gateway.request")
                    .tag("vendor", "authorize_net")
                    .tag("operation", type)
                    .tag("status", String.valueOf(resp.statusCode()))
                    .register(meterRegistry)
                    .record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);

            JsonNode root = mapper.readTree(resp.body());
            GatewayResponse gr = new GatewayResponse();
            String result = root.path("messages").path("resultCode").asText("");
            gr.approved = "Ok".equalsIgnoreCase(result);
            gr.referenceId = root.path("transactionResponse").path("transId").asText(null);
            return gr;
        } catch (Exception e) {
            Timer.builder("gateway.request.error")
                    .tag("vendor", "authorize_net")
                    .register(meterRegistry)
                    .record(1, java.util.concurrent.TimeUnit.NANOSECONDS);
            GatewayResponse gr = new GatewayResponse(); gr.approved = false; gr.referenceId = null; return gr;
        }
    }

    private String buildRequest(String type, String token, String ref, BigDecimal amount) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append('{')
          .append("\"createTransactionRequest\":{")
          .append("\"merchantAuthentication\":{\"name\":\"").append(escape(apiLoginId)).append("\",\"transactionKey\":\"").append(escape(transactionKey)).append("\"},")
          .append("\"transactionRequest\":{")
          .append("\"transactionType\":\"").append(type).append("\",")
          .append("\"amount\":\"").append(amount.toPlainString()).append("\"");
        if (token != null) {
            sb.append(",\"payment\":{\"opaqueData\":{\"dataDescriptor\":\"COMMON.ACCEPT.INAPP.PAYMENT\",\"dataValue\":\"")
              .append(escape(token)).append("\"}} ");
        }
        if (ref != null) {
            sb.append(",\"refTransId\":\"").append(escape(ref)).append("\"");
        }
        sb.append('}').append('}').append('}');
        return sb.toString();
    }

    private static String escape(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }

    public static class GatewayResponse {
        public boolean approved;
        public String referenceId;
    }
}


