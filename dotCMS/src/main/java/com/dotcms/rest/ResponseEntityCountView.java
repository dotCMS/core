package com.dotcms.rest;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected Integer and related
 * @author jsanca
 */
public class ResponseEntityCountView extends ResponseEntityView<CountView> {
    public ResponseEntityCountView(final CountView entity) {
        super(entity);
    }
}
