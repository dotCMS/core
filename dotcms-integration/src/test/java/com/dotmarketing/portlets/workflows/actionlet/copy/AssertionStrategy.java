package com.dotmarketing.portlets.workflows.actionlet.copy;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

@FunctionalInterface
public interface AssertionStrategy {

    void apply(Contentlet original, Contentlet copy) throws DotDataException;

}
