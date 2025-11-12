package com.axelor.todo.module;

import com.axelor.app.AxelorModule;
import com.axelor.todo.service.TaskService;
import com.axelor.todo.service.TaskServiceImpl;

public class TodoModule extends AxelorModule {
  @Override
  protected void configure() {
    bind(TaskService.class).to(TaskServiceImpl.class);
  }
}
