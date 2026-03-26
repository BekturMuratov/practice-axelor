package com.axelor.apps.seo.websocket;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint(value = "/public/seo/websocket/payment/{bookingId}", configurator = PaymentWebSocketConfigurator.class)
public class PaymentWebSocketEndpoint {

    private static final Map<String, Set<Session>> SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("bookingId") String bookingId) {                                     //Метод вызывается при открытии нового WebSocket соединения.
        SESSIONS.computeIfAbsent(bookingId, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("bookingId") String bookingId) {                                    //Метод вызывается при закрытии соединения WebSocket.
        removeSession(bookingId, session);
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("bookingId") String bookingId) {                   //Метод вызывается при ошибке WebSocket.
        removeSession(bookingId, session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {                                                            //Метод используется для ping проверки
        if ("ping".equalsIgnoreCase(message)) {
            try {
                session.getBasicRemote().sendText("pong");
            } catch (IOException e) {
            }
        }
    }

    public static void sendToBooking(String bookingId, String jsonMessage) {                                            //Метод для отправки сообщения на конкретный bookingId
        Set<Session> sessions = SESSIONS.get(bookingId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(jsonMessage);
                } catch (IOException e) {
                    removeSession(bookingId, session);
                }
            } else {
                removeSession(bookingId, session);
            }
        }
    }

    private static void removeSession(String bookingId, Session session) {                                              //Если после удаления нет активных сессий для bookingId — удаляет ключ.
        Set<Session> sessions = SESSIONS.get(bookingId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                SESSIONS.remove(bookingId);
            }
        }
    }
}