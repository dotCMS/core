package com.dotmarketing.portlets.workflows.business;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.osgi.framework.BundleContext;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.osgi.HostActivator;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.actionlet.ArchiveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CheckinContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CheckoutContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.CommentOnWorkflowActionlet;
import com.dotmarketing.portlets.workflows.actionlet.DeleteContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.MultipleApproverActionlet;
import com.dotmarketing.portlets.workflows.actionlet.NotifyAssigneeActionlet;
import com.dotmarketing.portlets.workflows.actionlet.NotifyUsersActionlet;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.ResetTaskActionlet;
import com.dotmarketing.portlets.workflows.actionlet.TwitterActionlet;
import com.dotmarketing.portlets.workflows.actionlet.UnarchiveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.UnpublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowSearcher;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;

public class WorkflowAPIImpl implements WorkflowAPI, WorkflowAPIOsgiService {

	private List<Class> actionletClasses;
	
	private static Map<String, WorkFlowActionlet> actionletMap;

	private WorkFlowFactory wfac = FactoryLocator.getWorkFlowFactory();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WorkflowAPIImpl() {
		
		actionletClasses = new ArrayList<Class>();
		
		// Add default actionlet classes
		actionletClasses.addAll(Arrays.asList(new Class[] {
			CommentOnWorkflowActionlet.class, 
			NotifyUsersActionlet.class,
			ArchiveContentActionlet.class,
			DeleteContentActionlet.class,
			CheckinContentActionlet.class,
			CheckoutContentActionlet.class,
			UnpublishContentActionlet.class,
			PublishContentActionlet.class,
			NotifyAssigneeActionlet.class,
			UnarchiveContentActionlet.class,
			ResetTaskActionlet.class,
			MultipleApproverActionlet.class,
			TwitterActionlet.class
		}));
		
		refreshWorkFlowActionletMap();

		// Register main service
        BundleContext context = HostActivator.instance().getBundleContext();
        Hashtable<String, String> props = new Hashtable<String, String>();
        context.registerService(WorkflowAPIOsgiService.class.getName(), this, props);
	}
	
