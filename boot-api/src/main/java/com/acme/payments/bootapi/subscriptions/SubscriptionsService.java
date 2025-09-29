package com.acme.payments.bootapi.subscriptions;

public interface SubscriptionsService {
    void validateCreate(SubscriptionDtos.CreateRequest req);
    void validateCancel(String id);
    void createSubscription(SubscriptionDtos.CreateRequest req);
    void cancelSubscription(String id);
}


