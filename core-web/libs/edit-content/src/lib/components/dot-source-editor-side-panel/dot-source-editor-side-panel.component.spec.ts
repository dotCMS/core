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
import { AfterViewInit, Component, forwardRef, input, output } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { Confirmation, ConfirmationService, ConfirmEventType } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DrawerModule } from 'primeng/drawer';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { DOT_VELOCITY_LANGUAGE_ID, DotMessagePipe } from '@dotcms/ui';

import { DotSourceEditorSidePanelComponent } from './dot-source-editor-side-panel.component';
import { DotSourceEditorFile, DotSourceEditorService } from './dot-source-editor.service';

import { DotSourceEditorRequest } from '../../models/dot-source-editor.model';
import { DotSidePanelNavController } from '../../services/dot-side-panel-nav.service';

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
    identifier: 'vtl-id',
    languageId: 2,
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
    let navController: { acquire: Mock; release: Mock; isTop: Mock };
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
                            DrawerModule,
                            ButtonModule,
                            ConfirmDialogModule,
                            FormsModule,
                            MonacoEditorStubComponent,
                            MockPipe(DotMessagePipe, (key: string) => key)
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
        navController = {
            acquire: vi.fn(),
            release: vi.fn(),
            isTop: vi.fn().mockReturnValue(true)
        };

        spectator = createComponent({
            providers: [{ provide: DotSidePanelNavController, useValue: navController }]
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

        it('saves the edited source into the opened file and language, keeping its name', async () => {
            await open();
            typeInEditor('#set($title = "Bye")');

            spectator.click(button('source-editor-save'));

            expect(save).toHaveBeenCalledWith({
                identifier: 'vtl-id',
                languageId: 2,
                fileName: 'header.vtl',
                source: '#set($title = "Bye")'
            });
        });

        it('reports the save and stays open, with nothing left to save', async () => {
            await open();
            const saved = vi.fn();
            const closed = vi.fn();
            spectator.output('saved').subscribe(saved);
            spectator.output('closed').subscribe(closed);
            typeInEditor('#set($title = "Bye")');

            spectator.click(button('source-editor-save'));
            spectator.detectChanges();

            expect(saved).toHaveBeenCalledTimes(1);
            expect(closed).not.toHaveBeenCalled();
            expect(button('source-editor-save').disabled).toBe(true);
        });

        it('keeps the edits and reports the error when the save fails', async () => {
            const error = new HttpErrorResponse({ status: 400 });
            save.mockReturnValue(throwError(() => error));
            await open();
            const saved = vi.fn();
            spectator.output('saved').subscribe(saved);
            typeInEditor('#set($title = "Bye")');

            spectator.click(button('source-editor-save'));
            spectator.detectChanges();

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
            expect(saved).not.toHaveBeenCalled();
            expect(editor()?.value).toBe('#set($title = "Bye")');
            expect(button('source-editor-save').disabled).toBe(false);
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
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

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
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            spectator.component.requestClose();

            expect(closed).toHaveBeenCalledTimes(closes);
        });

        it('does not close while a save runs', async () => {
            save.mockReturnValue(NEVER);
            await open();
            typeInEditor('#set($title = "Bye")');
            spectator.click(button('source-editor-save'));
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            spectator.component.requestClose();

            expect(closed).not.toHaveBeenCalled();
        });

        it('closes on Escape, through the same guard', async () => {
            await open();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            pressKey({ key: 'Escape' });

            expect(closed).toHaveBeenCalledTimes(1);
        });

        it('leaves Escape alone when another side panel is in front', async () => {
            navController.isTop.mockReturnValue(false);
            await open();
            const closed = vi.fn();
            spectator.output('closed').subscribe(closed);

            pressKey({ key: 'Escape' });

            expect(closed).not.toHaveBeenCalled();
        });

        it('leaves the side-panel stack and gives the shortcuts back when destroyed', async () => {
            await open();

            spectator.fixture.destroy();

            const event = new KeyboardEvent('keydown', {
                key: 's',
                metaKey: true,
                bubbles: true,
                cancelable: true
            });
            document.dispatchEvent(event);

            expect(navController.release).toHaveBeenCalled();
            expect(event.defaultPrevented).toBe(false);
        });
    });
});
