package com.axelor.apps.seo.service;

import com.axelor.apps.seo.rest.dto.MetaSelectDTO;

import java.util.List;

public interface SelectService {
    List<MetaSelectDTO> getSelection(String name);
}
