package com.dotcms.cli.command;

import static org.apache.commons.lang3.BooleanUtils.toStringYesNo;

import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.cli.common.HelpOptionMixin;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.annotation.SecuredPassword;
import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@ActivateRequestContext
@CommandLine.Command(
        name = InstanceCommand.NAME,
        header = "@|bold,blue Prints a list of available dotCMS instances.|@",
        description = {
                " A List with all the available dotCMS instances is printed.",
                " The info includes API URL, active user and the current profile.",
                " This list of available servers comes from the file [dot-service.yml]",
                " Located in a folder named dotCMS under the user's home directory.",
                " Use to activate/switch dotCMS instance. @|yellow -act  --activate|@",
                " followed by the instance name.",
                "" // empty line left here on purpose to make room at the end
        }
)
public class InstanceCommand implements Callable<Integer>, DotCommand {

    public static final String NAME = "instance";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOptionMixin helpOption;

    @SecuredPassword
    @Inject
    ServiceManager serviceManager;

    @CommandLine.Option(names = {"-act", "--activate"}, arity = "1", description = "Activate an instance by entering it's name.")
    String activate;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws IOException {

        // Checking for unmatched arguments
        output.throwIfUnmatchedArguments(spec.commandLine());

        final List<ServiceBean> services = serviceManager.services();

        output.info("Available registered dotCMS servers.");

        for (final ServiceBean serviceBean : services) {
            final String color = serviceBean.active() ? "green" : "blue";
            output.info(String.format(
                    " Profile [@|bold,underline,%s %s|@], Uri [@|bold,underline,%s %s|@], active [@|bold,underline,%s %s|@]. ",
                    color, serviceBean.name(), color, serviceBean.url(), color,
                    toStringYesNo(serviceBean.active())));
        }

        if (null != activate) {
            Optional<ServiceBean> optional = get(activate, services);
            if (optional.isEmpty()) {
                // The selected option is not valid
                output.error(String.format(
                        " The instance name [%s] does not match any configured server! Use --list option. ",
                        activate));
                return ExitCode.SOFTWARE;
            } else {
                ServiceBean serviceBean = optional.get();
                serviceBean = serviceBean.withActive(true);
                serviceManager.persist(serviceBean);
                output.info(String.format(
                        " The instance name [@|bold,underline,green %s|@] is now the active one.",
                        activate));
            }
        }

        return ExitCode.OK;
    }

    Optional<ServiceBean> get(final String suffix, final List<ServiceBean> services) {
        return services.stream().filter(serviceBean -> suffix.equals(serviceBean.name())).findFirst();
    }

    List<ServiceBean> beansList(final Map<String, URI> servers,
            final Map<String, ServiceBean> serviceBeanByName) {
        final List<ServiceBean> beans = new ArrayList<>();
        for (final Map.Entry<String, URI> entry : servers.entrySet()) {
            final String suffix = entry.getKey();
            ServiceBean bean = serviceBeanByName.get(suffix);
            if(null == bean){
               bean = ServiceBean.builder().active(false).name(suffix).credentials(null).build();
            }
            beans.add(bean);

        }
        return beans;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public OutputOptionMixin getOutput() {
        return output;
    }
}
