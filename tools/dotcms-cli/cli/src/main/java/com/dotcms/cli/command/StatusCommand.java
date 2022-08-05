package com.dotcms.cli.command;

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
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(
        name = StatusCommand.NAME,
        header = "@|bold,green Provide User login and dotCMS profile Status.|@ @|bold No additional params are expected.|@ ",
        description = {

        })
public class StatusCommand implements Callable<Integer> {

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
    public Integer call() {

        final Optional<ServiceBean> optional = serviceManager.services().stream()
                .filter(ServiceBean::active).findFirst();

        if (optional.isEmpty()) {
            output.info(String.format(
                    " @|bold,underline,cyan No active profile is configured|@ Please use %s Command.",
                    InstanceCommand.NAME));
        } else {
            final ServiceBean serviceBean = optional.get();
            final String suffix = serviceBean.name();
            final URI uri = clientConfig.servers().get(suffix);
            if (null == serviceBean.credentials()) {
                output.info(String.format(
                        "Active instance is [@|bold,underline,blue %s|@] API is [@|bold,underline,blue %s|@] @|bold,underline No active user|@ Use %s Command.",
                        serviceBean.name(), uri, LoginCommand.NAME)
                );
            } else {

                output.info(String.format(
                        "Active instance is [@|bold,underline,blue %s|@] API is [@|bold,underline,blue %s|@] User [@|bold,underline,blue %s|@]",
                        serviceBean.name(), uri, serviceBean.credentials().user()));

                final Optional<String> userId = authenticationContext.getUser();
                if (userId.isEmpty()) {
                    output.info(
                            "@|bold,underline Current profile does not have a logged in user.|@");
                } else {
                    final Optional<char[]> token = authenticationContext.getToken();
                    if (token.isEmpty()) {
                        output.error(String.format(
                                "I did not find a valid token for saved user %s. Please login again.",
                                userId.get()));
                    } else {
                        try {
                            final UserAPI userAPI = clientFactory.getClient(UserAPI.class);
                            final User user = userAPI.getCurrent();
                            output.info(String.format("You're currently logged in as %s.", user.email()));
                            return ExitCode.OK;
                        } catch (Exception wae) {
                            output.error(
                                    "Unable to get current user from API. Token could have expired. Please login again!",
                                    wae);
                        }
                    }
                }
            }
        }
        return ExitCode.SOFTWARE;
    }

}
