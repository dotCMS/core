package com.dotmarketing.portlets.workflows.model.transform;

import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
        scheme.setArchived(ConversionUtils.toBooleanFromDb(map.getOrDefault("archived",false)));
        scheme.setEntryActionId((String) map.get("entry_action_id"));
        scheme.setId((String) map.get("id"));
        scheme.setName((String) map.get("name"));
        scheme.setVariableName((String) map.get("variable_name"));
        scheme.setDescription((String) map.get("description"));
        scheme.setModDate((Date) map.get("mod_date"));
        scheme.setMandatory(ConversionUtils.toBooleanFromDb(map.getOrDefault("mandatory",false)));
        scheme.setDefaultScheme(ConversionUtils.toBooleanFromDb(map.getOrDefault("default_scheme",false)));
        return scheme;
    }
}

