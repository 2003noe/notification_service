package com.microfinancehub.notification_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification")
@Getter @Setter
public class NotificationProperties {
    private String fromEmail;
    private String fromName;
    private String smsFrom;
    private int    retryMax = 3;

    public int getRetryMax() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}