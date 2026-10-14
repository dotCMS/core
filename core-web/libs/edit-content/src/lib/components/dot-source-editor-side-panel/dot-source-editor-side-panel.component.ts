import { MonacoEditorLoaderService, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { EMPTY } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, ConfirmEventType } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { catchError, filter, switchMap, take, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    DOT_MONACO_BASE_OPTIONS,
    DotKeyboardShortcutService,
    DotMessagePipe,
    DotSidePanelContent,
    DotSidePanelRef,
    ensureDotVelocityLanguageRegistered
} from '@dotcms/ui';

import { DotSourceEditorService, DotSourceEditorUploadError } from './dot-source-editor.service';

import { DotSourceEditorRequest } from '../../models/dot-source-editor.model';

/** The minimal Monaco editor surface this panel uses. */
interface DotSourceEditorInstance {
    focus(): void;
}

/**
 * Edits a file's source in Monaco, as a side panel's content over the screen that opened it
 * (Content Drive, the page editor). A developer tool: it opens straight on the code, and saves and
 * closes with the button or `Cmd/Ctrl + S`.
 *
 * It opens in the same root side panel as the other editors, with the Edit Content panel's
 * unsaved-changes prompt. Escape typed inside the editor stays with Monaco, which marks every
 * Escape handled; as in any IDE, Escape there never closes the file.
 *
 * It loads and saves the source itself, so a failed save leaves the edits on screen. It reports a
 * save through its output and closes through its `DotSidePanelRef`, and the opener decides what they
 * mean for its own view.
 */
@Component({
    selector: 'dot-source-editor-side-panel',
    imports: [
        ButtonModule,
        ConfirmDialogModule,
        FormsModule,
        MessageModule,
        MonacoEditorModule,
        ProgressSpinnerModule,
        DotMessagePipe
    ],
    // Its own prompt instance, so the prompt renders in this panel's `<p-confirmDialog>`.
    providers: [ConfirmationService, DotSourceEditorService],
    templateUrl: './dot-source-editor-side-panel.component.html',
    // Fills the drawer: the editor takes the height the buttons below it leave.
    host: { class: 'flex min-h-0 flex-1 flex-col gap-4' }
})
export class DotSourceEditorSidePanelComponent implements DotSidePanelContent {
    readonly #panel = inject(DotSidePanelRef);
    readonly #confirmation = inject(ConfirmationService);
    readonly #messages = inject(DotMessageService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #sourceEditor = inject(DotSourceEditorService);
    readonly #destroyRef = inject(DestroyRef);

    /** The file to open. It loads once this is set. */
    readonly request = input<DotSourceEditorRequest | null>(null);

    /** A save landed: the opener refreshes its view. The panel closes straight after. */
    readonly saved = output<void>();

    protected readonly $status = signal<ComponentStatus>(ComponentStatus.LOADING);

    /**
     * Why the last save failed, shown above the buttons when the shared HTTP handler cannot explain
     * it: the server's reason, or an empty string when it gave none. `null` with nothing to report.
     */
    protected readonly $saveError = signal<string | null>(null);

    /** The source as it stands in the editor. */
    protected readonly $source = signal('');

    /** The source as it was last loaded or saved; the editor differs from it when dirty. */
    readonly #savedSource = signal('');

    /** The file's name, kept on save so the asset is not renamed. Known once the file loads. */
    #fileName = '';

