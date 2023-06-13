package com.dotcms.enterprise.linkchecker;

import java.text.DateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dotcms.enterprise.license.LicenseLevel;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.linkchecker.util.LinkCheckerUtil;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.ThreadSafeSimpleDateFormat;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class LinkCheckerJob implements Job, StatefulJob {
    
    private static DateFormat luceneDateFormat=new ThreadSafeSimpleDateFormat("yyyyMMddHHmmss");
    
    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        if(LicenseUtil.getLevel()< LicenseLevel.STANDARD.level)
            return;
        
        Logger.info(this, "Checking for broken links");
        
        Set<Structure> structures=new HashSet<Structure>();
        try {
            // get structures with wysiwyg fields
            try {
    	        for(Structure st : StructureFactory.getStructures()) {
    	        	if(st.isArchived() == false) {
    	        		for(Field field : FieldsCache.getFieldsByStructureInode(st.getInode()))
    	        			if(field.getFieldType().equals(Field.FieldType.WYSIWYG.toString()))
    	        				structures.add(st);
    	        	}
    	        }
            }
            catch (Exception e) {
            	Logger.error(com.dotcms.enterprise.linkchecker.LinkCheckerJob.class, e.getMessage());
            	throw new JobExecutionException(e);
            }
            
            StringBuilder query=new StringBuilder();
            
            if(ctx.getPreviousFireTime()!=null) {
                // what's changed since our last run (if any)
                query.append("+modDate:[")
                     .append(luceneDateFormat.format(ctx.getPreviousFireTime()))
                     .append(" TO ")
                     .append(luceneDateFormat.format(ctx.getFireTime()))
                     .append("] ");
            }
            else
                Logger.info(this, "First run! Checking for bad links on all content with wysiwyg fields");
            
            // adding all relevant structures
            query.append("+(");
            for(Structure st : structures)
                query.append(" structureName:").append(st.getVelocityVarName()).append(' ');
            query.append(") ");
            
            List<Contentlet> contents=null;
            int offset=0;
            final int pageSize=100;
            int processed=0;
            int badlinks=0;
        
            do {
                contents=APILocator.getContentletAPI().search(
                        query.toString(), pageSize, offset, "modDate", 
                        APILocator.getUserAPI().getSystemUser(), false);
                processed+=contents.size();
                for(Contentlet con : contents) {
                    List<InvalidLink> links=null;
                    try {
                    	if(con.isArchived() == false) {
                        	links=APILocator.getLinkCheckerAPI().findInvalidLinks(con);
                        }
                        // maybe was edited without creating a new version
                        // lets delete what we did before just in case
                        APILocator.getLinkCheckerAPI().deleteInvalidLinks(con);
                    }
                    catch(Exception ex) {
                        Logger.warn(this, "error parsing html in content \""+con.getTitle()+"\" value. Detail: "+ex.getMessage());
                    }       
                    if(links!=null && links.size()>0) {
                        try {
                            HibernateUtil.startTransaction();
                            APILocator.getLinkCheckerAPI().saveInvalidLinks(con, links);
                            HibernateUtil.commitTransaction();
                        }
                        catch(Exception ex) {
                            try {
                                HibernateUtil.rollbackTransaction();
                            } catch (DotHibernateException e) {
                                Logger.warn(this, e.getMessage(),e);
                            }
                            Logger.warn(this, "error saving broken links: "+ex.getMessage());
                        } finally {
                            HibernateUtil.closeSessionSilently();
                        }

                        badlinks+=links.size();
                        
                        try {
                        	if(Config.getBooleanProperty("linkchecker.enable_email_notification",false)) {
	                            // notify the user
	                            User user = APILocator.getUserAPI().loadUserById(con.getModUser());
	                            Mailer mail = new Mailer();
	                            mail.setFromEmail(LanguageUtil.get(user, "checkURL.emailFrom"));
	                            mail.setFromName(LanguageUtil.get(user, "checkURL.emailFromFullName"));
	                            mail.setToEmail(user.getEmailAddress());
	                            mail.setSubject(LanguageUtil.get(user, "checkURL.emailSubject"));
	                            
	                            mail.setHTMLAndTextBody(LinkCheckerUtil.buildEmailBodyWithLinksList(
	                                    LanguageUtil.get(user, "checkURL.emailBody"), 
	                                    user.getFullName(), con.getTitle(), links));
	                            
	                            mail.sendMessage();
                        	}
                        }
                        catch(Exception ex) {
                            Logger.warn(this, ex.getMessage(), ex);
                        }
                    }
                }
                offset+=pageSize;
            } while(contents.size()>0);
            Logger.info(this, "Finished checking for broken links. Processed "+processed+" contentlets. Found "+badlinks+" broken links");
            
        } 
        catch(Exception ex) {
            Logger.error(this, ex.getMessage(), ex);
        }
        finally {
            DbConnectionFactory.closeSilently();
        }
    }
    
}
