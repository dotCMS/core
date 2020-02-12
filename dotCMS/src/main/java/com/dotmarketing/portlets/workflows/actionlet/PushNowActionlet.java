package com.dotmarketing.portlets.workflows.actionlet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This Workflow Actionlet allows users to Push Publish a piece of Content when executing a Workflow Action. It takes
 * two parameters:
 * <ol>
 *     <li><b>Name of the Environment (String):</b> The name of the Push Publishing environment that will receive the
 *     Contentlet.</li>
 *     <li><b>Force the Push? true or false (String):</b> Type in true if the push will be forced. Otherwise, type in
 *     false. Defaults to false.</li>
 * </ol>
 *
 * @author Oscar Arrieta
 * @version 3.2
 * @since Mar 4, 2015
 */
public class PushNowActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;
    private static final String ENVIRONMENT_DELIMITER = ",";
    private static final String PARAM_ENVIRONMENT = "environment";
    private static final String PARAM_FORCE_PUSH = "force";

    private PublisherAPI publisherAPI = PublisherAPI.getInstance();

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        final List<WorkflowActionletParameter> params = new ArrayList<>();
        params.add(new WorkflowActionletParameter(PARAM_ENVIRONMENT, "Name of the Environment", "", true));
        params.add(new WorkflowActionletParameter(PARAM_FORCE_PUSH, "Force the Push? true or false", "false", true));
        return params;
    }

    @Override
    public String getName() {
        return "Push Now";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will automatically publish the the content object to the specified environment(s). Multiple Environments can be separated by a comma";
    }

    @Override
    public void executeAction(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {
        final List<String> identifiers = new ArrayList<>();
        final String environments = params.get(PARAM_ENVIRONMENT).getValue();
        try {
            final boolean forcePush = "true".equals(params.get(PARAM_FORCE_PUSH).getValue()) ? Boolean.TRUE : Boolean.FALSE;
            if (!UtilMethods.isSet(environments)) {
                Logger.error(this, "There are no Push Publishing environments set to send the bundle.");
            }
            final String[] whereToSend = environments.split(ENVIRONMENT_DELIMITER);
            final List<Environment> envsToSendTo = new ArrayList<>();
            // Lists of Environments to push to
            for (String environmentName : whereToSend) {
                if (UtilMethods.isSet(environmentName)) {
                    environmentName = environmentName.trim();
                    final Environment environment = APILocator.getEnvironmentAPI().findEnvironmentByName(environmentName);
                    if (null != environment && UtilMethods.isSet(environment.getId())) {
                        envsToSendTo.add(environment);
                    } else {
                        Logger.error(this, "The Environment '" + environmentName + "' does not exist.");
                    }
                }
            }
            if (envsToSendTo.isEmpty()) {
                throw new WorkflowActionFailureException("There are no environments to send the bundle.");
            }
            // make sure the user has permissions to push
            final boolean isAdmin = APILocator.getUserAPI().isCMSAdmin(processor.getUser());
            final List<Role> roleList = APILocator.getRoleAPI().loadRolesForUser(processor.getUser().getUserId(),true);
            final List<Environment> permissionedEnv = new ArrayList<>();
            if (isAdmin) {
                final List<Environment> environmentList = APILocator.getEnvironmentAPI().findEnvironmentsWithServers();
                for (final Environment e : environmentList) {
                    permissionedEnv.add(e);
                }
            } else {
                for (final Role role : roleList){
                    try {
                        permissionedEnv.addAll(APILocator.getEnvironmentAPI().findEnvironmentsByRole(role.getId()));
                    } catch (final Exception e) {
                        Logger.warn(this, String.format("An error occurred when verifying Role '%s' [%s]: %s", role
                                .getName(), role.getId(), e.getMessage()));
                    }
                }
            }
            final List<Environment> finalEnvs = new ArrayList<>();
            for (final Environment environment : envsToSendTo) {
                if (permissionedEnv.contains(environment)) {
                    finalEnvs.add(environment);
                }
            }
            // Publish now
            final Date publishDate = new Date();
            final Contentlet contentlet = processor.getContentlet();
            identifiers.add(contentlet.getIdentifier());
            final Bundle bundle = new Bundle(null, publishDate, null, processor.getUser().getUserId(), forcePush);
            APILocator.getBundleAPI().saveBundle(bundle, finalEnvs);
            publisherAPI.addContentsToPublish(identifiers, bundle.getId(), publishDate, processor.getUser());
        } catch (final DotPublisherException e) {
            final String errorMsg = String.format("An error occurred when adding the Identifiers %s to the bundle " +
                    "for Environments [%s]: %s", identifiers, environments, e.getMessage());
            Logger.debug(this, errorMsg);
            throw new WorkflowActionFailureException(errorMsg, e);
        } catch (final DotDataException e) {
            final String errorMsg = String.format("An error occurred when saving the bundle for Environments [%s]: %s", environments, e.getMessage());
            Logger.debug(this, errorMsg);
            throw new WorkflowActionFailureException(errorMsg, e);
        }
    }

}
