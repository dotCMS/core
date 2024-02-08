package com.ettrema.httpclient;

/**
 *
 * @author j2ee
 */
public interface ResourceListener {

    void onChanged( Resource r );

    void onDeleted( Resource r );
}
