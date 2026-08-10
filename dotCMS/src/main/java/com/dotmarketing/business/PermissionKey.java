package com.dotmarketing.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.PermissionAPI.PermissionableType;

/**
 * The key an asset inherits permissions under — what {@code permission_reference} rows are stored
 * and looked up against.
 *
 * <p><strong>Experimental.</strong> This type exists to make a distinction the codebase currently
 * cannot express. {@code resolvePermissionType} returns a {@code String}, and that String has two
 * unrelated origins that the signature flattens into one:</p>
 *
 * <ul>
 *   <li>a value from a closed catalogue — {@code Host}, {@code IHTMLPage}, {@code Container},
 *       {@code Template}, {@code Structure} — already enumerated in {@link PermissionableType};</li>
 *   <li>a value decided at runtime by the content itself — whatever an asset returns from
 *       {@code getPermissionType()}, or the class name a {@code NavResult} says it encloses.</li>
 * </ul>
 *
 * <p>Nothing in {@code String} tells a caller which one it received, and nothing stops a typo from
 * passing for either. The two record shapes below say it in the type.</p>
 *
 * <h2>Why the permitted subtypes are nested</h2>
 *
 * <p>Not style — a language rule. A sealed type in the <em>unnamed module</em> requires every
 * permitted subtype to live in the same package, and dotCMS has no {@code module-info.java}:</p>
 *
 * <pre>{@code error: class X in unnamed module cannot extend a sealed class in a different package}</pre>
 *
 * <p>That is also why this is a new type rather than {@code Permissionable} being sealed in place.
 * Its fourteen implementors are spread across nine packages, and their canonical names are
 * persisted data — {@code permission_reference.permission_type} stores strings like
 * {@code com.dotmarketing.portlets.folders.model.Folder}, which appear hardcoded in this package's
 * SQL. Moving them into one package to satisfy {@code permits} would be a data migration, not a
 * refactor.</p>
 *
 * @author Fabrizio Araya
 */
public sealed interface PermissionKey permits PermissionKey.KnownType, PermissionKey.DeclaredByAsset {

    /**
     * The fully qualified name this key is stored under. Both shapes collapse to the same column
     * value — the distinction they carry is for the code between the resolver and the database, not
     * for the database.
     *
     * @return the permission type string
     */
    String canonicalName();

    /**
     * A key that comes from the known catalogue.
     *
     * @param type the catalogued permissionable type
     */
    record KnownType(PermissionableType type) implements PermissionKey {

        @Override
        public String canonicalName() {
            return type.getCanonicalName();
        }
    }

    /**
     * A key the asset itself declared at runtime. There is no catalogue entry backing it, and no
     * guarantee the value corresponds to any type the permission system knows about.
     *
     * @param canonicalName whatever the asset said
     */
    record DeclaredByAsset(String canonicalName) implements PermissionKey {
    }

    /**
     * The key for a Host.
     *
     * <p><strong>This constant is the first thing worth discussing.</strong> It should read
     * {@code new KnownType(PermissionableType.HOSTS)}, and it cannot: <em>the catalogue has no
     * {@code HOSTS} constant.</em> {@link PermissionableType} lists HTMLPAGES, CONTAINERS, FOLDERS,
     * LINKS, TEMPLATES, TEMPLATE_LAYOUTS, STRUCTURES, CONTENTLETS, CATEGORY and RULES — and
     * {@code resolvePermissionType} has returned {@code Host.class.getCanonicalName()} all along.</p>
     *
     * <p>With a {@code String} return type the gap was invisible: one canonical name looks like any
     * other. Asking the compiler to sort every value into one of two shapes is what surfaced it.
     * Classifying a Host as "declared by the asset" is a placeholder, not a claim that it is right —
     * the fix is to add the constant, which changes what
     * {@code PermissionHelper} publishes over REST, and that is a decision for the review rather
     * than for this branch.</p>
     */
    PermissionKey HOST = new DeclaredByAsset(Host.class.getCanonicalName());
}
