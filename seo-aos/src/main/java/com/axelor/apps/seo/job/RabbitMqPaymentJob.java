package com.axelor.apps.seo.job;

import com.axelor.apps.base.job.ThreadedBaseJob;
import com.axelor.apps.seo.service.RabbitMQService;
import org.quartz.JobExecutionContext;

import javax.inject.Inject;

public class RabbitMqPaymentJob extends ThreadedBaseJob{
    private final RabbitMQService service;

    @Inject
    public RabbitMqPaymentJob(RabbitMQService service) {
        this.service = service;
    }

    @Override
    public void executeInThread(JobExecutionContext context) {
        try{
            service.receivePaymentNotifications();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}