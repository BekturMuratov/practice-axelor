package com.axelor.apps.seo.events.listeners;

import com.axelor.apps.seo.db.Requisite;

import javax.persistence.PostPersist;

public class RequisiteListener {

    @PostPersist
    public void afterPersist(Requisite requisite) {
        String prefix = "REQ";
        String number = String.format("%09d", requisite.getId());
        requisite.setRequisiteNumber(prefix + number);
    }
}
