package com.axelor.apps.gst.db.service;

import com.axelor.apps.gst.model.ISequence;
import com.axelor.apps.gst.model.repo.ISequenceRepository;
import com.google.inject.Inject;
import javax.transaction.Transactional;

public class GstSequenceServiceImpl implements GstSequenceService {

  @Inject private ISequenceRepository sequenceRepository;

  @Override
  @Transactional
  public String setSequence(ISequence sequence) {
    String suffix = sequence.getSufix();
    String prefix = sequence.getPrefix();
    String newNextNumber = null;
    int padding = (int) sequence.getPadding();
    String newPadding = "";

    for (int i = 0; i < padding; i++) {
      newPadding = newPadding + "0";
    }

    if (suffix == null) {
      newNextNumber = prefix + newPadding;
    } else {
      newNextNumber = prefix + newPadding + suffix;
    }

    sequence.setNextNumber(newNextNumber);
    return newNextNumber;
  }

  @Transactional
  public void setReference(ISequence sequence) {
    if (sequence != null) {
      String suffix = sequence.getSufix();
      String prefix = sequence.getPrefix();
      String nextNumber;
      int paddingValue =
          Integer.parseInt(
                  (sequence
                      .getNextNumber()
                      .substring(prefix.length(), prefix.length() + sequence.getPadding())))
              + 1;
      String newPadding = Integer.toString(paddingValue);
      while (newPadding.length() < sequence.getPadding()) {
        newPadding = "0" + newPadding;
      }
      if (suffix == null) {
        nextNumber = prefix + newPadding;
      } else {
        nextNumber = prefix + newPadding + suffix;
      }
      sequence.setNextNumber(nextNumber);
      ;
    }
  }
}
