/**
 * `@dotcms/ai/tools` — ready-made dotCMS tools for a host's own MCP server or agent.
 *
 * Two layers, the same split as the runtime's two verbs:
 *  - the tool factories (`searchTool()`, `pageCreateTool()`, …) — the model-facing layer. Each
 *    returns a plain object shaped for the Vercel AI SDK and the MCP TypeScript SDK alike,
 *    whose `execute` validates the model's input and never throws.
 *  - the operations (`createPage`, `placeContent`, `verifyPage`, `uploadAssets`,
 *    `downloadAssets`) — the direct layer. Typed options in, typed manifest out, typed
 *    errors thrown. Use these when YOU write the call; the tools are formatting on top.
 *
 * This is the top layer of the package: it builds on `@dotcms/ai/runtime`, and nothing below
 * it may import it (lint-enforced).
 */

// ---- Model-facing tools --------------------------------------------------------------

export { executeTool } from './definitions/execute';
export { searchTool } from './definitions/search';
export { pageCreateTool } from './definitions/page-create';
export { pagePlaceContentTool } from './definitions/page-place-content';
export { pageVerifyTool } from './definitions/page-verify';
export { uploadAssetsTool } from './definitions/upload-assets';
export { downloadAssetsTool } from './definitions/download-assets';

export type {
    DotCMSTool,
    DotCMSToolAnnotations,
    DotCMSToolOptions,
    ExecuteToolOptions,
    RequestToolOptions
} from './toolkit';

// What every tool resolves to on failure, and the guard to tell it from a normal result.
export { isToolFailure } from './tool-runtime';
export type { ToolFailure } from './tool-runtime';

// ---- Direct operations -----------------------------------------------------------------

export { createPage } from './page-create';
export type { CreatePageManifest, CreatePageOptions } from './page-create';

export { placeContent } from './page-place-content';
export type {
    PagePlaceContentManifest,
    PagePlaceContentOptions,
    PlaceMode,
    PlaceOp,
    SlotAddress,
    SlotAssignment,
    SlotResult
} from './page-place-content';

export { verifyPage } from './page-verify';
export type {
    SlotVerdict,
    UrlMapResult,
    VerifyHtmlResult,
    VerifyMode,
    VerifyPageManifest,
    VerifyPageOptions,
    VerifySlotResult
} from './page-verify';

export { downloadAssets, uploadAssets } from './assets-transfer';
export type {
    AssetManifestFailure,
    AssetManifestFile,
    AssetManifestSkipped,
    DownloadAssetsManifest,
    DownloadAssetsOptions,
    OverwriteMode,
    UploadAssetsManifest,
    UploadAssetsOptions
} from './assets-transfer';
