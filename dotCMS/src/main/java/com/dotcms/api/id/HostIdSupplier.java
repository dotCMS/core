package com.dotcms.api.id;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import io.vavr.control.Try;

public class HostIdSupplier implements IdSupplier {


    @Override
    public String getId(Object... incoming) {
        Contentlet contentlet = Try.of(()-> (Contentlet) incoming[0]).getOrElseThrow(e->new DotRuntimeException("Unable to create Host id based on " + incoming, e));

        if (!contentlet.isHost()) {
            throw new DotRuntimeException("Unable to create id based on " + contentlet);
        }


        return hasher("host", contentlet.getStringProperty(Host.HOST_NAME_KEY));
        

    }
    
    
    


}
