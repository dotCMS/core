/**
 * 
 */
package com.dotmarketing.plugin.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @author Jason Tesser
 * @since 1.6.5b
 * A simple POJO for plugin properties
 */
public class PluginProperty implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -9172277836780504755L;
	private String pluginId;
	private String propkey;
	private String originalValue;
	private String currentValue;
	
	/**
	 * @return the pluginId
	 */
	public String getPluginId() {
		return pluginId;
	}
	/**
	 * @param pluginId the pluginId to set
	 */
	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}
	/**
	 * @return the originalValue
	 */
	public String getOriginalValue() {
		return originalValue;
	}
	/**
	 * @param originalValue the originalValue to set
	 */
	public void setOriginalValue(String originalValue) {
		this.originalValue = originalValue;
	}
	/**
	 * @return the currentValue
	 */
	public String getCurrentValue() {
		return currentValue;
	}
	/**
	 * @param currentValue the currentValue to set
	 */
	public void setCurrentValue(String currentValue) {
		this.currentValue = currentValue;
	}
	
	@Override
	public boolean equals(Object other) {
		 if ( !(other instanceof PluginProperty) ) return false;
		 PluginProperty castOther = (PluginProperty) other;
	        return new EqualsBuilder()
	            .append(this.propkey, castOther.propkey)
	            .append(this.pluginId, castOther.pluginId)
	            .append(this.originalValue, castOther.originalValue)
	            .append(this.currentValue, castOther.currentValue)
	            .isEquals();
	}
	
	@Override
	public int hashCode() {
        return new HashCodeBuilder()
            .append(propkey)
            .append(pluginId)
            .toHashCode();
    }
	/**
	 * @return the propkey
	 */
	public String getPropkey() {
		return propkey;
	}
	/**
	 * @param propkey the propkey to set
	 */
	public void setPropkey(String propkey) {
		this.propkey = propkey;
	}
}
