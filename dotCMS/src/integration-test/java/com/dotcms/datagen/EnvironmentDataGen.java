package com.dotcms.datagen;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;


public class EnvironmentDataGen extends AbstractDataGen<Environment> {

    private String name = null;
    private boolean pushToAll = true;

    public EnvironmentDataGen name(final String name){
        this.name = name;
        return this;
    }

    public EnvironmentDataGen pushToAll(final boolean pushToAll){
        this.pushToAll = pushToAll;
        return this;
    }

    @Override
    public Environment next() {
        final Environment environment = new Environment();
        environment.setName(
                name == null ? "Environment_" + System.currentTimeMillis() : name);
        environment.setPushToAll(pushToAll);

        return environment;
    }

    @Override
    public Environment persist(final Environment environment) {
        try {
            APILocator.getEnvironmentAPI().saveEnvironment(environment, list());
            return APILocator.getEnvironmentAPI().findEnvironmentById(environment.getId());
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
}
