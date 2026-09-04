# 04 · Pages

A page is a contentlet of an HTMLPAGE base type, so it follows the action-firing pattern in
[03-content.md](03-content.md) — but don't fire it by hand. Use the page flow the MCP server
provides; its description covers folder creation, required-field validation, the URL-collapse
trap and `cacheTtl`.

**Where it fits:** after the template is published ([05-templates.md](05-templates.md)), before
placement ([09-placement.md](09-placement.md)). A newly created page renders blank until content
is placed on it and the page is re-published.
