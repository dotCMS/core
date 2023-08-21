package com.dotcms.rest;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected String and related
 * @author jsanca
 */
public class ResponseEntityStringView extends ResponseEntityView<String> {
    public ResponseEntityStringView(final String entity) {
        super(entity);
    }
}
