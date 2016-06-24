package com.dotcms.publisher.util;

/**
 * Defines the types of assets that can be included in a bundle during the Push
 * Publish process. It is important to keep track of the different types of
 * pusheable assets given that there are specific bundlers and handlers for each
 * of them. This distinction is very important when filling up the publishing
 * queue with the objects the user wants to push.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since Mar 9, 2016
 *
 */
public enum PusheableAsset {

	HTMLPAGE("htmlpage"), CONTENT_TYPE("structure"), TEMPLATE("template"), CONTAINER("containers"), FOLDER("folder"), SITE(
			"host"), CATEGORY("category"), LINK("links"), WORKFLOW("workflow"), LANGUAGE("language"), RULE("rule"), USER(
			"user"), OSGI("osgi"), CONTENTLET("contentlet");

	private String type = null;

	/**
	 * Default constructor.
	 * 
	 * @param type
	 *            - The type of the pusheable asset.
	 */
	private PusheableAsset(String type) {
		this.type = type;
	}

	/**
	 * Returns the type of this pusheable asset as a <code>String</code>.
	 * 
	 * @return The type of asset that can be pushed.
	 */
	public String getType() {
		return this.type;
	}

	@Override
	public String toString() {
		return super.name();
	}

}
