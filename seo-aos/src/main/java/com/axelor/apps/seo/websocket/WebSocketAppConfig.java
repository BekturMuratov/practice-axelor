package com.axelor.apps.seo.websocket;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WebSocketAppConfig implements ServerApplicationConfig {

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
        return Collections.emptySet();
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {                                           //Метод для регистрации endpoints с аннотацией @ServerEndpoint.
        Set<Class<?>> endpoints = new HashSet<>();
        endpoints.add(PaymentWebSocketEndpoint.class);
        return endpoints;
    }
}