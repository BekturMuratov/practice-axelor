package com.axelor.apps.seo.service.impl;

import com.axelor.apps.seo.rest.dto.MetaSelectDTO;
import com.axelor.apps.seo.service.SelectService;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

public class SelectServiceImpl implements SelectService {

    private final EntityManager entityManager;

    @Inject
    public SelectServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<MetaSelectDTO> getSelection(String name) {
        return entityManager.createQuery(
                        "SELECT new com.axelor.apps.seo.rest.dto.MetaSelectDTO(i.value, i.title) " +
                                "FROM MetaSelectItem i " +
                                "WHERE i.select.name = :name " +
                                "ORDER BY i.order",
                        MetaSelectDTO.class
                )
                .setParameter("name", name)
                .getResultList();
    }
}
