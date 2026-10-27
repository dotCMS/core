import { DynamicDialogConfig } from 'primeng/dynamicdialog';

/**
 * How `DotEditContentDialogComponent` opens in a centered dialog. Each caller spreads it and adds
 * the dialog's `data` and `header`.
 *
 * Kept apart from the dialog component on purpose: the relationship field imports that component
 * with a dynamic `import()` to break the cycle dialog → layout → form → fields → relationship
 * field, so this file must not import it, or anything that leads back to the fields.
 */
export const EDIT_CONTENT_DIALOG_CONFIG: DynamicDialogConfig = {
    appendTo: 'body',
    baseZIndex: 10000,
    closable: true,
    closeOnEscape: true,
    draggable: false,
    keepInViewport: true,
    modal: true,
    resizable: true,
    position: 'center',
    width: '95%',
    height: '95%',
    maskStyleClass: 'p-dialog-mask-dynamic p-dialog-create-content',
    style: { 'max-width': '1400px', 'max-height': '900px' }
};
