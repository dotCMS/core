package com.dotmarketing.portlets.workflows.model;

import java.util.Date;

public interface WorkflowTimelineItem {

    public Date createdDate(); 
    
    public String roleId();
    
    public String actionId();
    
    public String stepId();
    
    public String commentDescription();
    
    public String taskId();
    
    public String type();
    
}
