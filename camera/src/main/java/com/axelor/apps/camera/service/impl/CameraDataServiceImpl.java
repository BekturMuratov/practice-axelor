package com.axelor.apps.camera.service.impl;

import com.axelor.app.AppSettings;
import com.axelor.apps.camera.db.CameraData;
import com.axelor.apps.camera.service.CameraDataService;
import com.axelor.apps.camera.service.CrudService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;


public class CameraDataServiceImpl implements CameraDataService {

    private final CrudService crudService;
    private final ObjectMapper objectMapper;

    @Inject
    public CameraDataServiceImpl(CrudService crudService, ObjectMapper objectMapper) {
        this.crudService = crudService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void listenQueue() {
        String host = AppSettings.get().get("rabbit.mq.url");
        int port = Integer.parseInt(AppSettings.get().get("rabbit.mq.port"));
        String queueName = AppSettings.get().get("rabbit.mq.queue");
        String username = AppSettings.get().get("rabbit.mq.username");
        String password = AppSettings.get().get("rabbit.mq.password");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);

        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(queueName,true, false,false,null);
            System.out.println(" [*] Waiting for messages in queue '" + queueName + "'...");

            DeliverCallback deliverCallback =(consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [x] Received '" + message + "'");
                saveCameraData(message);
            };

            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        }
        catch (IOException | TimeoutException e) {
            e.printStackTrace();

        }
    }


    private void saveCameraData(String json) {
        try {
            CameraData data = objectMapper.readValue(json, CameraData.class);
            crudService.persistObject(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
