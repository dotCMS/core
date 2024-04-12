import { fromEvent } from 'rxjs';

import { NgIf, NgStyle, NgSwitch, NgSwitchCase } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    ElementRef,
    EventEmitter,
    NgZone,
    Output,
    ViewChild,
    inject
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { MessageService } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSWorkflowActionEvent
} from '@dotcms/dotcms-models';
import { DotSpinnerModule, SafeUrlPipe } from '@dotcms/ui';

import {
    CreateContentletAction,
    CreateFromPaletteAction,
    DialogStatus,
    DotEmaDialogStore
} from './store/dot-ema-dialog.store';

import { DotEmaWorkflowActionsService } from '../../services/dot-ema-workflow-actions/dot-ema-workflow-actions.service';
import { NG_CUSTOM_EVENTS } from '../../shared/enums';
import { ActionPayload, VTLFile } from '../../shared/models';
import { EmaFormSelectorComponent } from '../ema-form-selector/ema-form-selector.component';

@Component({
    selector: 'dot-edit-ema-dialog',
    standalone: true,
    templateUrl: './dot-ema-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        NgIf,
        NgSwitch,
        NgSwitchCase,
        NgStyle,
        SafeUrlPipe,
        EmaFormSelectorComponent,
        DialogModule,
        DotSpinnerModule
    ],
    providers: [DotEmaDialogStore, DotEmaWorkflowActionsService]
})
export class DotEmaDialogComponent {
    @ViewChild('iframe') iframe: ElementRef<HTMLIFrameElement>;

    @Output() action = new EventEmitter<{ event: CustomEvent; payload: ActionPayload }>();

    private readonly destroyRef$ = inject(DestroyRef);
    private readonly store = inject(DotEmaDialogStore);
    private readonly workflowActions = inject(DotEmaWorkflowActionsService);
    private readonly ngZone = inject(NgZone);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly messageService = inject(MessageService);

    protected readonly dialogState = toSignal(this.store.dialogState$);
    protected readonly dialogStatus = DialogStatus;

    protected get ds() {
        return this.dialogState();
    }

    /**
     *
     * @memberof DotEmaDialogComponent
     */
    resetDialog() {
        this.store.resetDialog();
    }

    /**
     * Add contentlet
     *
     * @param {ActionPayload} payload
     * @memberof EditEmaEditorComponent
     */
    addContentlet(payload: ActionPayload): void {
        this.store.addContentlet({
            containerId: payload.container.identifier,
            acceptTypes: payload.container.acceptTypes ?? '*',
            language_id: payload.language_id,
            payload
        });
    }

    /**
     * Add Form
     *
     * @param {ActionPayload} _payload
     * @memberof EditEmaEditorComponent
     */
    addForm(payload: ActionPayload): void {
        this.store.addFormContentlet(payload);
    }

    /**
     * Add Widget
     *
     * @param {ActionPayload} payload
     * @memberof EditEmaEditorComponent
     */
    addWidget(payload: ActionPayload): void {
        this.store.addContentlet({
            containerId: payload.container.identifier,
            acceptTypes: DotCMSBaseTypesContentTypes.WIDGET,
            language_id: payload.language_id,
            payload
        });
    }

    /**
     * Edit contentlet
     *
     * @param {Partial<DotCMSContentlet>} contentlet
     * @memberof DotEmaDialogComponent
     */
    editContentlet(contentlet: Partial<DotCMSContentlet>) {
        this.store.editContentlet({
            inode: contentlet.inode,
            title: contentlet.title
        });
    }

    /**
     * Edits a VTL contentlet.
     *
     * @param {VTLFile} vtlFile - The VTL file to edit.
     * @memberof DotEmaDialogComponent
     */
    editVTLContentlet(vtlFile: VTLFile) {
        this.store.editContentlet({
            inode: vtlFile.inode,
            title: vtlFile.name
        });
    }

    /**
     * Edit URL Content Map Contentlet
     *
     * @param {DotCMSContentlet} { inode, title }
     * @memberof DotEmaDialogComponent
     */
    editUrlContentMapContentlet({ inode, title }: DotCMSContentlet) {
        this.store.editUrlContentMapContentlet({
            inode,
            title
        });
    }

    /**
     * Create contentlet in the edit content
     *
     * @param {CreateContentletAction} { url, contentType, payload }
     * @memberof DotEmaDialogComponent
     */
    createContentlet({ url, contentType, payload }: CreateContentletAction) {
        this.store.createContentlet({
            url,
            contentType,
            payload
        });
    }

    /**
     * Create contentlet from palette
     *
     * @param {CreateFromPaletteAction} { variable, name, payload }
     * @memberof DotEmaDialogComponent
     */
    createContentletFromPalette({ variable, name, payload }: CreateFromPaletteAction) {
        this.store.createContentletFromPalette({
            variable,
            name,
            payload
        });
    }

    /**
     * Show loading iframe
     *
     * @param {string} [title]
     * @memberof DotEmaDialogComponent
     */
    showLoadingIframe(title?: string) {
        this.store.loadingIframe(title ?? '');
    }

    /**
     * Handle workflow event
     *
     * @param {DotCMSWorkflowActionEvent} event
     * @memberof DotEmaDialogComponent
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
     * @memberof DotEmaDialogComponent
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
     * @memberof DotEmaDialogComponent
     */
    private callEmbeddedFunction(callback: string, args: unknown[] = []) {
        this.ngZone.run(() => {
            this.iframe.nativeElement.contentWindow?.[callback]?.(...args);
        });
    }

    /**
     * Iframe load event
     *
     * @protected
     * @memberof DotEmaDialogComponent
     */
    protected onIframeLoad() {
        this.store.setStatus(this.dialogStatus.INIT);
        // This event is destroyed when you close the dialog

        fromEvent(
            // The events are getting sended to the document
            this.iframe.nativeElement.contentWindow.document,
            'ng-event'
        )
            .pipe(takeUntilDestroyed(this.destroyRef$))
            .subscribe((event: CustomEvent) => {
                this.action.emit({ event, payload: this.dialogState().payload });

                if (event.detail.name === NG_CUSTOM_EVENTS.OPEN_WIZARD) {
                    this.handleWorkflowEvent(event.detail.data);
                } else if (
                    event.detail.name === NG_CUSTOM_EVENTS.SAVE_PAGE &&
                    event.detail.payload.isMoveAction
                ) {
                    this.reloadIframe();
                }
            });
    }

    /**
     * Form selected event
     *
     * @protected
     * @param {string} identifier
     * @memberof DotEmaDialogComponent
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

        this.action.emit({ event: customEvent, payload: this.dialogState().payload });
    }
}
