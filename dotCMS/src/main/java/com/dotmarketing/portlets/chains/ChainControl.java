package com.dotmarketing.portlets.chains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.chains.model.Chain;
import com.dotmarketing.portlets.chains.model.ChainState;

/**
 * 
 * This Data object contains the environment of parameters, request, attributes 
 * set by the chain executor and the subsequent execution of links
 * 
 * This data class gets passed to every link in the chain and used by those 
 * links to pass and retrieve data in and out the execution environment
 * 
 * @author davidtorresv
 *
 */
public final class ChainControl {

	private Map<String, Object> chainProperties;
	private Map<String, List<String>> errorMessages;
	private Map<String, List<String>> messages;
	private String executionResult;
	
	//Default control attributes available for all the links on the chain
	//and they get reset during the execution of every link
	public final static String REQUEST = "_chain_request";
	public final static String SESSION = "_chain_session";
	public final static String CHAIN = "_chain_object";
	public final static String CURRENT_STATE = "_chain_current_state";
	
	/** 
	 * This variables let the links know and control the return values that
	 * the chain is going to return in case of success or failure 
	 */
	public final static String SUCCESS_VALUE = "_chain_success";
	public final static String FAILURE_VALUE = "_chain_failure";
	
	/** 
	 * Control property that could be set on the 
	 * If this property is set to true in a case of a failure the whole chain transaction will be roll-backed 
	 * This property could be set as a "true"/"false" string value or a true/false boolean value
	 * the chain executor will handle both cases
	 * By default the chain executor will treat it as true so it will roll-back the transaction is case of a failure, either
	 * if the link returns false or the link throws an exception
	 * This is a volatile property that will get reset to true after the execution the link that sets it to true
	 *
	 */
	public final static String ROLLBACK_ON_ERROR = "_chain_rollback_on_error";	
	/** 
	 * Control property that could be set on the 
	 * If this property is set to true at the end of the execution of the link the whole chain transaction will be roll-backed 
	 * and restarted
	 * This property could be set as a "true"/"false" string value or a true/false boolean value
	 * the chain executor will handle both cases
	 * By default the chain executor will treat it as false so it will not roll-back the transaction at the end of the 
	 * execution of the link
	 * This is a volatile property that will get reset to false after the execution the link that sets it to true
	 *
	 */
	public final static String ROLLBACK_ONLY = "_chain_rollback_only";
	/** 
	 * If this property is set to true, the chain transaction will be committed and restarted in case 
	 * the link finished on success 
	 * This property could be set as a "true"/"false" string value or a true/false boolean value
	 * the chain executor will handle both cases
	 * This is a volatile property that will get reset to false after the execution the link that sets it to true
	 */
	public final static String RESTART_TRANS = "_chain_restart_transaction";
	/** 
	 * If this property is set to true in case of an exception thrown by one of the links of the chain, the execution of
	 * the whole chain will continue 
	 * This property could be set as a "true"/"false" string value or a true/false boolean value
	 * the chain executor will handle both cases
	 * By default the chain executor will treat it as false so it will not continue in case of an exception
	 * This is a volatile property that will get reset to false after the execution the link that sets it to true
	 */
	public final static String CONTINUE_ON_EXCEPTION = "_chain_continue_on_exception";
	/** 
	 * If this property is set to true even if the link returns failure(false) the execution of
	 * the whole chain will continue 
	 * This property could be set as a "true"/"false" string value or a true/false boolean value
	 * the chain executor will handle both cases
	 * By default the chain executor will treat it as false so it will not continue in case of an exception
	 * This is a volatile property that will get reset to false after the execution the link that sets it to true
	 */
	public static final String CONTINUE_ON_FAILURE = "_chain_continue_on_failure";
	
	protected ChainControl(Chain chain, String sucessUrl, String failureUrl) {
		this(null, null, chain, sucessUrl, failureUrl);
	}
	
	protected ChainControl(HttpServletRequest request, HttpSession session, Chain chain, String sucessValue, String failureValue) {
		chainProperties = new HashMap<String, Object>();
		errorMessages = new HashMap<String, List<String>>();
		messages = new HashMap<String, List<String>>();
		setRequest(request);
		setSession(session);
		setChain(chain);
		putChainProperty(SUCCESS_VALUE, sucessValue);
		putChainProperty(FAILURE_VALUE, failureValue);		
	}
	
	protected void setChainProperties(Map<String, Object> chainProperties) {
		this.chainProperties = chainProperties;
	}
	
	/**
	 * Returns a map of all the properties assigned during the execution of the chain
	 * @return
	 */
	public Map<String, Object> getChainProperties() {
		return new HashMap<String, Object> (chainProperties);
	}
	/**
	 * Retrieves an specific property
	 * @param key
	 * @return
	 */
	public Object getChainProperty(String key) {
		return chainProperties.get(key);
	}
	/**
	 * Assigns a property to the chain environment
	 * @param key
	 * @param value
	 * @return
	 */
	public Object putChainProperty(String key, Object value) {
		
		//Preventing of a manual set of control attributes
		if(key.equals(REQUEST))
			throw new DotRuntimeException("You can't manipulate the request object from the attributes");
		if(key.equals(SESSION))
			throw new DotRuntimeException("You can't manipulate the session object from the attributes");
		if(key.equals(CHAIN))
			throw new DotRuntimeException("You can't manipulate the chain object from the attributes");
		if(key.equals(CURRENT_STATE))
			throw new DotRuntimeException("You can't manipulate the current state object from the attributes");
		
		return chainProperties.put(key, value);
		
	}
	
