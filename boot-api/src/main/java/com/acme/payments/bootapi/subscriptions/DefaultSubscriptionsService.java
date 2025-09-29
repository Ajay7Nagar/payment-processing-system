package com.acme.payments.bootapi.subscriptions;

import com.acme.payments.bootapi.error.ApiException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DefaultSubscriptionsService implements SubscriptionsService {
    @Override
    public void validateCreate(SubscriptionDtos.CreateRequest req) {
        validateMoney(req.amount());
        validateSchedule(req.schedule());
    }

    @Override
    public void validateCancel(String id) {
        if (id == null || id.isBlank()) {
            throw ApiException.unprocessable("INVALID_REQUEST", "Subscription id required");
        }
    }

    private void validateMoney(SubscriptionDtos.Money money) {
        if (!"INR".equals(money.currency())) {
            throw ApiException.unprocessable("CURRENCY_NOT_SUPPORTED", "Only INR is supported");
        }
        try {
            BigDecimal amt = new BigDecimal(money.amount());
            if (amt.scale() != 2) {
                throw ApiException.unprocessable("INVALID_REQUEST", "Amount must have two decimal places");
            }
        } catch (NumberFormatException ex) {
            throw ApiException.unprocessable("INVALID_REQUEST", "Invalid amount format");
        }
    }

    private void validateSchedule(SubscriptionDtos.Schedule s) {
        if (s.type() == SubscriptionDtos.ScheduleType.EVERY_N_DAYS) {
            if (s.nDays() == null || s.nDays() < 1) {
                throw ApiException.unprocessable("INVALID_REQUEST", "nDays required for EVERY_N_DAYS");
            }
        }
        if (s.type() == SubscriptionDtos.ScheduleType.MONTHLY && s.billingDay() != null) {
            if (s.billingDay() < 1 || s.billingDay() > 31) {
                throw ApiException.unprocessable("INVALID_REQUEST", "billingDay must be 1..31");
            }
        }
    }
}


