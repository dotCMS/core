package com.dotcms.business;

/**
 * Implements the {@link SystemAPI}
 * @author jsanca
 */
public class SystemAPIImpl implements SystemAPI {

    private final SystemTable systemTable = new SystemTableImpl();

    @Override
    public SystemTable getSystemTable() {
        return systemTable;
    }
}
