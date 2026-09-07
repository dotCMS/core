import { Spectator, createComponentFactory, mockProvider } from '@openng/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { Editor } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';

import { DotContentletNodeViewComponent } from './contentlet.component';

import { EditorStore } from '../../../store/editor.store';

/** Fluent mock of `editor.chain().focus().setNodeSelection(pos).run()`. */
function mockEditor() {
    const chain = {
        focus: jest.fn(() => chain),
        setNodeSelection: jest.fn(() => chain),
        run: jest.fn(() => true)
    };
    const editor = { chain: jest.fn(() => chain) } as unknown as Editor;

    return { editor, chain };
}

const NODE = { attrs: { data: { title: 'Cross-country Skiing', identifier: 'id-1' } } };

describe('DotContentletNodeViewComponent — click-to-select (#36985)', () => {
    let spectator: Spectator<DotContentletNodeViewComponent>;

    const createComponent = createComponentFactory({
        component: DotContentletNodeViewComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        providers: [
            mockProvider(DotMessageService, { get: (key: string) => key }),
            {
                provide: EditorStore,
                useValue: { languageIso: () => 'en', languageId: () => 1 }
            }
        ]
    });

    /** Creates the node view with the required ngx-tiptap inputs, `getPos` returning `pos`. */
    function create(pos: number | undefined, editor: Editor) {
        spectator = createComponent({
            props: {
                editor,
                node: NODE,
                getPos: () => pos,
                decorations: [],
                innerDecorations: {},
                view: {},
                selected: false,
                extension: {},
                HTMLAttributes: {},
                updateAttributes: jest.fn(),
                deleteNode: jest.fn()
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
            } as any
        });
    }

    it('selects the node on mousedown using its own getPos()', () => {
        const { editor, chain } = mockEditor();
        create(7, editor);

        const event = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
        const preventDefault = jest.spyOn(event, 'preventDefault');
        spectator.element.dispatchEvent(event);

        expect(preventDefault).toHaveBeenCalled();
        expect(chain.setNodeSelection).toHaveBeenCalledWith(7);
        expect(chain.run).toHaveBeenCalledTimes(1);
    });

    it('does nothing when getPos() is unresolved', () => {
        const { editor, chain } = mockEditor();
        create(undefined, editor);

        const event = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
        const preventDefault = jest.spyOn(event, 'preventDefault');
        spectator.element.dispatchEvent(event);

        expect(preventDefault).not.toHaveBeenCalled();
        expect(editor.chain).not.toHaveBeenCalled();
        expect(chain.setNodeSelection).not.toHaveBeenCalled();
    });
});
