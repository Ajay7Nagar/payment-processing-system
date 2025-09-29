package com.acme.payments.bootapi.subscriptions;

import org.springframework.stereotype.Service;

@Service
public class DefaultSubscriptionsService implements SubscriptionsService {
    @Override
    public void validateCreate(SubscriptionDtos.CreateRequest req) {
        // placeholder validation
    }

    @Override
    public void validateCancel(String id) {
        // placeholder validation
    }

    @Override
    public void createSubscription(SubscriptionDtos.CreateRequest req) {
        // placeholder create logic
    }

    @Override
    public void cancelSubscription(String id) {
        // placeholder cancel logic
    }
}


