package com.dotmarketing.portlets.workflows.model.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into {@link com.dotmarketing.portlets.workflows.model.WorkflowComment}
 * instances
 */
public class WorkflowCommentTransformer implements DBTransformer {
    final List<WorkflowComment> list;


    public WorkflowCommentTransformer(List<Map<String, Object>> initList){
        List<WorkflowComment> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<WorkflowComment> asList() {
        return this.list;
    }

    @NotNull
    private static WorkflowComment transform(Map<String, Object> map)  {
        final WorkflowComment comment;
        comment = new WorkflowComment();
        comment.setId((String) map.get("id"));
        comment.setCreationDate((Date) map.get("creation_date"));
        comment.setPostedBy((String) map.get("posted_by"));
        comment.setComment((String) map.get("wf_comment"));
        comment.setWorkflowtaskId((String) map.get("workflowtask_id"));
        return comment;
    }
}

