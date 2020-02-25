package com.dotmarketing.portlets.containers.model;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageViewSerializer;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@JsonSerialize(using = ContainerViewSerializer.class)
public class ContainerView {
    private Container container;
    private Host host;

    public ContainerView(final Container container, final Host host) {
        this.container = container;
        this.host = host;
    }

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
