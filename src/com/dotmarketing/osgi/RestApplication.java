package com.dotmarketing.osgi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.osgi.framework.wiring.BundleWiring;

import com.dotmarketing.util.Logger;

/**
 * This class is used to load REST resources into a dynamic plugin
 * 
 * @author jorith.vandenheuvel
 *
 */
@ApplicationPath("/")
public class RestApplication extends Application{
	private final BundleWiring bundleWiring;
	private final String restBasePackage;
	private final Set<Class<?>> restClasses;
	
	/**
	 * Creates a new RestApplication
	 * 
	 * @param bundleWiring The BundleWiring object of the current bundle
	 * @param restBasePackage The package that will be (recursively) searched for REST resources. All classes with
	 * an @Path or @Provider annotation will be added.
	 */
	public RestApplication(BundleWiring bundleWiring, String restBasePackage) {
		this.bundleWiring = bundleWiring;
		this.restBasePackage = restBasePackage;
		
		this.restClasses = getRestClasses();
	}
	

	public Set<Class<?>> getClasses() {
		return this.restClasses;
	}
	
	private Set<Class<?>> getRestClasses() {
		Collection<String> classes = bundleWiring.listResources(restBasePackage.replace('.', '/'), "*.class", BundleWiring.LISTRESOURCES_RECURSE);
		Set<Class<?>> restClasses = new HashSet<Class<?>>();
		
        for (String rawClassName : classes) {
        	String className = rawClassName.split(".class")[0].replace('/', '.');

        	try {
				Class<?> clazz = (Class<?>) Class.forName(className);
				
				if (clazz.getAnnotation(Path.class) != null || clazz.getAnnotation(Provider.class) != null) {
					Logger.info(this, "Added REST resource: " + clazz.getName());
					restClasses.add(clazz);
				}
				
			} catch (ClassNotFoundException e) {
				Logger.error(this, "Class not found", e);
				throw new RuntimeException(e);
			}
        }
		return Collections.unmodifiableSet(restClasses);
	}
}
