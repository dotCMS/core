package com.dotcms.tika;

import org.apache.tika.Tika;

/**
 * @author Jonathan Gamba
 * 1/16/18
 */
public class TikaBuilderImpl implements TikaServiceBuilder {

    @Override
    public TikaProxyService createTikaService() {
        return new TikaProxy(new Tika());
    }

}
