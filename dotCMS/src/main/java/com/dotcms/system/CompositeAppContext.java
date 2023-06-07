package com.dotcms.system;

import java.util.stream.Stream;

/**
 * This class is a composite of AppContexts, it will try to get the attribute from the first context that has it.
 * @author jsanca
 */
public class CompositeAppContext implements AppContext {

    private final AppContext storeContext;
    private final AppContext[] contexts;

    public CompositeAppContext(final AppContext storeContext, final AppContext... contexts) {
        this.storeContext = storeContext;
        this.contexts = null == contexts? new AppContext[0] : contexts;
    }

    @Override
    public <T> T getAttribute(final String attributeName) {
        return (T)Stream.of(this.contexts).filter(context -> context.getAttribute(attributeName) != null)
                .map(c -> c.getAttribute(attributeName)).findFirst().orElse(null);
    }

    @Override
    public <T> void setAttribute(String attributeName, T attributeValue) {
        if (null != this.storeContext) {
            this.storeContext.setAttribute(attributeName, attributeValue);
        }
    }

    @Override
    public String getId() {
        return AppContext.super.getId();
    }
}
