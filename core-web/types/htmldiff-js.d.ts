declare module 'htmldiff-js' {
    /**
     * Minimal declaration for the untyped `htmldiff-js` package. Only the default
     * export's `execute` is used, by DotDiffPipe.
     */
    const HtmlDiff: {
        execute(oldHtml: string, newHtml: string): string;
    };

    export default HtmlDiff;
}
