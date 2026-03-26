package com.axelor.apps.seo.job;

import com.axelor.apps.base.job.ThreadedBaseJob;
import com.axelor.apps.seo.service.RabbitMQService;
import org.quartz.JobExecutionContext;

import javax.inject.Inject;

public class RabbitMqJob extends ThreadedBaseJob {
    private final RabbitMQService service;

    @Inject
    public RabbitMqJob(RabbitMQService service) {
        this.service = service;
    }

    @Override
    public void executeInThread(JobExecutionContext context) {
        try{
            service.receiveMessages();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
