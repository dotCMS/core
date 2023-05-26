package com.dotcms.tika;

import java.io.Serializable;

/**
 * Builder interface that should be implemented in order to create {@link TikaProxyService} instances
 *
 * @author Jonathan Gamba
 * 1/16/18
 */
public interface TikaServiceBuilder extends Serializable {

    /**
     * Returns a new instance of a {@link TikaProxyService}, each time this method is called
     * the new instance of {@link TikaProxyService} creates a new instance of
     * {@link org.apache.tika.Tika} in order to use it inside de {@link TikaProxyService}
     */
    public TikaProxyService createTikaService();

}