package com.dotmarketing.portlets.campaigns.factories;


import com.dotmarketing.beans.Inode;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.campaigns.model.Campaign;
import com.dotmarketing.portlets.campaigns.model.Click;
import com.dotmarketing.portlets.campaigns.model.Recipient;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;

/**
 *  Description of the Class
 *
 *@author     nan
 *@created    September 21, 2002
 */
public class ClickFactory {

	/**
	 *  Gets the newsletter attribute of the ClickFactory class
	 *
	 *@param  id  Description of the Parameter
	 *@return     The newsletter value
	 */
	/*public static final Click getClick(String id) {
        
        try{
            return getClick(Long.parseLong(id));
        }
        catch(Exception e){
            return new Click();  
        }


	}*/
	public static final Click getClick(String id) {
		HibernateUtil dh = new HibernateUtil(Click.class);
		Click click ;
		try {
			click = (Click) dh.load(id);
		} catch (DotHibernateException e) {
			Logger.error(ClickFactory.class, "Unable to get object", e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return click;
	}
	public static final Click getClickByLinkAndRecipient(String link, Recipient r) {
		if (link == null || (!InodeUtils.isSet(r.getInode()))) {
			return new Click();
		}
		
		//i have errors with hibernate replacing the http:// with http?// so im removing it from the url and doing a like!!!!
		link = link.replaceAll("http(s)?://","");

		String condition = "link like '%"+link+"'";

		return (Click) InodeFactory.getChildOfClassbyCondition(r,Click.class, condition);


	}

	public static Click save(Click click) {
		try {
			HibernateUtil.saveOrUpdate(click);
		} catch (DotHibernateException e) {
			Logger.error(ClickFactory.class, "Unable to save object", e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return click;
	}
    
    public static final Click getClickByLinkAndCampaign(String link, Campaign q) {
        if (link == null || (!InodeUtils.isSet(q.getInode()))) {
            return new Click();
        }
		//i have errors with hibernate replacing the http:// with http?// so im removing it from the url and doing a like!!!!
		link = link.replaceAll("http(s)?://","");

		String condition = "link like '%"+link+"'";

		return (Click) InodeFactory.getChildOfClassbyCondition(q,Click.class, condition);


    }
    
    public static java.util.List getClicksByParent(Inode i) {
    	
    	return InodeFactory.getChildrenClass(i, Click.class);

    }
    
    public static java.util.List getClicksByParentOrderByCount(Inode i) {
    	
    	
		return InodeFactory.getChildrenClassByConditionAndOrderBy(i, Click.class, "0=0", "click_count desc");
    	

    }
    
    
    
    
}
