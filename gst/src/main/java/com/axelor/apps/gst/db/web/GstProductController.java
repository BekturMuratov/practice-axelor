package com.axelor.apps.gst.db.web;

import com.axelor.apps.gst.model.IInvoice;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class GstProductController {

  public void getSelectedProductIds(ActionRequest request, ActionResponse response) {

    @SuppressWarnings("unchecked")
    List<Integer> idList = (List<Integer>) request.getContext().get("_ids");

    if (idList == null) {
      response.setError("select atleast one product from grid");
    } else {
      String idStringList = StringUtils.join(idList, ",");
      request.getContext().put("ids", idStringList);
    }
  }

  public void generateNewInvoice(ActionRequest request, ActionResponse response) {
    @SuppressWarnings("unchecked")
    List<Integer> idList = (List<Integer>) request.getContext().get("_ids");

    if (idList == null) {
      response.setError("Select Any Product for Invoice Generation");
    } else {
      response.setView(
          ActionView.define("invoices")
              .model(IInvoice.class.getName())
              .add("form", "invoice-form")
              .context("productIds", idList)
              .map());
    }
  }
}
