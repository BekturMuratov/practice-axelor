package com.axelor.apps.seo.service.impl;

import com.axelor.apps.seo.db.AllMassifJson;
import com.axelor.apps.seo.integrations.bakai.dto.PaymentNotificationDto;
import com.axelor.apps.seo.service.CameraService;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.apps.seo.service.PaymentNotificationService;
import com.axelor.apps.seo.service.RabbitMQService;
import com.axelor.apps.seo.web.dto.RequestCameraData;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static com.axelor.apps.seo.utils.ConnectionUtils.*;

@Singleton
public class RabbitMQServiceImpl implements RabbitMQService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String QUEUE_NAME = getRabbitMqQueueName();
    private static final String PAYMENT_QUEUE = getRabbitMqPaymentQueueName();

    private final CameraService cameraService;
    private final CrudService crudService;
    private final ObjectMapper objectMapper;
    private final PaymentNotificationService paymentNotificationService;

    private final ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private String consumerTag;
    private boolean consumerStarted = false;

    private final ConnectionFactory paymentFactory;                                                                     // Payment consumer
    private Connection paymentConnection;
    private Channel paymentChannel;
    private String paymentConsumerTag;
    private boolean paymentConsumerStarted = false;

    @Inject
    public RabbitMQServiceImpl(CameraService cameraService, CrudService crudService, PaymentNotificationService paymentNotificationService) {
        this.cameraService = cameraService;
        this.crudService = crudService;
        this.paymentNotificationService = paymentNotificationService;
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new JavaTimeModule());

        this.factory = new ConnectionFactory();
        factory.setHost(getRabbitMqUrl());
        factory.setUsername(getRabbitMqUserName());
        factory.setPassword(getRabbitMqPassword());

        this.paymentFactory = new ConnectionFactory();
        paymentFactory.setHost(getRabbitMqPaymentUrl());
        paymentFactory.setUsername(getRabbitMqPaymentUserName());
        paymentFactory.setPassword(getRabbitMqPaymentPassword());
    }

    @Override
    public void receiveMessages() throws IOException, TimeoutException {
        synchronized (this) {
            if (!consumerStarted || connection == null || !connection.isOpen() || channel == null || !channel.isOpen()) {
                LOGGER.info("Starting RabbitMQ consumer for '{}'", QUEUE_NAME);

                closeConnection();

                connection = factory.newConnection();
                channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                channel.basicQos(1);

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    try {
                        processMessage(message);
                        channel.basicAck(deliveryTag, false);
                    } catch (Exception e) {
                        LOGGER.error("Error processing message: {}", e.getMessage(), e);
                        channel.basicNack(deliveryTag, false, true);
                    }
                };

                this.consumerTag = channel.basicConsume(QUEUE_NAME, false, deliverCallback, tag -> {
                });
                consumerStarted = true;

                LOGGER.info("RabbitMQ consumer started");
            } else {
                LOGGER.info("Consumer already running");
            }
        }
    }

    @Override
    public void receivePaymentNotifications() throws IOException, TimeoutException {
        synchronized (this) {
            if (paymentConsumerStarted && paymentConnection != null && paymentConnection.isOpen()
                    && paymentChannel != null && paymentChannel.isOpen()) {
                LOGGER.info("Payment consumer already running");
                return;
            }
            LOGGER.info("Starting RabbitMQ payment consumer for '{}'", PAYMENT_QUEUE);

            closePaymentConnection();

            paymentConnection = paymentFactory.newConnection();
            paymentChannel = paymentConnection.createChannel();
            paymentChannel.queueDeclare(PAYMENT_QUEUE, true, false, false, null);
            paymentChannel.basicQos(1);

            DeliverCallback deliverCallback = (tag, delivery) -> {
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                try {
                    processPaymentMessage(message);
                    paymentChannel.basicAck(deliveryTag, false);
                } catch (Exception e) {
                    LOGGER.error("Error processing payment message: {}", e.getMessage(), e);
                    paymentChannel.basicNack(deliveryTag, false, true);
                }
            };

            paymentConsumerTag = paymentChannel.basicConsume(PAYMENT_QUEUE, false, deliverCallback, tag -> {});
            paymentConsumerStarted = true;
            LOGGER.info("RabbitMQ payment consumer started");
        }
    }

    private void processMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            AllMassifJson allMassifJson = new AllMassifJson();
            allMassifJson.setAllJson(jsonString);
            String syscode = jsonNode.path("crossRecordSyscode").asText();
            allMassifJson.setCrossRecordSyscode(syscode);
            crudService.persistObject(allMassifJson);

            RequestCameraData responseFromJson = objectMapper.readValue(message, RequestCameraData.class);
            cameraService.saveCameraData(responseFromJson);
            LOGGER.info("Successfully processed message: {}", message);
        } catch (Exception e) {
            LOGGER.error("Error when processing message: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void processPaymentMessage(String message) {
        try {
            LOGGER.info("Received payment notification: {}", message);
            PaymentNotificationDto notification = objectMapper.readValue(message, PaymentNotificationDto.class);
            paymentNotificationService.processPaymentNotification(notification);
            LOGGER.info("Successfully processed payment notification: {}", notification.getOperationID());
        } catch (Exception e) {
            LOGGER.error("Error processing payment message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process payment message", e);
        }
    }

    private void closeConnection() {
        try {
            if (channel != null && channel.isOpen()) {
                if (consumerTag != null) {
                    channel.basicCancel(consumerTag);
                    LOGGER.info("Cancelled consumer: {}", consumerTag);
                }
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing RabbitMQ connection: {}", e.getMessage(), e);
        } finally {
            consumerTag = null;
            channel = null;
            connection = null;
            consumerStarted = false;
        }
    }

    private void closePaymentConnection() {                                                                             //Закрытие payment-соединения
        try {
            if (paymentChannel != null && paymentChannel.isOpen()) {
                if (paymentConsumerTag != null) {
                    paymentChannel.basicCancel(paymentConsumerTag);
                    LOGGER.info("Cancelled payment consumer: {}", paymentConsumerTag);
                }
                paymentChannel.close();
            }
            if (paymentConnection != null && paymentConnection.isOpen()) {
                paymentConnection.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing RabbitMQ payment connection: {}", e.getMessage(), e);
        } finally {
            paymentConsumerTag = null;
            paymentChannel = null;
            paymentConnection = null;
            paymentConsumerStarted = false;
        }
    }
}
