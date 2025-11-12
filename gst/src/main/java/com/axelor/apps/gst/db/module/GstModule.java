package com.axelor.apps.gst.db.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.gst.db.repo.GstPartyRepository;
import com.axelor.apps.gst.db.repo.GstProductRepository;
import com.axelor.apps.gst.db.service.GstInvoiceService;
import com.axelor.apps.gst.db.service.GstInvoiceServiceImpl;
import com.axelor.apps.gst.db.service.GstSequenceService;
import com.axelor.apps.gst.db.service.GstSequenceServiceImpl;
import com.axelor.apps.gst.model.repo.IPartyRepository;
import com.axelor.apps.gst.model.repo.IProductRepository;

public class GstModule  extends AxelorModule {
    @Override
    protected void configure() {
        bind(IPartyRepository.class).to(GstPartyRepository.class);
        bind(IProductRepository.class).to(GstProductRepository.class);
        bind(GstInvoiceService.class).to(GstInvoiceServiceImpl.class);
        bind(GstSequenceService.class).to(GstSequenceServiceImpl.class);
    }
}
