import { DynamicDialogConfig } from 'primeng/dynamicdialog';

/**
 * Shared centered-modal configuration for editor dialogs that mount
 * `DotBrowserSelectorComponent` from `@dotcms/ui` (currently the dotCMS image and
 * video pickers). Locking sizing, mask styling, and the picker's data-payload
 * defaults in one place keeps every browse-an-asset flow consistent and avoids
 * drift when a new mime-type variant is added.
 *
 * Callers provide only the dialog header and the contentlet mime-type allowlist
 * (e.g. `['image']` or `['video']`). Everything else mirrors the file-field's
 * configuration so customers see the same UX whether they're picking an asset
 * for a file field or for a Story Block.
 */
export function buildBrowserSelectorConfig(opts: {
    header: string;
    mimeTypes: string[];
}): DynamicDialogConfig {
    return {
        header: opts.header,
        appendTo: 'body',
        closeOnEscape: true,
        closable: true,
        dismissableMask: true,
        draggable: false,
        keepInViewport: false,
        maskStyleClass: 'p-dialog-mask-dynamic',
        resizable: false,
        modal: true,
        width: '90%',
        style: { 'max-width': '1040px' },
        contentStyle: { overflow: 'auto', 'min-height': '45rem' },
        data: {
            mimeTypes: opts.mimeTypes,
            showLinks: false,
            showDotAssets: true,
            showPages: false,
            showFiles: true,
            showFolders: false,
            showWorking: true,
            showArchived: false,
            sortByDesc: true
        }
    };
}
