package com.dotmarketing.beans;

import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.fileUpload.model.FileUpload;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.user.model.UserComment;

/**
 * Round 1 of this experiment declared Inode {@code non-sealed} and stopped there. This is the level
 * it gave up on.
 *
 * <p>The six names below are the real direct subclasses — and they are not the six anyone guesses.
 * Container, Link and Template are not here (they arrive through {@link WebAsset}), and Contentlet is
 * not here at all: it implements Permissionable on its own and is no relation of Inode. See
 * experiment 4.</p>
 */
public sealed class Inode implements Permissionable permits

        WebAsset, Structure, Field, Category, FileUpload, UserComment {
}