	/**
	 * Will set the continue on failure property on the chain context that will force the execution of the rest of the 
	 * chain even if the current executed link failed returning false
	 */
	public void setContinueOnFailure () {
		putChainProperty(CONTINUE_ON_FAILURE, true);
	}
	
	/**
	 * Returns true if the current link is set to continue even if throws an exception
	 * @return
	 */
	public boolean isSetToContinueOnFailure () {
        boolean continueOnFailure = false;
        Object continueOnFailureProp = this.getChainProperty(ChainControl.CONTINUE_ON_FAILURE);
        if(continueOnFailureProp != null && 
        		(continueOnFailureProp instanceof Boolean || continueOnFailureProp instanceof String))
        	continueOnFailure = (continueOnFailureProp instanceof Boolean)?
        		(Boolean)continueOnFailureProp:Boolean.parseBoolean((String)continueOnFailureProp);
        return continueOnFailure;
	}
	
	/**
	 * Will set the continue on exception property on the chain context that will force the execution of the rest of the 
	 * chain even if the current executed link failed throwing an exception
	 */
	public void setContinueOnException () {
		putChainProperty(CONTINUE_ON_FAILURE, true);
	}
	
	/**
	 * Returns true if the current link is set to continue even if throws an exception
	 * @return
	 */
	public boolean isSetToContinueOnException () {
        boolean continueOnException = false;
        Object continueOnExceptionProp = this.getChainProperty(ChainControl.CONTINUE_ON_EXCEPTION);
        if(continueOnExceptionProp != null && 
        		(continueOnExceptionProp instanceof Boolean || continueOnExceptionProp instanceof String))
        	continueOnException = (continueOnExceptionProp instanceof Boolean)?
        		(Boolean)continueOnExceptionProp:Boolean.parseBoolean((String)continueOnExceptionProp);
        return continueOnException;
	}
	
	/**
	 * Will set the roll-back flag that will cause a transaction roll-back at the end of the execution of the link
	 */
	public void setRollbackOnly () {
		putChainProperty(ROLLBACK_ONLY, true);
	}

	/**
	 * Returns true if the current link is set to continue even if throws an exception
	 * @return
	 */
	public boolean isSetToRollbackOnly () {
        boolean rollbackOnly = false;
        Object rollbackOnlyProp = this.getChainProperty(ChainControl.ROLLBACK_ONLY);
        if(rollbackOnlyProp != null && 
        		(rollbackOnlyProp instanceof Boolean || rollbackOnlyProp instanceof String))
        	rollbackOnly = (rollbackOnlyProp instanceof Boolean)?
        		(Boolean)rollbackOnlyProp:Boolean.parseBoolean((String)rollbackOnlyProp);
        return rollbackOnly;
	}
	
	/**
	 * Will disable the roll-back on error flag, that will cause that in case of an error the while chain transaction will not
	 * be roll-backed
	 */
	public void setToNoRollbackOnError () {
		putChainProperty(ROLLBACK_ONLY, false);
	}
	
	/**
	 * Returns true if the current link is set to continue even if throws an exception
	 * @return
	 */
	public boolean isSetToRollbackOnError () {
        boolean rollbackOnError = true;
        Object rollbackOnErrorProp = this.getChainProperty(ChainControl.ROLLBACK_ON_ERROR);
        if(rollbackOnErrorProp != null && 
        		(rollbackOnErrorProp instanceof Boolean || rollbackOnErrorProp instanceof String))
        	rollbackOnError = (rollbackOnErrorProp instanceof Boolean)?
        		(Boolean)rollbackOnErrorProp:Boolean.parseBoolean((String)rollbackOnErrorProp);
        return rollbackOnError;
	}	

	/**
	 * Will disable the roll-back on error flag, that will cause that in case of an error the while chain transaction will not
	 * be roll-backed
	 */
	public void setToRestartTransaction () {
		putChainProperty(RESTART_TRANS, true);
	}
	
	/**
	 * Returns true if the current link is set to continue even if throws an exception
	 * @return
	 */
	public boolean isSetToRestartTransaction () {
        boolean restartTransaction = false;
        Object restartTransProp = this.getChainProperty(ChainControl.RESTART_TRANS);
        if(restartTransProp != null && 
        		(restartTransProp instanceof Boolean || restartTransProp instanceof String))
        	restartTransaction = (restartTransProp instanceof Boolean)?
        		(Boolean)restartTransProp:Boolean.parseBoolean((String)restartTransProp);
        return restartTransaction;
	}	
	
	/**
	 * Returns the property that hold the success URL
	 * @return
	 */
	public String getSuccessValue () {
		return (String) getChainProperty(SUCCESS_VALUE);
	}
	
