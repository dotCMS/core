package com.dotmarketing.portlets.structure.model;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Treeable;
/** Inherits Permissionable from Inode and declares it again — hence two permits clauses. */
public final class Structure extends Inode implements Permissionable, Treeable {
}
