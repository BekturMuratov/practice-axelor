package com.axelor.apps.seo.service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface RabbitMQService {
    void receiveMessages() throws IOException, TimeoutException;
    void receivePaymentNotifications() throws IOException, TimeoutException;
}
