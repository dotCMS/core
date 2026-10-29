import { createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { Mock, vi } from 'vitest';

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Editor } from '@tiptap/core';
import { Document } from '@tiptap/extension-document';
import { Paragraph } from '@tiptap/extension-paragraph';
import { Subscript } from '@tiptap/extension-subscript';
import { Superscript } from '@tiptap/extension-superscript';
import { Text } from '@tiptap/extension-text';

import { DotAiConfigService, DotMessageService } from '@dotcms/data-access';

import { DotBubbleMenuComponent } from './dot-bubble-menu.component';
import { getContentletDataFromSelection } from './utils';

// What the author has selected is the util's job; these specs only choose what it answers.
vi.mock('./utils', () => ({ getContentletDataFromSelection: vi.fn(() => ({})) }));

/**
 * Builds a spy for `editor.chain()` that records the order methods are called on
 * the fluent chain and returns the chain itself for each call (except `run`).
 */
function makeOrderedEditorChainSpy(editor: Editor) {
    const callOrder: string[] = [];
    const runSpy = vi.fn();

    const chain: Record<string, Mock> = {
        focus: vi.fn(() => {
            callOrder.push('focus');

            return chain;
        }),
        unsetSubscript: vi.fn(() => {
            callOrder.push('unsetSubscript');

            return chain;
        }),
        unsetSuperscript: vi.fn(() => {
            callOrder.push('unsetSuperscript');

            return chain;
        }),
        toggleSuperscript: vi.fn(() => {
            callOrder.push('toggleSuperscript');

            return chain;
        }),
        toggleSubscript: vi.fn(() => {
            callOrder.push('toggleSubscript');

            return chain;
        }),
        run: runSpy
    };

    vi.spyOn(editor, 'chain').mockReturnValue(chain as never);

    return { chain, callOrder, runSpy };
}

describe('DotBubbleMenuComponent - superscript/subscript mutual exclusion', () => {
    let component: DotBubbleMenuComponent;
    let editor: Editor;

    beforeEach(async () => {
        editor = new Editor({
            extensions: [Document, Paragraph, Text, Superscript, Subscript],
            content: '<p>Hello world</p>'
        });

        await TestBed.configureTestingModule({
            imports: [DotBubbleMenuComponent],
            providers: [
                {
                    provide: DotAiConfigService,
                    useValue: { checkPluginInstallation: () => of(false) }
                },
                {
                    provide: DotMessageService,
                    useValue: { get: (key: string) => key }
                }
            ],
            schemas: [NO_ERRORS_SCHEMA]
        })
            .overrideComponent(DotBubbleMenuComponent, { set: { template: '<div></div>' } })
            .compileComponents();

        const fixture = TestBed.createComponent(DotBubbleMenuComponent);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('editor', editor);
        fixture.detectChanges();
    });

    afterEach(() => {
        editor.destroy();
    });

    describe('toggleSuperscript', () => {
        it('should remove subscript before enabling superscript', () => {
            const { chain, callOrder, runSpy } = makeOrderedEditorChainSpy(editor);

            component['toggleSuperscript']();

            expect(callOrder).toEqual(['focus', 'unsetSubscript', 'toggleSuperscript']);
            expect(chain.unsetSubscript).toHaveBeenCalledTimes(1);
            expect(chain.toggleSuperscript).toHaveBeenCalledTimes(1);
            expect(runSpy).toHaveBeenCalledTimes(1);
        });
    });

    describe('toggleSubscript', () => {
        it('should remove superscript before enabling subscript', () => {
            const { chain, callOrder, runSpy } = makeOrderedEditorChainSpy(editor);

            component['toggleSubscript']();

            expect(callOrder).toEqual(['focus', 'unsetSuperscript', 'toggleSubscript']);
            expect(chain.unsetSuperscript).toHaveBeenCalledTimes(1);
            expect(chain.toggleSubscript).toHaveBeenCalledTimes(1);
            expect(runSpy).toHaveBeenCalledTimes(1);
        });
    });
});

/**
 * #38303 (R8) — editing an embedded contentlet only asks. Whoever mounts the editor opens it; the
 * bubble menu no longer prompts, writes a "return to" breadcrumb or navigates the parent window.
 */
describe('DotBubbleMenuComponent - editing an embedded contentlet', () => {
    let spectator: Spectator<DotBubbleMenuComponent>;
    let editor: Editor;

    const createComponent = createComponentFactory({
        component: DotBubbleMenuComponent,
        detectChanges: false,
        overrideComponents: [[DotBubbleMenuComponent, { set: { template: '<div></div>' } }]],
        providers: [
            {
                provide: DotAiConfigService,
                useValue: { checkPluginInstallation: () => of(false) }
            },
            { provide: DotMessageService, useValue: { get: (key: string) => key } }
        ]
    });

    beforeEach(() => {
        editor = new Editor({ extensions: [Document, Paragraph, Text], content: '<p>Hi</p>' });
        spectator = createComponent();
        spectator.setInput('editor', editor);
        spectator.detectChanges();
    });

    afterEach(() => {
        editor.destroy();
        vi.restoreAllMocks();
    });

    it('emits contentletEdit with the selected contentlet inode and content type', () => {
        vi.mocked(getContentletDataFromSelection).mockReturnValue({
            inode: 'embedded-inode',
            contentType: 'Blog'
        });
        const emitted = vi.fn();
        spectator.output('contentletEdit').subscribe(emitted);

        spectator.component['goToContentlet']();

        expect(emitted).toHaveBeenCalledWith({ inode: 'embedded-inode', contentType: 'Blog' });
    });

    it('neither prompts nor writes a "return to" breadcrumb', () => {
        vi.mocked(getContentletDataFromSelection).mockReturnValue({
            inode: 'embedded-inode',
            contentType: 'Blog'
        });
        const confirm = vi.spyOn(window, 'confirm');
        const setItem = vi.spyOn(Storage.prototype, 'setItem');

        spectator.component['goToContentlet']();

        expect(confirm).not.toHaveBeenCalled();
        expect(setItem).not.toHaveBeenCalled();
    });

    it('emits nothing when the selection names no contentlet', () => {
        vi.mocked(getContentletDataFromSelection).mockReturnValue({});
        const emitted = vi.fn();
        spectator.output('contentletEdit').subscribe(emitted);

        spectator.component['goToContentlet']();

        expect(emitted).not.toHaveBeenCalled();
    });
});
