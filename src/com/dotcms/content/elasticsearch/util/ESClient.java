package com.dotcms.content.elasticsearch.util;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.util.Iterator;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;

import com.dotmarketing.util.Config;
import com.liferay.util.FileUtil;

public class ESClient {



	private static Node _nodeInstance;
	
	
	final String syncMe = "esSync";
	public Client getClient(){
		if(_nodeInstance ==null){
			synchronized (syncMe) {
				if(_nodeInstance ==null){
					loadConfig();
					initNode();
					
				}
			}
		}
		return _nodeInstance.client();
		
		
	}
	

	private void initNode(){
		String node_id = "dotCMS_" + Config.getStringProperty("DIST_INDEXATION_SERVER_ID");
		_nodeInstance = nodeBuilder(). 
        settings(ImmutableSettings.settingsBuilder(). 
        put("name", node_id).
        build()). 
        build(). 
        start();
		try {
		    // wait a bit while the node gets available for requests
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            
        }
	}
	

	
	
	
	
	public void shutDownNode(){
		if(_nodeInstance != null){
			_nodeInstance.close();
		}
		
	}
	
	private  void loadConfig(){

		Iterator<String> it = Config.getKeys();
		while(it.hasNext()){
			String key = it.next();
			if(key ==null) continue;
			if(key.startsWith("es.")){
				// if we already have a key, use it
				if(System.getProperty(key) == null){
					if(key.equalsIgnoreCase("es.path.data") || key.equalsIgnoreCase("es.path.work")){
                        String esPath = Config.getStringProperty(key);
                      if( new File(esPath).isAbsolute()){
                    	  System.setProperty(key,esPath);
                      }else                    	
                        System.setProperty(key, FileUtil.getRealPath(esPath));
                    }
					else{
                        System.setProperty(key, Config.getStringProperty(key));
                    }

					//logger.debug( "Copying esdata folder..." );							
				}
			}
		}

		
	}
}
