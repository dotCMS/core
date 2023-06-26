package com.dotcms.listeners;

import com.dotcms.rest.config.DotRestApplication;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.util.Logger;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;

import java.util.concurrent.atomic.AtomicReference;

public class ReloadListener extends AbstractContainerLifecycleListener {

    AtomicReference<Container> container = new AtomicReference<>();

//    public void reload() {
//        Container container = this.container.get();
//        if (container!=null) {
//            container.reload(DotRestApplication.setClasses());
//        }
//    }

    @Override
    public void onReload(Container container) {
        Logger.info(this.getClass(),":::: ReloadListener.onReload() ::::");
//        reload();
    }

    @Override
    public void onShutdown(Container container) {
        Logger.info(this.getClass(),":::: ReloadListener.onShutdown() ::::");
    }

    @Override
    public void onStartup(Container container) {
        Logger.info(this.getClass(),":::: ReloadListener.onStartup() ::::");
//        this.container.set(container);
    }

}
