package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publishing.FilterDescriptor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.MultiKeyValue;
import com.dotmarketing.portlets.workflows.model.MultiSelectionWorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This Workflow Actionlet allows users to Push Publish a piece of Content when executing a Workflow Action. It takes
 * two parameters:
 * <ol>
 *     <li><b>Name of the Environment (String):</b> The name of the Push Publishing environment that will receive the
 *     Contentlet.</li>
 *     <li><b>Filter Key (String):</b> Type in the filter key of one of the Push Publishing filters (the filter key is the name
 *     of the file). Defaults to the filter set as default.</li>
 * </ol>
 * It's worth noting that, as its name implies, this Workflow Actionlet <b>allows users to {@code Push} content ONLY,
 * and NOT to {@code Remove} or {@code Push Remove}.</b>
 *
 * @author Oscar Arrieta
 * @version 3.2
 * @since Mar 4, 2015
 */
public class PushNowActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    private static final String ENVIRONMENT_DELIMITER = ",";
    private static final String ACTIONLET_NAME = "Push Now";
    private static final String ACTIONLET_DESCRIPTION = "This actionlet will automatically publish the the content " +
            "object to the specified environment(s). Multiple environments can be separated by a comma (',')";
    private static final String PARAM_ENVIRONMENT = "environment";
    private static final String PARAM_FILTER_KEY = "filterKey";

    private final PublisherAPI publisherAPI = PublisherAPI.getInstance();
    private final EnvironmentAPI environmentAPI = APILocator.getEnvironmentAPI();
    private final BundleAPI bundleAPI = APILocator.getBundleAPI();
    private final UserAPI userAPI = APILocator.getUserAPI();
    private final RoleAPI roleAPI = APILocator.getRoleAPI();

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        final List<WorkflowActionletParameter> params = new ArrayList<>();
        //Environment Param
        params.add(new WorkflowActionletParameter(PARAM_ENVIRONMENT, Try.of(()->LanguageUtil.get("pushNowActionlet.environments.name")).getOrElse("Name of the Environments"), "", true));
        //Filter Param
        final Collection<FilterDescriptor> filterDescriptorMap = Try.of(()->APILocator.getPublisherAPI().getFiltersDescriptorsByRole(APILocator.systemUser())).get();
        final FilterDescriptor defaultFilter = filterDescriptorMap.stream().filter(filterDescriptor -> filterDescriptor.isDefaultFilter()).findFirst().get();
        final List<MultiKeyValue> multiKeyValueFilterList = new ArrayList<>();
        filterDescriptorMap.stream().forEach(filterDescriptor -> multiKeyValueFilterList.add(new MultiKeyValue(filterDescriptor.getKey(),filterDescriptor.getTitle())));
        params.add(new MultiSelectionWorkflowActionletParameter(PARAM_FILTER_KEY, Try.of(()->LanguageUtil.get("pushNowActionlet.filter")).getOrElse("Name of the Environments"), defaultFilter.getKey(), true,()->multiKeyValueFilterList));
        return params;
    }

    @Override
    public String getName() {
        return ACTIONLET_NAME;
    }

    @Override
    public String getHowTo() {
        return ACTIONLET_DESCRIPTION;
    }

    @Override
    public void executeAction(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {
        final List<String> identifiers = new ArrayList<>();
        final Contentlet contentlet = processor.getContentlet();
        final User user = processor.getUser();
        final String environments = params.get(PARAM_ENVIRONMENT).getValue();
        try {
            if (!UtilMethods.isSet(environments)) {
                Logger.error(this, "There are no Push Publishing environments set to send the bundle.");
            }
            final String[] whereToSend = environments.split(ENVIRONMENT_DELIMITER);
            final List<Environment> envsToSendTo = new ArrayList<>();
            // Lists of Environments to push to
            for (String environmentName : whereToSend) {
                if (UtilMethods.isSet(environmentName)) {
                    environmentName = environmentName.trim();
                    final Environment environment = this.environmentAPI.findEnvironmentByName(environmentName);
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
            final boolean isAdmin = this.userAPI.isCMSAdmin(user);
            final List<Role> roleList = this.roleAPI.loadRolesForUser(user.getUserId(),true);
            final List<Environment> permissionedEnv = new ArrayList<>();
            if (isAdmin) {
                final List<Environment> environmentList = this.environmentAPI.findEnvironmentsWithServers();
                for (final Environment environment : environmentList) {
                    permissionedEnv.add(environment);
                }
            } else {
                for (final Role role : roleList){
                    try {
                        permissionedEnv.addAll(this.environmentAPI.findEnvironmentsByRole(role.getId()));
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
            // Push Publish now
            final Date publishDate = new Date();
            identifiers.add(contentlet.getIdentifier());
            final String filterKey = params.get(PARAM_FILTER_KEY).getValue();
            final FilterDescriptor filterDescriptor = APILocator.getPublisherAPI().getFilterDescriptorByKey(filterKey);
            final boolean forcePush = (boolean) filterDescriptor.getFilters().getOrDefault(FilterDescriptor.FORCE_PUSH_KEY,false);
            final Bundle bundle = new Bundle(null, publishDate, null, user.getUserId(), forcePush,filterDescriptor.getKey());
            this.bundleAPI.saveBundle(bundle, finalEnvs);
            this.publisherAPI.addContentsToPublish(identifiers, bundle.getId(), publishDate, user);
        } catch (final DotPublisherException e) {
            final String errorMsg = String.format("An error occurred when adding Contentlet with ID '%s' to the " +
                    "bundle for Environments [%s]: %s", contentlet.getIdentifier(), environments, e.getMessage());
            Logger.debug(this, errorMsg);
            throw new WorkflowActionFailureException(errorMsg, e);
        } catch (final DotDataException e) {
            final String errorMsg = String.format("An error occurred when saving the bundle for Environments [%s]: " +
                    "%s", environments, e.getMessage());
            Logger.debug(this, errorMsg);
            throw new WorkflowActionFailureException(errorMsg, e);
        }
    }

}