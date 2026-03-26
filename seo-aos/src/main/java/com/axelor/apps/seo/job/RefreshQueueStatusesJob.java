package com.axelor.apps.seo.job;

import com.axelor.apps.base.job.ThreadedBaseJob;
import com.axelor.apps.seo.service.RegistrationService;
import org.quartz.JobExecutionContext;

import javax.inject.Inject;

public class RefreshQueueStatusesJob extends ThreadedBaseJob {
    @Inject
    private RegistrationService service;
    @Override
    public void executeInThread(JobExecutionContext context) {
        try {
            service.updateRegistrationSortingAndLiveQueue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
