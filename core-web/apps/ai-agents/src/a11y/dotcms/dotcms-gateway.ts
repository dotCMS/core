import type { RenderSources, SavedAsset, ScanResult } from './dotcms-client';

/**
 * The capabilities the agent needs from dotCMS — the seam between the fix loop and
 * the concrete {@link DotcmsClient}. Orchestration (run-fix, research tools) depends
 * on THIS interface, not the implementation, so it can be faked in tests and swapped
 * without touching the loop. `DotcmsClient` is the production implementation; specs
 * provide a stub.
 *
 * Every method goes through the path-allowlisted sandbox in the real client — there
 * is intentionally no publish/delete capability here (the agent only writes the
 * WORKING version; a human publishes from the UI).
 */
export interface DotcmsGateway {
    /** Scan / re-scan a page URL through the page-scanner proxy → normalized axe. */
    scan(url: string): Promise<ScanResult>;
    /** List the source files (theme VTL/CSS + container content-type VTLs) for a page. */
    locate(uri: string, hostId: string): Promise<RenderSources>;
    /** Read the raw text of one asset by its host-qualified path. */
    read(path: string): Promise<string>;
    /** Fetch the compiled stylesheet (with inline sourcemap) the page loads. */
    fetchStylesheet(absoluteUrl: string): Promise<string>;
    /** Save an edited file to its WORKING version (never published). */
    saveWorking(path: string, content: string, mime?: string): Promise<SavedAsset>;
}