	/**
	 * Returns the property that hold the success URL
	 * @return the old success URL
	 */
	public String setSuccessValue (String successValue) {
		return (String) chainProperties.put(SUCCESS_VALUE, successValue);
	}
	
	/**
	 * Returns the property that hold the success URL
	 * @return
	 */
	public String getFailureValue () {
		return (String) getChainProperty(FAILURE_VALUE);
	}
	
	/**
	 * Returns the property that hold the success URL
	 * @return the old failure URL
	 */
	public String setFailureValue (String failureValue) {
		return (String) chainProperties.put(FAILURE_VALUE, failureValue);
	}
	
	protected void setRequest(HttpServletRequest request) {
		chainProperties.put(REQUEST, request);
	}
	
	/**
	 * Retrieves the underlying request object
	 * @return
	 */
	public HttpServletRequest getRequest() {
		return (HttpServletRequest) chainProperties.get(REQUEST);
	}
	
	protected void setSession(HttpSession session) {
		chainProperties.put(SESSION, session);
	}
	
	/**
	 * Retrieves the underlying session object
	 * @return
	 */
	public HttpSession getSession() {
		return (HttpSession) chainProperties.get(SESSION);
	}
	
	protected void setChain(Chain chain) {
		chainProperties.put(CHAIN, chain);
	}
	
	/**
	 * Retrieves the underlying chain object
	 * @return
	 */
	public Chain getChain() {
		return (Chain) chainProperties.get(CHAIN);
	}	
	
	protected void setCurrentState(ChainState state) {
		chainProperties.put(CURRENT_STATE, state);
	}
	
	/**
	 * Retrieves the underlying configuration chain state object
	 * @return
	 */
	public ChainState getCurrentState() {
		return (ChainState) chainProperties.get(CURRENT_STATE);
	}

	/**
	 * Retrieves the list of all the errors messages set during the 
	 * execution of the chain
	 * @return
	 */
	public List<String> getAllErrorMessages() {
		List<String> allErrors = new ArrayList<String>();
		for(List<String> mess : errorMessages.values()) {
			allErrors.addAll(mess);
		}
		return allErrors;
	}
	

	/**
	 * Retrieves the list of all the errors messages on a 
	 * map separated by keys
	 * @return
	 */
	public Map<String, List<String>> getErrorMessages() {
		return new HashMap<String, List<String>> (errorMessages);
	}
	
	/**
	 * Retrieves all the errors messages set under an specific key,
	 * useful to categorize error messages
	 * @param key
	 * @return
	 */
	public List<String> getErrorMessages(String key) {
		return errorMessages.get(key);
	}
	
	/**
	 * Let you add an error message and categorize it with the given key
	 * @param key
	 * @param error
	 */
	public void addErrorMessage(String key, String error) {
		List<String> errorsList = errorMessages.get(key);
		if(errorsList == null) errorsList = new ArrayList<String>();
		errorsList.add(error);
		errorMessages.put(key, errorsList);
	}
	
	/**
	 * Let you add an error message with no key/categorization
	 * @param error
	 */
	public void addErrorMessage(String error) {
		List<String> errorsList = errorMessages.get(null);
		if(errorsList == null) errorsList = new ArrayList<String>();
		errorsList.add(error);
		errorMessages.put(null, errorsList);
	}
	
	/**
	 * Retrieves all the messages assigned during the execution of the chain
	 * regarding the key the messages was saved on
	 * @return
	 */
	public List<String> getAllMessages() {
		List<String> allMessages = new ArrayList<String>();
		for(List<String> mess : messages.values()) {
			allMessages.addAll(mess);
		}
		return allMessages;
	}
	
	/**
	 * Retrieves the list of all the errors messages on a 
	 * map separated by keys
	 * @return
	 */
	public Map<String, List<String>> getMessages() {
		return new HashMap<String, List<String>> (messages);
	}
	
	/**
	 * Retrieves all the messages assigned under the specified key
	 * useful to categorize messages
	 * @param key
	 * @return
	 */
	public List<String> getMessages(String key) {
		return messages.get(key);
	}
	
	/**
	 * Adds an specific message categorized by the given key
	 * @param key
	 * @param message
	 */
	public void addMessage(String key, String message) {
		List<String> messagesList = messages.get(key);
		if(messagesList == null) messagesList = new ArrayList<String>();
		messagesList.add(message);
		errorMessages.put(key, messagesList);
	}
	
	/**
	 * Adds an specific message categorized by the given key
	 * @param key
	 * @param message
	 */
	public void addMessage(String message) {
		List<String> messagesList = messages.get(null);
		if(messagesList == null) messagesList = new ArrayList<String>();
		messagesList.add(message);
		errorMessages.put(null, messagesList);
	}

	protected void setExecutionResult(String executionResult) {
		this.executionResult = executionResult;
	}

	/**
	 * Retrieves the value (usually a resultant url when executed a web invoked chain) of the result 
	 * after the chain got executed
	 * @return
	 */
	public String getExecutionResult() {
		return executionResult;
	}	
	
}
