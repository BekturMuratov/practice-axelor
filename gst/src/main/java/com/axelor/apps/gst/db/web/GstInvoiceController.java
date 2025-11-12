package com.axelor.apps.gst.db.web;

import com.axelor.apps.gst.db.service.GstInvoiceService;
import com.axelor.apps.gst.model.IInvoice;
import com.axelor.apps.gst.model.IInvoiceLine;
import com.axelor.apps.gst.model.IProduct;
import com.axelor.apps.gst.model.repo.IProductRepository;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;

public class GstInvoiceController {

    @Inject private GstInvoiceService gstInvoiceService;
    @Inject IProductRepository productRepo;

    public void setPartyDetail(ActionRequest request, ActionResponse response) {
        IInvoice invoice = request.getContext().asType(IInvoice.class);
        invoice = gstInvoiceService.setPartyDetail(invoice);
        response.setValue("shippingAddress", invoice.getShippingAddress());
        response.setValue("invoiceAddress", invoice.getInvoiceAddress());
        response.setValue("partyContact", invoice.getPartyContact());
    }

    public void setTotalAmount(ActionRequest request, ActionResponse response) {
        IInvoice invoice = request.getContext().asType(IInvoice.class);
        invoice = gstInvoiceService.setGstAmount(invoice);
        response.setValue("netIgst", invoice.getNetIgst());
        response.setValue("netCgst", invoice.getNetCgst());
        response.setValue("netSgst", invoice.getNetSgst());
        response.setValue("netAmount", invoice.getNetAmount());
        response.setValue("grossAmount", invoice.getGrossAmount());
    }


    public void calculateGst(ActionRequest request, ActionResponse response) {
        IInvoiceLine invoiceLine = request.getContext().asType(IInvoiceLine.class);
        IInvoice invoice = request.getContext().asType(IInvoice.class);

        Boolean isIgst = invoice.getInvoiceAddress().getState().equals(invoice.getCompany().getAddress().getState())
                ? false
                : true;

        invoiceLine = gstInvoiceService.calculateInvoiceLineGst(invoiceLine, isIgst);
        response.setValue("igst", invoiceLine.getIgst());
        response.setValue("cgst", invoiceLine.getCgst());
        response.setValue("sgst", invoiceLine.getSgst());
        response.setValue("grossAmount", invoiceLine.getGrossAmount());
    }

    public void setInvoiceLineOnChangeParty(ActionRequest request, ActionResponse response) {
        IInvoice invoice = request.getContext().asType(IInvoice.class);
        List<IInvoiceLine> invoiceLineList = new ArrayList<>();
        invoiceLineList = gstInvoiceService.setInvoiceDetailOnChange(invoice);
        invoice = gstInvoiceService.setGstAmount(invoice);
        response.setValue("netIgst", invoice.getNetIgst());
        response.setValue("netCgst", invoice.getNetCgst());
        response.setValue("netSgst", invoice.getNetSgst());
        response.setValue("grossAmount", invoice.getGrossAmount());
        response.setValue("invoiceItemsList",invoiceLineList);
    }


    @SuppressWarnings("unchecked")
    public void getSelectedProducts(ActionRequest request, ActionResponse response) {
        List<Integer> productIdList = new ArrayList<>();
        productIdList = (List<Integer>) request.getContext().get("productIds");
        if(productIdList != null) {
            List<IInvoiceLine> invoiceLineList = new ArrayList<>();
            for(Integer ProductId : productIdList) {
                IProduct product = productRepo.find(ProductId.longValue());
                System.out.println(product);
                IInvoiceLine invoiceLine = new IInvoiceLine();
                invoiceLine.setProduct(product);
                invoiceLine =gstInvoiceService.setSelectedProductInvoice(invoiceLine);
                invoiceLineList.add(invoiceLine);
            }

            response.setValue("invoiceItemsList", invoiceLineList);
        }
    }

    public void setProductDetail(ActionRequest request, ActionResponse response) {
        IInvoiceLine invoiceLine = request.getContext().asType(IInvoiceLine.class);
        invoiceLine = gstInvoiceService.setSelectedProductInvoice(invoiceLine);
        response.setValues(invoiceLine);
    }
}
