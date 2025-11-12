package com.axelor.todo.web;

import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.todo.model.Task;
import com.axelor.todo.service.TaskService;
import com.google.inject.Inject;
import java.util.Map;

public class TodoController {
  @Inject TaskService taskService;

  public void data(ActionRequest request, ActionResponse response) {
    response.setValues(
        Map.of(
            "name", "Захватить мир",
            "description", "Отбой ребята! Это шутка!"));
    response.setNotify("Поля заполнены!");
  }

  public void done(ActionRequest request, ActionResponse response) {
    Task task = request.getContext().asType(Task.class);
    taskService.done(task, response);
  }
}
