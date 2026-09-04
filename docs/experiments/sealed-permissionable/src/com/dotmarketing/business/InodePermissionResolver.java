package com.dotmarketing.business;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.fileUpload.model.FileUpload;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.user.model.UserComment;
import com.dotmarketing.portlets.workflowmessages.model.WorkflowMessage;

/**
 * This is the switch that sealing {@code Inode} pays for, and the only one.
 *
 * <p>Nine cases, no {@code default}, and it is exhaustive for a reason worth stating: WebAsset is
 * sealed over exactly Container, Link, Template and WorkflowMessage, so matching those four covers
 * WebAsset itself without a case for it. Exhaustiveness recurses through the sealed subtree.</p>
 *
 * <p>Add a seventh Inode subclass and this method — not {@link PermissionResolver} — is what stops
 * compiling. That is experiment 7.</p>
 */
public final class InodePermissionResolver {

    private InodePermissionResolver() {
    }

    public static String resolve(final Inode inode) {

        return switch (inode) {

            // the WebAsset branch, which is where Container, Link and Template actually live
            case Template _ -> Template.class.getCanonicalName();
            case Container _ -> Container.class.getCanonicalName();
            case Link _ -> Link.class.getCanonicalName();
            case WorkflowMessage _ -> WorkflowMessage.class.getCanonicalName();

            // and the five direct subclasses that are not WebAssets
            case Structure _ -> Structure.class.getCanonicalName();
            case Field _ -> Field.class.getCanonicalName();
            case Category _ -> Category.class.getCanonicalName();
            case FileUpload _ -> FileUpload.class.getCanonicalName();
            case UserComment _ -> UserComment.class.getCanonicalName();

            // ── and this last case is the one the experiment did not see coming ──────────────────
            // Inode is not abstract, in the model or in dotCMS. A bare inode row is a real,
            // instantiable Inode, so the compiler demands a case for the base type itself:
            //
            //   error: the switch expression does not cover all possible input values
            //
            // It is a `default` in all but name — same catch-all, same silence when a subclass is
            // added, except the compiler still names the new type at the permits clause. The only way
            // to be rid of it is to make Inode abstract, which is a behavioural change, not a
            // declaration change.
            case Inode _ -> Inode.class.getCanonicalName();
        };
    }
}
