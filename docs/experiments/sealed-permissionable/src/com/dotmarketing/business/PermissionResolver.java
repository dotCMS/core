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
import com.dotmarketing.portlets.categories.business.Categorizable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;

/**
 * The shape {@code PermissionBitFactoryImpl#resolvePermissionType} could take if the hierarchy were
 * sealed: a pattern switch with <b>no {@code default}</b>.
 *
 * <p>Three things in here are the actual result of the experiment.</p>
 *
 * <p><b>The Inode branch is one case.</b> {@code case Inode i} covers Container, Link, Template,
 * Structure, Field, Category and the rest, whether Inode is sealed or not — so sealing Inode buys this
 * switch nothing. It pays off one level down, in {@link InodePermissionResolver}.</p>
 *
 * <p><b>The last eight cases are a tax, not a design.</b> They exist so the switch is exhaustive.
 * Every one of them is unreachable in practice: a Treeable is always some asset matched further up.
 * They are needed because a {@code non-sealed} interface can still be implemented by anything, and
 * the alternative is sealing all eight — which cascades to their implementors.</p>
 *
 * <p><b>Guards do not count.</b> The Host-by-content-type case is guarded, so it contributes nothing
 * to exhaustiveness; the unguarded {@code case Contentlet _} below it is what makes the Contentlet
 * subtree total. That guard is also the honest limit of the whole idea — see the README.</p>
 */
public final class PermissionResolver {

    private static final String HOST = "Host";

    private PermissionResolver() {
    }

    public static String resolve(final Permissionable permissionable) {

        return switch (permissionable) {

            // ── the Contentlet subtree, most specific first ──────────────────────────────────────
            case Host _ -> HOST;
            case Contentlet c when HOST.equals(c.contentTypeVariable()) -> HOST;
            case HTMLPageAsset _ -> IHTMLPage.class.getCanonicalName();
            case FileAsset _ -> Contentlet.class.getCanonicalName();
            case Contentlet _ -> Contentlet.class.getCanonicalName();

            // ── everything with its own top-level branch ─────────────────────────────────────────
            case Folder _ -> Folder.class.getCanonicalName();
            case Identifier _ -> Identifier.class.getCanonicalName();

            // one case for the whole Inode subtree; the detail lives one level down
            case Inode i -> InodePermissionResolver.resolve(i);

            case ContentType _ -> ContentType.class.getCanonicalName();
            case NavResult _ -> NavResult.class.getCanonicalName();
            case Environment _ -> Environment.class.getCanonicalName();
            case WorkflowAction _ -> WorkflowAction.class.getCanonicalName();
            case Rule _ -> Rule.class.getCanonicalName();
            case UserProxy _ -> UserProxy.class.getCanonicalName();
            case PermissionableProxy _ -> PermissionableProxy.class.getCanonicalName();

            // ── the eight-case tax: one per non-sealed sub-interface ─────────────────────────────
            case Treeable _ -> Treeable.class.getCanonicalName();
            case Ruleable _ -> Ruleable.class.getCanonicalName();
            case Categorizable _ -> Categorizable.class.getCanonicalName();
            case IFileAsset _ -> IFileAsset.class.getCanonicalName();
            case KeyValue _ -> KeyValue.class.getCanonicalName();
            case VanityUrl _ -> VanityUrl.class.getCanonicalName();
            case IPersona _ -> IPersona.class.getCanonicalName();
            case IHTMLPage _ -> IHTMLPage.class.getCanonicalName();
        };
    }
}
