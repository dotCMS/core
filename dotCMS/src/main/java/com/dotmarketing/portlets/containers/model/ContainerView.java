package com.dotmarketing.portlets.containers.model;

import com.dotmarketing.beans.Host;

import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * It is a {@link Container} for UI, it make sure to return the container relative path from {@link ContainerView#host}
 */
@JsonSerialize(using = ContainerViewSerializer.class)
public class ContainerView {
    private final Container container;

    public ContainerView(final Container container) {
        this.container = container;
    }

    /**
     * If {@link ContainerView#container} is a {@link FileAssetContainer} then return the Container relative oath
     * If the container is not a {@link FileAssetContainer} then return null.
     *
     * @return
     */
    public String getPath(){
        if (FileAssetContainerUtil.getInstance().isFileAssetContainer(container)) {
            final FileAssetContainer fileAssetContainer = (FileAssetContainer) container;

            return FileAssetContainerUtil.getInstance().getFullPath(fileAssetContainer);
        } else {
            return null;
        }
    }

    public Container getContainer() {
        return container;
    }
}
