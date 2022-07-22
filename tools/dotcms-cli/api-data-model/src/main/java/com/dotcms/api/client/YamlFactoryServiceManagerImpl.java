package com.dotcms.api.client;

import com.dotcms.model.config.ServiceBean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.arc.DefaultBean;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@DefaultBean
@ApplicationScoped
public class YamlFactoryServiceManagerImpl implements ServiceManager{

    private static final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory())
            .findAndRegisterModules();

    //for testing purposes Override
    @ConfigProperty(name = "com.dotcms.service.config", defaultValue = "dot-service.yml")
    String dotServiceYml;

    @Override
    public void persist(ServiceBean service) throws IOException {
        final List<ServiceBean> beans = services();
        final List<ServiceBean> merged = mergeServiceBeans(beans, service);
        try (OutputStream outputStream = Files.newOutputStream(filePath())) {
            ymlMapper.writeValue(outputStream, merged);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        cached = null;
    }

    private List<ServiceBean> cached;

    @Override
    public List<ServiceBean> services() {
        //TODO: make his thread safe
        if(null != cached){
           return cached;
        }
        final Path path = filePath();
        final File yaml = path.toFile();
        if(!yaml.exists()){
            return new ArrayList<>();
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            cached = ymlMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        return cached;
    }

    @Override
    public void clear() {
        final Path path = filePath();
        final File yaml = path.toFile();
        if(yaml.exists()){
           yaml.delete();
        }
        cached = null;
    }

    Path filePath() {
        final Path homeDir = Path.of(System.getProperty("user.home"),".dotcms").toAbsolutePath();
        final File homeDirFile = homeDir.toFile();
        homeDirFile.mkdirs();
        return Path.of(homeDir.toString(), dotServiceYml);
    }


    private List<ServiceBean> mergeServiceBeans(final List<ServiceBean> beans, ServiceBean newServiceBean) {
        int activeCount = 0;
        List<ServiceBean> merged = new ArrayList<>();
        final Iterator<ServiceBean> iterator = beans.iterator();
        while (iterator.hasNext()) {
            ServiceBean serviceBean = iterator.next();
            //if the new incoming bean is meant to be the new active one.. We mark all others inactive
            serviceBean = newServiceBean.active() ? serviceBean.withActive(false) : serviceBean;
            if (newServiceBean.name().equals(serviceBean.name())) {
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
