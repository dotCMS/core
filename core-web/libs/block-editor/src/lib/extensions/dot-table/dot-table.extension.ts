import { Table } from '@tiptap/extension-table';

export function DotTableExtension() {
    return Table.extend({
        resizable: true,

        // This is the trick to the empty tables
        // What is happening is that ProseMirror is copying the table instead of moving it
        // Before, we were removing that copy from the DOM manually, but it was messy
        // So here we just override all plugins and prevent ProseMirror of doing funny stuff
        // https://github.com/ueberdosis/tiptap/issues/2250
        addProseMirrorPlugins() {
            return [];
        }
    });
}
