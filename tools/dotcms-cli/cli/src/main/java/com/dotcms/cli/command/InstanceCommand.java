package com.dotcms.cli.command;

import com.dotcms.api.client.DotCmsClientConfig;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.annotation.SecuredPassword;
import com.dotcms.model.config.ServiceBean;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.BooleanUtils.toStringYesNo;

@ActivateRequestContext
@CommandLine.Command(
        name = InstanceCommand.NAME,
        description = "@|bold,green Prints a list of available dotCMS instances.|@ "
                + "Use to activate/switch dotCMS instance. @|bold,cyan -a  --activate|@ followed by the profile name."
)
public class InstanceCommand implements Callable<Integer> {

    static final String NAME = "instance";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @Inject
    DotCmsClientConfig clientConfig;

    @SecuredPassword
    @Inject
    ServiceManager serviceManager;

    @CommandLine.Option(names = {"-act", "--activate"}, arity = "1", description = "Activate a profile by entering it's name.")
    String activate;

    @Override
    public Integer call() {

        final Map<String, URI> servers = clientConfig.servers();
        if (servers.isEmpty()) {
            output.error(
                    "No dotCMS instances are configured. They should be included in the application.properties or via .env file.");
            return ExitCode.SOFTWARE;
        } else {

            final List<ServiceBean> services = serviceManager.services();
            final Map<String, ServiceBean> serviceBeanByName = services.stream()
                    .collect(Collectors.toMap(ServiceBean::name, Function.identity(),
                            (serviceBean1, serviceBean2) -> serviceBean1));

                output.info("Available registered dotCMS servers.");

                for (final Map.Entry<String, URI> entry : servers.entrySet()) {
                    final String suffix = entry.getKey();
                    final URI uri = entry.getValue();
                    final boolean active =
                            serviceBeanByName.containsKey(suffix) && serviceBeanByName.get(suffix)
                                    .active();
                    final String color = active ? "green" : "blue";

                    output.info(String.format(
                            " Profile [@|bold,underline,%s %s|@], Uri [@|bold,underline,%s %s|@], active [@|bold,underline,%s %s|@]. ",
                            color, suffix, color, uri, color, toStringYesNo(active)));
                }

            if (null != activate) {

                final List<ServiceBean> beans = beansList(servers, serviceBeanByName);
                Optional<ServiceBean> optional = get(activate, beans);
                if (optional.isEmpty()) {
                    // The selected option is not valid
                    output.error(String.format(" The instance name [%s] does not match any configured server! Use --list option. ", activate));
                    return ExitCode.SOFTWARE;
                } else {
                    ServiceBean serviceBean = optional.get();
                    serviceBean = serviceBean.withActive(true);
                    try {
                        serviceManager.persist(serviceBean);
                        output.info(String.format(" The instance name [@|bold,underline,green %s|@] is now the active profile.", activate));
                    } catch (IOException e) {
                        output.error("Unable to persist the new selected service ",e);
                        return ExitCode.SOFTWARE;
                    }
                }
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

}