	public WorkFlowActionlet newActionlet(String className) throws DotDataException {
		for ( Class<WorkFlowActionlet> z : actionletClasses ) {
			if ( z.getName().equals(className.trim())) {
				try {
					return z.newInstance();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public String addActionlet(Class workFlowActionletClass) {
		actionletClasses.add(workFlowActionletClass);
		refreshWorkFlowActionletMap();
		return workFlowActionletClass.getCanonicalName();
	}
	
	public void removeActionlet(String workFlowActionletName) {
		WorkFlowActionlet actionlet = actionletMap.get(workFlowActionletName);
		actionletClasses.remove(actionlet.getClass());
		refreshWorkFlowActionletMap();
	}

	public List<WorkflowTask> searchTasks(WorkflowSearcher searcher) throws DotDataException {
		return wfac.searchTasks(searcher);
	}

	public WorkflowTask findTaskByContentlet(Contentlet contentlet) throws DotDataException {
		return wfac.findTaskByContentlet(contentlet);
	}

	public WorkflowStep findStepByContentlet(Contentlet contentlet) throws DotDataException{
		return wfac.findStepByContentlet(contentlet);
	}

	public WorkflowTask findTaskById(String id) throws DotDataException {
		return wfac.findWorkFlowTaskById(id);
	}

	public List<WorkflowScheme> findSchemes(boolean showArchived) throws DotDataException {
		return wfac.findSchemes(showArchived);
	}

	public WorkflowScheme findDefaultScheme() throws DotDataException {
		return wfac.findDefaultScheme();
	}

	public boolean isDefaultScheme(WorkflowScheme scheme) throws DotDataException {
		if (scheme == null || scheme.getId() == null) {
			return false;
		}
		if (wfac.findDefaultScheme().getId().equals(scheme.getId())) {
			return true;
		}
		return false;
	}

	public WorkflowScheme findScheme(String id) throws DotDataException {
		return wfac.findScheme(id);
	}
	public void saveSchemeForStruct(Structure struc, WorkflowScheme scheme) throws DotDataException {
		
		try{
			HibernateUtil.startTransaction();
			wfac.saveSchemeForStruct(struc.getInode(), scheme);
		}
		catch(DotDataException e){
			HibernateUtil.rollbackTransaction();
			throw e;
		}
		finally{
			HibernateUtil.commitTransaction();
		}
	}
	public WorkflowScheme findSchemeForStruct(Structure struct) throws DotDataException {
		
		
		if(struct ==null || ! UtilMethods.isSet(struct.getInode()) || LicenseUtil.getLevel() < 200){
			return findDefaultScheme();
		}
		try{
			return wfac.findSchemeForStruct(struct.getInode());
		}
		catch(Exception e){
			return findDefaultScheme();
		}
		
		
		
		
		
		
	}

	public void saveScheme(WorkflowScheme scheme) throws DotDataException {

		wfac.saveScheme(scheme);

	}

	public void deleteScheme(WorkflowScheme scheme) throws DotDataException {
		// TODO Auto-generated method stub

	}

	public void activateScheme(WorkflowScheme scheme) throws DotDataException {
		// TODO Auto-generated method stub

	}

	public void deactivateScheme(WorkflowScheme scheme) throws DotDataException {
		// TODO Auto-generated method stub

	}

	public List<WorkflowStep> findSteps(WorkflowScheme scheme) throws DotDataException {
		// TODO Auto-generated method stub
		return wfac.findSteps(scheme);
	}

	public void saveStep(WorkflowStep step) throws DotDataException {

		if (!UtilMethods.isSet(step.getName()) || !UtilMethods.isSet(step.getSchemeId())) {
			throw new DotStateException("Step name and Scheme are required");
		}
		wfac.saveStep(step);

	}

	public void deleteStep(WorkflowStep step) throws DotDataException {
		
		
		boolean localTransaction = startLocalTransaction();
		try{
			List<WorkflowAction> actions = wfac.findActions(step);
			for(WorkflowAction action : actions){
				List<WorkflowActionClass> actionClasses = wfac.findActionClasses(action);
				for(WorkflowActionClass actionClass : actionClasses){
					wfac.deleteWorkflowActionClassParameters(actionClass);
					wfac.deleteActionClass(actionClass);
				}
				wfac.deleteAction(action);
			}
			
		
		
			wfac.deleteStep(step);
		}
		catch(Exception e){
			if(localTransaction){
				HibernateUtil.rollbackTransaction();
			}
			throw new DotDataException(e.getMessage(), e);	

			
		}
		finally{
			if(localTransaction){
				HibernateUtil.commitTransaction();
			}
		}

	}

	public void reorderStep(WorkflowStep step, int order) throws DotDataException {
		WorkflowScheme scheme = findScheme(step.getSchemeId());
		List<WorkflowStep> steps = null;

		try {
			steps = findSteps(scheme);
		} catch (Exception e) {
			throw new DotDataException(e.getLocalizedMessage());
		}
		List<WorkflowStep> newSteps = new ArrayList<WorkflowStep>();
		order = (order < 1) ? 1 : (order > steps.size()) ? steps.size() : order;
		for (int i = 0; i < steps.size(); i++) {
			WorkflowStep s = steps.get(i);
			if (s.equals(step)) {
				continue;
			}
			newSteps.add(s);
		}

		newSteps.add(order-1, step);
		int newOrder=0;
		for(WorkflowStep newStep : newSteps){
			newStep.setMyOrder(newOrder++);
			saveStep(newStep);
		}
		
		
		
		

	}

	public void deleteComment(WorkflowComment comment) throws DotDataException {
		wfac.deleteComment(comment);
	}

	public List<WorkflowComment> findWorkFlowComments(WorkflowTask task) throws DotDataException {
		return wfac.findWorkFlowComments(task);
	}

	public void saveComment(WorkflowComment comment) throws DotDataException {
		if(UtilMethods.isSet(comment.getComment())){
			wfac.saveComment(comment);
		}
	}

	public List<WorkflowHistory> findWorkflowHistory(WorkflowTask task) throws DotDataException {
		return wfac.findWorkflowHistory(task);
	}

	public void deleteWorkflowHistory(WorkflowHistory history) throws DotDataException {
		wfac.deleteWorkflowHistory(history);
	}

	public void saveWorkflowHistory(WorkflowHistory history) throws DotDataException {
		wfac.saveWorkflowHistory(history);
	}

	public void deleteWorkflowTask(WorkflowTask task) throws DotDataException {
		boolean local = startLocalTransaction();
		try{
			wfac.deleteWorkflowTask(task);
		
		}catch(Exception e){
			if(local){
				HibernateUtil.rollbackTransaction();
			}
			
		}
		finally{
			if(local){
				HibernateUtil.commitTransaction();
			}
			
		}
	}

	public WorkflowTask findWorkFlowTaskById(String id) throws DotDataException {
		return wfac.findWorkFlowTaskById(id);
	}

	public List<File> findWorkflowTaskFiles(WorkflowTask task) throws DotDataException {
		return wfac.findWorkflowTaskFiles(task);
	}

	public void saveWorkflowTask(WorkflowTask task, WorkflowProcessor processor) throws DotDataException {
		wfac.saveWorkflowTask(task);
		WorkflowHistory history = new WorkflowHistory();
		history.setWorkflowtaskId(task.getId());
		history.setActionId(processor.getAction().getId());
		history.setCreationDate(new Date());
		history.setMadeBy(processor.getUser().getUserId());
		history.setStepId(processor.getNextStep().getId());
		
		String comment = (UtilMethods.isSet(processor.getWorkflowMessage()))? processor.getWorkflowMessage() : "";
		
		
		try {
			history.setChangeDescription(
					LanguageUtil.format(processor.getUser().getLocale(), "workflow.history.description", new String[]{
						processor.getUser().getFullName(), 
						processor.getAction().getName(),
						processor.getNextStep().getName(),
						processor.getNextAssign().getName(),
						comment}, false)
					);
		} catch (LanguageException e) {
			Logger.error(WorkflowAPIImpl.class,e.getMessage(),e);
		}
		saveWorkflowHistory(history);
	}

	public void attachFileToTask(WorkflowTask task, String fileInode) throws DotDataException {
		wfac.attachFileToTask(task, fileInode);
	}

	public void removeAttachedFile(WorkflowTask task, String fileInode) throws DotDataException {
		wfac.removeAttachedFile(task, fileInode);
	}

	public List<WorkflowAction> findActions(WorkflowStep step, User user) throws DotDataException,
			DotSecurityException {
		List<WorkflowAction> actions = wfac.findActions(step);
		actions = APILocator.getPermissionAPI().filterCollection(actions, PermissionAPI.PERMISSION_USE, true, user);
		return actions;
	}

	
	/**
	 * This method will return the list of workflows actions available to a user on any give
	 * piece of content, based on how and who has the content locked and what workflow step the content
	 * is in
	 */
	public List<WorkflowAction> findAvailableActions(Contentlet contentlet, User user) throws DotDataException,
			DotSecurityException {
		if(contentlet == null || contentlet.getStructure() ==null){
			throw new DotStateException("content is null");
		}
		List<WorkflowAction> actions= new ArrayList<WorkflowAction>();
		if("Host".equals(contentlet.getStructure().getVelocityVarName())){
			return actions;
		}
		boolean isNew  = !UtilMethods.isSet(contentlet.getIdentifier());
		boolean isLocked = contentlet.isLocked();
		boolean canLock = false;
		String lockedUserId =  null;
		try{
			canLock = APILocator.getContentletAPI().canLock(contentlet, user);
			lockedUserId =  APILocator.getVersionableAPI().getLockedBy(contentlet);
		}
		catch(Exception e){
			
		}

		boolean hasLock = user.getUserId().equals(lockedUserId);
		
		
		
		WorkflowStep step= findStepByContentlet(contentlet);
		

		List<WorkflowAction> unfilteredActions = findActions(step, user);
		if(hasLock || isNew){
			return unfilteredActions;
		}
		else if(canLock){
			for(WorkflowAction a : unfilteredActions){
				if(!a.requiresCheckout()){
					actions.add(a);
				}
			}
		}

		return actions;
		
		
		
		
		
	}
	
	
	
	
	
	public void reorderAction(WorkflowAction action, int order) throws DotDataException {


		
		
		WorkflowStep step = findStep(action.getStepId());
		List<WorkflowAction> actions = null;
		List<WorkflowAction> newActions = new ArrayList<WorkflowAction>();
		try {
			actions = findActions(step, APILocator.getUserAPI().getSystemUser());
		} catch (Exception e) {
			throw new DotDataException(e.getLocalizedMessage());
		}
		order = (order < 0) ? 0 : (order >= actions.size()) ? actions.size()-1 : order;
		for (int i = 0; i < actions.size(); i++) {
			WorkflowAction a = actions.get(i);
			if (action.equals(a)) {
				continue;
			}
			newActions.add(a);
		}
		newActions.add(order, action);
		int newOrder = 0;
		for(WorkflowAction a : newActions){
			a.setOrder(newOrder++);
			
			saveAction(a);
		}

	}

	public WorkflowAction findAction(String id, User user) throws DotDataException, DotSecurityException {

		WorkflowAction action = wfac.findAction(id);
		if (!APILocator.getPermissionAPI().doesUserHavePermission(action, PermissionAPI.PERMISSION_USE, user, true)) {
			throw new DotSecurityException("User " + user + " cannot read action " + action.getName());
		}
		return action;
	}

	public void saveAction(WorkflowAction action, List<Permission> perms) throws DotDataException {
		try {
			HibernateUtil.startTransaction();
			this.saveAction(action);
			APILocator.getPermissionAPI().removePermissions(action);
			if(perms != null){
				for (Permission p : perms) {
					p.setInode(action.getId());
					APILocator.getPermissionAPI().save(p, action, APILocator.getUserAPI().getSystemUser(), false);
				}
			}
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		} finally {
			HibernateUtil.commitTransaction();
		}

	}

	private void saveAction(WorkflowAction action) throws DotDataException {
		wfac.saveAction(action);
	}

	public WorkflowStep findStep(String id) throws DotDataException {
		return wfac.findStep(id);
	}

	public void deleteAction(WorkflowAction action) throws DotDataException {
		
		List<WorkflowActionClass> l = findActionClasses(action);
		if(l!=null && l.size()>0){
			for(WorkflowActionClass clazz : l){
				deleteActionClass(clazz);
				
			}
		}
		
		
		
		wfac.deleteAction(action);
	}

	public List<WorkflowActionClass> findActionClasses(WorkflowAction action) throws DotDataException {
		return  wfac.findActionClasses(action);
	}

	private void refreshWorkFlowActionletMap() {
		actionletMap = null;
		if (actionletMap == null) {
			synchronized (this.getClass()) {
				if (actionletMap == null) {
					
					List<WorkFlowActionlet> actionletList = new ArrayList<WorkFlowActionlet>();
					
					// get the dotmarketing-config.properties actionlet classes
					String customActionlets = Config.getStringProperty(WebKeys.WORKFLOW_ACTIONLET_CLASSES);

					StringTokenizer st = new StringTokenizer(customActionlets, ",");
					while (st.hasMoreTokens()) {
						String clazz = st.nextToken();
						try {
							WorkFlowActionlet actionlet = (WorkFlowActionlet) Class.forName(clazz.trim()).newInstance();
							actionletList.add(actionlet);
						} catch (Exception e) {
							Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
						}
					}

					// get the included (shipped with) actionlet classes
					for (Class<WorkFlowActionlet> z : actionletClasses) {
						try {
							actionletList.add(z.newInstance());
						} catch (InstantiationException e) {
							Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
						} catch (IllegalAccessException e) {
							Logger.error(WorkflowAPIImpl.class, e.getMessage(), e);
						}
					}
					
					Collections.sort(actionletList, new ActionletComparator());
					actionletMap = new LinkedHashMap<String, WorkFlowActionlet>();
					for(WorkFlowActionlet actionlet : actionletList){
						
						try {
							actionletMap.put(actionlet.getClass().getCanonicalName(),actionlet.getClass().newInstance());
						} catch (InstantiationException e) {
							Logger.error(WorkflowAPIImpl.class,e.getMessage(),e);
						} catch (IllegalAccessException e) {
							Logger.error(WorkflowAPIImpl.class,e.getMessage(),e);
						}
					}
				}
			}

		}
	}
	
	private Map<String, WorkFlowActionlet> getActionlets() throws DotRuntimeException {
		return actionletMap;
	}
	
	private class ActionletComparator implements Comparator<WorkFlowActionlet>{

		public int compare(WorkFlowActionlet o1, WorkFlowActionlet o2) {
			return o1.getLocalizedName().compareTo(o2.getLocalizedName());

		}
		
		
	}

	public WorkFlowActionlet findActionlet(String clazz) throws DotRuntimeException {
		return getActionlets().get(clazz);
	}

	public List<WorkFlowActionlet> findActionlets() throws DotDataException {
		List<WorkFlowActionlet> l = new ArrayList<WorkFlowActionlet>();
		Map<String,WorkFlowActionlet>  m = getActionlets();
		for (String x : m.keySet()) {
			l.add(getActionlets().get(x));
		}
		return l;

	}

	public WorkflowActionClass findActionClass(String id) throws DotDataException {
		return wfac.findActionClass(id);
	}

	public void deleteActionClass(WorkflowActionClass actionClass) throws DotDataException {
		wfac.deleteActionClass(actionClass);
	}

	public void saveActionClass(WorkflowActionClass actionClass) throws DotDataException {
		wfac.saveActionClass(actionClass);
	}

	public void reorderActionClass(WorkflowActionClass actionClass, int order) throws DotDataException {

		try {
			WorkflowAction baseAction = findAction(actionClass.getActionId(), APILocator.getUserAPI().getSystemUser());
			List<WorkflowActionClass> actionClasses = null;
			try {
				actionClasses = findActionClasses(baseAction);
			} catch (Exception e) {
				throw new DotDataException(e.getLocalizedMessage());
			}
			List<WorkflowActionClass> newActionClasses = new ArrayList<WorkflowActionClass>();
			for (int i = 0; i < actionClasses.size(); i++) {
				WorkflowActionClass a = actionClasses.get(i);
				if (actionClass.equals(a)) {
					continue;
				}
				newActionClasses.add(a);
			
			}
			order = (order<0) ? 0: (order >= actionClasses.size() ? actionClasses.size()-1 : order);
			newActionClasses.add(order, actionClass);
			
			int i =0;
			for(WorkflowActionClass action : newActionClasses){
				action.setOrder(i++);
				saveActionClass(action);
			}

		} catch (Exception e) {
			throw new DotWorkflowException(e.getMessage());
		} 
	}
    public Map<String, WorkflowActionClassParameter> findParamsForActionClass(WorkflowActionClass actionClass) throws  DotDataException{
    	return wfac.findParamsForActionClass(actionClass);
    }
    
    
    
    public void saveWorkflowActionClassParameters(List<WorkflowActionClassParameter> params) throws DotDataException{
    	
    	if(params ==null || params.size() ==0){
    		return;
    	}
    	
    	try {
			boolean localTransaction = startLocalTransaction();
			
			WorkflowActionClass actionClass= wfac.findActionClass(params.get(0).getActionClassId());
			//wfac.deleteWorkflowActionClassParameters(actionClass);
			
			for(WorkflowActionClassParameter param : params){
				wfac.saveWorkflowActionClassParameter(param);
			}
			if(localTransaction){
				HibernateUtil.commitTransaction();
			}
		} catch (Exception e) {
			Logger.error(WorkflowAPIImpl.class,e.getMessage(),e);
			HibernateUtil.rollbackTransaction();
			DbConnectionFactory.closeConnection();
			
		}
		finally{
			HibernateUtil.commitTransaction();
		}
    }
    
    
    
    
    
    boolean startLocalTransaction() throws DotDataException{
    	boolean startTransaction = false;
    	
    	try {
    		startTransaction = DbConnectionFactory.getConnection().getAutoCommit();
			if(startTransaction){
				HibernateUtil.startTransaction();
			}
		} catch (SQLException e) {
			Logger.error(WorkflowAPIImpl.class,e.getMessage(),e);
			throw new DotDataException(e.getMessage());
		}
		return startTransaction;
    }
	
	public WorkflowProcessor fireWorkflowPreCheckin(Contentlet contentlet) throws DotDataException,DotWorkflowException, DotContentletValidationException{
		WorkflowProcessor processor = new WorkflowProcessor(contentlet);
		if(!processor.inProcess()){
			return processor;
		}
		
		if(processor.getScheme() != null && processor.getScheme().isMandatory()){
			if(!UtilMethods.isSet(processor.getAction())){
				throw new DotWorkflowException("A workflow action in workflow : " + processor.getScheme().getName() + " must be executed"  );
			}
		}
				
		
		
		
		
		List<WorkflowActionClass> actionClasses = processor.getActionClasses();
		if(actionClasses != null){
			for(WorkflowActionClass actionClass : actionClasses){
				WorkFlowActionlet actionlet= actionClass.getActionlet();
				Map<String,WorkflowActionClassParameter> params = findParamsForActionClass(actionClass);
				actionlet.executePreAction(processor, params);
				//if we should stop processing further actionlets
				if(actionlet.stopProcessing()){
					break;
				}
			}
		}
		
		
		
		
		return processor;
		
		
		
	}
	
	public void fireWorkflowPostCheckin(WorkflowProcessor processor) throws DotDataException,DotWorkflowException{
		boolean local = startLocalTransaction();

		try{
			if(!processor.inProcess()){
				return;
			}

			processor.getContentlet().setStringProperty("wfActionId", processor.getAction().getId());

				
			
			
			
			
			List<WorkflowActionClass> actionClasses = processor.getActionClasses();
			if(actionClasses != null){
				for(WorkflowActionClass actionClass : actionClasses){
					WorkFlowActionlet actionlet= actionClass.getActionlet();
					Map<String,WorkflowActionClassParameter> params = findParamsForActionClass(actionClass);
					actionlet.executeAction(processor, params);
					
					//if we should stop processing further actionlets
					if(actionlet.stopProcessing()){
						break;
					}
				}
			}
			
			WorkflowTask task = processor.getTask();
				if(task != null){
				Role r = APILocator.getRoleAPI().getUserRole(processor.getUser());
				if(task.isNew()){
		
					task.setCreatedBy(r.getId());
					task.setWebasset(processor.getContentlet().getIdentifier());
					if(processor.getWorkflowMessage() != null){
						task.setDescription(processor.getWorkflowMessage());
					}
				}
				task.setTitle(processor.getContentlet().getTitle());
				task.setModDate(new java.util.Date());
				task.setAssignedTo(processor.getNextAssign().getId());
				task.setStatus(processor.getNextStep().getId());
				
				saveWorkflowTask(task,processor);
				if(processor.getWorkflowMessage() != null){
					WorkflowComment comment = new WorkflowComment();
					comment.setComment(processor.getWorkflowMessage());
					
					comment.setWorkflowtaskId(task.getId());
					comment.setCreationDate(new Date());
					comment.setPostedBy(r.getId());
					saveComment(comment);
				}
			}
		}catch(Exception e){
			if(local){
				HibernateUtil.rollbackTransaction();
			}
			throw new DotWorkflowException(e.getMessage());
			
		}
		finally{
			if(local){
				HibernateUtil.commitTransaction();
			}
			
		}
		
		
	}
		
	private void updateTask(WorkflowProcessor processor) throws DotDataException{
		WorkflowTask task = processor.getTask();
		task.setModDate(new java.util.Date());
		if(task.isNew()){
			Role r = APILocator.getRoleAPI().getUserRole(processor.getUser());
			task.setCreatedBy(r.getId());
			task.setTitle(processor.getContentlet().getTitle());
		}

		
		if(processor.getWorkflowMessage() != null){
			WorkflowComment comment = new WorkflowComment();
			comment.setComment(processor.getWorkflowMessage());
			comment.setWorkflowtaskId(task.getId());
			saveComment(comment);
		}
		
	}
	
	
	public WorkflowProcessor fireWorkflowNoCheckin(Contentlet contentlet) throws DotDataException,DotWorkflowException, DotContentletValidationException{
		
		WorkflowProcessor processor =fireWorkflowPreCheckin(contentlet);
		
		fireWorkflowPostCheckin(processor);
		return processor;
		
	}
	
	
	
	
	
	public int countTasks(WorkflowSearcher searcher)  throws DotDataException{
		return wfac.countTasks(searcher);
	}
	
	public void copyWorkflowActionClassParameter(WorkflowActionClassParameter from, WorkflowActionClass to) throws DotDataException{
		wfac.copyWorkflowActionClassParameter(from, to);
	}
	public void copyWorkflowActionClass(WorkflowActionClass from, WorkflowAction to) throws DotDataException{
		wfac.copyWorkflowActionClass(from, to);
	}
	public void copyWorkflowAction(WorkflowAction from, WorkflowStep to) throws DotDataException{
		wfac.copyWorkflowAction(from, to);
	}
	public void copyWorkflowStep(WorkflowStep from, WorkflowScheme to) throws DotDataException{
		wfac.copyWorkflowStep(from, to);
	}
    public  WorkflowScheme  createDefaultScheme() throws DotDataException, DotSecurityException{
    	return wfac.createDefaultScheme();
    }
    
    public WorkflowAction findEntryAction(Contentlet contentlet, User user)  throws DotDataException, DotSecurityException {
    	
		WorkflowScheme scheme = findSchemeForStruct(contentlet.getStructure());
		WorkflowStep entryStep = null;
		List<WorkflowStep> wfSteps = findSteps(scheme);
		
		for(WorkflowStep wfStep : wfSteps){
			if(!UtilMethods.isSet(entryStep))
				entryStep = wfStep;
			if(wfStep.getMyOrder() < entryStep.getMyOrder())
				entryStep = wfStep;
		}
		
		WorkflowAction entryAction = null;
		List<WorkflowAction> wfActions = findActions(entryStep, user);
		
		for(WorkflowAction wfAction : wfActions){
			if(!UtilMethods.isSet(entryAction))
				entryAction = wfAction;
			if(wfAction.getOrder() < entryAction.getOrder())
				entryAction = wfAction;
		}
		
		if (!APILocator.getPermissionAPI().doesUserHavePermission(entryAction, PermissionAPI.PERMISSION_USE, user, true)) {
			throw new DotSecurityException("User " + user + " cannot read action " + entryAction.getName());
		}
    	return entryAction;
    }
}