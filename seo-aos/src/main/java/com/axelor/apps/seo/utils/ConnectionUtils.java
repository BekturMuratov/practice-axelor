package com.axelor.apps.seo.utils;

import com.axelor.app.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ConnectionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final AppSettings APP_SETTINGS = AppSettings.get();

    public static String getFileServerPhoto(){return APP_SETTINGS.get("file.server.photo");}

    public static String getApiGatewayUrl() {return  APP_SETTINGS.get("api.gateway.url");}
    public static String getApiGatewayUsername() {return  APP_SETTINGS.get("api.gateway.username");}
    public static String getApiGatewayPassword() {
        String encodedPassword = APP_SETTINGS.get("api.gateway.password");
        return new String(Base64.getDecoder().decode(encodedPassword), StandardCharsets.UTF_8);
    }

    public static String getApiGatewayBakaiUrl() {return  APP_SETTINGS.get("api.gateway.bakai.url");}
    public static String getApiGatewayBakaiUsername() {return  APP_SETTINGS.get("api.gateway.bakai.username");}
    public static String getApiGatewayBakaiPassword() {
        String encodedPassword = APP_SETTINGS.get("api.gateway.bakai.password");
        return new String(Base64.getDecoder().decode(encodedPassword), StandardCharsets.UTF_8);
    }

    public static String getBasicAuthorization(String username, String password) {
        String usernamePassword = String.format("%s:%s", username, password);
        return "Basic " + Base64.getEncoder().encodeToString(usernamePassword.getBytes());
    }

    public static String getRabbitMqUrl(){
        return APP_SETTINGS.get("rabbit.mq.url");
    }
    public static String getRabbitMqUserName(){
        return APP_SETTINGS.get("rabbit.mq.username");
    }
    public static String getRabbitMqPassword(){
        return APP_SETTINGS.get("rabbit.mq.password");
    }
    public static String getRabbitMqQueueName(){
        return APP_SETTINGS.get("rabbit.mq.queue");
    }

    public static String getRabbitMqPaymentUrl(){
        return APP_SETTINGS.get("rabbit.mq.payment.url");
    }
    public static String getRabbitMqPaymentUserName(){
        return APP_SETTINGS.get("rabbit.mq.payment.username");
    }
    public static String getRabbitMqPaymentPassword(){
        return APP_SETTINGS.get("rabbit.mq.payment.password");
    }
    public static String getRabbitMqPaymentQueueName(){
        return APP_SETTINGS.get("rabbit.mq.payment.queue");
    }
}
