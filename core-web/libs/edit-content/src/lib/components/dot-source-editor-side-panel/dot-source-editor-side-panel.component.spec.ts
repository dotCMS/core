import { MonacoEditorLoaderService } from '@materia-ui/ngx-monaco-editor';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { MockPipe } from 'ng-mocks';
import { BehaviorSubject, NEVER, Observable, of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';
import { AfterViewInit, Component, forwardRef, input, output, signal } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { Confirmation, ConfirmationService, ConfirmEventType } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { ProgressSpinner, ProgressSpinnerModule } from 'primeng/progressspinner';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DOT_VELOCITY_LANGUAGE_ID, DotMessagePipe, DotSidePanelRef } from '@dotcms/ui';

import { DotSourceEditorSidePanelComponent } from './dot-source-editor-side-panel.component';
import {
    DotSourceEditorFile,
    DotSourceEditorService,
    DotSourceEditorUploadError
} from './dot-source-editor.service';

import { DotSourceEditorRequest } from '../../models/dot-source-editor.model';

/**
 * Stands in for `ngx-monaco-editor`: a value accessor like the real one, without Monaco's AMD
 * loader. `type` does what typing in the editor does.
 */
@Component({
    // eslint-disable-next-line @angular-eslint/component-selector -- must match the real element
    selector: 'ngx-monaco-editor',
    template: '',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MonacoEditorStubComponent),
            multi: true
        }
    ]
})
class MonacoEditorStubComponent implements ControlValueAccessor, AfterViewInit {
    readonly options = input<Record<string, unknown>>();
    readonly init = output<{ focus: () => void }>();

    readonly focus = vi.fn();
    value = '';
    #onChange: (value: string) => void = () => undefined;

    ngAfterViewInit(): void {
        this.init.emit({ focus: this.focus });
    }

    writeValue(value: string): void {
        this.value = value ?? '';
    }

    registerOnChange(fn: (value: string) => void): void {
        this.#onChange = fn;
    }

    registerOnTouched(): void {
        // Not read by the panel.
    }

    type(value: string): void {
        this.value = value;
        this.#onChange(value);
    }
}

const REQUEST: DotSourceEditorRequest = {
    inode: 'vtl-inode',
    title: 'header.vtl',
    language: DOT_VELOCITY_LANGUAGE_ID
};

const FILE: DotSourceEditorFile = {
    fileName: 'header.vtl',
    source: '#set($title = "Hello")'
};

