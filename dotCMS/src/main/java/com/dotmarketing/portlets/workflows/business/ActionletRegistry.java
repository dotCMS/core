package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

/**
 * Holds the workflow actionlets known to a {@link WorkflowAPIImpl}: the registered actionlet
 * classes and a map of one instance per actionlet, keyed by canonical class name and ordered by
 * localized name.
 */
class ActionletRegistry {

	private final List<Class<? extends WorkFlowActionlet>> actionletClasses = new ArrayList<>();

	private static Map<String, WorkFlowActionlet> actionletMap;

	/**
	 * Creates a registry with the given built-in actionlet classes and builds the actionlet map.
	 *
	 * @param builtInActionlets the actionlet classes shipped with dotCMS
	 */
	ActionletRegistry(final Collection<Class<? extends WorkFlowActionlet>> builtInActionlets) {
		actionletClasses.addAll(builtInActionlets);
		refresh();
	}

	/**
	 * Creates a new instance of the registered actionlet class with the given name.
	 *
	 * @param className the fully qualified class name
	 * @return a new actionlet instance, or {@code null} when no registered class has that name
	 */
	WorkFlowActionlet newActionlet(final String className) {
		for ( Class<? extends WorkFlowActionlet> z : actionletClasses ) {
			if ( z.getName().equals(className.trim())) {
				try {
					return z.newInstance();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * Returns the registered actionlet class with the given name.
	 *
	 * @param className the fully qualified class name
	 * @return the class, or {@code null} when none is registered under that name
	 */
	Class<? extends WorkFlowActionlet> getActionletClass(final String className) {
		for ( Class<? extends WorkFlowActionlet> z : actionletClasses ) {
			if ( z.getName().equals(className.trim())) {
				return z;
			}
		}
		return null;
	}

	/**
	 * Registers an actionlet class, replacing any class with the same canonical name, and
	 * rebuilds the actionlet map.
	 *
	 * @param workFlowActionletClass the actionlet class to register
	 */
	void add(final Class<? extends WorkFlowActionlet> workFlowActionletClass) {
		//Prevent dupes
		removeClass(workFlowActionletClass);
		actionletClasses.add(workFlowActionletClass);
		refresh();
	}

	/**
	 * Unregisters the actionlet with the given canonical class name and rebuilds the actionlet map.
	 *
	 * @param workFlowActionletName the canonical class name of the actionlet
	 * @return the actionlet instance that was registered under that name
	 */
	WorkFlowActionlet remove(final String workFlowActionletName) {
		final WorkFlowActionlet actionlet = actionletMap.get(workFlowActionletName);
		removeClass(actionlet.getClass());
		refresh();
		return actionlet;
	}

	/**
	 * Finds the actionlet registered under the given canonical class name.
	 *
	 * @param clazz the canonical class name
	 * @return the actionlet, or {@code null} when none is registered under that name
	 */
	WorkFlowActionlet find(final String clazz) {
		return actionletMap.get(clazz);
	}

	/**
	 * Returns every registered actionlet, ordered by localized name.
	 *
	 * @return a new list of the actionlets
	 */
	List<WorkFlowActionlet> findAll() {
		final List<WorkFlowActionlet> workFlowActionlets = new ArrayList<>();
		final Map<String,WorkFlowActionlet>  actionlets  = actionletMap;
		for (final String actionletName : actionlets.keySet()) {
			workFlowActionlets.add(actionletMap.get(actionletName));
		}
		return workFlowActionlets;
	}

	/**
	 * This method applies an additional "remove-code" in case the first direct remove attempt reports to have failed.
	 * The reason is that the same class could have been loaded from different ClassLoaders (When they come from OSGI)
	 * If removing the class directly from the class instance fails then we look it up by name.
	 * @param workFlowActionletClass
	 */
	private void removeClass(final Class<? extends WorkFlowActionlet> workFlowActionletClass) {
		final boolean found = actionletClasses.remove(workFlowActionletClass);
		if (!found) {
			final String canonicalName = workFlowActionletClass.getCanonicalName();
			final Optional<Class<? extends WorkFlowActionlet>> optionalClass = actionletClasses
					.stream().filter(s -> s.getCanonicalName().equals(canonicalName))
					.findFirst();
			optionalClass.ifPresent(actionletClasses::remove);
		}
	}

	private void refresh() {
		actionletMap = null;
		if (actionletMap == null) {
			synchronized (this.getClass()) {
				if (actionletMap == null) {

					List<WorkFlowActionlet> actionletList = new ArrayList<>();

					// get the dotmarketing-config.properties actionlet classes
					String customActionlets = Config.getStringProperty(WebKeys.WORKFLOW_ACTIONLET_CLASSES, "");

					StringTokenizer st = new StringTokenizer(customActionlets, ",");
					while (st.hasMoreTokens()) {
						String clazz = st.nextToken();
						try {
							WorkFlowActionlet actionlet = (WorkFlowActionlet) Class.forName(clazz.trim()).getDeclaredConstructor().newInstance();
							actionletList.add(actionlet);
						} catch (Exception e) {
							Logger.error(WorkflowAPIImpl.class, e.getMessage());
							Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
						}
					}

					// get the included (shipped with) actionlet classes
					for (Class<? extends WorkFlowActionlet> z : actionletClasses) {
						try {
							actionletList.add(z.newInstance());
						} catch (InstantiationException e) {
							Logger.error(WorkflowAPIImpl.class, e.getMessage());
							Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
						} catch (IllegalAccessException e) {
							Logger.error(WorkflowAPIImpl.class, e.getMessage());
							Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
						}
					}

					Collections.sort(actionletList, new ActionletComparator());
					actionletMap = new LinkedHashMap<>();
					for(WorkFlowActionlet actionlet : actionletList){

						try {
							actionletMap.put(actionlet.getClass().getCanonicalName(),actionlet.getClass().newInstance());
							if ( !actionletClasses.contains( actionlet.getClass() ) ) {
								actionletClasses.add( actionlet.getClass() );
							}
						} catch (InstantiationException e) {
							Logger.error(WorkflowAPIImpl.class,e.getMessage());
							Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
						} catch (IllegalAccessException e) {
							Logger.error(WorkflowAPIImpl.class,e.getMessage());
							Logger.debug(WorkflowAPIImpl.class, e.getMessage(), e);
						}
					}
				}
			}

		}
	}

	private static class ActionletComparator implements Comparator<WorkFlowActionlet>{

		public int compare(WorkFlowActionlet o1, WorkFlowActionlet o2) {
			return o1.getLocalizedName().compareTo(o2.getLocalizedName());

		}
	}
}
