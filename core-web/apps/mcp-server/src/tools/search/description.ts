export const searchDescription = `
Universal search function for dotCMS content.

Use this function for ALL search operations.
It performs a Drive Search API query (no Lucene syntax required) and returns raw API results.

The function posts structured parameters directly to:
POST /api/v1/drive/search

----------------------------------------
Required / Common Parameters
----------------------------------------

- assetPath (string)
  Root path to search from.
  Examples:
  "//"                → entire system
  "//SiteName/"       → specific site

- filters.text (string)
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

- baseTypes (string[])
  dotCMS base types to include.
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
`;