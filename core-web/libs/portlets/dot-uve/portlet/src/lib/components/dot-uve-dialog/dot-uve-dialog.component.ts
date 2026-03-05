import { fromEvent } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    ElementRef,
    EventEmitter,
    NgZone,
    Output,
    ViewChild,
    computed,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { MessageService } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';

import { take } from 'rxjs/operators';

import { DotMessageService, DotUiColorsService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSWorkflowActionEvent,
    DotContentCompareEvent
} from '@dotcms/dotcms-models';
import { DotContentCompareComponent } from '@dotcms/portlets/dot-uve/ui';
import { DotCMSPage, DotCMSURLContentMap } from '@dotcms/types';
import { DotSpinnerComponent, SafeUrlPipe } from '@dotcms/ui';

import { DotUveDialogStore } from './store/dot-uve-dialog.store';

import { DotUveWorkflowActionsService } from '../../services/dot-uve-workflow-actions/dot-uve-workflow-actions.service';
import { DialogStatus, NG_CUSTOM_EVENTS } from '../../shared/enums';
import {
    ActionPayload,
    CreateContentletAction,
    CreateFromPaletteAction,
    DialogAction,
    EditContentletPayload,
    VTLFile
} from '../../shared/models';
import { DotUveFormSelectorComponent } from '../dot-uve-form-selector/dot-uve-form-selector.component';

@Component({
    selector: 'dot-uve-dialog',
    templateUrl: './dot-uve-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        SafeUrlPipe,
        DotUveFormSelectorComponent,
        DialogModule,
        DotSpinnerComponent,
        DotContentCompareComponent
    ],
    providers: [DotUveDialogStore, DotUveWorkflowActionsService]
})
export class DotUveDialogComponent {
    @ViewChild('iframe') iframe: ElementRef<HTMLIFrameElement>;

    @Output() action = new EventEmitter<DialogAction>();
    @Output() reloadFromDialog = new EventEmitter<void>();

    $compareData = signal<DotContentCompareEvent | null>(null);

    $compareDataExists = computed(() => !!this.$compareData());

    private readonly destroyRef$ = inject(DestroyRef);
    private readonly store = inject(DotUveDialogStore);
    private readonly workflowActions = inject(DotUveWorkflowActionsService);
    private readonly ngZone = inject(NgZone);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly messageService = inject(MessageService);
    private readonly dotUiColorsService = inject(DotUiColorsService);

    protected readonly dialogState = toSignal(this.store.dialogState$);
    protected readonly dialogStatus = DialogStatus;

    protected get ds() {
        return this.dialogState();
    }

    /**
     *
     * @memberof DotUveDialogComponent
     */
    resetDialog() {
        this.store.resetDialog();
    }

    resetActionPayload() {
        this.store.resetActionPayload();
    }

    /**
     * Add contentlet
     *
     * @param {ActionPayload} actionPayload
     * @memberof DotUveEditorComponent
     */
    addContentlet(actionPayload: ActionPayload): void {
        this.store.addContentlet({
            containerId: actionPayload.container.identifier,
            acceptTypes: actionPayload.container.acceptTypes ?? '*',
            language_id: actionPayload.language_id,
            actionPayload
        });
    }

    /**
     * Add Form
     *
     * @param {ActionPayload} actionPayload
     * @memberof DotUveEditorComponent
     */
    addForm(payload: ActionPayload): void {
        this.store.addFormContentlet(payload);
    }

    /**
     * Add Widget
     *
     * @param {ActionPayload} actionPayload
     * @memberof DotUveEditorComponent
     */
    addWidget(actionPayload: ActionPayload): void {
        this.store.addContentlet({
            containerId: actionPayload.container.identifier,
            acceptTypes: DotCMSBaseTypesContentTypes.WIDGET,
            language_id: actionPayload.language_id,
            actionPayload
        });
    }

    /**
     * Edit contentlet
     *
     * @param {EditContentletPayload} contentlet
     * @memberof DotUveDialogComponent
     */
    editContentlet(payload: EditContentletPayload) {
        this.store.editContentlet(payload);
    }

    /**
     * Edits a VTL contentlet.
     *
     * @param {VTLFile} vtlFile - The VTL file to edit.
     * @memberof DotUveDialogComponent
     */
    editVTLContentlet(vtlFile: VTLFile) {
        this.store.editContentlet({
            inode: vtlFile.inode,
            title: vtlFile.name
        });
    }

    /**
     * Translate page
     *
     * @param {({ page: DotPage; newLanguage: number | string })} { page, newLanguage }
     * @memberof DotUveDialogComponent
     */
    translatePage({ page, newLanguage }: { page: DotCMSPage; newLanguage: number | string }) {
        this.store.translatePage({ page, newLanguage });
    }

    /**
     * Edit URL Content Map Contentlet
     *
     * @param {DotCMSURLContentMap} { inode, title }
     * @memberof DotUveDialogComponent
     */
    editUrlContentMapContentlet({ inode, title }: DotCMSURLContentMap) {
        this.store.editUrlContentMapContentlet({
            inode,
            title
        });
    }

    /**
     * Create contentlet in the edit content
     *
     * @param {CreateContentletAction} { url, contentType, payload }
     * @memberof DotUveDialogComponent
     */
    createContentlet({ url, contentType, actionPayload }: CreateContentletAction) {
        this.store.createContentlet({
            url,
            contentType,
            actionPayload
        });
    }

    /**
     * Create contentlet from palette
     *
     * @param {CreateFromPaletteAction} { variable, name, payload }
     * @memberof DotUveDialogComponent
     */
    createContentletFromPalette({
        variable,
        name,
        actionPayload,
        language_id
    }: CreateFromPaletteAction) {
        this.store.createContentletFromPalette({
            variable,
            name,
            actionPayload,
            language_id
        });
    }

