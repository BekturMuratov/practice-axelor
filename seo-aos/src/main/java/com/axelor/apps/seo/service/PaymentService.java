package com.axelor.apps.seo.service;

import javax.ws.rs.core.Response;

public interface PaymentService {
    Response generateQr(String requisiteNumber);
    String buildPaymentPageHtml(String requisiteNumber);
}