import { fromEvent } from 'rxjs';

import { NgIf, NgStyle, NgSwitch, NgSwitchCase } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    ElementRef,
    EventEmitter,
    Output,
    ViewChild,
    inject
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { DialogModule } from 'primeng/dialog';

import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { DotSpinnerModule, SafeUrlPipe } from '@dotcms/ui';

import {
    CreateFromPaletteAction,
    DialogStatus,
    DotEmaDialogStore
} from './store/dot-ema-dialog.store';

import { NG_CUSTOM_EVENTS } from '../../shared/enums';
import { ActionPayload } from '../../shared/models';
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
    providers: [DotEmaDialogStore]
})
export class DotEmaDialogComponent {
    @ViewChild('iframe') iframe: ElementRef<HTMLIFrameElement>;

    @Output() action = new EventEmitter<{ event: CustomEvent; payload: ActionPayload }>();

    private readonly destroyRef$ = inject(DestroyRef);
    private readonly store = inject(DotEmaDialogStore);

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
     * @param {ActionPayload} payload
     * @memberof EditEmaEditorComponent
     */
    editContentlet(payload: Partial<ActionPayload>) {
        this.store.editContentlet({
            inode: payload.contentlet.inode,
            title: payload.contentlet.title
        });
    }

    /**
     * Create contentlet form
     *
     * @param {{ url: string; contentType: string }} { url, contentType }
     * @memberof DotEmaDialogComponent
     */
    createContentlet({ url, contentType }: { url: string; contentType: string }) {
        this.store.createContentlet({
            url,
            contentType
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
