package com.dotcms.cli.command;

import javax.enterprise.context.control.ActivateRequestContext;

@ActivateRequestContext
public class ContentTypeCommand implements Runnable {

    io.quarkus.registry.config.RegistryConfig config;

    @Override
    public void run() {

    }
}
