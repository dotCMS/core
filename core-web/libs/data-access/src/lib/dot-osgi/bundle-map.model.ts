/**
 * OSGi bundle representation matching backend BundleMap.
 * @see dotCMS OSGIResource ResponseEntityBundleListView
 */

/** OSGi bundle states (org.osgi.framework.Bundle constants). */
export const BUNDLE_STATE = {
    UNINSTALLED: 1,
    INSTALLED: 2,
    RESOLVED: 4,
    STARTING: 8,
    STOPPING: 16,
    ACTIVE: 32
} as const;

/** Union of valid OSGi bundle state integers (values of {@link BUNDLE_STATE}). */
export type OsgiBundleState = (typeof BUNDLE_STATE)[keyof typeof BUNDLE_STATE];

export interface BundleMap {
    bundleId: number;
    symbolicName: string;
    location: string;
    jarFile: string;
    state: OsgiBundleState;
    version: string;
    separator: string;
    isSystem: boolean;
    javaVersion?: string;
    javaClassVersion?: number;
    isMultiRelease?: boolean;
    isBuiltWithMaven?: boolean;
    usesDotcmsApis?: boolean;
    dotcmsCoreDependencyVersion?: string;
}

/** Unified row model for the plugins table (installed bundles + undeployed jars). */
export interface PluginRow {
    jarFile: string;
    symbolicName: string;
    state: OsgiBundleState | 'undeployed';
    bundleId?: number;
    version?: string;
}
