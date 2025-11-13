package com.axelor.apps.gst.db.web;

import com.axelor.apps.gst.db.service.GstSequenceService;
import com.axelor.apps.gst.model.IInvoice;
import com.axelor.apps.gst.model.IParty;
import com.axelor.apps.gst.model.ISequence;
import com.axelor.apps.gst.model.repo.ISequenceRepository;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import javax.transaction.Transactional;

public class GstSequenceController {
  @Inject private GstSequenceService sequenceService;

  @Transactional
  public void setSequenceData(ActionRequest request, ActionResponse response) {
    ISequence sequence = request.getContext().asType(ISequence.class);
    ISequence isSequence =
        Beans.get(ISequenceRepository.class)
            .all()
            .filter("self.model.name=?", sequence.getModel().getName())
            .fetchOne();
    if (isSequence == null) {
      String nextNumber = sequenceService.setSequence(sequence);
      response.setValue("nextNumber", nextNumber);
    }
  }

  @Transactional
  public void setPartySequence(ActionRequest request, ActionResponse response) {
    IParty party = request.getContext().asType(IParty.class);
    if (party.getReference() == null) {
      ISequence sequence =
          Beans.get(ISequenceRepository.class)
              .all()
              .filter("self.model.name = ?", "Party")
              .fetchOne();
      if (sequence != null) {
        party.setReference(sequence.getNextNumber());
        response.setValues(party);
        sequenceService.setReference(sequence);
      } else {
        response.setError("sequnce for Party was not found");
      }
    }
  }

  @Transactional
  public void setInvoiceSequence(ActionRequest request, ActionResponse response) {
    IInvoice invoice = request.getContext().asType(IInvoice.class);
    if (invoice.getReference() == null) {
      ISequence sequence =
          Beans.get(ISequenceRepository.class)
              .all()
              .filter("self.model.name = ?", "Invoice")
              .fetchOne();
      if (sequence != null) {
        invoice.setReference(sequence.getNextNumber());
        response.setValues(invoice);
        sequenceService.setReference(sequence);
      } else {
        response.setError("sequence for Invoice was not found");
      }
    }
  }
}
