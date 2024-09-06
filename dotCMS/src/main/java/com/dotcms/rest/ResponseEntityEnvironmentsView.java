package com.dotcms.rest;

import com.dotcms.publisher.environment.bean.Environment;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Set;

/**
 * A view for a collection of environments.

 * @author jsanca
 */
public class ResponseEntityEnvironmentsView extends ResponseEntityView<Collection<Environment>> {
    public ResponseEntityEnvironmentsView(final Collection<Environment> environments) {
        super(environments);
    }
}
