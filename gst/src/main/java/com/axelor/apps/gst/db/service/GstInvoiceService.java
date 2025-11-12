package com.axelor.apps.gst.db.service;

import com.axelor.apps.gst.model.IInvoice;
import com.axelor.apps.gst.model.IInvoiceLine;

import java.util.List;

public interface GstInvoiceService {
    public IInvoice setGstAmount(IInvoice invoice);

    public IInvoice setPartyDetail(IInvoice invoice);

    public IInvoiceLine calculateInvoiceLineGst(IInvoiceLine invoiceLine, Boolean state);

    public IInvoiceLine setSelectedProductInvoice(IInvoiceLine invoiceLine);

    public List<IInvoiceLine> setInvoiceDetailOnChange(IInvoice invoice);
}
