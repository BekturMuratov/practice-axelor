package com.axelor.apps.camera.job;

import com.axelor.apps.base.job.ThreadedBaseJob;
import com.axelor.apps.base.job.UncheckedJobExecutionException;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.apps.camera.service.CameraDataService;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class CameraJob extends ThreadedBaseJob {

    @Override
    public void executeInThread(JobExecutionContext context) {
        try {
            Beans.get(CameraDataService.class).listenQueue();
        } catch (Exception e) {
            TraceBackService.trace(e);
            throw new UncheckedJobExecutionException(e);
        }
    }
}