describe('DotSourceEditorSidePanelComponent', () => {
    let spectator: Spectator<DotSourceEditorSidePanelComponent>;
    let load: Mock<(inode: string) => Observable<DotSourceEditorFile>>;
    let save: Mock<() => Observable<unknown>>;
    /** The panel's ref, reduced to what the content uses. */
    let panel: { close: Mock; contentStyleClass: ReturnType<typeof signal<string>> };
    /** Whether Monaco's AMD loader has finished; tests fire it to simulate the load. */
    let monacoLoaded$: BehaviorSubject<boolean>;

    const createComponent = createComponentFactory({
        component: DotSourceEditorSidePanelComponent,
        overrideComponents: [
            [
                DotSourceEditorSidePanelComponent,
                {
                    set: {
                        imports: [
                            ButtonModule,
                            ConfirmDialogModule,
                            FormsModule,
                            MessageModule,
                            MonacoEditorStubComponent,
                            ProgressSpinnerModule,
                            // The key, then any arguments, so a test can see what was passed in.
                            MockPipe(DotMessagePipe, (key: string, args: string[] = []) =>
                                [key, ...args].join(' ')
                            )
                        ]
                    }
                }
            ]
        ],
        providers: [
            mockProvider(DotMessageService, { get: vi.fn((key: string) => key) }),
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() }),
            {
                provide: MonacoEditorLoaderService,
                useFactory: () => ({ isMonacoLoaded$: monacoLoaded$ })
            }
        ],
        // Replaces the panel's own providers, so its prompt service is listed again.
        componentProviders: [
            ConfirmationService,
            // Read per creation, so each test's `load` and `save` reach the panel.
            { provide: DotSourceEditorService, useFactory: () => ({ load, save }) }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        localStorage.clear();
        monacoLoaded$ = new BehaviorSubject(false);
        load = vi.fn(() => of(FILE));
        save = vi.fn(() => of({}));
        panel = { close: vi.fn(), contentStyleClass: signal('p-0!') };

        spectator = createComponent({
            providers: [{ provide: DotSidePanelRef, useValue: panel }]
        });
    });

    afterEach(() => {
        delete (globalThis as { monaco?: unknown }).monaco;
        // The factory's mocks are shared across tests: drop their calls, and undo any spy.
        vi.clearAllMocks();
        vi.restoreAllMocks();
    });

    /** Opens the panel on a file and lets the load and the editor's model write settle. */
    const open = async (request: DotSourceEditorRequest = REQUEST) => {
        spectator.setInput('request', request);
        spectator.detectChanges();
        await spectator.fixture.whenStable();
        spectator.detectChanges();
    };

    /** `appendTo="body"` teleports the drawer out of the fixture, so queries search the document. */
    const editor = (): MonacoEditorStubComponent | null =>
        spectator.query(MonacoEditorStubComponent);

    const button = (testId: string): HTMLButtonElement =>
        spectator
            .query(byTestId(testId), { root: true })
            ?.querySelector('button') as HTMLButtonElement;

    /** The message a save failure leaves above the buttons, if any. */
    const saveError = (): HTMLElement | null =>
        spectator.query<HTMLElement>(byTestId('source-editor-save-error'), { root: true });

    /** Types in the editor and renders the result. */
    const typeInEditor = (value: string) => {
        editor()?.type(value);
        spectator.detectChanges();
    };

    const pressKey = (init: KeyboardEventInit): KeyboardEvent => {
        const event = new KeyboardEvent('keydown', { bubbles: true, cancelable: true, ...init });
        document.dispatchEvent(event);
        spectator.detectChanges();

        return event;
    };

    /** The panel's own confirmation service, the one its `<p-confirmDialog>` renders. */
    const panelConfirmation = (): ConfirmationService =>
        spectator.inject(ConfirmationService, true);

    /** Answers the next prompt the way the author would. */
    const answerPrompt = (answer: 'keep' | 'discard' | 'dismiss') =>
        vi.spyOn(panelConfirmation(), 'confirm').mockImplementation((options: Confirmation) => {
            if (answer === 'keep') {
                options.accept?.();
            } else {
                options.reject?.(
                    answer === 'discard' ? ConfirmEventType.REJECT : ConfirmEventType.CANCEL
                );
            }

            return panelConfirmation();
        });

    describe('loading', () => {
        it('opens straight on the source of the requested version', async () => {
            await open();

            expect(load).toHaveBeenCalledWith('vtl-inode');
            expect(editor()?.value).toBe(FILE.source);
            expect(editor()?.options()).toEqual(
                expect.objectContaining({ language: DOT_VELOCITY_LANGUAGE_ID, readOnly: false })
            );
        });

        it('puts the cursor in the editor', async () => {
            await open();

            expect(editor()?.focus).toHaveBeenCalled();
        });

        it('shows a spinner until the source arrives', async () => {
            load.mockReturnValue(NEVER);

            await open();

            expect(
                spectator.query(byTestId('source-editor-loading'), { root: true })
            ).not.toBeNull();
            // The drawer's own spinner, so the one it shows while the panel loads is not swapped.
            expect(spectator.query(ProgressSpinner)).not.toBeNull();
            expect(editor()).toBeNull();
        });

        it('reports a failed load and shows no editor', async () => {
            const error = new HttpErrorResponse({ status: 404 });
            load.mockReturnValue(throwError(() => error));

            await open();

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
            expect(spectator.query(byTestId('source-editor-error'), { root: true })).not.toBeNull();
            expect(editor()).toBeNull();
        });

        it('shows the load error without the HTTP handler when the failure is not a server error', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);
            load.mockReturnValue(throwError(() => new Error('No file to edit')));

            await open();

            expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
            expect(spectator.query(byTestId('source-editor-error'), { root: true })).not.toBeNull();
        });

        it('registers Velocity with Monaco as soon as Monaco loads', () => {
            const register = vi.fn();
            (globalThis as { monaco?: unknown }).monaco = {
                languages: { getLanguages: () => [], register, setMonarchTokensProvider: vi.fn() }
            };

            monacoLoaded$.next(true);

            expect(register).toHaveBeenCalledWith(
                expect.objectContaining({ id: DOT_VELOCITY_LANGUAGE_ID })
            );
        });
    });

    describe('saving', () => {
        it('keeps Save disabled until the source changes', async () => {
            await open();

            expect(button('source-editor-save').disabled).toBe(true);

            typeInEditor('#set($title = "Bye")');

            expect(button('source-editor-save').disabled).toBe(false);
        });

        it('saves the edited source on the opened version, keeping its name', async () => {
            await open();
            typeInEditor('#set($title = "Bye")');

            spectator.click(button('source-editor-save'));

            expect(save).toHaveBeenCalledWith({
                inode: 'vtl-inode',
                fileName: 'header.vtl',
                source: '#set($title = "Bye")'
            });
        });

        it('reports the save, then closes without asking', async () => {
            await open();
            const events: string[] = [];
            spectator.output('saved').subscribe(() => events.push('saved'));
            panel.close.mockImplementation(() => events.push('closed'));
            const confirm = vi.spyOn(panelConfirmation(), 'confirm');
            typeInEditor('#set($title = "Bye")');

            spectator.click(button('source-editor-save'));

            // Saved first, so the opener refreshes before it clears the panel.
            expect(events).toEqual(['saved', 'closed']);
            expect(confirm).not.toHaveBeenCalled();
        });

        it('stays open with the edits and reports the error when the save fails', async () => {
            const error = new HttpErrorResponse({ status: 400 });
            save.mockReturnValue(throwError(() => error));
            await open();
            const saved = vi.fn();
            const closed = panel.close;
            spectator.output('saved').subscribe(saved);
            typeInEditor('#set($title = "Bye")');

            spectator.click(button('source-editor-save'));
            spectator.detectChanges();

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
            expect(saved).not.toHaveBeenCalled();
            expect(closed).not.toHaveBeenCalled();
            expect(editor()?.value).toBe('#set($title = "Bye")');
            expect(button('source-editor-save').disabled).toBe(false);
            // The shared handler already explains it: no second message in the panel.
            expect(saveError()).toBeNull();
        });

        it('says why on screen when the server refuses the file, keeping the edits', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);
            save.mockReturnValue(
                throwError(() => new DotSourceEditorUploadError('File too large'))
            );
            await open();
            typeInEditor('#set($title = "Bye")');

            spectator.click(button('source-editor-save'));
            spectator.detectChanges();

            expect(saveError()?.textContent?.trim()).toBe(
                'edit.content.source-editor.save-error-reason File too large'
            );
            // In the bar with the buttons, to their left.
            expect(
                spectator.query(byTestId('source-editor-actions'), { root: true })
                    ?.firstElementChild
            ).toBe(saveError());
            expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
            expect(panel.close).not.toHaveBeenCalled();
            expect(editor()?.value).toBe('#set($title = "Bye")');
            expect(button('source-editor-save').disabled).toBe(false);
        });

        it('says only that the save failed when the server gives no reason', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);
            save.mockReturnValue(throwError(() => new DotSourceEditorUploadError()));
            await open();
            typeInEditor('#set($title = "Bye")');

            spectator.click(button('source-editor-save'));
            spectator.detectChanges();

            expect(saveError()?.textContent?.trim()).toBe('edit.content.source-editor.save-error');
        });

        it('clears the message when the author saves again', async () => {
            vi.spyOn(console, 'error').mockImplementation(() => undefined);
            save.mockReturnValueOnce(throwError(() => new DotSourceEditorUploadError()));
            save.mockReturnValueOnce(NEVER);
            await open();
            typeInEditor('#set($title = "Bye")');
            spectator.click(button('source-editor-save'));
            spectator.detectChanges();

            spectator.click(button('source-editor-save'));
            spectator.detectChanges();

            expect(saveError()).toBeNull();
        });

        it('makes the editor read-only while the save runs', async () => {
            save.mockReturnValue(NEVER);
            await open();
            typeInEditor('#set($title = "Bye")');

            spectator.click(button('source-editor-save'));
            spectator.detectChanges();

            expect(editor()?.options()).toEqual(expect.objectContaining({ readOnly: true }));
        });

        it('saves with Cmd/Ctrl + S and keeps the browser from opening its own Save', async () => {
            await open();
            typeInEditor('#set($title = "Bye")');

            const event = pressKey({ key: 's', metaKey: true });

            expect(save).toHaveBeenCalledTimes(1);
            expect(event.defaultPrevented).toBe(true);
        });

        it('swallows Cmd/Ctrl + S with nothing to save, without saving', async () => {
            await open();

            const event = pressKey({ key: 's', ctrlKey: true });

            expect(save).not.toHaveBeenCalled();
            expect(event.defaultPrevented).toBe(true);
        });
    });

    describe('closing', () => {
        it('closes at once when nothing changed', async () => {
            await open();
            const confirm = vi.spyOn(panelConfirmation(), 'confirm');
            const closed = panel.close;

            spectator.click(button('source-editor-cancel'));

            expect(confirm).not.toHaveBeenCalled();
            expect(closed).toHaveBeenCalledTimes(1);
        });

        it("asks with the Edit Content side panel's wording before discarding changes", async () => {
            await open();
            typeInEditor('#set($title = "Bye")');
            const confirm = vi.spyOn(panelConfirmation(), 'confirm');

            spectator.component.requestClose();

            expect(confirm).toHaveBeenCalledWith(
                expect.objectContaining({
                    header: 'edit.content.unsaved.changes.title',
                    message: 'edit.content.unsaved.changes.message',
                    acceptLabel: 'edit.content.unsaved.changes.keep',
                    rejectLabel: 'edit.content.unsaved.changes.discard'
                })
            );
        });

        it.each([
            ['Keep editing', 'keep', 0],
            ['Discard', 'discard', 1],
            ['dismissing the prompt', 'dismiss', 0]
        ] as const)('closes with changes only on Discard: %s', async (_label, answer, closes) => {
            await open();
            typeInEditor('#set($title = "Bye")');
            answerPrompt(answer);
            const closed = panel.close;

            spectator.component.requestClose();

            expect(closed).toHaveBeenCalledTimes(closes);
        });

        it('does not close while a save runs', async () => {
            save.mockReturnValue(NEVER);
            await open();
            typeInEditor('#set($title = "Bye")');
            spectator.click(button('source-editor-save'));
            const closed = panel.close;

            spectator.component.requestClose();

            expect(closed).not.toHaveBeenCalled();
        });

        // The X, Escape and the mask belong to the drawer, which sends them to `requestClose`;
        // they are tested with it, in `@dotcms/ui`.

        it('gives Cmd/Ctrl + S back when destroyed', async () => {
            await open();

            spectator.fixture.destroy();

            const event = new KeyboardEvent('keydown', {
                key: 's',
                metaKey: true,
                bubbles: true,
                cancelable: true
            });
            document.dispatchEvent(event);

            expect(event.defaultPrevented).toBe(false);
        });
    });

    describe('layout', () => {
        it('asks the drawer for its own padding, in a column the editor and the buttons share', () => {
            expect(panel.contentStyleClass()).toBe('flex flex-col');
        });
    });
});
