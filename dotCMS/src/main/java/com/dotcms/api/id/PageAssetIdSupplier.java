package com.dotcms.api.id;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import io.vavr.control.Try;

public class PageAssetIdSupplier implements IdSupplier {



    @Override
    public String getId(Object... incoming) {
        Contentlet contentlet = Try.of(()-> (Contentlet) incoming[0]).getOrElseThrow(e->new DotRuntimeException("Unable to create PageAsset id based on " + incoming, e));

        if (!contentlet.isHTMLPage()) {
            throw new DotRuntimeException("Not a PageAsset to create id based on " + incoming);
        }
        
        return hasher("pageAsset", toHost(contentlet.getHost()).getHostname() , toFolder(contentlet.getFolder()).getPath() , contentlet.getStringProperty(HTMLPageAssetAPI.URL_FIELD));
        

    }


}
