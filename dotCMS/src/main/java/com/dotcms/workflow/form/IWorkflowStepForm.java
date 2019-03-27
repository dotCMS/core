package com.dotcms.workflow.form;

public interface IWorkflowStepForm {

    String getStepName();

    boolean isEnableEscalation();

    String getEscalationAction();

    String getEscalationTime();

    boolean isStepResolved();

}
