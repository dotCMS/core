package com.dotmarketing.cache;

import java.util.Properties;

import org.jboss.cache.config.Dynamic;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.loader.FileCacheLoader;
import org.jboss.cache.util.Util;

public class FileCacheLoaderConfig extends IndividualCacheLoaderConfig {
	private static final long serialVersionUID = 4626734068542420865L;

	private String location;
	@Dynamic
	private boolean checkCharacterPortability = true;

	public FileCacheLoaderConfig() {
		setClassName(DotJbossCacheLoader2.class.getName());
	}

	/**
	 * For use by {@link FileCacheLoader}.
	 * 
	 * @param base
	 *            generic config object created by XML parsing.
	 */
	FileCacheLoaderConfig(IndividualCacheLoaderConfig base) {
		setClassName(DotJbossCacheLoader2.class.getName());
		populateFromBaseConfig(base);
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		testImmutability("location");
		this.location = location;
	}

	public boolean isCheckCharacterPortability() {
		return checkCharacterPortability;
	}

	public void setCheckCharacterPortability(boolean checkCharacterPortability) {
		testImmutability("check.character.portability");
		this.checkCharacterPortability = checkCharacterPortability;
	}

	@Override
	public void setProperties(Properties props) {
		super.setProperties(props);

		if (props != null) {
			setLocation(props.getProperty("location"));
			String prop = props.getProperty("check.character.portability");
			setCheckCharacterPortability((prop == null || Boolean.valueOf(prop)));
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FileCacheLoaderConfig && equalsExcludingProperties(obj)) {
			return Util.safeEquals(location, ((FileCacheLoaderConfig) obj).location);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 31 * hashCodeExcludingProperties() + (location == null ? 0 : location.hashCode());
	}

	@Override
	public FileCacheLoaderConfig clone() throws CloneNotSupportedException {
		return (FileCacheLoaderConfig) super.clone();
	}
}
