package com.dotcms.business;

import javax.enterprise.context.ApplicationScoped;

/**
 * Implements the {@link SystemAPI}
 * @author jsanca
 */
@ApplicationScoped
public class SystemAPIImpl implements SystemAPI {

    private final SystemTable systemTable = new SystemTableImpl();

    @Override
    public SystemTable getSystemTable() {
        return systemTable;
    }
}
