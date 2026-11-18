import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotJspIframeDialogData } from '@dotcms/ui';

/**
 * The Edit Permissions dialog for a folder: the legacy permissions JSP in an iframe dialog.
 *
 * Opened by the shell when the store holds a `FOLDER_PERMISSIONS` dialog, whether the folder
 * context menu asked for it or a `folderPermissions` link did (#37759, FR-030). Kept in one place
 * so both paths open exactly the same dialog.
 *
 * @param identifier The folder whose permissions to edit.
 * @param header The dialog header, already translated.
 * @returns The `DialogService.open` config for `DotJspIframeDialogComponent`.
 */
export function folderPermissionsDialogConfig(
    identifier: string,
    header: string
): DynamicDialogConfig<DotJspIframeDialogData> {
    const params = new URLSearchParams({ folderIdentifier: identifier, popup: 'true' });

    return {
        header,
        width: 'min(92vw, 75rem)',
        contentStyle: { overflow: 'hidden' },
        data: {
            url: `/html/portlet/ext/folders/permissions.jsp?${params.toString()}`,
            titleKey: 'Permissions',
            emptyKey: 'dot.permissions.iframe.dialog.no-asset',
            testIdPrefix: 'permissions'
        },
        modal: true,
        appendTo: 'body',
        closable: true,
        closeOnEscape: true,
        draggable: false,
        resizable: false,
        position: 'center'
    };
}
