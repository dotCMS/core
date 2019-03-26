package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import java.util.List;

// This interface should have default package access
public abstract class WorkflowCache implements Cachable {

  public final String defaultKey = "DEFAULT_WORKFLOW_SCHEME";
  protected static String PRIMARY_GROUP = "WorkflowCache";
  protected static String TASK_GROUP = "WorkflowTaskCache";
  protected static String STEP_GROUP = "WorkflowStepCache";
  protected static String ACTION_GROUP = "WorkflowActionCache";
  protected static String ACTION_CLASS_GROUP = "WorkflowActionClassCache";

  protected abstract WorkflowScheme add(WorkflowScheme scheme);

  public abstract WorkflowScheme getScheme(String key);

  protected abstract WorkflowStep getStep(String key);

  protected abstract List<WorkflowStep> getSteps(Contentlet con);

  protected abstract WorkflowTask getTask(Contentlet key);

  protected abstract List<WorkflowStep> addSteps(Contentlet contentlet, List<WorkflowStep> steps);

  protected abstract WorkflowStep addStep(WorkflowStep step);

  protected abstract WorkflowTask addTask(WorkflowTask step);

  protected abstract WorkflowTask addTask(Contentlet contentlet, WorkflowTask task);

  protected abstract WorkflowStep add(WorkflowStep step);

  protected abstract List<WorkflowAction> addActions(
      WorkflowStep step, List<WorkflowAction> actions);

  protected abstract List<WorkflowAction> addActions(
      WorkflowScheme scheme, List<WorkflowAction> actions);

  protected abstract List<WorkflowActionClass> addActionClasses(
      WorkflowAction action, List<WorkflowActionClass> actionClasses);

  protected abstract List<WorkflowAction> getActions(WorkflowStep step);

  protected abstract List<WorkflowActionClass> getActionClasses(final WorkflowAction action);

  protected abstract List<WorkflowAction> getActions(WorkflowScheme scheme);

  public abstract void clearCache();

  protected abstract void remove(Contentlet contentlet);

  public abstract void remove(WorkflowScheme scheme);

  public abstract void remove(WorkflowStep step);

  public abstract void remove(WorkflowAction action);

  protected abstract void removeActions(WorkflowStep step);

  protected abstract void removeActions(WorkflowScheme scheme);

  protected abstract void remove(WorkflowTask task);

  protected abstract List<WorkflowScheme> getSchemesByStruct(String key);

  protected abstract List<WorkflowScheme> addForStructure(
      String struct, List<WorkflowScheme> scheme);

  protected abstract void removeStructure(String struct);

  protected void flushSteps() {
    CacheLocator.getCacheAdministrator().flushGroup(STEP_GROUP);
  }

  protected void flushTasks() {
    CacheLocator.getCacheAdministrator().flushGroup(TASK_GROUP);
  }

  protected abstract void clearStepsCache();

  public abstract WorkflowScheme getDefaultScheme();

  protected abstract WorkflowScheme addDefaultScheme(WorkflowScheme scheme);

  public String getPrimaryGroup() {
    return PRIMARY_GROUP;
  }

  public String[] getGroups() {
    return new String[] {PRIMARY_GROUP, TASK_GROUP, STEP_GROUP};
  }

  protected abstract void add404Task(Contentlet contentlet);

  protected abstract boolean is404(Contentlet contentlet);
}
