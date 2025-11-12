package com.axelor.apps.camera.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.camera.job.CameraJob;
import com.axelor.apps.camera.service.CameraDataService;
import com.axelor.apps.camera.service.CrudService;
import com.axelor.apps.camera.service.impl.CameraDataServiceImpl;
import com.axelor.apps.camera.service.impl.CrudServiceImpl;

public class CameraModule extends AxelorModule {

    @Override
    protected void configure() {
        bind(CrudService.class).to(CrudServiceImpl.class);
        bind(CameraDataService.class).to(CameraDataServiceImpl.class);
        bind(CameraJob.class);
    }
}