    /** Whether the editor holds changes that have not been saved. */
    protected readonly $dirty = computed(() => this.$source() !== this.#savedSource());

    /** Whether Save would do anything: a loaded file, with changes, and no save already running. */
    protected readonly $canSave = computed(
        () => this.$status() === ComponentStatus.LOADED && this.$dirty()
    );

    protected readonly $editorOptions = computed(() => ({
        ...DOT_MONACO_BASE_OPTIONS,
        language: this.request()?.language ?? 'plaintext',
        // Read-only while saving, so nothing typed during the request is silently left unsaved.
        readOnly: this.$status() === ComponentStatus.SAVING
    }));

    protected readonly ComponentStatus = ComponentStatus;

    constructor() {
        // The drawer's own padding, so the bordered editor lines up with the title, in a column the
        // editor and the buttons below it share.
        this.#panel.contentStyleClass.set('flex flex-col');

        // Claimed for the panel's lifetime, through the same registry as Escape, so the browser's
        // "Save page" never opens over the editor, even when there is nothing to save.
        const withdrawSave = inject(DotKeyboardShortcutService).register({
            combination: 'mod+s',
            label: 'edit.content.source-editor.shortcut.save',
            handler: () => this.save()
        });
        this.#destroyRef.onDestroy(withdrawSave);

        // Velocity has to be registered before the editor creates its model, or the file opens as
        // plain text. Subscribed here, before any editor exists, so it runs first once Monaco loads.
        inject(MonacoEditorLoaderService)
            .isMonacoLoaded$.pipe(filter(Boolean), take(1), takeUntilDestroyed())
            .subscribe(() => ensureDotVelocityLanguageRegistered());

        toObservable(this.request)
            .pipe(
                filter((request): request is DotSourceEditorRequest => !!request),
                tap(() => this.$status.set(ComponentStatus.LOADING)),
                switchMap(({ inode }) =>
                    this.#sourceEditor.load(inode).pipe(
                        catchError((error: unknown) => {
                            this.#reportServerError(error);
                            this.$status.set(ComponentStatus.ERROR);

                            return EMPTY;
                        })
                    )
                ),
                takeUntilDestroyed()
            )
            .subscribe(({ fileName, source }) => {
                this.#fileName = fileName;
                this.#savedSource.set(source);
                this.$source.set(source);
                this.$status.set(ComponentStatus.LOADED);
            });
    }

    /**
     * Saves the source as the file's working version, then closes the panel. A no-op without changes or
     * while a save runs.
     *
     * On failure the panel stays open with the edits. A server error goes through the shared
     * handler; any other failure, such as a file the server refused to stage, shows above the
     * buttons.
     */
    save(): void {
        const request = this.request();

        if (!request || !this.$canSave()) {
            return;
        }

        const source = this.$source();

        this.$saveError.set(null);
        this.$status.set(ComponentStatus.SAVING);

        this.#sourceEditor
            .save({
                inode: request.inode,
                fileName: this.#fileName,
                source
            })
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: () => {
                    this.#savedSource.set(source);
                    this.$status.set(ComponentStatus.LOADED);
                    this.saved.emit();
                    // Nothing is left unsaved, so no prompt: straight out.
                    this.#panel.close();
                },
                error: (error: unknown) => {
                    this.$status.set(ComponentStatus.LOADED);
                    this.#reportServerError(error);

                    if (!(error instanceof HttpErrorResponse)) {
                        this.$saveError.set(
                            error instanceof DotSourceEditorUploadError ? (error.reason ?? '') : ''
                        );
                    }
                }
            });
    }

    /**
     * Close intent: the X button, Cancel, Escape, or a click on the mask.
     *
     * Ignored while a save runs, since leaving would abandon a request whose outcome the author
     * then never sees. With unsaved changes it asks first, with the Edit Content panel's wording.
     */
    requestClose(): void {
        if (this.$status() === ComponentStatus.SAVING) {
            return;
        }

        if (!this.$dirty()) {
            this.#panel.close();

            return;
        }

        this.#confirmation.confirm({
            header: this.#messages.get('edit.content.unsaved.changes.title'),
            message: this.#messages.get('edit.content.unsaved.changes.message'),
            acceptLabel: this.#messages.get('edit.content.unsaved.changes.keep'),
            rejectLabel: this.#messages.get('edit.content.unsaved.changes.discard'),
            acceptIcon: 'hidden',
            rejectIcon: 'hidden',
            rejectButtonStyleClass: 'p-button-outlined',
            // "Keep editing", or X / ESC on the prompt itself: stay open with the edits.
            accept: () => undefined,
            reject: (type?: ConfirmEventType) => {
                if (type === ConfirmEventType.REJECT) {
                    this.#panel.close();
                }
            }
        });
    }

    /**
     * Puts the cursor in the editor as soon as it exists, so the author can type straight away.
     *
     * @param editor the Monaco editor instance emitted by `ngx-monaco-editor`
     */
    protected onEditorInit(editor: DotSourceEditorInstance): void {
        editor.focus();
    }

    /**
     * Hands a server error to the shared handler, which explains it to the author.
     *
     * Anything else (a version with no file to load, a file the server refused to stage) has no
     * status for the handler to explain, so it is only logged: the panel shows the failure itself,
     * as the load error or as the save error above the buttons.
     *
     * @param error what the request failed with
     */
    #reportServerError(error: unknown): void {
        if (error instanceof HttpErrorResponse) {
            this.#httpErrorManager.handle(error);

            return;
        }

        console.error('Edit Source request failed', error);
    }
}
