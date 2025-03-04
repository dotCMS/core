package com.dotcms.timemachine.ajax;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESIndexHelper;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.page.PageResource;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class TimeMachineAjaxAction extends IndexAjaxAction {

    private final NotificationAPI notificationAPI;

    public TimeMachineAjaxAction() {
        this(APILocator.getNotificationAPI(), new WebResource());
    }

    private TimeMachineAjaxAction(final NotificationAPI notificationAPI,
            final WebResource webResource) {
        super(webResource);
        this.notificationAPI = notificationAPI;
    }

    @VisibleForTesting
    public TimeMachineAjaxAction(final NotificationAPI notificationAPI, ESIndexHelper indexHelper, ESIndexAPI indexAPI,
            final WebResource webResource) {
        super(indexHelper, indexAPI, webResource);
        this.notificationAPI = notificationAPI;
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
         final HttpSession session = request.getSession(false);
         if(null != session){
            //Clean up attribute so TimeMachineFilter can operate
            session.removeAttribute(PageResource.IS_PAGE_RESOURCE);
         }

        Map<String, String> map = getURIParams();
        String cmd = map.get("cmd");
        java.lang.reflect.Method meth = null;
        Class partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
        Object arglist[] = new Object[] { request, response };
        try {
            final User user = getUser();
            if (user == null ||
                    !APILocator.getRoleAPI()
                            .doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
                SecurityLogger.logInfo(TimeMachineAjaxAction.class,
                        () -> String
                                .format("User `%s` does not have an Admin Role. Only users with `admin` Role should be accessing Time-Machine otherwise this feature will not work correctly.",
                                        user == null ? "unknown" : user.getUserId()));
                response.sendError(401);
                return;
            }
            meth = this.getClass().getMethod(cmd, partypes);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return;
        }
        try {
            meth.invoke(this, arglist);
        } catch (Exception e) {
            Logger.error(this, "Trying to run method:" + cmd);
            Logger.error(this, e.getMessage(), e);
        }
    }



    private static final ObjectWriter jsonWritter=new ObjectMapper().writerWithDefaultPrettyPrinter();

    public void getHostsWithTimeMachine(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<Host> hosts=APILocator.getTimeMachineAPI().getHostsWithTimeMachine();

        Collections.sort(hosts, new Comparator<Host>() {
           @Override
            public int compare(Host o1, Host o2) {
                return o1.getHostname().compareTo(o2.getHostname());
            }
        });

        List<Map<String,String>> list=new ArrayList<>(hosts.size());
        for(Host hh : hosts) {
            Map<String,String> m=new HashMap<>();
            m.put("id", hh.getIdentifier());
            m.put("hostname", hh.getHostname());
            list.add(m);
        }

        Map<String, Object> m=new HashMap<>();
        m.put("identifier", "id");
        m.put("label", "hostname");
        m.put("items", list);
        response.setContentType("application/json");
        jsonWritter.writeValue(response.getOutputStream(), m);
    }

    public void getAvailableTimeMachineForSite(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> map = getURIParams();

        String hostIdentifier=map.get("hostIdentifier");

        if(!validateParams(null, hostIdentifier, null)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Host host=APILocator.getHostAPI().find(hostIdentifier, getUser(), false);

        List<Date> snaps=APILocator.getTimeMachineAPI().getAvailableTimeMachineForSite(host);

        Collections.sort(snaps, new Comparator<Date>() {
           @Override
            public int compare(Date o1, Date o2) {
                return o2.compareTo(o1);
            }
        });
        Locale l = PublicCompanyFactory.getDefaultCompany().getLocale();

        DateFormat fmtPretty=DateFormat.getDateInstance(DateFormat.MEDIUM, l);

        List<Map<String,String>> list=new ArrayList<>(snaps.size());
        for(Date dd : snaps) {
            Map<String,String> m=new HashMap<>();
            m.put("id", Long.toString(dd.getTime()));
            m.put("pretty", fmtPretty.format(dd) + " -  " + UtilMethods.dateToHTMLTime(dd).toLowerCase());
            list.add(m);
        }

        Map<String, Object> m=new HashMap<>();
        m.put("identifier", "id");
        m.put("label", "pretty");
        m.put("items", list);
        response.setContentType("application/json");
        jsonWritter.writeValue(response.getOutputStream(), m);
    }

    public void getAvailableLangForTimeMachine(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> map = getURIParams();
        String hostIdentifier=map.get("hostIdentifier");
        String datestr=map.get("date");

        if(!validateParams(datestr, hostIdentifier, null)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Host host=APILocator.getHostAPI().find(hostIdentifier, getUser(), false);

        List<String> langs=APILocator.getTimeMachineAPI().getAvailableLangForTimeMachine(
                                 host, new Date(Long.parseLong(datestr)));

        List<Map<String,String>> list=new ArrayList<>();

        for(String lid : langs) {
            Language lang=APILocator.getLanguageAPI().getLanguage(lid);
            Map<String,String> m=new HashMap<>();
            m.put("id", lid);
            m.put("pretty", lang.getLanguage()+" - "+lang.getCountry());
            list.add(m);
        }

        Collections.sort(list, new Comparator<Map<String,String>>() {
            @Override
            public int compare(Map<String, String> m1,Map<String, String> m2) {
                return m1.get("pretty").compareTo(m2.get("pretty"));
            }
        });

        Map<String, Object> m=new HashMap<>();
        m.put("identifier", "id");
        m.put("label", "pretty");
        m.put("items", list);
        resp.setContentType("application/json");
        jsonWritter.writeValue(resp.getOutputStream(), m);
    }

    public void startBrowsing(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> map = getURIParams();
        String datestr=map.get("date");
        String hostIdentifier=map.get("hostIdentifier");
        String langid=map.get("langid");

        if(!validateParams(datestr, hostIdentifier, langid))
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        else {
            req.getSession().setAttribute("tm_host",
                    APILocator.getHostAPI().find(hostIdentifier, getUser(), false));
            req.getSession().setAttribute("tm_date", datestr);
            req.getSession().setAttribute("tm_lang", langid);
        }
    }

    public void startBrowsingFutureDate(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> map = getURIParams();
        String datestr=map.get("date");
        String hostIdentifier=map.get("hostIdentifier");
        String langid=map.get("langid");

        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        datestr=Long.toString(sdf.parse(datestr).getTime());

        if(!new Date().before(new Date(Long.parseLong(datestr))))
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        else {
            req.getSession().setAttribute("tm_host",
                    APILocator.getHostAPI().find(hostIdentifier, getUser(), false));
            req.getSession().setAttribute("tm_date", datestr);
            req.getSession().setAttribute("tm_lang", langid);
            req.getSession().setAttribute("dotcache", "refresh");
        }
    }

    public void stopBrowsing(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        req.getSession().removeAttribute("tm_date");
        req.getSession().removeAttribute("tm_lang");
        req.getSession().removeAttribute("tm_host");
        req.getSession().removeAttribute("dotcache");
    }

    private boolean validateParams(String datestr, String hostIdentifier, String langid) {
        try {
            // validating
            if(datestr!=null)
                Long.parseLong(datestr);
            if(hostIdentifier!=null) {
                Host hh=APILocator.getHostAPI().find(hostIdentifier, getUser(), false);
                if(hh==null || !UtilMethods.isSet(hh.getIdentifier()))
                    throw new Exception();
            }
            if(langid!=null) {
                Language ll=APILocator.getLanguageAPI().getLanguage(langid);
                if(ll==null || !UtilMethods.isSet(ll.getId()))
                    throw new Exception();
            }
        }
        catch(Exception ex) {
            return false;
        }
        return true;
    }

    public void disableJob(HttpServletRequest req, HttpServletResponse resp) throws Exception {
    	String date = DateUtil.getCurrentDate();

    	ActivityLogger.logInfo(getClass(), "Deleting Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );
    	AdminLogger.log(getClass(), "Deleting Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );

    	try {
    		APILocator.getTimeMachineAPI().removeQuartzJob();
    	} catch(DotRuntimeException e) {
    		ActivityLogger.logInfo(getClass(), "Error Deleting Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );
        	AdminLogger.log(getClass(), "Error Deleting Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );
    		throw e;
    	}

    	ActivityLogger.logInfo(getClass(), "Modifying Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );
    	AdminLogger.log(getClass(), "Modifying Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );

    }

    public void saveJobConfig(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String cronExp=req.getParameter("cronExp");
        String[] hostIdentifiers=req.getParameterValues("snaphost");
        boolean allhost=req.getParameter("allhosts")!=null;
        boolean incremental=req.getParameter("incremental")!=null;
        String[] langids=req.getParameterValues("lang");
        Map<String, String> map = getURIParams();
        boolean runnow=map.get("run")!=null;
        String date = DateUtil.getCurrentDate();

        ActivityLogger.logInfo(getClass(), "Modifying Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );
        AdminLogger.log(getClass(), "Modifying Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );

        List<Host> hosts=new ArrayList<>();

        List<Language> langs=new ArrayList<>(langids.length);

        if(allhost)
            hosts=APILocator.getHostAPI().findAll(getUser(), false);
        else
            for(String h : hostIdentifiers)
                hosts.add(APILocator.getHostAPI().find(h, getUser(), false));

        for(String id : langids)
            langs.add(APILocator.getLanguageAPI().getLanguage(id));


        try {
        	APILocator.getTimeMachineAPI().setQuartzJobConfig(cronExp,hosts,allhost,langs, incremental);

        }catch (Exception ex) {
        	Logger.error(this, ex.getMessage(),ex);
        	ActivityLogger.logInfo(getClass(), "Error Modifying Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );
            AdminLogger.log(getClass(), "Error Modifying Job", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );
        	writeError(resp, ex.getCause().getMessage());
        	return;
        }

        ActivityLogger.logInfo(getClass(), "Job Modified", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );
        AdminLogger.log(getClass(), "Job Modified", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );

        if(runnow) {
            final List<Host> dhosts=hosts;
            final List<Language> dlangs=langs;
            final boolean inc = incremental;
            new Thread() {
                @CloseDBIfOpened
                public void run() {
                	String date = DateUtil.getCurrentDate();
                    ActivityLogger.logInfo(getClass(), "Job Started", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );
                    AdminLogger.log(getClass(), "Job Started", "User:" + getUser().getUserId() + "; Date: " + date + "; Job Identifier: timemachine"  );
                    APILocator.getTimeMachineAPI().startTimeMachine(dhosts, dlangs,inc);

                    try {
                        //Create a new notification to inform the snapshot was created
                        TimeMachineAjaxAction.this.generateNotification( getUser().getLocale(), getUser().getUserId() );
                    } catch ( Exception e ) {
                        Logger.error( this, "Error creating notification after creation of the Time machine Snapshot.", e );
                    }finally {
                        try {
                            HibernateUtil.closeSession();
                        } catch (DotHibernateException e) {
                            Logger.warn(this, e.getMessage(), e);
                        }
                    }
               }
            }.start();
        }
    }

    public void generateNotification (final Locale userLocale,
                                      final String userId) throws LanguageException, DotDataException {


        this.notificationAPI.generateNotification(
                new I18NMessage("notification.timemachine.created.info.title"), // title = Time Machine
                new I18NMessage("TIMEMACHINE-SNAPSHOT-CREATED" ), // message = Time Machine Snapshot created.
                null, // no actions
                NotificationLevel.INFO,
                NotificationType.GENERIC,
                userId,
                userLocale
        );
    } // generateNotification.

    @Override
    public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}
