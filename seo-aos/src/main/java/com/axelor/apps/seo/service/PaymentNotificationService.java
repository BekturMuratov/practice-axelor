package com.axelor.apps.seo.service;

import com.axelor.apps.seo.integrations.bakai.dto.PaymentNotificationDto;

public interface PaymentNotificationService {
    void processPaymentNotification(PaymentNotificationDto notification);
}