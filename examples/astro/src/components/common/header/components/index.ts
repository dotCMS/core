// AISearchDialog is deliberately NOT re-exported here: this barrel is imported for
// SearchButton, and re-exporting the dialog makes it — and the dotCMS AI client it pulls in —
// statically reachable from the header, which puts it in the initial bundle. Header.tsx loads
// it with React.lazy instead.
export { default as SearchButton } from "./SearchButton";
export { default as SearchResult } from "./SearchResult";
export * from "./icons";
