package com.dotmarketing.portlets.containers.model;

import com.dotmarketing.beans.Host;

import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * It is a {@link Container} for UI, it make sure to return the container relative path from {@link ContainerView#host}
 */
@JsonSerialize(using = ContainerViewSerializer.class)
public class ContainerView {
    private Container container;
    private Host host;

    public ContainerView(final Container container, final Host host) {
        this.container = container;
        this.host = host;
    }

    /**
     * If {@link ContainerView#container} is a {@link FileAssetContainer} then return the Container relative oath
     * if the container is into {@link ContainerView#host} otherwise ir return the absolute path.
     * If the container is not a {@link FileAssetContainer} then return null.
     *
     * @return
     */
    public String getPath(){
        if (FileAssetContainerUtil.getInstance().isFileAssetContainer(container)) {
            final FileAssetContainer fileAssetContainer = (FileAssetContainer) container;

            return !host.getIdentifier().equals(container.getIdentifier()) ?
                    FileAssetContainerUtil.getInstance().getFullPath(fileAssetContainer) :
                    fileAssetContainer.getPath();
        } else {
            return null;
        }
    }

    public Container getContainer() {
        return container;
    }

    public Host getHost() {
        return host;
    }
}
