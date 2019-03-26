package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.List;

// This interface should have default package access
public class WorkflowCacheImpl extends WorkflowCache {

  private DotCacheAdministrator cache;
  static final String FOUR_OH_FOUR_TASK = "404WorkflowTask";

  public WorkflowCacheImpl() {
    cache = CacheLocator.getCacheAdministrator();
  }

  protected WorkflowScheme addDefaultScheme(WorkflowScheme scheme) {

    cache.put(defaultKey, scheme, getPrimaryGroup());
    return scheme;
  }

  protected List<WorkflowScheme> addForStructure(String struct, List<WorkflowScheme> schemes) {
    String key = struct;

    // Add the key to the cache
    cache.put(key, schemes, getPrimaryGroup());
    return schemes;
  }

  protected WorkflowScheme add(WorkflowScheme scheme) {

    String key = scheme.getId();

    // Add the key to the cache
    cache.put(key, scheme, getPrimaryGroup());

    return scheme;
  }

  public WorkflowScheme getScheme(String key) {

    WorkflowScheme scheme = null;
    try {
      scheme = (WorkflowScheme) cache.get(key, getPrimaryGroup());
    } catch (DotCacheException e) {
      Logger.debug(this, "Cache Entry not found", e);
    }
    return scheme;
  }

  protected List<WorkflowScheme> getSchemesByStruct(String structId) {

    try {
      return (List<WorkflowScheme>) cache.get(structId, getPrimaryGroup());
    } catch (DotCacheException e) {
      Logger.debug(this, "Cache Entry not found", e);
    }
    return null;
  }

  protected void removeStructure(String struct) {
    if (struct != null) {
      cache.remove(struct, getPrimaryGroup());
    }
  }

  public void remove(WorkflowScheme scheme) {
    if (scheme != null && UtilMethods.isSet(scheme)) {
      cache.remove(scheme.getId(), getPrimaryGroup());
    }
  }

  public void remove(WorkflowStep step) {
    cache.remove(step.getId(), STEP_GROUP);
    cache.remove(step.getId(), ACTION_GROUP);
  }

  protected void removeActions(WorkflowStep step) {
    if (step != null) cache.remove(step.getId(), ACTION_GROUP);
  }

  protected void removeActions(final WorkflowScheme scheme) {
    if (scheme != null) cache.remove(scheme.getId(), ACTION_GROUP);
  }

  protected void remove(WorkflowTask task) {
    if (UtilMethods.isSet(task) && UtilMethods.isSet(task.getWebasset())) {
      cache.remove(
          this.getKey(task.getWebasset(), task.getLanguageId()), TASK_GROUP); // remove the task id.
      cache.remove(this.getKey(task.getWebasset(), task.getLanguageId()), STEP_GROUP);
    }
    if (UtilMethods.isSet(task) && UtilMethods.isSet(task.getId())) {
      cache.remove(task.getId(), TASK_GROUP); // remove the task.
    }
  }

  protected void remove(final Contentlet contentlet) {

    if (contentlet != null && UtilMethods.isSet(contentlet.getIdentifier())) {

      cache.remove(this.getKey(contentlet), STEP_GROUP);
      cache.remove(this.getKey(contentlet), TASK_GROUP);
    }
  }

  public WorkflowScheme getDefaultScheme() {
    return getScheme(defaultKey);
  }

  public void clearCache() {
    // clear the cache
    for (String x : getGroups()) {
      cache.flushGroup(x);
    }
  }

  @Override
  protected void clearStepsCache() {
    // clear the cache
    cache.flushGroup(STEP_GROUP);
  }

  protected WorkflowStep getStep(String key) {
    WorkflowStep step = null;
    try {
      step = (WorkflowStep) cache.get(key, STEP_GROUP);
    } catch (DotCacheException e) {
      Logger.debug(this, "Cache Entry not found", e);
    }
    return step;
  }

  protected WorkflowStep add(WorkflowStep step) {
    String key = step.getId();

    // Add the key to the cache
    cache.put(key, step, STEP_GROUP);

    return step;
  }

  protected boolean is404(final Contentlet contentlet) {

    String taskId = null;

    try {

      taskId = (String) cache.get(this.getKey(contentlet), TASK_GROUP);
    } catch (DotCacheException e) {
      Logger.error(this.getClass(), e.getMessage());
    }

    return (FOUR_OH_FOUR_TASK.equals(taskId));
  }

