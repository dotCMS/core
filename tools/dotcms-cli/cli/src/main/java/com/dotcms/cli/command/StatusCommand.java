package com.dotcms.cli.command;

import static com.dotcms.cli.common.CommandUtils.instanceSuffix;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.UserAPI;
import com.dotcms.api.client.DotCmsClientConfig;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.user.User;
import java.net.URI;
import java.util.Optional;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = StatusCommand.NAME,
        header = "Provide User login and dotCMS profile Status.",
        description = {

        })
public class StatusCommand implements Runnable {

    static final String NAME = "status";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    ServiceManager serviceManager;

    @Inject
    DotCmsClientConfig clientConfig;

    @Override
    public void run() {

        final Optional<ServiceBean> optional = serviceManager.services().stream()
                .filter(ServiceBean::active).findFirst();

        if (optional.isEmpty()) {
            output.info(String.format(
                    " @|bold,underline,cyan No active profile is configured|@ Please use %s Command.",
                    InstanceCommand.NAME));
        } else {
            final ServiceBean serviceBean = optional.get();
            final String suffix = instanceSuffix(serviceBean.name());
            final URI uri = clientConfig.servers().get(suffix);
            if (null == serviceBean.credentials()) {
                output.info(String.format(
                        "Active profile is [@|bold,underline,blue %s |@]. API is [@|bold,underline,blue %s |@]. @|bold,underline No active user.|@ Please use %s Command ",
                        serviceBean.name(), uri, LoginCommand.NAME));
                return;
            }

            output.info(String.format("Active profile is [ @|bold,underline,blue %s @|]. API is [ @|bold,underline,blue %s |@]. User [@|bold,underline,blue %s |@].",
                    serviceBean.name(), uri, serviceBean.credentials()));

            final Optional<String> userId = authenticationContext.getUser();
            if (userId.isEmpty()) {
                output.info(
                        " @|bold,underline,blue Current profile does not have a logged in user.|@");
            } else {
                final Optional<String> token = authenticationContext.getToken();
                if (token.isEmpty()) {
                    output.error(String.format("I did not find a valid token for saved user %s. Please login again." ,
                            userId.get()));
                } else {
                    try {
                        final UserAPI userAPI = clientFactory.getClient(UserAPI.class);
                        final User user = userAPI.getCurrent();
                        output.info(String.format("You're currently logged in as %s.",
                                user.email()));
                    } catch (WebApplicationException wae) {
                        output.error(
                                "Unable to get current user from API. Token could have expired. Please login again!",
                                wae);
                    }
                }
            }
        }
    }

}
