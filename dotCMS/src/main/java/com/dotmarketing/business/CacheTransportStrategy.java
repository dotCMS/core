package com.dotmarketing.business;

import com.dotcms.cache.transport.HazelcastCacheTransportEmbedded;
import com.dotcms.cache.transport.postgres.PostgresCacheTransport;
import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.jgroups.NullTransport;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import io.vavr.Lazy;
import io.vavr.control.Try;


/**
 * This class will return the correct Cache transport based on the environment. You get a null
 * transport if community, the PostgresCacheTransport if using postgres, or the old school hazelcast
 * transport if not overidden
 * 
 * @author will
 *
 */
public class CacheTransportStrategy {


    NullTransport nullTransport = new NullTransport();
    
    Lazy<CacheTransport> defaultTransport = Lazy.<CacheTransport>of(()-> {
        String cTransClazz = Config.getStringProperty("CACHE_INVALIDATION_TRANSPORT_CLASS",
                        isPostgres() 
                            ? PostgresCacheTransport.class.getCanonicalName()
                            : HazelcastCacheTransportEmbedded.class.getCanonicalName());
        
            return Try.of(()->(CacheTransport) Class.forName(cTransClazz).newInstance())
                            .getOrElseThrow(e ->new DotRuntimeException(e));
        
    });
    
    
    
    /**
     * returns the appropiate cache transport
     * @return
     */
    CacheTransport get() {
        if(!hasLicense()) {
            return nullTransport;
        }
        return defaultTransport.get();
        
    }
    
    
    
    private boolean hasLicense() {
        return LicenseUtil.getLevel()>200;
        
        
    }
    
    private boolean isPostgres() {
        return DbConnectionFactory.isPostgres();
        
    }
    
    
    
}
