declare module 'diff' {
    interface Hunk {
        oldStart: number;
        oldLines: number;
        newStart: number;
        newLines: number;
        lines: string[];
    }

    interface ParsedDiff {
        oldFileName?: string;
        newFileName?: string;
        oldHeader?: string;
        newHeader?: string;
        hunks: Hunk[];
    }

    interface StructuredPatchOptions {
        context?: number;
    }

    function structuredPatch(
        oldFileName: string,
        newFileName: string,
        oldStr: string,
        newStr: string,
        oldHeader?: string,
        newHeader?: string,
        options?: StructuredPatchOptions
    ): ParsedDiff;

    function createPatch(
        fileName: string,
        oldStr: string,
        newStr: string,
        oldHeader?: string,
        newHeader?: string,
        options?: StructuredPatchOptions
    ): string;
}
