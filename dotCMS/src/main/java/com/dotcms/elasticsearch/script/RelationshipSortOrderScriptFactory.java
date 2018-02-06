package com.dotcms.elasticsearch.script;

import java.io.IOException;
import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.index.fielddata.ScriptDocValues;

import java.util.List;
import java.util.Map;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;
import org.elasticsearch.script.SearchScript;


public class RelationshipSortOrderScriptFactory implements ScriptEngine {
    @Override
    public String getType() {
        return "expert_scripts";
    }

    @Override
    public <T> T compile(String scriptName, String scriptSource, ScriptContext<T> context, Map<String, String> params) {
        if (context.equals(SearchScript.CONTEXT) == false) {
            throw new IllegalArgumentException(getType() + " scripts cannot be used for context [" + context.name + "]");
        }
        // we use the script "source" as the script identifier
        if ("RelationshipSortOrder".equals(scriptSource)) {
            SearchScript.Factory factory = (p, lookup) -> new SearchScript.LeafFactory() {
                final String orderField;
                final String orderPrefix;
                {
                    if (p.containsKey("identifier") == false) {
                        throw new IllegalArgumentException("Missing parameter [identifier]");
                    }
                    if (p.containsKey("relName") == false) {
                        throw new IllegalArgumentException("Missing parameter [relName]");
                    }

                    orderField=(p.get("relName") + "-order").toLowerCase();
                    orderPrefix=p.get("identifier") +"_";
                }

                @Override
                public SearchScript newInstance(LeafReaderContext context) throws IOException {

                    return new SearchScript(p, lookup, context) {

                        @Override
                        public long runAsLong() {
                            String orderV="";
                            List<String> values =  (List<String>)((ScriptDocValues)getDoc().get(orderField)).getValues();
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

                        @Override
                        public double runAsDouble() {
                            return runAsLong();
                        }
                    };
                }

                @Override
                public boolean needs_score() {
                    return false;
                }
            };
            return context.factoryClazz.cast(factory);
        }
        throw new IllegalArgumentException("Unknown script name " + scriptSource);
    }

    @Override
    public void close() {
        // optionally close resources
    }
}

