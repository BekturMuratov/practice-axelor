package com.axelor.apps.seo.web;

import com.axelor.web.StaticResourceProvider;

import java.util.List;

public class SeoStaticResources implements StaticResourceProvider {
    @Override
    public void register(List<String> resources) {
        resources.add("js/seo.js");
        resources.add("js/vehiclePicUri.js");
        resources.add("js/qrImage.js");
    }
}
