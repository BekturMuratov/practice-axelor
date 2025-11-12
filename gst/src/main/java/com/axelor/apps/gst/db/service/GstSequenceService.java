package com.axelor.apps.gst.db.service;

import com.axelor.apps.gst.model.ISequence;

public interface GstSequenceService {
    public String setSequence(ISequence sequence);

    public void setReference(ISequence sequence);
}
