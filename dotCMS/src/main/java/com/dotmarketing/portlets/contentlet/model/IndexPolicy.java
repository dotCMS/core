package com.dotmarketing.portlets.contentlet.model;

/**
 * Encapsulates the indexing policy for a {@link Contentlet}
 *
 * <ul>
 * 	 <li>DEFER, you do not care about when is gonna be reindex your content, usually usefull on batch processing.</li>
 * 	 <li>WAIT_FOR, you want to wait until the content is ready to be searchable.</li>
 * 	 <li>FORCE, you want to force the content searchable immediate, however this policy is not highly scalable.</li>
 * </ul>
 *
 * @author jsanca
 */
public enum IndexPolicy {

    DEFER,
    WAIT_FOR,
    FORCE
}
