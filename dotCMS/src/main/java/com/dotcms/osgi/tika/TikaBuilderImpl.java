package com.dotcms.osgi.tika;

import com.dotcms.tika.TikaProxyService;
import com.dotcms.tika.TikaServiceBuilder;
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