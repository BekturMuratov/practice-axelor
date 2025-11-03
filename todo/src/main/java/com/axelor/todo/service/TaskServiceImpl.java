package com.axelor.todo.service;

import com.axelor.rpc.ActionResponse;
import com.axelor.todo.model.Task;
import com.axelor.todo.model.repo.TaskRepository;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.joda.time.LocalDateTime;

import java.util.Date;

@Singleton
public class TaskServiceImpl implements TaskService{
    @Inject
    TaskRepository taskRepository;

    @Override
    public void done(Task task, ActionResponse response) {
        task.setStatus("Done");
        task.setUpdated_at(LocalDateTime.now().toString());

        response.setValue("status", "Done");
        response.setValue("updated_at", new Date().toString());
    }
}
