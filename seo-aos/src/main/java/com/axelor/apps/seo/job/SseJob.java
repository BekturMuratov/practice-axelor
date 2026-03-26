package com.axelor.apps.seo.job;

import com.axelor.apps.base.job.ThreadedBaseJob;
import com.axelor.apps.seo.utils.sse.SseWebService;
import com.google.inject.Inject;
import org.quartz.JobExecutionContext;

public class SseJob extends ThreadedBaseJob {

    @Inject
    private SseWebService sseWebService;

    @Override
    public void executeInThread(JobExecutionContext context) {
        try {
            sseWebService.cleanOldSinks();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
