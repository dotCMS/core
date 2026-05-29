import { of } from 'rxjs';

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Editor } from '@tiptap/core';
import { Document } from '@tiptap/extension-document';
import { Paragraph } from '@tiptap/extension-paragraph';
import { Subscript } from '@tiptap/extension-subscript';
import { Superscript } from '@tiptap/extension-superscript';
import { Text } from '@tiptap/extension-text';

import { DotAiService, DotContentTypeService, DotMessageService } from '@dotcms/data-access';

import { DotBubbleMenuComponent } from './dot-bubble-menu.component';

/**
 * Builds a spy for `editor.chain()` that records the order methods are called on
 * the fluent chain and returns the chain itself for each call (except `run`).
 */
function makeOrderedEditorChainSpy(editor: Editor) {
    const callOrder: string[] = [];
    const runSpy = jest.fn();

    const chain: Record<string, jest.Mock> = {
        focus: jest.fn(() => {
            callOrder.push('focus');

            return chain;
        }),
        unsetSubscript: jest.fn(() => {
            callOrder.push('unsetSubscript');

            return chain;
        }),
        unsetSuperscript: jest.fn(() => {
            callOrder.push('unsetSuperscript');

            return chain;
        }),
        toggleSuperscript: jest.fn(() => {
            callOrder.push('toggleSuperscript');

            return chain;
        }),
        toggleSubscript: jest.fn(() => {
            callOrder.push('toggleSubscript');

            return chain;
        }),
        run: runSpy
    };

    jest.spyOn(editor, 'chain').mockReturnValue(chain as never);

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
                    provide: DotAiService,
                    useValue: { checkPluginInstallation: () => of(false) }
                },
                { provide: DotContentTypeService, useValue: {} },
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
