package com.axelor.apps.camera.web;

import com.axelor.apps.camera.service.CameraDataService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class CameraDataController {
    public void startListener(ActionRequest request, ActionResponse response) {
        CameraDataService service = Beans.get(CameraDataService.class);
        new Thread(service::listenQueue).start();
        response.setNotify("CameraData listener started!");
    }
}
