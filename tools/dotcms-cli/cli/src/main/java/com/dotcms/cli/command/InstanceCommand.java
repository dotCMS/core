package com.dotcms.cli.command;

import static org.apache.commons.lang3.BooleanUtils.toStringYesNo;

import com.dotcms.api.client.DotCmsClientConfig;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import picocli.CommandLine;

@ActivateRequestContext
@CommandLine.Command(
        name = InstanceCommand.NAME,
        header = "@|bold,green Prints a list of available dotCMS instances.|@ "
                + "Use to activate/switch dotCMS instance. @|bold,cyan -a  --activate|@ followed by the profile name.",
        description = {

        }
)
public class InstanceCommand implements Runnable {

    static final String NAME = "instance";

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @Inject
    DotCmsClientConfig clientConfig;

    @Inject
    ServiceManager serviceManager;

    @CommandLine.Option(names = {"-a", "--activate"}, description = "Activate a profile by entering it's name.", arity = "1")
    String activate;

    @Override
    public void run() {

        final Map<String, URI> servers = clientConfig.servers();

        output.info("@|bold,underline,green dotCMS profiles|@");

        if (servers.isEmpty()) {
            output.error(
                    "No dotCMS instances are configured. They should be included in the application.properties or via .env file.");
        } else {

            final List<ServiceBean> services = serviceManager.services();
            final Map<String, ServiceBean> serviceBeanByName = services.stream()
                    .collect(Collectors.toMap(ServiceBean::name, Function.identity(),
                            (serviceBean1, serviceBean2) -> serviceBean1));

            output.info("Available registered dotCMS servers.");

            for (final Map.Entry<String, URI> entry : servers.entrySet()) {
                final String suffix = entry.getKey();
                final URI uri = entry.getValue();
                final boolean active = serviceBeanByName.containsKey(suffix) ? serviceBeanByName.get(suffix).active() : false;
                final String color = active ? "green" : "blue";

                output.info(String.format(" Profile [@|bold,underline,%s %s|@], Uri [@|bold,underline,%s %s|@], active [@|bold,underline,%s %s|@]. ",
                        color, suffix, color, uri, color, toStringYesNo(active)));
            }

            if (null != activate) {

                final List<ServiceBean> beans = beansList(servers, serviceBeanByName);
                Optional<ServiceBean> optional = get(activate, beans);
                if (optional.isEmpty()) {
                    // The selected option is not valid
                    output.info(String.format(" The Selected instance [@|bold,blue %s|@] @|bold,underline does not exist!|@", activate));
                } else {
                    ServiceBean serviceBean = optional.get();
                    serviceBean = serviceBean.withActive(true);
                    try {
                        serviceManager.persist(serviceBean);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }


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
               //final String profileName = instanceName(suffix);
               bean = ServiceBean.builder().active(false).name(suffix).credentials(null).build();
            }
            beans.add(bean);

        }
        return beans;
    }

}
