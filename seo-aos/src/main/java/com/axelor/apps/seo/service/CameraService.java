package com.axelor.apps.seo.service;

import com.axelor.apps.seo.web.dto.RequestCameraData;
import java.io.IOException;

public interface CameraService {
    void saveCameraData(RequestCameraData requestCameraData) throws IOException;
}
