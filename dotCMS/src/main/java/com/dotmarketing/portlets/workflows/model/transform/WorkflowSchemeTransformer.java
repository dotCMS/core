package com.dotmarketing.portlets.workflows.model.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into {@link WorkflowScheme}
 * instances
 */
public class WorkflowSchemeTransformer implements DBTransformer<WorkflowScheme> {
    final List<WorkflowScheme> list;


    public WorkflowSchemeTransformer(List<Map<String, Object>> initList){
        List<WorkflowScheme> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<WorkflowScheme> asList() {
        return this.list;
    }

    @NotNull
    public static WorkflowScheme transform(Map<String, Object> map)  {
        final WorkflowScheme scheme = new WorkflowScheme();
        scheme.setArchived((Boolean) map.get("archived"));
        scheme.setEntryActionId((String) map.get("entry_action_id"));
        scheme.setId((String) map.get("id"));
        scheme.setName((String) map.get("name"));
        scheme.setDescription((String) map.get("description"));
        scheme.setModDate((Date) map.get("mod_date"));
        scheme.setMandatory((Boolean) map.get("mandatory"));
        scheme.setDefaultScheme((Boolean) map.get("default_scheme"));
        return scheme;
    }
}

