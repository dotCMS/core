/**
 * `@dotcms/ai/tools` — ready-made dotCMS tools for a host's own MCP server or agent.
 *
 * Two public layers, the same split as the runtime's two verbs:
 *  - `definitions/` — the tool factories (`searchTool()`, `pageCreateTool()`, …), the
 *    model-facing layer. Each returns a plain object shaped for the Vercel AI SDK and the MCP
 *    TypeScript SDK alike, whose `execute` validates the model's input and never throws.
 *  - `operations/` — `createPage`, `placeContent`, `verifyPage`, `lookupContent`,
 *    `upsertContent`, `createContentType`, `updateContentType`, `uploadAssets`,
 *    `downloadAssets`, the direct layer. Typed options in, typed manifest out, typed errors
 *    thrown. Use these when YOU write the call; the tools are formatting on top.
 *
 * Both are built on `toolkit/`, which is internal. See ./README.md for what goes where.
 *
 * This is the top layer of the package: it builds on `@dotcms/ai/runtime`, and nothing below
 * it may import it (lint-enforced).
 */

// ---- The connection every tool is given ------------------------------------------------

export { dotcmsConnection } from './toolkit/connection';
export type { DotCMSConnection, DotCMSConnectionConfig, Resolvable } from './toolkit/connection';

// ---- Model-facing tools --------------------------------------------------------------

export { executeTool } from './definitions/execute';
export { searchTool } from './definitions/search';
export { pageCreateTool } from './definitions/page-create';
export { pagePlaceContentTool } from './definitions/page-place-content';
export { pageVerifyTool } from './definitions/page-verify';
export { lookupTool } from './definitions/lookup';
export { upsertContentTool } from './definitions/upsert-content';
export { createContentTypeTool } from './definitions/content-type-create';
export { updateContentTypeTool } from './definitions/content-type-update';
export { uploadAssetsTool } from './definitions/upload-assets';
export { downloadAssetsTool } from './definitions/download-assets';

export type {
    AnyDotCMSTool,
    AssetToolOptions,
    DotCMSTool,
    DotCMSToolAnnotations,
    ExecuteToolOptions,
    RequestToolOptions,
    UploadToolOptions
} from './toolkit/types';

// What every tool resolves to on failure, and the guard to tell it from a normal result.
export { isToolFailure } from './toolkit/tool-runtime';
export type { ToolFailure, ToolFailureCode } from './toolkit/tool-runtime';

// The code tools' result shape, and what `tool.toModelOutput()` returns. Rendering a result
// for a text-only transport (MCP) is the tool's own `tool.toText(result)`.
export type { CodeToolResult, JSONValue, ToolModelOutput } from './toolkit/results';

// ---- Direct operations -----------------------------------------------------------------

export { createPage } from './operations/page-create';
export type { CreatePageManifest, CreatePageOptions } from './operations/page-create';

export { placeContent } from './operations/page-place-content';
export type {
    PagePlaceContentManifest,
    PagePlaceContentOptions,
    PlaceMode,
    PlaceOp,
    SlotAddress,
    SlotAssignment,
    SlotResult
} from './operations/page-place-content';

export { verifyPage } from './operations/page-verify';
export type {
    SlotVerdict,
    UrlMapResult,
    VerifyHtmlResult,
    VerifyMode,
    VerifyPageManifest,
    VerifyPageOptions,
    VerifySlotResult
} from './operations/page-verify';

export { lookupContent } from './operations/lookup';
export type {
    LookupManifest,
    LookupOptions,
    LookupResult,
    LookupVersion
} from './operations/lookup';

export { upsertContent } from './operations/upsert-content';
export type {
    RelationshipIssue,
    RelationshipIssueCode,
    UpsertContentManifest,
    UpsertContentOptions
} from './operations/upsert-content';

export { createContentType } from './operations/content-type-create';
export type {
    CreateContentTypeManifest,
    CreateContentTypeOptions,
    CreatedField
} from './operations/content-type-create';

export { updateContentType } from './operations/content-type-update';
export type {
    FieldUpdate,
    UpdateContentTypeManifest,
    UpdateContentTypeOptions
} from './operations/content-type-update';

export type { FieldInput, FieldType } from './operations/shared/content-type-fields';

export { uploadAssets } from './operations/upload-assets';
export type { UploadAssetsManifest, UploadAssetsOptions } from './operations/upload-assets';

export { downloadAssets } from './operations/download-assets';
export type {
    DownloadAssetsManifest,
    DownloadAssetsOptions,
    DownloadKind,
    OverwriteMode
} from './operations/download-assets';

export type {
    AssetManifestFailure,
    AssetManifestFile,
    AssetManifestSkipped
} from './operations/shared/asset-common';
