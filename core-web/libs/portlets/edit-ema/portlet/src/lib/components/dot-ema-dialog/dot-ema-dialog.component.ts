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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MessageService } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSWorkflowActionEvent, DotContentCompareEvent } from '@dotcms/dotcms-models';
import { DotContentCompareModule } from '@dotcms/portlets/dot-ema/ui';
import { DotSpinnerModule, SafeUrlPipe } from '@dotcms/ui';

import { DotEditorDialogService } from './services/dot-editor-dialog.service';

import { DotEmaWorkflowActionsService } from '../../services/dot-ema-workflow-actions/dot-ema-workflow-actions.service';
import { DialogStatus, NG_CUSTOM_EVENTS } from '../../shared/enums';
import { DialogAction } from '../../shared/models';
import { EmaFormSelectorComponent } from '../ema-form-selector/ema-form-selector.component';

@Component({
    selector: 'dot-edit-ema-dialog',
    standalone: true,
    templateUrl: './dot-ema-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        SafeUrlPipe,
        EmaFormSelectorComponent,
        DialogModule,
        DotSpinnerModule,
        DotContentCompareModule
    ],
    providers: [DotEmaWorkflowActionsService]
})
export class DotEmaDialogComponent {
    @ViewChild('iframe') iframe: ElementRef<HTMLIFrameElement>;

    @Output() action = new EventEmitter<DialogAction>();
    @Output() reloadFromDialog = new EventEmitter<void>();

    $compareData = signal<DotContentCompareEvent | null>(null);
    $compareDataExists = computed(() => !!this.$compareData());

    private readonly ngZone = inject(NgZone);
    private readonly destroyRef$ = inject(DestroyRef);
    private readonly dialogService = inject(DotEditorDialogService);
    private readonly workflowActions = inject(DotEmaWorkflowActionsService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly messageService = inject(MessageService);

    protected readonly ds = this.dialogService.state;
    protected readonly dialogStatus = DialogStatus;

    protected get iframeWindow() {
        return this.iframe.nativeElement.contentWindow;
    }

    /**
     * Handle workflow event
     *
     * @param {DotCMSWorkflowActionEvent} event
     * @memberof DotEmaDialogComponent
     */
    private handleWorkflowEvent(event: DotCMSWorkflowActionEvent) {
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
    private reloadIframe() {
        this.iframeWindow.location.reload();
    }

    /**
     * Call embedded function
     *
     * @private
     * @param {string} callback
     * @param {unknown[]} args
     * @memberof DotEmaDialogComponent
     */
    private callEmbeddedFunction(
        callback: string,
        args: unknown[] = [],
        whenFinished?: () => void
    ) {
        this.ngZone.run(() => {
            this.iframeWindow?.[callback]?.(...args);
            whenFinished?.();
        });
    }

    protected onHide() {
        const event = new CustomEvent('ng-event', {
            detail: {
                name: NG_CUSTOM_EVENTS.DIALOG_CLOSED
            }
        });

        this.emitAction(event);
        this.dialogService.resetDialog();
    }

    /**
     * Iframe load event
     *
     * @protected
     * @memberof DotEmaDialogComponent
     */
    protected onIframeLoad() {
        this.dialogService.setStatus(this.dialogStatus.INIT);
        // This event is destroyed when you close the dialog

        fromEvent(this.iframeWindow.document, 'ng-event')
            .pipe(takeUntilDestroyed(this.destroyRef$))
            .subscribe((event: CustomEvent) => {
                this.emitAction(event);
                switch (event.detail.name) {
                    case NG_CUSTOM_EVENTS.DIALOG_CLOSED: {
                        this.dialogService.resetDialog();

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
                        if (!this.ds().form.isTranslation) {
                            this.dialogService.setDirty();
                        } else {
                            this.dialogService.setSaved();

                            if (event.detail.payload.isMoveAction) {
                                this.reloadIframe();
                            }
                        }

                        break;
                    }

                    case NG_CUSTOM_EVENTS.OPEN_WIZARD: {
                        this.handleWorkflowEvent(event.detail.data);
                        break;
                    }

                    case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                        this.dialogService.setSaved();

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
        this.callEmbeddedFunction(name, args, () => this.reloadFromDialog.emit());
    }

    /**
     * Close compare dialog
     *
     * @memberof DotEmaDialogComponent
     */
    closeCompareDialog() {
        this.$compareData.set(null);
    }

    private emitAction(event: CustomEvent) {
        const dialogState = this.ds();
        const { actionPayload, form, clientAction } = dialogState;

        const dialogAction: DialogAction = { event, actionPayload, form, clientAction };
        this.action.emit(dialogAction);
    }
}
