package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class SQLResultsViewTool implements ViewTool {

    private static final UserAPI userAPI = APILocator.getUserAPI();
    Context ctx;
    private InternalContextAdapterImpl ica;
    ArrayList<HashMap<String, String>> errorResults;

    public void init(Object obj) {

        ViewContext context = (ViewContext) obj;
        ctx = context.getVelocityContext();
        errorResults = new ArrayList<HashMap<String, String>>();

    }

    public ArrayList<HashMap<String, String>> getSQLResults(String dataSource, String sql, int startRow, int maxRow) {

        if(!canUserEvaluate()){
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("hasDotConnectSQLError", "true");
            map.put("dotConnectSQLError", "Content Editor cannot execute the SQLResultsViewTool becase it does not have enough permissions to do it");
            Logger.error(this,"Content Editor cannot execute the SQLResultsViewTool becase it does not have enough permissions to do it");
            errorResults.add(map);
            return errorResults;
        } else {

            try{

                if (!UtilMethods.isSet(sql)) {
                    //SQL Query is not Set. Return Empty List
                    return new ArrayList<HashMap<String, String>>();
                }

                try {

                    if(!isSQLValid(sql)){
                        return errorResults;
                    }
                    DotConnect dc = new DotConnect();

                    dc.setSQL(sql);
                    if(!UtilMethods.isSet(startRow)){
                        startRow = 0;
                    }else{
                        if (startRow > 0) {
                            dc.setStartRow(startRow);
                        }
                    }

                    if(!UtilMethods.isSet(maxRow)){
                        maxRow = 0;
                    }else{
                        if (maxRow > 0) {
                            dc.setMaxRows(maxRow);
                        }
                    }

                    if (dataSource.equals("default")) {
                        if(!Config.getBooleanProperty("ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB", false)){
                            Logger.error(this,"getSQLResults Tool is trying to execute queries using the default connection pool.");
                            Logger.debug(this,"ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB is set to false");
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put("hasDotConnectSQLError", "true");
                            map.put("dotConnectSQLError", "getSQLResults Tool is trying to execute queries using the default connection pool. ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB is set to false");
                            errorResults.add(map);
                            return errorResults;
                        } else{
                            return dc.getResults();
                        }
                    } else {
                        return dc.getResults(dataSource);
                    }
                } catch (Exception e) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("hasDotConnectSQLError", "true");
                    map.put("dotConnectSQLError", "There was a sql error:" + e.getMessage());
                    errorResults.add(map);
                    return errorResults;
                }
            }catch(Exception e){
                Logger.error(this,"There was a problem retrieving the content where SQLResultsViewTool was called");
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("hasDotConnectSQLError", "true");
                map.put("dotConnectSQLError", "There was a sql error:" + e.getMessage());
                errorResults.add(map);
                return errorResults;
            }
        }
    }

    public ArrayList<HashMap<String, String>> getParameterizedSQLResults(String dataSource, String sql, ArrayList<Object> params, int startRow, int maxRow) {

        if(!canUserEvaluate()){
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("hasDotConnectSQLError", "true");
            map.put("dotConnectSQLError", "Content Editor cannot execute the SQLResultsViewTool becase it does not have enough permissions to do it");
            Logger.error(this,"Content Editor cannot execute the SQLResultsViewTool becase it does not have enough permissions to do it");
            errorResults.add(map);
            return errorResults;
        } else {

            try{

                if (!UtilMethods.isSet(sql)) {
                    //SQL Query is not Set. Return Empty List
                    return new ArrayList<HashMap<String, String>>();
                }

                try {

                    if(!isSQLValid(sql)){
                        return errorResults;
                    }

                    int counter = 0;
                    for( int i=0; i<sql.length(); i++ ) {
                        if( sql.charAt(i) == '?') {
                            counter++;
                        }
                    }

                    if(counter != params.size()){
                        //amount of params is different. Check the params
                        HashMap<String, String> map = new HashMap<String, String>();
                        map.put("hasDotConnectSQLError", "true");
                        map.put("dotConnectSQLError", "getParameterizedSQLResults Tool won't work if the amount of params in SQL is different than size of params list");
                        errorResults.add(map);
                        return errorResults;
                    }

                    for(Object obj:params){
                        if(!isSQLValid(obj.toString())){
                            return errorResults;
                        }
                    }

                    DotConnect dc = new DotConnect();

                    dc.setSQL(sql);
                    if(!UtilMethods.isSet(startRow)){
                        startRow = 0;
                    }else{
                        if (startRow > 0) {
                            dc.setStartRow(startRow);
                        }
                    }

                    if(!UtilMethods.isSet(maxRow)){
                        maxRow = 0;
                    }else{
                        if (maxRow > 0) {
                            dc.setMaxRows(maxRow);
                        }
                    }

                    for(Object obj : params){
                        if(UtilMethods.isSet(obj))
                            dc.addParam(obj);
                    }

                    if (dataSource.equals("default")) {
                        if(!Config.getBooleanProperty("ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB", false)){
                            Logger.error(this,"getSQLResults Tool is trying to execute queries using the default connection pool.");
                            Logger.debug(this,"ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB is set to false");
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put("hasDotConnectSQLError", "true");
                            map.put("dotConnectSQLError", "getSQLResults Tool is trying to execute queries using the default connection pool. ALLOW_VELOCITY_SQL_ACCESS_TO_DOTCMS_DB is set to false");
                            errorResults.add(map);
                            return errorResults;
                        } else{
                            return dc.getResults();
                        }
                    } else {
                        return dc.getResults(dataSource);
                    }
                } catch (Exception e) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("hasDotConnectSQLError", "true");
                    map.put("dotConnectSQLError", "There was a sql error:" + e.getMessage());
                    errorResults.add(map);
                    return errorResults;
                }
            }catch(Exception e){
                Logger.error(this,"There was a problem retrieving the content where SQLResultsViewTool was called");
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("hasDotConnectSQLError", "true");
                map.put("dotConnectSQLError", "There was a sql error:" + e.getMessage());
                errorResults.add(map);
                return errorResults;
            }
        }
    }


    protected boolean canUserEvaluate(){
        try{
            ica = new InternalContextAdapterImpl(ctx);
            String fieldResourceName = ica.getCurrentTemplateName();
            String inode = null;
            String userId = null;
            if (fieldResourceName.indexOf("field") > -1) {
                inode = fieldResourceName.substring(fieldResourceName.lastIndexOf("/") + 1, fieldResourceName.indexOf("_"));
                Contentlet con = APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), true);
                userId = con.getModUser();
            } else if (fieldResourceName.indexOf(".vtl") > -1) {
                String[] segments = fieldResourceName.split("/");
                inode = segments[segments.length-3];
                Contentlet con = APILocator.getContentletAPI().find(inode, APILocator.getUserAPI().getSystemUser(), true);
                userId = con.getModUser();
            }
            else if (fieldResourceName.indexOf("template") > -1) {
                inode = fieldResourceName.substring(fieldResourceName.lastIndexOf("/") + 1, fieldResourceName.indexOf("."));
                Template t = APILocator.getTemplateAPI().findWorkingTemplate(inode,
                        APILocator.getUserAPI().getSystemUser(), true);
                userId = t.getModUser();

            } else if (fieldResourceName.indexOf("container") > -1) {
                inode = fieldResourceName.substring(fieldResourceName.lastIndexOf("/") + 1, fieldResourceName.indexOf("."));
                Container c = APILocator.getContainerAPI().getWorkingContainerById(inode,
                        APILocator.getUserAPI().getSystemUser(), true);
                userId = c.getModUser();
            }
            if(userId ==null){
                return false;
            }

            User mu = userAPI.loadUserById(userId, APILocator.getUserAPI().getSystemUser(), true);
            Role scripting =APILocator.getRoleAPI().loadRoleByKey("Scripting Developer");
            return APILocator.getRoleAPI().doesUserHaveRole(mu, scripting);
        }
        catch(Exception e){
            Logger.warn(this.getClass(), "Scripting called with error" + e);
            return false;
        }
    }

    protected boolean isSQLValid (String sql) {
        if (sql.toLowerCase().indexOf("user_") > -1) {
            Logger.error(this,"getSQLResults Tool is trying to query the user_ table");
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("hasDotConnectSQLError", "true");
            map.put("dotConnectSQLError", "getSQLResults Tool is trying to query the user_ table");
            errorResults.add(map);
            return false;
        }
        if (sql.toLowerCase().indexOf("cms_role") > -1) {
            Logger.error(this,"getSQLResults Tool is trying to query the cms_role table");
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("hasDotConnectSQLError", "true");
            map.put("dotConnectSQLError", "getSQLResults Tool is trying to query the cms_role table");
            errorResults.add(map);
            return false;
        }
        if (sql.toLowerCase().indexOf("delete ") > -1 || sql.toLowerCase().indexOf("drop ") > -1
                || sql.toLowerCase().indexOf("truncate ") > -1 || sql.toLowerCase().indexOf("alter ") > -1
                || sql.toLowerCase().indexOf("create ") > -1 || sql.toLowerCase().indexOf("update ") > -1) {
            Logger.error(this,"getSQLResults Tool is trying to run a forbidden query");
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("hasDotConnectSQLError", "true");
            map.put("dotConnectSQLError", "getSQLResults Tool is trying to run a forbidden query");
            errorResults.add(map);
            return false;
        }
        return true;
    }
}