package com.example.payments.application.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "subscription")
public class SubscriptionProperties {

    private final Retry retry = new Retry();

    public Retry getRetry() {
        return retry;
    }

    public static class Retry {

        private String scheduleCron = "0 */5 * * * *";
        private int autoCancelDays = 30;

        public String getScheduleCron() {
            return scheduleCron;
        }

        public void setScheduleCron(String scheduleCron) {
            this.scheduleCron = scheduleCron;
        }

        public int getAutoCancelDays() {
            return autoCancelDays;
        }

        public void setAutoCancelDays(int autoCancelDays) {
            this.autoCancelDays = autoCancelDays;
        }
    }
}
