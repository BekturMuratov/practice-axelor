package com.axelor.apps.seo.websocket;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class PaymentWebSocketConfigurator extends ServerEndpointConfig.Configurator {                                   //Разрешает cross-origin подключения от фронтенда. test

    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {                                    //Создание экземпляра endpoint
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new InstantiationException(e.getMessage());
        }
    }

    @Override
    public boolean checkOrigin(String originHeaderValue) {
        return true;
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {       //handshake WebSocket перед установкой соединения
        super.modifyHandshake(sec, request, response);
    }
}