package com.dotcms.rest;

import com.dotcms.publisher.environment.bean.Environment;

/**
 * View class for Environment
 * @author jsanca
 */
public class ResponseEntityEnvironmentView extends ResponseEntityView<Environment> {

    public ResponseEntityEnvironmentView(final Environment entity) {
        super(entity);
    }
}
