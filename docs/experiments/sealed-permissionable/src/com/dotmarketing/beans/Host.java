package com.dotmarketing.beans;

import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * A Contentlet subclass that also re-declares Permissionable — so, like Structure and WebAsset, it is
 * named in two permits clauses.
 *
 * <p>Worth keeping in mind next to the resolver: a Site can reach that code as a {@code Host}
 * instance <em>or</em> as a plain Contentlet whose content type is named Host. Only the first of those
 * is a Java type.</p>
 */
public final class Host extends Contentlet implements Permissionable, Treeable {
}
