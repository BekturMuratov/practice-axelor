package com.axelor.apps.camera.service;

import com.axelor.app.AppSettings;
import com.axelor.apps.camera.db.CameraData;
import com.axelor.apps.camera.db.repo.CameraDataRepository;
import com.axelor.db.JPA;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;


public class CameraDataServiceImpl implements CameraDataService {


    private final CameraDataRepository cameraDataRepository;

    @Inject
    public CameraDataServiceImpl(CameraDataRepository repository) {
        this.cameraDataRepository = repository;
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
            ObjectMapper mapper = new ObjectMapper();
            CameraData data = mapper.readValue(json, CameraData.class);

            JPA.runInTransaction(() -> {
                cameraDataRepository.save(data);
            });

            System.out.println("Saved CameraData: " + data.getCrossRecordSysCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
