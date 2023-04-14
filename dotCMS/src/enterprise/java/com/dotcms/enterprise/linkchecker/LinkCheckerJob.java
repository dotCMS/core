/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

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

    private static final DateFormat luceneDateFormat = new ThreadSafeSimpleDateFormat("yyyyMMddHHmmss");

    @Override
    public void execute(final JobExecutionContext ctx) throws JobExecutionException {

        if(LicenseUtil.getLevel()< LicenseLevel.STANDARD.level) {
            return;
        }
        
        Logger.info(this, "Checking for broken links");
        
        final Set<Structure> structures = new HashSet<>();

        try {
            // get structures with wysiwyg fields
            try {

    	        for(final Structure structure : StructureFactory.getStructures()) {

    	        	if(!structure.isArchived()) {

    	        		for(final Field field : FieldsCache.getFieldsByStructureInode(structure.getInode())) {

                            if (field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {

                                structures.add(structure);
                            }
                        }
    	        	}
    	        }
            } catch (Exception e) {
            	Logger.error(com.dotcms.enterprise.linkchecker.LinkCheckerJob.class, e.getMessage());
            	throw new JobExecutionException(e);
            }

            final StringBuilder query = new StringBuilder();
            if(ctx.getPreviousFireTime()!=null) {
                // what's changed since our last run (if any)
                query.append("+modDate:[")
                     .append(luceneDateFormat.format(ctx.getPreviousFireTime()))
                     .append(" TO ")
                     .append(luceneDateFormat.format(ctx.getFireTime()))
                     .append("] ");
            } else {
                Logger.info(this,
                        "First run! Checking for bad links on all content with wysiwyg fields");
            }

            // adding all relevant structures
            query.append("+(");

            for(final Structure structure : structures) {

                query.append(" structureName:").append(structure.getVelocityVarName()).append(' ');
            }

            query.append(") ");

            List<Contentlet> contents = null;
            int offset         = 0;
            final int pageSize = 100;
            int processed      = 0;
            int badlinks       = 0;
        
            do {

                contents = APILocator.getContentletAPI()
                        .search(query.toString(), pageSize, offset, "modDate",
                            APILocator.getUserAPI().getSystemUser(), false);

                processed += contents.size();

                for(final Contentlet contentlet : contents) {

                    List<InvalidLink> links = null;

                    try {

                    	if(!contentlet.isArchived()) {

                        	links = APILocator.getLinkCheckerAPI().findInvalidLinks(contentlet);
                        }
                        // maybe was edited without creating a new version
                        // lets delete what we did before just in case
                        APILocator.getLinkCheckerAPI().deleteInvalidLinks(contentlet);
                    } catch(Exception ex) {

                        Logger.warn(this, "error parsing html in content \""+contentlet.getTitle()+"\" value. Detail: "+ex.getMessage());
                    }

                    if(links!=null && links.size()>0) {
                        try {
                            HibernateUtil.startTransaction();
                            APILocator.getLinkCheckerAPI().saveInvalidLinks(contentlet, links);
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
	                            User user = APILocator.getUserAPI().loadUserById(contentlet.getModUser());
	                            Mailer mail = new Mailer();
	                            mail.setFromEmail(LanguageUtil.get(user, "checkURL.emailFrom"));
	                            mail.setFromName(LanguageUtil.get(user, "checkURL.emailFromFullName"));
	                            mail.setToEmail(user.getEmailAddress());
	                            mail.setSubject(LanguageUtil.get(user, "checkURL.emailSubject"));

	                            mail.setHTMLAndTextBody(LinkCheckerUtil.buildEmailBodyWithLinksList(
	                                    LanguageUtil.get(user, "checkURL.emailBody"),
	                                    user.getFullName(), contentlet.getTitle(), links));

	                            mail.sendMessage();
                        	}
                        } catch(Exception ex) {
                            Logger.warn(this, ex.getMessage(), ex);
                        }
                    }
                }
                offset+=pageSize;
            } while(contents.size()>0);

            Logger.info(this, "Finished checking for broken links. Processed "+
                    processed + " contentlets. Found " + badlinks + " broken links");

        } catch(Exception ex) {

            Logger.error(this, ex.getMessage(), ex);
        } finally {

            DbConnectionFactory.closeSilently();
        }
    }
    
}
