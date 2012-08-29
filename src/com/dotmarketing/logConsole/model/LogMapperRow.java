package com.dotmarketing.logConsole.model;

import com.dotmarketing.util.UtilMethods;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class LogMapperRow implements Serializable {

    private static final long serialVersionUID = 1L;

    Boolean enabled;
    String log_name;
    String description;

    public static long getSerialversionuid () {
        return serialVersionUID;
    }

    public String getDescription () {
        return description;
    }

    public void setDescription ( String description ) {
        this.description = description;
    }

    public Boolean getEnabled () {
        return enabled;
    }

    public void setEnabled ( Boolean enabled ) {
        this.enabled = enabled;
    }

    public String getLog_name () {
        return log_name;
    }

    public void setLog_name ( String log_name ) {
        this.log_name = log_name;
    }

    @Override
    public String toString () {
        return ToStringBuilder.reflectionToString( this );
    }

    public Map<String, Object> getMap () {

        Map<String, Object> oMap = new HashMap<String, Object>();
        oMap.put( "description", this.getDescription() );
        oMap.put( "log_name", this.getLog_name() );
        oMap.put( "enabled", this.getEnabled() );

        return oMap;
    }

    public boolean isNew () {
        return UtilMethods.isSet( log_name );
    }

}