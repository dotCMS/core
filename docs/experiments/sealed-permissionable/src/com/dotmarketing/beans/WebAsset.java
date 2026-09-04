package com.dotmarketing.beans;

import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflowmessages.model.WorkflowMessage;

/**
 * The layer everybody forgets. It is the direct Inode subclass; Container, Link and Template hang off
 * it, not off Inode.
 *
 * <p>It also re-declares {@code implements Permissionable}, which it already has from Inode — so it
 * has to appear in two permits clauses.</p>
 */
public sealed abstract class WebAsset extends Inode implements Permissionable, Treeable, Ruleable

        permits Container, Link, Template, WorkflowMessage {
}
