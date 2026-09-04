package com.dotmarketing.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.rendering.velocity.viewtools.navigation.NavResult;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.portlets.categories.business.Categorizable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;

/**
 * Sealed, with its permitted subtypes in nine OTHER packages — the real layout, not a simplification.
 *
 * <p>Two things about this clause are worth more than the clause itself.</p>
 *
 * <p><b>Fourteen classes, but only eleven distinct branches.</b> {@code Structure} and
 * {@code WebAsset} already inherit Permissionable through {@code Inode}, and {@code Host} through
 * {@code Contentlet} — they re-declare {@code implements Permissionable} anyway. A sealed interface
 * counts a declaration, not an inheritance, so all three have to be named here. Delete the redundant
 * {@code implements} in those three files and the clause shrinks by three.</p>
 *
 * <p><b>The eight sub-interfaces are not optional either.</b> Anything that names a sealed type in
 * its {@code extends} clause is a permitted subtype and must be listed. Each of them then has to be
 * declared {@code sealed} or {@code non-sealed} — and every one that stays {@code non-sealed} costs a
 * {@code case} of its own in every exhaustive switch, because a non-sealed interface can still be
 * implemented by anything. See {@link PermissionResolver}.</p>
 */
public sealed interface Permissionable permits

        // the eleven classes that only implement Permissionable
        Contentlet, Folder, Identifier, Inode, ContentType, NavResult, Environment, WorkflowAction,
        Rule, UserProxy, PermissionableProxy,

        // three more that inherit it as well, and say so anyway
        Structure, WebAsset, Host,

        // and the eight interfaces that extend it
        Treeable, Ruleable, Categorizable, IFileAsset, KeyValue, VanityUrl, IPersona, IHTMLPage {

    /**
     * The real one is abstract and every asset returns a canonical class name from it; a default
     * keeps the model to one file per type without changing anything the experiments measure.
     */
    default String getPermissionType() {
        return getClass().getCanonicalName();
    }
}
