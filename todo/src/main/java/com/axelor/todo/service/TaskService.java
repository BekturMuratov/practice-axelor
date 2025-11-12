package com.axelor.todo.service;

import com.axelor.rpc.ActionResponse;
import com.axelor.todo.model.Task;

public interface TaskService {
  void done(Task task, ActionResponse response);
}
