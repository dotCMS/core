package com.dotmarketing.portlets.campaigns.factories;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.campaigns.model.Campaign;
import com.dotmarketing.portlets.campaigns.model.Click;
import com.dotmarketing.portlets.campaigns.model.Recipient;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;


/**
 *
 * @author  will
 */
public class CampaignFactory {
    public static void unlockAllCampaigns() throws DotHibernateException {
        List campaigns = CampaignFactory.getCampaigns();
        java.util.Iterator i = campaigns.iterator();

        while (i.hasNext()) {
            Campaign c = (Campaign) i.next();

            if (c.isLocked()) {
                c.setLocked(false);
                HibernateUtil.saveOrUpdate(c);
                try {
					HibernateUtil.flush();
				} catch (DotHibernateException e) {
					Logger.error(CampaignFactory.class, "unlockAllCampaigns failed:"+ e,e);
				}
            }
        }

    }

    public static java.util.List getCampaignsByUser(User u) {
       return getCampaignsByUser(u, "","");	
    }
    
    public static java.util.List getCampaignsByUser(User u, String condition, String orderby) {
        HibernateUtil dh = new HibernateUtil(Campaign.class);
        List<Campaign> campaignList = new ArrayList<Campaign>();
        
        if(!UtilMethods.isSet(orderby)){
        	orderby = "start_date desc";
        }
        
        String conditionQuery = "";
        if(UtilMethods.isSet(condition)){
        	conditionQuery = " and title like '%"+condition+"%' ";
        } 
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.campaigns.model.Campaign where type='campaign' and user_id = ? "+conditionQuery+" order by "+ orderby);
			dh.setParam(u.getUserId());
			campaignList = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(CampaignFactory.class, "getCampaignsByUser failed:" + e,e);
		}

