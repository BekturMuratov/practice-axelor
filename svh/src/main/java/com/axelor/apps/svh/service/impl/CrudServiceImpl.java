package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.service.CrudService;
import com.axelor.db.JPA;
import com.axelor.db.Model;

public class CrudServiceImpl implements CrudService {

  @Override
  public <T extends Model> T persistObject(T entity) {
    if (!JPA.em().getTransaction().isActive()) {
      JPA.em().getTransaction().begin();
    }
    try {
      T savedEntity = JPA.save(entity);
      JPA.em().getTransaction().commit();
      return savedEntity;
    } catch (Exception e) {
      JPA.em().getTransaction().rollback();
      throw e;
    }
  }

  @Override
  public <T extends Model> void removeObject(T entity) {
    if (!JPA.em().getTransaction().isActive()) {
      JPA.em().getTransaction().begin();
    }
    try {
      JPA.remove(entity);
      JPA.em().getTransaction().commit();
    } catch (Exception e) {
      JPA.em().getTransaction().rollback();
      throw e;
    }
  }
}
