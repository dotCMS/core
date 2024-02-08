package com.dotcms.cli.command;

import static org.apache.commons.lang3.BooleanUtils.toStringYesNo;

import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.Prompt;
import com.dotcms.model.annotation.SecuredPassword;
import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(
        name = InitCommand.NAME,
        header = "@|bold,blue Initialize the cli and Configures dotCMS instances.|@",
        description = {
                " Creates the initial configuration file for the dotCMS CLI.",
                " The file is created in the user's home directory.",
                " The file is named [dot-service.yml]",
                " This file is used to store the configuration of the dotCMS instances.",
                " An instance can be activated by using the command @|yellow instance -act <instanceName>|@",
                " Typically an instance holds the API URL, the user and the profile-name.",
                " Running this command is mandatory before using the CLI.",
                "" // empty line left here on purpose to make room at the end
        }
)
public class InitCommand implements Callable<Integer>, DotCommand {

    public static final String NAME = "init";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public String getName() {
        return NAME;
    }

    @Inject
    @SecuredPassword
    ServiceManager serviceManager;

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }

    @CommandLine.Option(names = {"-n",
            "--name"}, arity = "1", description = "Instance name")
    String name;

    @CommandLine.Option(names = {"-u",
            "--url"}, arity = "1", description = "API URL")
    URL url;

    @CommandLine.Option(names = {"-a",
            "--active"}, description = "Active", defaultValue = "false")
    boolean active;

    @Override
    public Integer call() throws Exception {
        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        //Default action prints the current configuration
        final List<ServiceBean> services = serviceManager.services();
          printServices(services);
        //if no params are passed then we will ask the user if they want to add a default configuration
        if (null == name || null == url) {
            if (services.isEmpty()) {
                final boolean yesOrNo = Prompt.yesOrNo(true,
                        "A default configuration will written ? y/n: ");
                if (yesOrNo) {
                    addDefaults();
                    output.info("A Basic default configuration has been saved .");
                    return ExitCode.OK;
                }
            }
            //if no params are passed and no default configuration is written then we will exit and show usage
            return ExitCode.USAGE;
        }

        //if params are passed then we will save the configuration
        final Optional<ServiceBean> serviceBean = serviceManager.get(name);
        if (serviceBean.isPresent()) {
            final boolean yesOrNo = Prompt.yesOrNo(true,
                    " An instance named [%s] already exists! \n Do you want to override and the new instance ? y/n: ");
            if (yesOrNo) {
                serviceManager.persist(
                        ServiceBean.builder().name(name).url(url).active(active).build());
            } else {
                output.info(" Configuration not saved.");
                return ExitCode.OK;
            }
        } else {
            serviceManager.persist(
                    ServiceBean.builder().name(name).url(url).active(active).build());
            output.info("Configuration saved.");
        }

        return ExitCode.OK;
    }

    private void printServices(List<ServiceBean> services) {
        if (!services.isEmpty()) {
            for (ServiceBean service : services) {
                final String message = shortFormat(service);
                output.info(message);
            }
            output.info(" The CLI has been already initialized. \n");
        } else {
            output.info(" No instances are configured. \n");
        }
    }

    String shortFormat(ServiceBean serviceBean) {
        final String color = serviceBean.active() ? "green" : "blue";
        return String.format(
                " Profile [@|bold,underline,%s %s|@], Uri [@|bold,underline,%s %s|@], active [@|bold,underline,%s %s|@]. ",
                color, serviceBean.name(), color, serviceBean.url(), color,
                toStringYesNo(serviceBean.active()));
    }

    void addDefaults() throws IOException {
        serviceManager.persist(
                        ServiceBean.builder().name("local").url(new URL("http://localhost:8080/"))
                                .active(true).build())
                .persist(ServiceBean.builder().name("demo")
                        .url(new URL("https://demo.dotcms.com/")).active(false).build());
    }

}
