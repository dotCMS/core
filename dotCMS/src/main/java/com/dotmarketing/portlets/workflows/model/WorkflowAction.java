package com.dotmarketing.portlets.workflows.model;

import com.dotmarketing.business.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import java.io.Serializable;
import java.util.*;

/**
 * Encapsulate the workflow action information.
 *
 *
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowAction implements Permissionable, Serializable{

	private static final long serialVersionUID = 1L;
	/**
	 * Key to store when the next step is current step.
	 */
	public  static final String CURRENT_STEP = "currentstep";

	private String id;


	private String name;

	@Deprecated
	private String stepId;
	private String schemeId;
	private String condition;
	private String nextStep;
	private String nextAssign;
	private String icon;
	private boolean roleHierarchyForAssign;
	private boolean requiresCheckout;
	private boolean assignable;
	private boolean commentable;
	private int order;
	private boolean saveActionlet;
	private boolean publishActionlet;
	private boolean unpublishActionlet;
	private boolean archiveActionlet;
	private boolean pushPublishActionlet;
	private boolean unarchiveActionlet;
	private boolean deleteActionlet;
	private boolean destroyActionlet;
	private Set<WorkflowState> showOn = Collections.emptySet();

	public WorkflowAction() {
	}

	/**
	 * True if the action should be show on archived status.
	 * @return boolean
	 */
	public boolean shouldShowOnArchived () {
		return this.showOn.contains(WorkflowState.ARCHIVED);
	}

	/**
	 * True if the action should be show on new status.
	 * @return boolean
	 */
	public boolean shouldShowOnNew () {
		return this.showOn.contains(WorkflowState.NEW);
	}

	/**
	 * True if the action should be show on publishActionlet status.
	 * @return boolean
	 */
	public boolean shouldShowOnPublished () {
		return this.showOn.contains(WorkflowState.PUBLISHED);
	}

	/**
	 * True if the action should be show on unpublish status.
	 * @return boolean
	 */
	public boolean shouldShowOnUnpublished () {
		return this.showOn.contains(WorkflowState.UNPUBLISHED);
	}

    /**
     * True if the action should be show on unpublish status.
     * @return boolean
     */
    public boolean shouldShowOnListing () {
        return this.showOn.contains(WorkflowState.LISTING);
    }
    
    /**
     * True if the action should be show on unpublish status.
     * @return boolean
     */
    public boolean shouldShowOnEdit () {
        return this.showOn.contains(WorkflowState.EDITING);
    }
    
	/**
	 * True if the action should be show on locked status.
	 * @return boolean
	 */
	public boolean shouldShowOnLock () {
		return this.showOn.contains(WorkflowState.LOCKED);
	}

	/**
	 * True if the action should be show on unlocked status.
	 * @return boolean
	 */
	public boolean shouldShowOnUnlock () {
		return this.showOn.contains(WorkflowState.UNLOCKED);
	}


	/**
	 * Returns the set of the status to show the action.
	 * @return Set of {@link WorkflowState}
	 */
	public Set<WorkflowState> getShowOn() {
		return showOn;
	}

	/**
	 * Set the set set of the status to show the action.
	 * @param showOn {@link Set} of {@link WorkflowState}
	 */
	@JsonSetter("showOn")
	public void setShowOn(final Set<WorkflowState> showOn) {
		if (null != showOn) {
			this.showOn = showOn;
		}
	}

	/**
	 * Set the set set of the status to show the action.
	 * @param showOn Array of {@link WorkflowState}
	 */
	public void setShowOn(final WorkflowState... showOn) {
		if (null != showOn) {
			this.setShowOn(new HashSet<>(Arrays.asList(showOn)));
		}
	}


	@JsonIgnore
	public String getPermissionId() {
		return this.getId();
	}

	public String getOwner() {
		return null;
	}

	public void setOwner(String owner) {
		// Not implemented
	}

	@JsonIgnore
	@Override
	public String toString() {
		return "WorkflowAction [id=" + id + ", name=" + name + ", stepId=" + stepId + ", nextStep=" + nextStep + "]";
	}
  private static final List<PermissionSummary> acceptedPermissions = ImmutableList.of(new PermissionSummary("use","use-permission-description", PermissionAPI.PERMISSION_USE));
	
  @JsonIgnore
	public List<PermissionSummary> acceptedPermissions() {
		return acceptedPermissions;
	}

	/**
	 * Returns true if the action has at least one action let that saves
	 * @return Boolean true if has save action
	 */
    public boolean hasSaveActionlet() {
    	return this.saveActionlet;
    }

	/**
	 * Returns true if the action has at least one action let that publish
	 * @return Boolean true if has publish action
	 */
    public boolean hasPublishActionlet() {
    	return this.publishActionlet;
    }

	/**
	 * Returns true if the action has at least one action let that publish publish
	 * @return Boolean true if has push publish action
	 */

	public boolean hasPushPublishActionlet() {
		return this.pushPublishActionlet;
	}

	/**
	 * Returns true if the action has at least one action let that unpublish
	 * @return Boolean true if has unpublish action
	 */
	public boolean hasUnpublishActionlet() {
		return unpublishActionlet;
	}

	/**
	 * Returns true if the action has at least one action let that archive
	 * @return Boolean true if has archive action
	 */
	public boolean hasArchiveActionlet() {

		return archiveActionlet;
	}

	/**
	 * Returns true if the action has at least one action let that unarchive
	 * @return Boolean true if has unarchive action
	 */
	public boolean hasUnarchiveActionlet() {

		return unarchiveActionlet;
	}

	/**
	 * Returns true if the action has at least one action let that delete
	 * @return Boolean true if has delete action
	 */
	public boolean hasDeleteActionlet() {

		return deleteActionlet;
	}

	/**
	 * Returns true if the action has at least one action let that destroy
	 * @return Boolean true if has destroy action
	 */
	public boolean hasDestroyActionlet() {
		return destroyActionlet;
	}

	public void setSaveActionlet(boolean saveActionlet) {
		this.saveActionlet = saveActionlet;
	}

	public void setPublishActionlet(boolean publishActionlet) {
		this.publishActionlet = publishActionlet;
	}

	public void setPushPublishActionlet(boolean pushPublishActionlet) {
		this.pushPublishActionlet = pushPublishActionlet;
	}

	public void setUnpublishActionlet(final boolean unpublishActionlet) {
		this.unpublishActionlet = unpublishActionlet;
	}

	public void setArchiveActionlet(final boolean archiveActionlet) {
		this.archiveActionlet = archiveActionlet;
	}

	public void setUnarchiveActionlet(final boolean unarchiveActionlet) {
		this.unarchiveActionlet = unarchiveActionlet;
	}

	public void setDeleteActionlet(final boolean deleteActionlet) {
		this.deleteActionlet = deleteActionlet;
	}

	public void setDestroyActionlet(final boolean destroyActionlet) {
		this.destroyActionlet = destroyActionlet;
	}

	@JsonIgnore
	public List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
		return null;
	}

	@JsonIgnore
	public Permissionable getParentPermissionable() throws DotDataException {
		return null;
	}

	@JsonIgnore
	public String getPermissionType() {
		return this.getClass().getCanonicalName();
	}

	@JsonIgnore
	public boolean isParentPermissionable() {
		return false;
	}

	/**
	 * @return boolean
	 */
	@JsonIgnore
	@Deprecated
	public boolean requiresCheckout() {
		return requiresCheckout;
	}

	/**
	 * @return boolean
	 */
	@JsonIgnore
	@Deprecated
	public boolean isRequiresCheckout() {
		return requiresCheckout;
    }

	/**
	 * @param requiresCheckout
	 */
	@Deprecated
	public void setRequiresCheckout(boolean requiresCheckout) {
		this.requiresCheckout = requiresCheckout;
	}

	public boolean isRoleHierarchyForAssign() {
		return roleHierarchyForAssign;
	}

	public void setRoleHierarchyForAssign(boolean roleHierarchyForAssign) {
		this.roleHierarchyForAssign = roleHierarchyForAssign;
	}

	public String getIcon() {
		if(!UtilMethods.isSet(icon)){
			return "workflowIcon";
		}
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Reflects the old relation between Action and step
	 * @deprecated this is keep just by legacy reason, new apps should not use it
	 */
	@JsonIgnore
	@Deprecated
	public String getStepId() {
		return stepId;
	}

	/**
	 * Reflects the old relation between Action and step
	 * @deprecated this is keep just by legacy reason, new apps should not use it
	 */
	@Deprecated
	public void setStepId(String stepId) {
		this.stepId = stepId;
	}

	/**
	 * Returns the ID of the Workflow Scheme that this action belongs to.
	 *
	 * @return The Workflow Scheme ID.
	 */
	public String getSchemeId() {
		return schemeId;
	}

	/**
	 * Sets the ID of the Workflow Scheme that this action belongs to.
	 *
	 * @param schemeId The Workflow Scheme ID.
	 */
	public void setSchemeId(String schemeId) {
		this.schemeId = schemeId;
	}

	public String getCondition() {
		return null == condition? StringPool.BLANK: condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	/**
	 * Return true if the next step is the current step
	 * @return boolean
	 */
	public boolean isNextStepCurrentStep() {

		return CURRENT_STEP.equalsIgnoreCase(this.getNextStep());
	}

	public String getNextStep() {
		return nextStep;
	}

	public void setNextStep(String nextStep) {
		this.nextStep = nextStep;
	}

	public String getNextAssign() {
		return nextAssign;
	}

	public void setNextAssign(String nextAssign) {
		this.nextAssign = nextAssign;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public boolean isAssignable() {
		return assignable;
	}

	public void setAssignable(boolean assignable) {
		this.assignable = assignable;
	}

	public boolean isCommentable() {
		return commentable;
	}

	public void setCommentable(boolean commentable) {
		this.commentable = commentable;
	}

	@JsonIgnore
	public boolean isNew(){
		return !UtilMethods.isSet(id);
		
	}

	@Override
	public boolean equals(Object obj) {
		if(obj ==null || ! (obj instanceof WorkflowAction)) return false;
		return ((WorkflowAction)obj).getId().equals(this.getId());
	}

	@Override
	public int hashCode() {

		return Objects.hash(id);
	}


}