    /**
     * Show loading iframe
     *
     * @param {string} [title]
     * @memberof DotUveDialogComponent
     */
    showLoadingIframe(title?: string) {
        this.store.loadingIframe(title ?? '');
    }

    /**
     * Handle workflow event
     *
     * @param {DotCMSWorkflowActionEvent} event
     * @memberof DotUveDialogComponent
     */
    handleWorkflowEvent(event: DotCMSWorkflowActionEvent) {
        this.workflowActions
            .handleWorkflowAction(event, this.callEmbeddedFunction.bind(this))
            .pipe(take(1))
            .subscribe(({ callback, args, summary, detail }) => {
                // We know is an error when we have summary and detail
                if (summary && detail) {
                    this.messageService.add({
                        life: 2000,
                        severity: 'error',
                        summary,
                        detail
                    });
                } else {
                    this.callEmbeddedFunction(callback, args);
                    this.messageService.add({
                        severity: 'success',
                        summary: this.dotMessageService.get('Workflow-Action'),
                        detail: this.dotMessageService.get('edit.content.fire.action.success'),
                        life: 2000
                    });
                }
            });
    }

    /**
     * Reload iframe
     *
     * @memberof DotUveDialogComponent
     */
    reloadIframe() {
        this.iframe.nativeElement.contentWindow.location.reload();
    }

    /**
     * Call embedded function
     *
     * @private
     * @param {string} callback
     * @param {unknown[]} args
     * @memberof DotUveDialogComponent
     */
    private callEmbeddedFunction(
        callback: string,
        args: unknown[] = [],
        whenFinished?: () => void
    ) {
        this.ngZone.run(() => {
            this.iframe.nativeElement.contentWindow?.[callback]?.(...args);
            whenFinished?.();
        });
    }

    /**
     * Open dialog on URL
     *
     * @param {string} url
     * @param {string} title
     * @memberof DotUveDialogComponent
     */
    openDialogOnUrl(url: string, title: string) {
        this.store.openDialogOnURL({ url, title });
    }

    protected onHide() {
        const event = new CustomEvent('ng-event', {
            detail: {
                name: NG_CUSTOM_EVENTS.DIALOG_CLOSED
            }
        });

        this.emitAction(event);
        this.resetDialog();
    }

    /**
     * Iframe load event
     *
     * @protected
     * @memberof DotUveDialogComponent
     */
    protected onIframeLoad() {
        this.store.setStatus(this.dialogStatus.INIT);

        // Inject CSS variables into iframe
        const iframeDoc = this.getIframeDocument();
        if (iframeDoc) {
            this.dotUiColorsService.setColors(iframeDoc.querySelector('html'));
        }

        // This event is destroyed when you close the dialog
        fromEvent(
            // The events are getting sended to the document
            iframeDoc,
            'ng-event'
        )
            .pipe(takeUntilDestroyed(this.destroyRef$))
            .subscribe((event: CustomEvent) => {
                this.emitAction(event);

                switch (event.detail.name) {
                    case NG_CUSTOM_EVENTS.DIALOG_CLOSED: {
                        this.store.resetDialog();

                        break;
                    }

                    case NG_CUSTOM_EVENTS.COMPARE_CONTENTLET: {
                        this.ngZone.run(() => {
                            this.$compareData.set(<DotContentCompareEvent>event.detail.data);
                        });
                        break;
                    }

                    case NG_CUSTOM_EVENTS.EDIT_CONTENTLET_UPDATED: {
                        // The edit content emits this for savings when translating a page and does not emit anything when changing the content
                        if (this.dialogState().form.isTranslation) {
                            this.store.setSaved();

                            if (event.detail.payload.isMoveAction) {
                                this.reloadIframe();
                            }
                        } else {
                            this.store.setDirty();
                        }

                        break;
                    }

                    case NG_CUSTOM_EVENTS.OPEN_WIZARD: {
                        this.handleWorkflowEvent(event.detail.data);
                        break;
                    }

                    case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                        this.store.setSaved();

                        if (event.detail.payload.isMoveAction) {
                            this.reloadIframe();
                        }

                        break;
                    }
                }
            });
    }

    /**
     * Form selected event
     *
     * @protected
     * @param {string} identifier
     * @memberof DotUveDialogComponent
     */
    protected onFormSelected(identifier: string) {
        const customEvent = new CustomEvent('ng-event', {
            detail: {
                name: NG_CUSTOM_EVENTS.FORM_SELECTED,
                data: {
                    identifier
                }
            }
        });

        this.emitAction(customEvent);
    }

    /**
     * Brings back the dialog to the previous state and triggers a reload event.
     * @param options - The options for bringing back the dialog.
     * @param options.name - The name of the dialog.
     * @param options.args - The arguments for the dialog.
     */
    bringBack({ name, args }: { name: string; args: string[] } = { name: '', args: [] }) {
        this.$compareData.set(null);
        // TODO: The 'emit' function requires a mandatory void argument
        this.callEmbeddedFunction(name, args, () => this.reloadFromDialog.emit());
    }

    /**
     * Close compare dialog
     *
     * @memberof DotUveDialogComponent
     */
    closeCompareDialog() {
        this.$compareData.set(null);
    }

    private emitAction(event: CustomEvent) {
        const { actionPayload, form, clientAction } = this.dialogState();

        this.action.emit({ event, actionPayload, form, clientAction });
    }

    /**
     * Get iframe document
     *
     * @protected
     * @returns {Document | null}
     * @memberof DotUveDialogComponent
     */
    protected getIframeDocument(): Document | null {
        return this.iframe?.nativeElement?.contentWindow?.document || null;
    }
}
