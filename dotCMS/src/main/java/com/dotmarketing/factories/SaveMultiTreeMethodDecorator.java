package com.dotmarketing.factories;

import com.dotcms.business.ParameterDecorator;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;

public class SaveMultiTreeMethodDecorator implements ParameterDecorator {

    @Override
    public Object[] decorate(final Object[] arguments) {

        final Object [] newArguments = new Object[arguments.length];

        for (int i=0; i < arguments.length; ++i) {

            if (arguments[i] instanceof MultiTree) {

                newArguments[i] = getMultiTreeArgument((MultiTree)arguments[i]);
            } else {
                newArguments[i] = arguments[i];
            }
        }

        return newArguments;
    }

    protected MultiTree getMultiTreeArgument(final MultiTree multiTree) {

        final String    containerId = multiTree.getContainer();
        if (FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId)) {

            try {
                multiTree.setContainer(FileAssetContainerUtil.getInstance().getContainerIdFromPath(containerId));
            } catch (DotDataException e) {
                /** quiet */
            }
        }

        return multiTree;
    }
}
