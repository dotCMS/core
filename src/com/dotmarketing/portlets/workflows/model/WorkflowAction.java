package com.dotmarketing.portlets.workflows.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;


public class WorkflowAction implements Permissionable, Serializable{
	@JsonIgnore
	public String getPermissionId() {
		return this.getId();
	}
	public String getOwner() {
		
		return null;
	}
	public void setOwner(String owner) {

		
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
	private static final long serialVersionUID = 1L;

	String id;
	String name;
	String stepId;
	String condition;
	String nextStep;
	String nextAssign;
	String icon;
	boolean roleHierarchyForAssign;
	boolean requiresCheckout;
	
	public boolean requiresCheckout() {
		return requiresCheckout;
	}
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
	int order;
	boolean assignable;
	boolean commentable;
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
	public String getStepId() {
		return stepId;
	}
	public void setStepId(String stepId) {
		this.stepId = stepId;
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