        return campaignList;
    }

    public static java.util.List getCampaigns() {
    	return getCampaigns("","");
    }
    
    public static java.util.List getCampaignsForViewer(String orderby) {
    	HibernateUtil dh = new HibernateUtil(Campaign.class);
    	List<Campaign>  campaignsForViewer = new ArrayList<Campaign>(); 
        if(!UtilMethods.isSet(orderby)){
        	orderby = "start_date desc";
        }
        
        try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.campaigns.model.Campaign where type='campaign' and completed_date is not null order by "+orderby);
			campaignsForViewer = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(CampaignFactory.class, "getCampaignsForViewer failed:" + e,e);
		}
        return campaignsForViewer;
    }
    
    public static java.util.List getCampaigns(String condition, String orderby) {
        HibernateUtil dh = new HibernateUtil(Campaign.class);
        List<Campaign> campaigns = new ArrayList<Campaign>(); 
        if(!UtilMethods.isSet(orderby)){
        	orderby = "start_date desc, completed_date desc";
        }
        
        String conditionQuery = "";
        if(UtilMethods.isSet(condition)){
        	conditionQuery = " where type='campaign' and lower(title) like '%"+condition.toLowerCase()+"%' ";
        }
        
        try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.campaigns.model.Campaign "+conditionQuery+" order by "+orderby);
			campaigns = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(CampaignFactory.class, "getCampaigns failed:" + e, e);
		}
        return campaigns;
    }

    public static Campaign getCampaign(String inode, String userId) {
        return getCampaign(inode,userId, "");
    }
    
    public static Campaign getCampaign(String inode, String userId, String orderby) {
        HibernateUtil dh = new HibernateUtil(Campaign.class);
        Campaign campaign = new Campaign();
        if(!UtilMethods.isSet(orderby)){
        	orderby = "start_date";
        }
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.campaigns.model.Campaign where type='campaign' and inode = ? and user_id = ? order by "+orderby);
			dh.setParam(inode);
			dh.setParam(userId);
			campaign = (Campaign) dh.load();
		} catch (DotHibernateException e) {
			Logger.error(CampaignFactory.class, "getCampaign failed:" + e, e);
		}
        return campaign;
    }

    public static Campaign getCampaign(String inode) {
        HibernateUtil dh = new HibernateUtil(Campaign.class);
        Campaign campaign = new Campaign();
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.campaigns.model.Campaign where type='campaign' and inode = ?");
			dh.setParam(inode);
			campaign = (Campaign) dh.load();
		} catch (DotHibernateException e) {
			Logger.error(CampaignFactory.class, "getCampaign failed:" + e ,e);
		}
        return campaign;
    }

    public static Campaign newInstance() {
        Campaign c = new Campaign();
        c.setType("campaign");
        c.setCStartDate(new java.util.Date());

        return c;
    }

    /*
     * deletes campaigns, recipients and clicks that are owned by the user
     */
    public static void deleteCampaign(Campaign c, String userId) throws DotDataException, DotSecurityException {
        if ((userId == null) || (c.getUserId() == null)) {
            return;
        }

        User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
        PermissionAPI perAPI = APILocator.getPermissionAPI();
        if (c.getUserId().equalsIgnoreCase(userId) || perAPI.doesUserHavePermission(c,PermissionAPI.PERMISSION_WRITE,user)) 
        {
            List recipients = RecipientFactory.getAllRecipientsByCampaign(c);
            Iterator i = recipients.iterator();

            while (i.hasNext()) {
                Recipient r = (Recipient) i.next();
                List clicks = ClickFactory.getClicksByParent(r);
                Iterator i2 = clicks.iterator();

                while (i2.hasNext()) {
                    Click click = (Click) i2.next();
                    InodeFactory.deleteInode(click);
                }

                InodeFactory.deleteInode(r);
            }
            
            if(perAPI.doesUserHavePermission(c,PermissionAPI.PERMISSION_WRITE,user)){
            	perAPI.removePermissions(c);
    		}

            InodeFactory.deleteInode(c);
        }
    }

    @SuppressWarnings("unchecked")
	public static synchronized List<Campaign> getWaitingCampaigns() {
        HibernateUtil dh = new HibernateUtil(Campaign.class);
        List<Campaign> list = null ;
        try {
			StringBuffer sb = new StringBuffer("from inode in class com.dotmarketing.portlets.campaigns.model.Campaign where type='campaign' and start_date < ? ");
			sb.append(" and active = " +com.dotmarketing.db.DbConnectionFactory.getDBTrue());
			sb.append(" and locked = " + com.dotmarketing.db.DbConnectionFactory.getDBFalse());
			sb.append(" and isrecurrent = " + com.dotmarketing.db.DbConnectionFactory.getDBFalse());
			sb.append(" and (parent_campaign = '0' or parent_campaign is null)");
			sb.append(" and completed_date is null");
			sb.append(" and (expiration_date is null or expiration_date > ?)");
			sb.append(" order by start_date");
			dh.setQuery(sb.toString());
			
			dh.setParam(new java.util.Date());
			dh.setParam(new java.util.Date());

			list = dh.list();
			Iterator i = list.iterator();

			while (i.hasNext()) {
				Logger.debug(CampaignFactory.class, "gettingWaitingCampaigns");
				
			    Campaign c = (Campaign) i.next();
			    c.setLocked(true);
			    HibernateUtil.flush();
			}
		} catch (DotHibernateException e) {
			Logger.error(CampaignFactory.class, "getWaitingCampaigns failed:" + e,e);
		}

        return list;
    }

    public static boolean isCampaignManagerAdmin (User user) throws PortalException, SystemException {
		List<Role> roles;
		try {
			roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		} catch (DotDataException e) {
			Logger.error(CampaignFactory.class,e.getMessage(),e);
			throw new SystemException(e);
		}
		Iterator<Role> rolesIt = roles.iterator();
		boolean isCampaignManagerAdmin = false;
		while (rolesIt.hasNext()) {
		    Role role = (Role) rolesIt.next();
		    if (role.getName().equals(Config.getStringProperty("CAMPAIGN_MANAGER_ADMIN"))) {
		    	isCampaignManagerAdmin = true;
		        break;
		    }
		}
		return isCampaignManagerAdmin;
    }


    public static java.util.List getChildCampaignsByParent(String inode) {
        HibernateUtil dh = new HibernateUtil(Campaign.class);
        List<Campaign> childCampaigns = null ;
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.campaigns.model.Campaign where type='campaign' and parent_campaign = ? order by inode desc");
			dh.setParam(inode);
			childCampaigns = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(CampaignFactory.class, "getChildCampaignsByParent failed:" + e,e);
		}
        return childCampaigns; 
    }

}
