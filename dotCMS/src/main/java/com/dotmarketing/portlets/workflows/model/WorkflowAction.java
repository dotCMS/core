package com.dotmarketing.portlets.workflows.model;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import java.io.Serializable;
import java.util.*;

/**
 * Encapsulate the workflow action information.
 *
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 */
public class WorkflowAction implements Permissionable, Serializable{


	private static final long serialVersionUID = 1L;

	private String id;
	private String name;
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
	private Set<WorkflowStatus> showOn = Collections.emptySet();

	/**
	 * True if the action has to be show on locked status.
	 * @return boolean
	 */
	public boolean showOnLock () {
		return this.showOn.contains(WorkflowStatus.LOCKED);
	}

	/**
	 * True if the action has to be show on unlocked status.
	 * @return boolean
	 */
	public boolean showOnUnlock () {
		return this.showOn.contains(WorkflowStatus.UNLOCKED);
	}

	/**
	 * Returns the set of the status to show the action.
	 * @return Set of {@link WorkflowStatus}
	 */
	public Set<WorkflowStatus> getShowOn() {
		return showOn;
	}

	/**
	 * Set the set set of the status to show the action.
	 * @param showOn {@link Set} of {@link WorkflowStatus}
	 */
	public void setShowOn(final Set<WorkflowStatus> showOn) {
		if (null != showOn) {
			this.showOn = showOn;
		}
	}


	/**
	 * Set the set set of the status to show the action.
	 * @param workflowStatusArray Array of WorkflowStatus
	 */
	public void setShowOn(final WorkflowStatus... workflowStatusArray) {

		final ImmutableSet.Builder builder =
				new ImmutableSet.Builder<WorkflowStatus>();

		for (WorkflowStatus status : workflowStatusArray) {

			builder.add(status);
		}

		this.setShowOn(builder.build());
	}



	public WorkflowAction() {
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

	@JsonIgnore
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
		accepted.add(new PermissionSummary("use",
				"use-permission-description", PermissionAPI.PERMISSION_USE));
		return accepted;
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
	public boolean requiresCheckout() {
		return requiresCheckout;
	}

	/**
	 * @return boolean
	 */
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
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
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

}
