package com.axelor.apps.gst.db.service;

import com.axelor.apps.gst.model.*;
import com.axelor.common.ObjectUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GstInvoiceServiceImpl implements GstInvoiceService {
  @Override
  public IInvoice setGstAmount(IInvoice invoice) {
    BigDecimal igst = BigDecimal.ZERO,
        sgst = BigDecimal.ZERO,
        cgst = BigDecimal.ZERO,
        grossAmount = BigDecimal.ZERO,
        netAmount = BigDecimal.ZERO;

    List<IInvoiceLine> invoiceLineList = invoice.getInvoiceItemsList();
    if (invoiceLineList != null) {
      for (IInvoiceLine invoiceLine : invoiceLineList) {
        igst = igst.add(invoiceLine.getIgst());
        sgst = sgst.add(invoiceLine.getSgst());
        cgst = cgst.add(invoiceLine.getCgst());
        netAmount = netAmount.add(invoiceLine.getNetAmount());
        grossAmount = grossAmount.add(invoiceLine.getGrossAmount());
      }
    }
    invoice.setNetIgst(igst);
    invoice.setNetAmount(netAmount);
    invoice.setNetSgst(sgst);
    invoice.setNetCgst(cgst);
    return invoice;
  }

  @Override
  public IInvoice setPartyDetail(IInvoice invoice) {
    IParty party = invoice.getParty();
    if (party != null) {
      List<IContact> contactList = party.getContactList();
      if (!ObjectUtils.isEmpty(contactList)) {
        for (IContact contact : contactList) {
          if (contact.getType().equals("primary")) {
            invoice.setPartyContact(contact);
          }
        }
      } else {
        invoice.setPartyContact(null);
      }
      List<IAddress> addressList = party.getAddressList();
      if (!ObjectUtils.isEmpty(addressList)) {
        for (IAddress address : addressList) {
          if (address.getType().equals("invoice") || address.getType().equals("default")) {
            invoice.setInvoiceAddress(address);
          }
        }
      } else {
        invoice.setInvoiceAddress(null);
      }
      if (!ObjectUtils.isEmpty(addressList)) {
        if (invoice.getIsInvoiceAddAsShipping() == true) {
          invoice.setShippingAddress(invoice.getInvoiceAddress());
        } else {
          for (IAddress address : addressList) {
            if (address.getType().equals("shipping") || address.getType().equals("default")) {
              invoice.setShippingAddress(address);
            }
          }
        }
      } else {
        invoice.setShippingAddress(null);
      }
    }
    return invoice;
  }

  @Override
  public IInvoiceLine calculateInvoiceLineGst(IInvoiceLine invoiceLine, Boolean isIgst) {
    BigDecimal gstAmount;
    BigDecimal invoiceCgst;

    BigDecimal grossAmount;
    gstAmount =
        invoiceLine.getNetAmount().multiply(invoiceLine.getGstRate()).divide(new BigDecimal(100));
    if (isIgst) {
      invoiceCgst = gstAmount.divide(new BigDecimal(2));
      invoiceLine.setCgst(invoiceCgst);
      invoiceLine.setSgst(invoiceCgst);
      grossAmount = invoiceLine.getNetAmount().add(invoiceCgst).add(invoiceCgst);
    } else {
      invoiceLine.setIgst(gstAmount);
      grossAmount = invoiceLine.getNetAmount().add(gstAmount);
    }
    invoiceLine.setGrossAmount(grossAmount);
    return invoiceLine;
  }

  @Override
  public IInvoiceLine setSelectedProductInvoice(IInvoiceLine invoiceLine) {

    IProduct product = invoiceLine.getProduct();
    if (product != null) {
      invoiceLine.setGstRate(product.getGstRate());
      invoiceLine.setHsbn(product.getHsbn());
      invoiceLine.setPrice(product.getSalePrice());
      invoiceLine.setQty(1);
      invoiceLine.setItem("[" + product.getCode() + "]" + product.getName());
      invoiceLine.setNetAmount(
          invoiceLine.getPrice().multiply(new BigDecimal(invoiceLine.getQty())));
    }
    return invoiceLine;
  }

  @Override
  public List<IInvoiceLine> setInvoiceDetailOnChange(IInvoice invoice) {
    List<IInvoiceLine> invoiceLineList = invoice.getInvoiceItemsList();
    List<IInvoiceLine> invoiceline = new ArrayList<IInvoiceLine>();

    IParty party = invoice.getParty();
    BigDecimal gstAmount = BigDecimal.ZERO;
    BigDecimal invoiceIgst = BigDecimal.ZERO;
    BigDecimal invoiceCsgt = BigDecimal.ZERO;
    BigDecimal grossAmount = BigDecimal.ZERO;
    if (invoiceLineList != null) {
      if (party != null) {
        for (IInvoiceLine invoiceLine : invoiceLineList) {
          gstAmount =
              invoiceLine
                  .getNetAmount()
                  .multiply(invoiceLine.getGstRate())
                  .divide(new BigDecimal(100));

          if (invoice.getCompany().getAddress().getState()
              == invoice.getInvoiceAddress().getState()) {
            invoiceCsgt = gstAmount.divide(new BigDecimal(2));
            grossAmount = invoiceLine.getNetAmount().add(invoiceCsgt).add(invoiceCsgt);
          } else {
            invoiceIgst = gstAmount;
            grossAmount = invoiceLine.getNetAmount().add(gstAmount);
          }
          invoiceLine.setCgst(invoiceCsgt);
          invoiceLine.setSgst(invoiceCsgt);
          invoiceLine.setIgst(invoiceIgst);
          invoiceLine.setGrossAmount(grossAmount);
          invoiceline.add(invoiceLine);
        }
      }
    }
    return invoiceline;
  }
}
