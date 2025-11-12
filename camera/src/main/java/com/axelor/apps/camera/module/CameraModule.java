package com.axelor.apps.camera.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.camera.job.CameraJob;
import com.axelor.apps.camera.service.CameraDataService;
import com.axelor.apps.camera.service.CameraDataServiceImpl;

public class CameraModule extends AxelorModule {
    @Override
    protected void configure() {
        bind(CameraDataService.class).to(CameraDataServiceImpl.class);
        bind(CameraJob.class);

    }
}