  protected WorkflowTask getTask(final Contentlet contentlet) {

    WorkflowTask task = null;
    try {

      final String taskId = (String) cache.get(this.getKey(contentlet), TASK_GROUP);
      task = (WorkflowTask) cache.get(taskId, TASK_GROUP);
    } catch (Exception e) {
      Logger.debug(this.getClass(), e.getMessage());
    }
    return task;
  }

  protected List<WorkflowStep> getSteps(Contentlet contentlet) {

    List<WorkflowStep> steps = null;
    try {
      steps = (List<WorkflowStep>) cache.get(this.getKey(contentlet), STEP_GROUP);
    } catch (Exception e) {
      Logger.debug(this.getClass(), e.getMessage());
    }
    return steps;
  }

  protected WorkflowStep addStep(WorkflowStep step) {
    if (step == null || !UtilMethods.isSet(step.getId())) {
      return null;
    }

    cache.put(step.getId(), step, STEP_GROUP);

    return step;
  }

  protected WorkflowTask addTask(final WorkflowTask task) {

    if (task == null || !UtilMethods.isSet(task.getId())) {
      return null;
    }

    cache.put(task.getId(), task, TASK_GROUP);

    return task;
  }

  protected List<WorkflowStep> addSteps(Contentlet contentlet, List<WorkflowStep> steps) {
    if (contentlet == null || !UtilMethods.isSet(contentlet.getIdentifier()) || steps == null) {
      return null;
    }

    // Add the Step id as a string to the cache
    cache.put(this.getKey(contentlet), steps, STEP_GROUP);
    return steps;
  }

  @Override
  protected void add404Task(final Contentlet contentlet) {
    if (contentlet == null || !UtilMethods.isSet(contentlet.getIdentifier())) {
      return;
    }
    // Add the key to the cache
    cache.put(this.getKey(contentlet), FOUR_OH_FOUR_TASK, TASK_GROUP);
    return;
  }

  protected WorkflowTask addTask(final Contentlet contentlet, final WorkflowTask task) {

    if (contentlet == null
        || !UtilMethods.isSet(contentlet.getIdentifier())
        || task == null
        || !UtilMethods.isSet(task.getId())) {

      return null;
    }

    // Add the key to the cache
    cache.put(this.getKey(contentlet), task.getId(), TASK_GROUP);
    return addTask(task);
  }

  private String getKey(final Contentlet contentlet) {

    return this.getKey(contentlet.getIdentifier(), contentlet.getLanguageId());
  }

  private String getKey(final String assetId, final long languageId) {

    return new StringBuilder(assetId).append("_").append(languageId).toString();
  }

  @Override
  protected List<WorkflowAction> addActions(WorkflowStep step, List<WorkflowAction> actions) {
    if (step == null || actions == null) return null;
    cache.put(step.getId(), actions, ACTION_GROUP);
    return actions;
  }

  protected List<WorkflowAction> addActions(
      final WorkflowScheme scheme, final List<WorkflowAction> actions) {

    if (scheme == null || actions == null) return null;

    cache.put(scheme.getId(), actions, ACTION_GROUP);
    return actions;
  }

  @Override
  protected List<WorkflowAction> getActions(WorkflowStep step) {
    if (step == null) return null;
    try {
      return (List<WorkflowAction>) cache.get(step.getId(), ACTION_GROUP);
    } catch (DotCacheException e) {
      Logger.debug(WorkflowCacheImpl.class, e.getMessage(), e);
    }
    return null;
  }

  @Override
  protected List<WorkflowActionClass> getActionClasses(final WorkflowAction action) {

    if (action != null) {
      try {
        return (List<WorkflowActionClass>) cache.get(action.getId(), ACTION_CLASS_GROUP);
      } catch (DotCacheException e) {
        Logger.debug(WorkflowCacheImpl.class, e.getMessage(), e);
      }
    }
    return null;
  }

  @Override
  protected List<WorkflowActionClass> addActionClasses(
      final WorkflowAction action, final List<WorkflowActionClass> actionClasses) {

    if (action == null || actionClasses == null) {
      return null;
    }

    cache.put(action.getId(), actionClasses, ACTION_CLASS_GROUP);
    return actionClasses;
  }

  @Override
  public void remove(final WorkflowAction action) {

    cache.remove(action.getId(), ACTION_CLASS_GROUP);
  }

  @Override
  protected List<WorkflowAction> getActions(WorkflowScheme scheme) {
    if (scheme == null) return null;
    try {
      return (List<WorkflowAction>) cache.get(scheme.getId(), ACTION_GROUP);
    } catch (DotCacheException e) {
      Logger.debug(WorkflowCacheImpl.class, e.getMessage(), e);
    }
    return null;
  }
}
