package com.axelor.apps.svh.module;

import com.axelor.app.AxelorModule;
import com.axelor.apps.svh.service.CrudService;
import com.axelor.apps.svh.service.TariffService;
import com.axelor.apps.svh.service.impl.CrudServiceImpl;
import com.axelor.apps.svh.service.impl.TariffServiceImpl;

public class SvhModule extends AxelorModule {
  @Override
  protected void configure() {
    bind(CrudService.class).to(CrudServiceImpl.class);
    bind(TariffService.class).to(TariffServiceImpl.class);
  }
}
