package com.dotmarketing.portlets.chains;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.chains.business.ChainAPI;
import com.dotmarketing.portlets.chains.business.ChainLinkCodeCompilationException;
import com.dotmarketing.portlets.chains.model.Chain;
import com.dotmarketing.portlets.chains.model.ChainState;
import com.dotmarketing.portlets.chains.model.ChainStateParameter;
import com.dotmarketing.util.Logger;

public class ChainsProcessor {

	private static Pattern variablesPattern = Pattern.compile("\\{([A-Za-z0-9_-]+)\\}");
	
	public static ChainControl executeChain(String chainKey) throws DotDataException, DotCacheException, DotRuntimeException, ChainLinkCodeCompilationException {
		ChainAPI api = APILocator.getChainAPI();
		Chain chain = api.loadChainByKey(chainKey);

		if (chain == null) {
			throw new DotRuntimeException("Couldn't find a suitable chain to execute with the key name = " + chainKey);
		}

		ChainControl control = new ChainControl(chain, chain.getSuccessValue(), chain.getFailureValue());
		return executeChain(chain, control);
	}
	
	public static ChainControl executeChain (String chainKey, HttpServletRequest request) throws DotDataException, DotCacheException, DotRuntimeException, ChainLinkCodeCompilationException {
		ChainAPI api = APILocator.getChainAPI();
		Chain chain = api.loadChainByKey(chainKey);

		if (chain == null) {
			throw new DotRuntimeException("Couldn't find a suitable chain to execute with the key name = " + chainKey);
		}

		ChainControl control = new ChainControl(request, request.getSession(), chain, chain.getSuccessValue(), chain.getFailureValue());
		return executeChain(chain, control);		
	}
	
	@SuppressWarnings("unchecked")
	public static ChainControl executeChain (Chain chain, ChainControl control) throws DotDataException, DotRuntimeException, DotCacheException, ChainLinkCodeCompilationException {
		
		try {
			
         	ChainAPI api = APILocator.getChainAPI();
			
			if(chain == null || control == null) {
				throw new DotRuntimeException("Unable to execute a null chain/control");
			}
				        
            List<ChainState> states = chain.getStates();
            HibernateUtil.startTransaction();

            boolean exceptionOcurred = false;
        	boolean lastLinkResult = true;

        	if(control.getRequest() != null) {
        		HttpServletRequest req = control.getRequest();
        		Enumeration<String> parameterNames = req.getParameterNames();
        		while(parameterNames.hasMoreElements()) {
        		    String e = parameterNames.nextElement();
        			control.putChainProperty(e, req.getParameter(e));
        		}
        	}
        	
        	for (ChainState state : states) {
        		
        		//Resetting default control properties
        		control.putChainProperty(ChainControl.ROLLBACK_ON_ERROR, true);
        		control.putChainProperty(ChainControl.ROLLBACK_ONLY, false);
        		control.putChainProperty(ChainControl.CONTINUE_ON_EXCEPTION, false);
        		control.putChainProperty(ChainControl.CONTINUE_ON_FAILURE, false);
        		control.putChainProperty(ChainControl.RESTART_TRANS, false);
        		
            	ChainLink link = api.instanciateChainLink(state.getLinkCodeId());
                List<ChainStateParameter> params = state.getParameters();
                for(ChainStateParameter p : params) {
                	
                	//Pattern substitution of variables block
                	//If a chain state parameter contains a variable value like myEmail={emailAddress}
                	//This code will try to replace the variable {emailAddress} with a preset emailAddress variable that could have
                	//been set by another link that happened before or a request parameter
                	//this code could also do string replacement like if you have a state parameter like phoneNumber={areaCode}-{phoneNumber}-{ext}, 
                	//this is useful if the request is submitting a parameter broken in pieces and the link expects it differently                	
                	Object value = "";
                	
                	Matcher variablesMatcher = variablesPattern.matcher(p.getValue());
        			if(variablesMatcher.matches()) {
                		String variable = variablesMatcher.group(1);
                		Object preValue = control.getChainProperty(variable);
                		if(preValue != null) {
                			value = preValue;
                		}        				
        			} else {
        				StringBuffer buff = new StringBuffer();
	                	while(variablesMatcher.find()) {
	                		String variable = variablesMatcher.group(1);
	                		Object preValue = control.getChainProperty(variable);
	                		if(preValue != null) {
	                			variablesMatcher.appendReplacement(buff, preValue.toString());
	                		}
	                	}
	                	variablesMatcher.appendTail(buff);
	                	value = buff.toString();
        			}
        			
                	control.putChainProperty(p.getName(), value);
                }

            	try {
                	lastLinkResult = link.execute(control);
                	exceptionOcurred = false;
                } catch (Exception linkException) {
                	Logger.error(link.getClass(), "Error ocurred executing the link id = " + link.getCode().getId());
                	exceptionOcurred = true;
                	control.addErrorMessage("link_exception", "Link: " + link.getTitle() + " throwed an exception: " + linkException.getMessage());
                }

                //False by default, if an exception occurred within a link code then the whole chain execution will be stopped
                boolean continueOnException = control.isSetToContinueOnException();
                //False by default, if an exception occurred within a link code then the whole chain execution will be stopped
                boolean continueOnFailure = control.isSetToContinueOnFailure();
                //False by default, if an exception occurred within a link code then the whole chain execution will be stopped
                boolean rollbackOnly = control.isSetToRollbackOnly();
                //False by default, if an exception occurred within a link code then the whole chain execution will be stopped
                boolean rollbackOnError = control.isSetToRollbackOnError();

                if(rollbackOnly || (rollbackOnError && (exceptionOcurred || !lastLinkResult))) {
                	HibernateUtil.rollbackTransaction();
                	HibernateUtil.startTransaction();
                } else if (control.isSetToRestartTransaction())
                {
                	HibernateUtil.commitTransaction();
                	HibernateUtil.startTransaction();                	
                }
                
                if((exceptionOcurred && !continueOnException) || (!lastLinkResult && !continueOnFailure)) {
                	//An error occurred that will stop the execution
                	break;
                }
                
                //This is set for the last iteration in case the last link throws an exception 
                //but we will ignore the exception so we go to the success url
                if(continueOnException)
                	exceptionOcurred = false;
                
                //This is set for the last iteration in case the last link returned failure
                //but we will ignore the last link result so we go to the success url
                if(continueOnFailure)
                	lastLinkResult = true;
                
            }

            HibernateUtil.commitTransaction();
            
            if (exceptionOcurred || !lastLinkResult) {
                String failure = control.getFailureValue();
                control.setExecutionResult(failure);
                return control;
            } else {
                String success = control.getSuccessValue();
                control.setExecutionResult(success);
                return control;
            }            
 
        } finally {
        	HibernateUtil.closeSession();
        }

	}
}
