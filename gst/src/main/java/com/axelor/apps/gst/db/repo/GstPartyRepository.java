package com.axelor.apps.gst.db.repo;

import com.axelor.apps.gst.model.IParty;
import com.axelor.apps.gst.model.repo.IPartyRepository;
import java.util.Map;

public class GstPartyRepository extends IPartyRepository {

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    if (!context.containsKey("json-enhance")) {
      return json;
    }

    try {
      Long id = (Long) json.get("id");
      IParty party = find(id);
      json.put("addresses", party.getAddressList().get(0));
      json.put("contacts", party.getContactList().get(0));
    } catch (Exception e) {
    }
    return json;
  }
}
