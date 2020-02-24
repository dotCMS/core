package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PushNowActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;
    private PublisherAPI publisherAPI = PublisherAPI.getInstance();

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        List<WorkflowActionletParameter> params = new ArrayList<WorkflowActionletParameter>();

        params.add(new WorkflowActionletParameter("environment", "Name of the Enviroment", "", true));
        params.add(new WorkflowActionletParameter("force", "Force the Push? true or false", "false", true));

        return params;
    }

    @Override
    public String getName() {
        return "Push Now";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will automatically publish the the content object to the specified enviroment(s). Multiple Environments can be separated by a comma";
    }

    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {
        try {
            // Gets available languages
            // List<Language> languages = languagesAPI.getLanguages();

            Contentlet ref = processor.getContentlet();

            boolean _contentPushNeverExpire = true;

            boolean forcePush = ("true".equals(params.get("force").getValue())) ? true : false;
            String envrions = params.get("environment").getValue();
            if (envrions == null) {
                Logger.error(this.getClass(), "There are no environments set to push to");
            }
            final String filterKey = params.get("filter").getValue();//TODO: We need to implement the select to the push now dialog

            String[] whereToSend = envrions.split(",");

            List<Environment> envsToSendTo = new ArrayList<Environment>();
            List<Environment> permissionedEnv = new ArrayList<Environment>();
            List<Environment> finalEnvs = new ArrayList<Environment>();


            // Lists of Environments to push to
            for (String name : whereToSend) {
                if (UtilMethods.isSet(name)) {
                    name = name.trim();
                    final Environment e = APILocator.getEnvironmentAPI().findEnvironmentByName(name);
                    if (e != null) {

                        envsToSendTo.add(e);
                    }else{
                        Logger.error(PushNowActionlet.class, "The Environment " + name + " does not exists");
                    }
                }

            }

            if(envsToSendTo.isEmpty()){
                throw new DotPublisherException("There are no enviroments to send the bundle");
            }


            // make sure the user has permissions to push
            boolean isAdmin = APILocator.getUserAPI().isCMSAdmin(processor.getUser());
            List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(processor.getUser().getUserId(),true);
            if(isAdmin){
                List<Environment> app = APILocator.getEnvironmentAPI().findEnvironmentsWithServers();
                for(Environment e:app)
                    permissionedEnv.add(e);
            }
            else{
                for(Role r: roles){
                    try {
                        permissionedEnv.addAll(APILocator.getEnvironmentAPI().findEnvironmentsByRole(r.getId()));
                    } catch (Exception e) {
                        Logger.error(PushNowActionlet.class, e.getMessage());
                    }
                }
            }
            for(Environment e : envsToSendTo){
                if(permissionedEnv.contains(e)){
                    finalEnvs.add(e);
                }
            }

            // publish now
            Date publishDate = new Date();

            List<String> identifiers = new ArrayList<String>();
            identifiers.add(ref.getIdentifier());

            Bundle bundle = new Bundle(null, publishDate, null, processor.getUser().getUserId(), forcePush,filterKey);
            APILocator.getBundleAPI().saveBundle(bundle, finalEnvs);

            publisherAPI.addContentsToPublish(identifiers, bundle.getId(), publishDate, processor.getUser());

        } catch (DotPublisherException e) {
            Logger.debug(PushNowActionlet.class, e.getMessage());
            throw new WorkflowActionFailureException(e.getMessage(),e);
        } catch (DotDataException e) {
            Logger.debug(PushNowActionlet.class, e.getMessage());
            throw new WorkflowActionFailureException(e.getMessage(),e);
        }

    }

}