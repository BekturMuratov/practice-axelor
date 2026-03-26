package com.axelor.apps.seo.integrations.bakai.helper;

import com.axelor.apps.seo.rest.exception.BusinessException;
import com.axelor.apps.seo.integrations.bakai.dto.*;
import com.axelor.i18n.I18n;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static com.axelor.apps.seo.utils.ConnectionUtils.*;

public class BakaiGateway {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClients.createDefault();

    public GenerateQrResponseDto generateQr(GenerateQrRequestDto requestDto) {
        try {
            String body = objectMapper.writeValueAsString(requestDto);
            String url = buildUrlForRabbitPayment();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization",
                    getBasicAuthorization(getApiGatewayBakaiUsername(),
                    getApiGatewayBakaiPassword()));
            httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();

            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (statusCode != 200) {
                throw new BusinessException("Bakai Gateway error: HTTP " + statusCode, 502);
            }

            if (responseBody == null || responseBody.isBlank() || !responseBody.trim().startsWith("{")) {
                LOGGER.warn("Gateway returned non-JSON, using empty DTO (stub mode)");
                return new GenerateQrResponseDto();
            }
            return objectMapper.readValue(responseBody, GenerateQrResponseDto.class);

        } catch (BusinessException e) {
            throw e;
        } catch (ConnectException | SocketTimeoutException e) {
            LOGGER.error("Bakai gateway timeout/connection error", e);
            throw new BusinessException(I18n.get("The payment service is temporarily unavailable"), 503);
        } catch (Exception e) {
            LOGGER.error("Unexpected error calling Bakai gateway", e);
            throw new BusinessException(I18n.get("Couldn't contact the payment gateway"), 502);
        }
    }

    private String buildUrlForRabbitPayment(){
        return getApiGatewayBakaiUrl() + "pay/ws/api/payment/create";
    }
}