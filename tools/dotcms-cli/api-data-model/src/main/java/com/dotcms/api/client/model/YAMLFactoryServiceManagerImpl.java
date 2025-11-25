package com.dotcms.api.client.model;

import com.dotcms.api.provider.YAMLMapperSupplier;
import com.dotcms.model.config.ServiceBean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class YAMLFactoryServiceManagerImpl implements ServiceManager {

    static final String DOT_SERVICE_YML = "dot-service.yml";

    private static final ObjectMapper mapper = new YAMLMapperSupplier().get();

    //for testing purposes Overridable
    @ConfigProperty(name = "com.dotcms.service.config", defaultValue = DOT_SERVICE_YML)
    String dotServiceYml;

    @Override
    @CanIgnoreReturnValue
    public ServiceManager persist(ServiceBean service) throws IOException {
        final List<ServiceBean> beans = services();
        final List<ServiceBean> merged = mergeServiceBeans(beans, service);
        try (OutputStream outputStream = Files.newOutputStream(filePath())) {
            mapper.writeValue(outputStream, merged);
        }
        cached = null;
        return this;
    }

    private List<ServiceBean> cached;

    @Override
    public List<ServiceBean> services() throws IOException {
        if(null != cached){
           return cached;
        }
        final Path path = filePath();
        final File yaml = path.toFile();
        if(!yaml.exists() || yaml.length() == 0 ){
            return List.of();
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            cached = mapper.readValue(inputStream, new TypeReference<>() {
            });
        }

        cached = cached.stream().sorted(
                Comparator.comparing(ServiceBean::active).reversed()
                        .thenComparing(ServiceBean::name)
        ).collect(Collectors.toList());

        return cached;
    }

    @Override
    @CanIgnoreReturnValue
    public ServiceManager removeAll() throws IOException {
        final Path path = filePath();
        final File yaml = path.toFile();
        if(yaml.exists()){
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        cached = null;
        return this;
    }

    Path filePath() throws IOException {
        final Path homeDir = Path.of(System.getProperty("user.home"),".dotcms").toAbsolutePath();
        if(Files.notExists(homeDir)){
            Files.createDirectories(homeDir);
        }
        return Path.of(homeDir.toString(), dotServiceYml);
    }


    private List<ServiceBean> mergeServiceBeans(final List<ServiceBean> beans, ServiceBean newServiceBean) {
        int activeCount = 0;
        List<ServiceBean> merged = new ArrayList<>();
        final Iterator<ServiceBean> iterator =  new ArrayList<>(beans).iterator();
        while (iterator.hasNext()) {
            ServiceBean serviceBean = iterator.next();
            //if the new incoming bean is meant to be the new active one... We mark all others inactive
            serviceBean = newServiceBean.active() ? serviceBean.withActive(false) : serviceBean;
            if (newServiceBean.name().equalsIgnoreCase(serviceBean.name())) {
                //Remove cuz it's about to get replaced with the new `newServiceBean`
                iterator.remove();
            } else {
                merged.add(serviceBean);
                //Precaution to guarantee that we'll always have at least one bean marked active
                if(serviceBean.active()){
                    activeCount++;
                }
            }
        }
        //if none active beans are left we force the new one to be active.
        if(0 == activeCount){
            newServiceBean = newServiceBean.withActive(true);
        }
        merged.add(newServiceBean);
        return merged;
    }

}
