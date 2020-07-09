package com.dotmarketing.portlets.workflows.model.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into {@link WorkflowTask}
 * instances
 */
public class WorkflowTaskTransformer implements DBTransformer<WorkflowTask> {
    final List<WorkflowTask> list;


    public WorkflowTaskTransformer(List<Map<String, Object>> initList){
        List<WorkflowTask> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<WorkflowTask> asList() {
        return this.list;
    }

    @NotNull
    public static WorkflowTask transform(Map<String, Object> map)  {
        final WorkflowTask task = new WorkflowTask();
        task.setBelongsTo((String) map.get("belongs_to"));
        task.setDueDate((Date) map.get("due_date"));
        task.setModDate((Date) map.get("mod_date"));
        task.setDescription((String) map.get("description"));
        task.setCreationDate((Date) map.get("creation_date"));
        task.setLanguageId((Long) map.get("language_id"));
        task.setTitle((String) map.get("title"));
        task.setCreatedBy((String) map.get("created_by"));
        task.setWebasset((String) map.get("webasset"));
        task.setId((String) map.get("id"));
        task.setAssignedTo((String) map.get("assigned_to"));
        task.setStatus((String) map.get("status"));
        return task;
    }
}

