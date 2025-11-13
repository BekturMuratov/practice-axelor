package com.axelor.apps.gst.db.repo;

import com.axelor.apps.gst.model.IProduct;
import com.axelor.apps.gst.model.repo.IProductRepository;
import java.util.Map;

public class GstProductRepository extends IProductRepository {

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    if (!context.containsKey("product-enhance")) {
      return json;
    }
    try {
      Long id = (Long) json.get("id");
      IProduct product = find(id);
      json.put("hasImage", product.getImage() != null);
    } catch (Exception e) {
    }
    return json;
  }
}
