package com.acme.payments.adapters.out.queue;

public interface EventQueue {
    void enqueue(String topic, String payload);
}


