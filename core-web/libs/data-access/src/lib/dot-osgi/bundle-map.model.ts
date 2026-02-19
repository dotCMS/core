/**
 * OSGi bundle representation matching backend BundleMap.
 * @see dotCMS OSGIResource ResponseEntityBundleListView
 */
export interface BundleMap {
    bundleId: number;
    symbolicName: string;
    location: string;
    jarFile: string;
    state: number;
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

/** OSGi bundle states (org.osgi.framework.Bundle constants) */
export const BUNDLE_STATE = {
    UNINSTALLED: 1,
    INSTALLED: 2,
    RESOLVED: 4,
    STARTING: 8,
    STOPPING: 16,
    ACTIVE: 32
} as const;
