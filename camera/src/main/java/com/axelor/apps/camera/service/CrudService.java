package com.axelor.apps.camera.service;

import com.axelor.db.Model;

public interface CrudService {

  <T extends Model> T persistObject(T entity);

  <T extends Model> void removeObject(T entity);
}
