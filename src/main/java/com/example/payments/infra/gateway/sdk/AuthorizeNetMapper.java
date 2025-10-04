package com.example.payments.infra.gateway.sdk;

import net.authorize.api.contract.v1.CreateTransactionRequest;
import net.authorize.api.contract.v1.CreateTransactionResponse;
import net.authorize.api.contract.v1.CreditCardType;
import net.authorize.api.contract.v1.MessageTypeEnum;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import net.authorize.api.contract.v1.OpaqueDataType;
import net.authorize.api.contract.v1.OrderType;
import net.authorize.api.contract.v1.PaymentType;
import net.authorize.api.contract.v1.TransactionRequestType;
import net.authorize.api.contract.v1.TransactionTypeEnum;
import com.example.payments.domain.payments.GatewayTransactionResult;
import com.example.payments.domain.payments.Money;
import com.example.payments.domain.payments.PaymentException;
import com.example.payments.infra.gateway.AuthorizeNetProperties;
import com.example.payments.infra.gateway.AuthorizeNetReferenceIdSanitizer;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AuthorizeNetMapper {

    public CreateTransactionRequest buildTransactionRequest(AuthorizeNetProperties properties, TransactionTypeEnum type,
            Money amount, String paymentData, String referenceId, String refTransId) {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setMerchantAuthentication(buildMerchantAuthentication(properties));
        String sanitisedRefId = AuthorizeNetReferenceIdSanitizer.resolve(referenceId);
        request.setRefId(sanitisedRefId);

        TransactionRequestType txn = new TransactionRequestType();
        txn.setTransactionType(type.value());
        txn.setAmount(amount.amount());
        txn.setOrder(buildOrder(sanitisedRefId));

        if (type == TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION || type == TransactionTypeEnum.AUTH_ONLY_TRANSACTION) {
            txn.setPayment(resolvePaymentForAuth(properties, paymentData));
        } else if (type == TransactionTypeEnum.REFUND_TRANSACTION) {
            txn.setPayment(buildMaskedCard(paymentData));
            txn.setRefTransId(refTransId);
        } else if (type == TransactionTypeEnum.PRIOR_AUTH_CAPTURE_TRANSACTION
                || type == TransactionTypeEnum.VOID_TRANSACTION) {
            txn.setRefTransId(refTransId);
        } else {
            throw new PaymentException("UNSUPPORTED_TYPE", "Unsupported transaction type " + type);
        }

        request.setTransactionRequest(txn);
        return request;
    }

    private PaymentType resolvePaymentForAuth(AuthorizeNetProperties properties, String paymentData) {
        if (paymentData == null || paymentData.isBlank()) {
            throw new PaymentException("INVALID_PAYMENT_DATA", "Payment token required");
        }

        CardDetails cardDetails = tryParseCard(paymentData);
        if (cardDetails != null) {
            return buildCreditCardPayment(cardDetails);
        }

        return buildOpaquePayment(properties.getAcceptPaymentDescriptor(), paymentData);
    }

    private CardDetails tryParseCard(String paymentData) {
        String trimmed = paymentData.trim();
        String[] parts = trimmed.split("\\|");
        int index = 0;
        if (parts.length > 0 && "card".equalsIgnoreCase(parts[0])) {
            index = 1;
        }

        if (parts.length - index < 1) {
            return null;
        }

        String number = parts[index].replaceAll("\\s", "");
        if (!number.matches("\\d{13,16}")) {
            return null;
        }

        String expiration = parts.length - index > 1 ? normalizeExpiration(parts[index + 1]) : "1225";
        String cardCode = parts.length - index > 2 && !parts[index + 2].isBlank() ? parts[index + 2] : null;

        return new CardDetails(number, expiration, cardCode);
    }

    private String normalizeExpiration(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 4) {
            return digits;
        }
        if (digits.length() == 6) {
            String month = digits.substring(4);
            String year = digits.substring(2, 4);
            return month + year;
        }
        if (digits.length() == 2) {
            return digits;
        }
        return "1225";
    }

    public GatewayTransactionResult toGatewayResult(TransactionTypeEnum type, CreateTransactionRequest request,
            CreateTransactionResponse response) {
        if (response == null) {
            throw new PaymentException("GATEWAY_ERROR", "Empty response from Authorize.Net");
        }

        if (response.getMessages() != null && response.getMessages().getResultCode() == MessageTypeEnum.ERROR) {
            String errorText = response.getMessages().getMessage() != null && !response.getMessages().getMessage().isEmpty()
                    ? response.getMessages().getMessage().get(0).getCode() + ":"
                            + response.getMessages().getMessage().get(0).getText()
                    : "Gateway returned error";
            throw new PaymentException("GATEWAY_DECLINED", errorText);
        }

        if (response.getTransactionResponse() == null) {
            throw new PaymentException("GATEWAY_ERROR", "Missing transaction response");
        }

        String transactionId = response.getTransactionResponse().getTransId();
        String responseCode = Optional.ofNullable(response.getTransactionResponse().getResponseCode()).orElse("0");
        String responseMessage = getResponseMessage(response);

        return GatewayTransactionResult.success(transactionId, responseCode, responseMessage, OffsetDateTime.now());
    }

    private static String getResponseMessage(CreateTransactionResponse response) {
        String responseMessage = null;
        if (response.getTransactionResponse().getMessages() != null
                && response.getTransactionResponse().getMessages().getMessage() != null
                && !response.getTransactionResponse().getMessages().getMessage().isEmpty()) {
            responseMessage = response.getTransactionResponse().getMessages().getMessage().get(0).getDescription();
        } else if (response.getTransactionResponse().getErrors() != null
                && response.getTransactionResponse().getErrors().getError() != null
                && !response.getTransactionResponse().getErrors().getError().isEmpty()) {
            responseMessage = response.getTransactionResponse().getErrors().getError().get(0).getErrorText();
        } else if (response.getMessages() != null && response.getMessages().getMessage() != null
                && !response.getMessages().getMessage().isEmpty()) {
            responseMessage = response.getMessages().getMessage().get(0).getText();
        }

        if (responseMessage == null) {
            responseMessage = "Approved";
        }
        return responseMessage;
    }

    public MerchantAuthenticationType buildMerchantAuthentication(AuthorizeNetProperties properties) {
        MerchantAuthenticationType authentication = new MerchantAuthenticationType();
        authentication.setName(properties.getApiLoginId());
        authentication.setTransactionKey(properties.getTransactionKey());
        return authentication;
    }

    private PaymentType buildOpaquePayment(String descriptor, String paymentNonce) {
        OpaqueDataType opaqueData = new OpaqueDataType();
        opaqueData.setDataDescriptor(descriptor);
        opaqueData.setDataValue(paymentNonce);
        PaymentType payment = new PaymentType();
        payment.setOpaqueData(opaqueData);
        return payment;
    }

    private PaymentType buildCreditCardPayment(CardDetails details) {
        CreditCardType creditCard = new CreditCardType();
        creditCard.setCardNumber(details.number());
        creditCard.setExpirationDate(details.expiration());
        if (details.cardCode() != null) {
            creditCard.setCardCode(details.cardCode());
        }

        PaymentType payment = new PaymentType();
        payment.setCreditCard(creditCard);
        return payment;
    }

    private PaymentType buildMaskedCard(String lastFour) {
        CreditCardType creditCard = new CreditCardType();
        creditCard.setCardNumber(lastFour);
        creditCard.setExpirationDate("XXXX");
        PaymentType payment = new PaymentType();
        payment.setCreditCard(creditCard);
        return payment;
    }

    private OrderType buildOrder(String referenceId) {
        OrderType order = new OrderType();
        order.setInvoiceNumber(referenceId);
        order.setDescription("Payment order " + referenceId);
        return order;
    }

    private record CardDetails(String number, String expiration, String cardCode) {
    }
}
