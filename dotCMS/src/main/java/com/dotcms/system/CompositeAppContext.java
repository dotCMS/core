package com.dotcms.system;

import java.util.stream.Stream;

/**
 * This class is a composite of one or more {@link AppContext} objects. When a specific attribute is requested, it will
 * try to get it from the first context that has it.
 *
 * @author jsanca
 * @since Jun 7th, 2023
 */
public class CompositeAppContext implements AppContext {

    private final AppContext writeContext;
    private final AppContext[] readContexts;

    public CompositeAppContext(final AppContext writeContext, final AppContext... readContexts) {
        this.writeContext = writeContext;
        this.readContexts = null == readContexts? new AppContext[0] : readContexts;
    }

    @Override
    public <T> T getAttribute(final String attributeName) {
        return (T)Stream.of(this.readContexts).filter(context -> context.getAttribute(attributeName) != null)
                .map(c -> c.getAttribute(attributeName)).findFirst().orElse(null);
    }

    @Override
    public <T> void setAttribute(String attributeName, T attributeValue) {
        if (null != this.writeContext) {
            this.writeContext.setAttribute(attributeName, attributeValue);
        }
    }

    @Override
    public String getId() {
        return AppContext.super.getId();
    }
}
