import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component, Input } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';

import { BlockEditorNode, UVE_MODE } from '@dotcms/types';
import { BlockEditorDefaultBlocks } from '@dotcms/types/internal';
import { getUVEState } from '@dotcms/uve';

import { DotCMSBlockEditorRendererNativeComponent } from './dotcms-block-editor-renderer-native.component';

const MOCK_UVE_STATE_EDIT = {
    mode: UVE_MODE.EDIT,
    persona: 'test',
    variantName: 'test',
    experimentId: 'test',
    publishDate: 'test',
    languageId: 'test'
};

@Component({
    selector: 'dotcms-block-editor-renderer-custom-component',
    template: '<div data-testid="custom-component">Custom Component</div>'
})
export class DotCMSBlockEditorRendererCustomComponent {
    @Input() node: BlockEditorNode | undefined;
}

jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn()
}));

/** Wraps `content` in a valid `doc` node so the native component renders it. */
const doc = (content: BlockEditorNode[]): BlockEditorNode => ({
    type: 'doc',
    content
});

describe('DotCMSBlockEditorRendererNativeComponent — semantic dispatch', () => {
    const getUVEStateMock = getUVEState as jest.Mock;

    let spectator: Spectator<DotCMSBlockEditorRendererNativeComponent>;
    const createComponent = createComponentFactory({
        component: DotCMSBlockEditorRendererNativeComponent
    });

    beforeEach(() => {
        getUVEStateMock.mockReturnValue(null);
        spectator = createComponent();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    const render = (content: BlockEditorNode[]) => {
        spectator.setInput('blocks', doc(content));
        // `isValidBlocks` rejects nodes with empty `content: []`; these tests focus on
        // the dispatch/DOM output, not validation (covered in the component spec), so
        // force the valid state to always exercise the render path.
        spectator.component.$blockEditorState.set({ error: null });
        spectator.detectChanges();
    };

    describe('Paragraph', () => {
        it('should render a real <p> tag', () => {
            render([{ type: BlockEditorDefaultBlocks.PARAGRAPH, content: [] }]);
            expect(spectator.query('p')).toBeTruthy();
        });
    });

    describe('Text', () => {
        it('should render the text node', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.PARAGRAPH,
                    content: [
                        { type: BlockEditorDefaultBlocks.TEXT, text: 'Hello', marks: [] }
                    ]
                }
            ]);
            expect(spectator.query('p')?.textContent).toContain('Hello');
        });

        it('should render bold marks as <strong>', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.PARAGRAPH,
                    content: [
                        {
                            type: BlockEditorDefaultBlocks.TEXT,
                            text: 'Bold',
                            marks: [{ type: 'bold', attrs: {} }]
                        }
                    ]
                }
            ]);
            expect(spectator.query('strong')).toBeTruthy();
        });

        it('should nest stacked marks with no wrapper element between them', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.PARAGRAPH,
                    content: [
                        {
                            type: BlockEditorDefaultBlocks.TEXT,
                            text: 'Few',
                            marks: [
                                { type: 'underline', attrs: {} },
                                { type: 'bold', attrs: {} }
                            ]
                        }
                    ]
                }
            ]);

            // <u> directly contains <strong> directly containing the raw text —
            // no dotcms-block-editor-renderer*-text element anywhere.
            const u = spectator.query('u');
            expect(u).toBeTruthy();
            expect((u?.children[0] as HTMLElement)?.tagName.toLowerCase()).toBe('strong');
            expect(u?.querySelector('strong')?.textContent?.trim()).toBe('Few');
            expect(spectator.query('dotcms-block-editor-renderer-native-text')).toBeNull();
            expect(spectator.query('dotcms-block-editor-renderer-text')).toBeNull();
        });

        it('should render a link mark as a native <a> with its class', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.PARAGRAPH,
                    content: [
                        {
                            type: BlockEditorDefaultBlocks.TEXT,
                            text: 'link',
                            marks: [
                                {
                                    type: 'link',
                                    attrs: { href: '/foo', class: 'cta' }
                                }
                            ]
                        }
                    ]
                }
            ]);

            const a = spectator.query('a');
            expect(a).toBeTruthy();
            expect(a?.getAttribute('href')).toBe('/foo');
            expect(a?.classList.contains('cta')).toBe(true);
        });
    });

    describe('Headings', () => {
        it('should render a real <h2> for level "2"', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.HEADING,
                    attrs: { level: '2' },
                    content: []
                }
            ]);
            expect(spectator.query('h2')).toBeTruthy();
        });

        it('should render a real <h6> when level is the number 6', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.HEADING,
                    attrs: { level: 6 },
                    content: []
                }
            ]);
            expect(spectator.query('h6')).toBeTruthy();
        });

        it('should default to <h1> when level is missing', () => {
            render([{ type: BlockEditorDefaultBlocks.HEADING, content: [] }]);
            expect(spectator.query('h1')).toBeTruthy();
        });
    });

    describe('Lists — semantic nesting (the a11y fix)', () => {
        it('should render <ul> directly containing <li> with no wrapper element between', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.BULLET_LIST,
                    content: [
                        {
                            type: BlockEditorDefaultBlocks.LIST_ITEM,
                            content: [
                                {
                                    type: BlockEditorDefaultBlocks.PARAGRAPH,
                                    content: [
                                        {
                                            type: BlockEditorDefaultBlocks.TEXT,
                                            text: 'Item 1',
                                            marks: []
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ]);

            const ul = spectator.query('ul');
            expect(ul).toBeTruthy();

            // The <li> must be a true element child of <ul> (comment nodes are allowed
            // between them, but no element wrapper).
            const elementChildren = Array.from(ul?.children ?? []);
            expect(elementChildren.length).toBe(1);
            expect(elementChildren[0].tagName.toLowerCase()).toBe('li');

            // The <li> contains a <p> with the text.
            const li = ul?.querySelector('li');
            expect(li?.querySelector('p')?.textContent).toContain('Item 1');

            // Crucially: NO dotcms-block-editor-renderer-* wrapper anywhere in the tree.
            expect(
                spectator.element.querySelector('[class*="dotcms-block-editor-renderer-"]')
            ).toBeNull();
            expect(spectator.element.innerHTML).not.toContain(
                'dotcms-block-editor-renderer-block'
            );
        });

        it('should render an ordered list as <ol><li>', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.ORDERED_LIST,
                    content: [{ type: BlockEditorDefaultBlocks.LIST_ITEM, content: [] }]
                }
            ]);

            const ol = spectator.query('ol');
            expect(ol).toBeTruthy();
            expect(ol?.querySelector('li')).toBeTruthy();
            expect((ol?.children[0] as HTMLElement)?.tagName.toLowerCase()).toBe('li');
        });
    });

    describe('Blockquote', () => {
        it('should render a real <blockquote>', () => {
            render([{ type: BlockEditorDefaultBlocks.BLOCK_QUOTE, content: [] }]);
            expect(spectator.query('blockquote')).toBeTruthy();
        });
    });

    describe('Code block', () => {
        it('should render <pre><code>', () => {
            render([{ type: BlockEditorDefaultBlocks.CODE_BLOCK, content: [] }]);
            expect(spectator.query('pre code')).toBeTruthy();
        });
    });

    describe('HTML elements', () => {
        it('should render a horizontal rule', () => {
            render([{ type: BlockEditorDefaultBlocks.HORIZONTAL_RULE, content: [] }]);
            expect(spectator.query('hr')).toBeTruthy();
        });

        it('should render a line break', () => {
            render([{ type: BlockEditorDefaultBlocks.HARDBREAK, content: [] }]);
            expect(spectator.query('br')).toBeTruthy();
        });
    });

    describe('Media / table / grid / contentlet', () => {
        it('should render the image component', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.DOT_IMAGE,
                    attrs: { src: 'image.jpg' },
                    content: []
                }
            ]);
            expect(spectator.query('dotcms-block-editor-renderer-image')).toBeTruthy();
        });

        it('should render the video component', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.DOT_VIDEO,
                    attrs: { src: 'video.mp4' },
                    content: []
                }
            ]);
            expect(spectator.query('dotcms-block-editor-renderer-video')).toBeTruthy();
        });

        it('should render the table component', () => {
            render([{ type: BlockEditorDefaultBlocks.TABLE, content: [] }]);
            expect(spectator.query('dotcms-block-editor-renderer-table')).toBeTruthy();
        });

        it('should render the grid block component', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.GRID_BLOCK,
                    attrs: { columns: [6, 6] },
                    content: []
                }
            ]);
            expect(spectator.query('dotcms-block-editor-renderer-grid-block')).toBeTruthy();
        });

        it('should render the contentlet component', () => {
            render([
                {
                    type: BlockEditorDefaultBlocks.DOT_CONTENT,
                    attrs: { identifier: '123' },
                    content: []
                }
            ]);
            expect(spectator.query('dotcms-block-editor-renderer-contentlet')).toBeTruthy();
        });
    });

    describe('Unknown block', () => {
        it('should render the unknown block message in edit mode', () => {
            getUVEStateMock.mockReturnValue(MOCK_UVE_STATE_EDIT);
            render([
                {
                    type: 'UNKNOWN_TYPE' as unknown as BlockEditorDefaultBlocks,
                    content: []
                }
            ]);
            expect(spectator.query(byTestId('unknown-block-type'))).toBeTruthy();
        });
    });

    describe('Custom renderers', () => {
        it('should use the custom renderer when provided for a block type', fakeAsync(() => {
            const customRenderers = {
                [BlockEditorDefaultBlocks.PARAGRAPH]: Promise.resolve(
                    DotCMSBlockEditorRendererCustomComponent
                )
            };

            spectator.setInput('customRenderers', customRenderers);
            spectator.setInput(
                'blocks',
                doc([{ type: BlockEditorDefaultBlocks.PARAGRAPH, content: [] }])
            );
            spectator.component.$blockEditorState.set({ error: null });
            spectator.detectChanges();

            tick();
            spectator.detectChanges();

            const custom = spectator.query(byTestId('custom-component'));
            expect(custom).toBeTruthy();
            // The default <p> should NOT be rendered for this node.
            expect(spectator.query('p')).toBeFalsy();
        }));
    });
});
