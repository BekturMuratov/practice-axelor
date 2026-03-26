package com.axelor.apps.seo.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.seo.events.LoginObserver;
import com.axelor.apps.seo.events.listeners.*;
import com.axelor.apps.seo.job.RabbitMqJob;
import com.axelor.apps.seo.job.RefreshQueueStatusesJob;
import com.axelor.apps.seo.job.SseJob;
import com.axelor.apps.seo.service.*;
import com.axelor.apps.seo.service.impl.*;
import com.axelor.apps.seo.utils.sse.*;

import javax.ws.rs.sse.Sse;

public class SeoModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(CrudService.class).to(CrudServiceImpl.class);
        bind(SseWebService.class);
        bind(SseBroadcasterManager.class);
        bind(Sse.class).toProvider(SseProvider.class);
        bind(SseJob.class);
        bind(RefreshQueueStatusesJob.class);
        bind(RegistrationService.class).to(RegistrationServiceImpl.class);
        bind(RegistrationListener.class);
        bind(BookingListener.class);
        bind(UserListener.class);
        bind(LoginObserver.class);
        bind(CameraService.class).to(CameraServiceImpl.class);
        bind(RabbitMQService.class).to(RabbitMQServiceImpl.class);
        bind(RabbitMqJob.class);
        bind(BookingService.class).to(BookingServiceImpl.class);
        bind(BandwidthSettingService.class).to(BandwidthSettingServiceImpl.class);
        bind(SelectService.class).to(SelectServiceImpl.class);
        bind(LetterService.class).to(LetterServiceImpl.class);
        bind(DashboardService.class).to(DashboardServiceImpl.class);
        bind(PaymentService.class).to(PaymentServiceImpl.class);
        bind(PaymentNotificationService.class).to(PaymentNotificationServiceImpl.class);
        bind(ReportService.class).to(ReportServiceImpl.class);
    }
}
