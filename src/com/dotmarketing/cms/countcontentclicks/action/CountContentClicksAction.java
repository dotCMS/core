package com.dotmarketing.cms.countcontentclicks.action;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;


public class CountContentClicksAction extends DispatchAction {
	
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	
	
	
	public ActionForward unspecified(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
	             throws Exception {
			
		
		    String inode = request.getParameter("inode").trim();
		    String redirectLink = request.getParameter("redirect").trim();
		    
		    if (!UtilMethods.isSet(redirectLink)) {
		    	redirectLink = "";
			}
		    
		    if(inode!=null){
		    	
		    	Contentlet contentlet = new Contentlet();	
				try{
					contentlet = conAPI.checkout(inode, APILocator.getUserAPI().getSystemUser(), true);
				}catch(DotDataException e){
					Logger.error(this, "Unable to look up content with inode " + inode, e);
				}
			
				Structure contentletStructure = StructureCache.getStructureByInode(contentlet.getStructureInode());
				Identifier contentletIdentifier = APILocator.getIdentifierAPI().find(contentlet);
		    	Field field;
		    	
		    	/* Validate if a NumberOfClicks field exists in the contentlet structure
				   if not, then create it and populate it.*/
			    
				if (!InodeUtils.isSet(contentletStructure.getField("NumberOfClicks").getInode())) {
					
					List<Field> fields = new ArrayList<Field>();
				    field = new Field("NumberOfClicks", Field.FieldType.TEXT, Field.DataType.TEXT, contentletStructure,
							          false, false, true, Integer.MAX_VALUE, "0", "0", "",true, true, true);
					FieldFactory.saveField(field);
					fields.add(field);
					FieldsCache.removeFields(contentletStructure);
					FieldsCache.addFields(contentletStructure,fields); 
					
				}
			   
				/* Get the  value from the NumberOfClicks field for this contentlet, if the value
				 * is null, then the field does not exists, otherwise increment its value by one
				 * and set it to the contentlet.
				 */
				field = contentletStructure.getField("NumberOfClicks");
				
				String countValue = (contentlet.getStringProperty(field.getVelocityVarName()) ==  null) ? field.getDefaultValue() : (String)contentlet.getStringProperty(field.getVelocityVarName());
				int numberOfClicks  = new Integer(countValue).intValue();
				contentlet.setStringProperty(field.getVelocityVarName(),String.valueOf(numberOfClicks));
				conAPI.checkin(contentlet,new HashMap<Relationship, List<Contentlet>>(),new ArrayList<Category>(),new ArrayList<Permission>() ,APILocator.getUserAPI().getSystemUser(),true);
		    }
		    ActionForward af = new ActionForward(SecurityUtils.stripReferer(redirectLink));
	        af.setRedirect(true);
	        return af;
		
	}
	
}
