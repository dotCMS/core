package com.dotcms.elasticsearch.script;

import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.script.AbstractLongSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

import java.util.List;
import java.util.Map;

public class RelationshipSortOrderScriptFactory implements NativeScriptFactory {
    
    @Override
    public ExecutableScript newScript(Map<String, Object> params) {
        return new RelationshipSortOrderScript(params.get("identifier").toString(), params.get("relName").toString());
    }
    
    public static class RelationshipSortOrderScript extends AbstractLongSearchScript {
        protected final String orderField;
        protected final String orderPrefix;
        
        public RelationshipSortOrderScript(String identifier,String relName) {
            orderField=(relName+"-order").toLowerCase();
            orderPrefix=identifier+"_";
        }
        
        @Override
        public long runAsLong() {
        	String orderV="";
        	List<String> values =  (List<String>)((ScriptDocValues)doc().get(orderField)).getValues(); 
        	for(String val : values){
        		if(val.indexOf(orderPrefix) != -1){
        			 orderV=val+" ";
        			 break;
        		}
        	}
            int index=orderV.indexOf(orderPrefix); 
            long order=0;
            if(index!=-1) {
                int end=orderV.indexOf(' ', index+1);
                if(end!=-1) {
                    order = Long.parseLong(orderV.substring(index+orderPrefix.length(),end));
                }
            }
            return order;
        }
    }

}
