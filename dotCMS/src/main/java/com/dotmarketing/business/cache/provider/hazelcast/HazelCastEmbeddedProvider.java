package com.dotmarketing.business.cache.provider.hazelcast;

import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.util.Logger;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by jasontesser on 3/14/17.
 */
public class HazelCastEmbeddedProvider extends HazelCastClientProvider {


    /**
   * 
   */
  private static final long serialVersionUID = 1L;


    protected HazelcastInstance hazelEmbedded = null;


  final String xmlFilePath;

    @Override
    public void init() throws Exception {
        Logger.info(this, "Setting Up HazelCast Embedded Config");
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(xmlFilePath);
            XmlConfigBuilder builder = new XmlConfigBuilder(is);
            hazel = Hazelcast.newHazelcastInstance(builder.build());
            initialized = true;
        } finally {
            if (is != null) {
                is.close();
            }
        }

//        Logger.info(this, "Setting Up HazelCast Client Config");
//        is = null;
//        try {
//            is = getClass().getClassLoader().getResourceAsStream("hazelcast-client.xml");
//            XmlClientConfigBuilder builder = new XmlClientConfigBuilder(is);
//            hazel = HazelcastClient.newHazelcastClient(builder.build());
//            initialized = true;
//        } finally {
//            if (is != null) {
//                is.close();
//            }
//        }
    }

    public HazelCastEmbeddedProvider(String xmlPath){
     this.xmlFilePath=xmlPath;
    }
    
    public HazelCastEmbeddedProvider(){
      this("hazelcast-embedded.xml");
    }
    @Override
    public String getName() {
        return "Hazelcast Embedded Provider";
    }

    @Override
    public String getKey() {
        return "HazelCastEmbeddedProvider";
    }

   
}


