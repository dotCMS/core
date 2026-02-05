export const searchDescription = `
Universal search function for dotCMS content.

Use this function for ALL search operations.
It performs a Drive Search API query (no Lucene syntax required) and returns raw API results.

The function posts structured parameters directly to:
POST /api/v1/drive/search

If context_initialization has not been called, call context_initialization first and then resume with this function.

IMPORTANT:
- Do NOT filter, collapse, summarize, or omit results unless explicitly instructed.
- If the user asks for "everything", ALL returned items must be listed.
- Internal content, file assets, widgets, dotAssets, and non-URL-mapped items MUST be included.

----------------------------------------
Required / Common Parameters
----------------------------------------

- assetPath (string, required)
  Root path to search from.
  Examples:
  "//SiteName/"       → specific site

  When searching items inside a folder, use the folder path as the assetPath.
  Examples:
  "//SiteName/folder/subfolder/"       → specific folder

- filters.text (string, case insensitive)
  Free-text query string. May be empty for broad listings. 

----------------------------------------
Optional Parameters
----------------------------------------

- includeSystemHost (boolean)
  Whether to include the system host in results.

- filters.filterFolders (boolean)
  If true, excludes folders from results.

- showFolders (boolean)
  If true, explicitly includes folders in results.

- language (string[])
  Language IDs as strings, e.g. ["1", "4600065"].
  Defaults to the system default language if known, otherwise "1" (English).
  Examples: ["1", "4600065"]

- contentTypes (string[])
  Optional list of content type variable names to restrict results.
  Examples: ["article", "blog", "product"]

- baseTypes (string[], optional)
  dotCMS base types to include. Refer to the section 'dotCMS Base Types Reference' for available values. If no baseTypes are queried, leave empty.
  Examples: ["HTMLPAGE", "FILEASSET", "CONTENT"]

- archived (boolean)
  Whether to include archived content.

----------------------------------------
Pagination & Sorting
----------------------------------------

- offset (number)
  Starting index for results (default: 0).

- maxResults (number)
  Maximum number of results to return (default: 20).

- sortBy (string)
  Sort expression, e.g. "modDate:desc".

----------------------------------------
dotCMS Base Types Reference
----------------------------------------

0: HTMLPAGE
   Page-based content types.

1: FILEASSET
   Uploaded files and media assets.

2: CONTENT
   Standard structured content types.

3: WIDGET
   Reusable widget instances sharing core code.

4: KEY_VALUE
   Key/value pair content types.

5: VANITY_URL
   Vanity URL content types with extensible fields.

6: DOTASSET
   Internal dotCMS assets without standard URL paths.

7: PERSONA
   Persona content types used for personalization.

----------------------------------------
Return Value
----------------------------------------

Returns the raw Drive Search API response.
No post-processing or filtering is applied by this tool.

DEFAULT OUTPUT EXPECTATION:
- When returning search results, list ALL items.
- Prefer human-readable fields in this order when available:
  1. Title / Name
  2. Content Type / Base Type
  3. Short descriptor (e.g. Blog, Asset, Widget)
- URLs are optional and should NOT be assumed necessary.

CONSISTENCY RULE:
- If the API returns N results, the response MUST account for all N.
- If fewer than N items are displayed, the agent must explain why BEFORE responding.

MIXED RESULT HANDLING:
- If results include multiple baseTypes (HTMLPAGE, FILEASSET, CONTENT, etc.),
  group them by baseType unless the user asks otherwise.
- Assets are valid search results and must not be discarded.

CORRECTION BEHAVIOR:
- If the user corrects a misunderstanding ("I never asked for URLs"),
  discard previous assumptions and re-answer from scratch using the tool output.

MENTAL MODEL:
The MCP server is the source of truth.
The agent's role is to faithfully expose its results, not reinterpret them.
`;
