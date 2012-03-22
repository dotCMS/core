package com.dotmarketing.portlets.contentlet.action;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.struts.ActionException;
import com.liferay.portlet.ActionRequestImpl;

public class OrderContentletAction  extends DotPortletAction {

	public static boolean debug = false;

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	
	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
	throws Exception {

		try {
			String cmd = req.getParameter("cmd");

			if (((cmd != null) && cmd.equals("generatemenu"))) {
				HibernateUtil.startTransaction();
				//regenerates menu files
				_orderMenuItemsDragAndDrop(req,res,config,form);
				HibernateUtil.commitTransaction();
				_sendToReferral(req,res,req.getParameter("referer"));
				return;
			}

			_getMenuItems(req,res,config,form);
			setForward(req,"portlet.ext.contentlet.order_contentlets");

		} catch (ActionException ae) {
			_handleException(ae,req);
		}		

	}

	private void _getMenuItems(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form)
	throws Exception {

		Container container = (Container) InodeFactory.getInode(req.getParameter("containerId"),Container.class);
		HTMLPage page= (HTMLPage) InodeFactory.getInode(req.getParameter("pageId"),HTMLPage.class);		
		String languaje_id = (String) ((ActionRequestImpl) req).getHttpServletRequest().getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);

		Identifier containerIdentifier = APILocator.getIdentifierAPI().find(container);
		Identifier pageIdentifier = APILocator.getIdentifierAPI().find(page);

		java.util.List<Contentlet> itemsList = new ArrayList<Contentlet>();
		//gets menu items for this folder parent
		java.util.List<Identifier> itemsIdentifierList = MultiTreeFactory.getChildrenClass(pageIdentifier, containerIdentifier, Identifier.class);
		for(Identifier ident : itemsIdentifierList){			
			List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatConts =
				(List<com.dotmarketing.portlets.contentlet.business.Contentlet>) APILocator.getVersionableAPI().findWorkingVersion(ident,APILocator.getUserAPI().getSystemUser(),false);
				for(com.dotmarketing.portlets.contentlet.business.Contentlet fatCont : fatConts)
				{
					if(InodeUtils.isSet(fatCont.getInode()))
					{
						Contentlet cont = conAPI.convertFatContentletToContentlet(fatCont);	
						if(cont.getLanguageId() == Long.parseLong(languaje_id))
						{
							itemsList.add(cont);
						}
					}
				}
		}
		req.setAttribute(WebKeys.MENU_ITEMS,itemsList);

	}

	private void _orderMenuItemsDragAndDrop(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form)
	throws Exception {
		try
		{
			Container container = (Container) InodeFactory.getInode(req.getParameter("containerId"),Container.class);
			HTMLPage page= (HTMLPage) InodeFactory.getInode(req.getParameter("pageId"),HTMLPage.class);

			Enumeration parameterNames = req.getParameterNames();
			HashMap<String,HashMap<Integer, String>> hashMap = new HashMap<String,HashMap<Integer, String>>();
			while(parameterNames.hasMoreElements())
			{
				String parameterName = (String) parameterNames.nextElement();			
				if(parameterName.startsWith("list"))
				{
					String value = req.getParameter(parameterName);
					String smallParameterName = parameterName.substring(0,parameterName.indexOf("["));
					String indexString = parameterName.substring(parameterName.indexOf("[") + 1,parameterName.indexOf("]"));
					int index = Integer.parseInt(indexString);
					if(hashMap.get(smallParameterName) == null)
					{
						HashMap<Integer, String> hashInodes = new HashMap<Integer, String>();				
						hashInodes.put(index,value);										
						hashMap.put(smallParameterName,hashInodes); 
					}
					else
					{
						HashMap<Integer, String> hashInodes = (HashMap<Integer, String>) hashMap.get(smallParameterName);
						hashInodes.put(index,value);					
					}
				}
			}

			Set<String> keys = hashMap.keySet();
			Iterator keysIterator = keys.iterator();
			while(keysIterator.hasNext())
			{
				String key = (String) keysIterator.next();
				HashMap hashInodes = (HashMap) hashMap.get(key);
				for(int i = 0;i < hashInodes.size();i++)
				{
					String inode = (String) hashInodes.get(i);
					Contentlet c = conAPI.find(inode, APILocator.getUserAPI().getSystemUser(), false);
					
					Identifier containerIdentifier = APILocator.getIdentifierAPI().find(container);
					Identifier pageIdentifier = APILocator.getIdentifierAPI().find(page);
					Identifier iden = APILocator.getIdentifierAPI().find(c);

					MultiTree multiTree = MultiTreeFactory.getMultiTree(pageIdentifier,containerIdentifier,iden);
					multiTree.setTreeOrder(i);
					MultiTreeFactory.saveMultiTree(multiTree);
				}			
			}
		}
		catch(Exception ex)
		{
			Logger.error(this, "_orderContentletItemsDragAndDrop: Exception ocurred.", ex);
			throw ex;
		}
	}


}
