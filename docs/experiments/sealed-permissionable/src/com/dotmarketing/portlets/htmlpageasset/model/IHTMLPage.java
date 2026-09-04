package com.dotmarketing.portlets.htmlpageasset.model;

import com.dotmarketing.business.Permissionable;

/**
 * A page is not an Inode and not a top-level Permissionable: the only implementor is
 * {@link HTMLPageAsset}, and that extends Contentlet.
 */
public non-sealed interface IHTMLPage extends Permissionable {
}
