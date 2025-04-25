import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component, Input } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';

import { ContentNode } from '@dotcms/types';
import { Blocks } from '@dotcms/types/internal';

import { DotCMSBlockEditorItemComponent } from './dotcms-block-editor-item.component';

import { DotBlockQuote, DotCodeBlock } from '../blocks/code.component';
import { DotContentletBlock } from '../blocks/contentlet.component';
import { DotImageBlock } from '../blocks/image.component';
import { DotBulletList, DotListItem, DotOrdererList } from '../blocks/list.component';
import { DotTableBlock } from '../blocks/table.component';
import { DotHeadingBlock, DotParagraphBlock, DotTextBlock } from '../blocks/text.component';
import { DotVideoBlock } from '../blocks/video.components';

@Component({
    selector: 'dotcms-block-editor-renderer-custom-component',
    standalone: true,
    template: '<div>Custom Component</div>'
})
export class DotCMSBlockEditorRendererCustomComponent {
    @Input() content: ContentNode[] = [];
}

describe('DotCMSBlockEditorRendererBlockComponent', () => {
    let spectator: Spectator<DotCMSBlockEditorItemComponent>;
    const createComponent = createComponentFactory({
        component: DotCMSBlockEditorItemComponent,
        shallow: true
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('Block Rendering', () => {
        describe('Paragraph Block', () => {
            beforeEach(() => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.PARAGRAPH,
                        attrs: { style: 'color: red' },
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();
            });

            it('should render paragraph component', () => {
                expect(spectator.query(DotParagraphBlock)).toBeTruthy();
            });
        });

        describe('Text Block', () => {
            beforeEach(() => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.TEXT,
                        text: 'Sample text',
                        marks: [{ type: 'bold', attrs: {} }],
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();
            });

            it('should render text component', () => {
                expect(spectator.query(DotTextBlock)).toBeTruthy();
            });

            it('should pass text and marks', () => {
                const textComponent = spectator.query(DotTextBlock);
                expect(textComponent?.text).toBe('Sample text');
                expect(textComponent?.marks).toBeTruthy();
            });
        });

        describe('Heading Block', () => {
            beforeEach(() => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.HEADING,
                        attrs: { level: '2', style: 'font-size: 24px' },
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();
            });

            it('should render heading component', () => {
                expect(spectator.query(DotHeadingBlock)).toBeTruthy();
            });

            it('should pass level attribute', () => {
                const heading = spectator.query(DotHeadingBlock);
                expect(heading?.level).toBe('2');
            });
        });

        describe('List Blocks', () => {
            it('should render bullet list', () => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.BULLET_LIST,
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                expect(spectator.query(DotBulletList)).toBeTruthy();
            });

            it('should render ordered list', () => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.ORDERED_LIST,
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                expect(spectator.query(DotOrdererList)).toBeTruthy();
            });

            it('should render list item', () => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.LIST_ITEM,
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                expect(spectator.query(DotListItem)).toBeTruthy();
            });
        });

        describe('Media Blocks', () => {
            it('should render image component', () => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.DOT_IMAGE,
                        attrs: { src: 'image.jpg' },
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                expect(spectator.query(DotImageBlock)).toBeTruthy();
            });

            it('should render video component', () => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.DOT_VIDEO,
                        attrs: { src: 'video.mp4' },
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                expect(spectator.query(DotVideoBlock)).toBeTruthy();
            });
        });

        describe('Other Blocks', () => {
            it('should render blockquote', () => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.BLOCK_QUOTE,
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                expect(spectator.query(DotBlockQuote)).toBeTruthy();
            });

            it('should render code block', () => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.CODE_BLOCK,
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                expect(spectator.query(DotCodeBlock)).toBeTruthy();
            });

            it('should render table', () => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.TABLE,
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                expect(spectator.query(DotTableBlock)).toBeTruthy();
            });

            it('should render contentlet', () => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.DOT_CONTENT,
                        attrs: { identifier: '123' },
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                expect(spectator.query(DotContentletBlock)).toBeTruthy();
            });
        });

        describe('HTML Elements', () => {
            it('should render horizontal rule', () => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.HORIZONTAL_RULE,
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                expect(spectator.query('hr')).toBeTruthy();
            });

            it('should render line break', () => {
                const content: ContentNode[] = [
                    {
                        type: Blocks.HARDBREAK,
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                expect(spectator.query('br')).toBeTruthy();
            });
        });

        describe('Custom Renderers', () => {
            it('should use custom renderer when provided', fakeAsync(() => {
                const customRenderers = {
                    [Blocks.PARAGRAPH]: Promise.resolve(DotCMSBlockEditorRendererCustomComponent)
                };

                const content: ContentNode[] = [
                    {
                        type: Blocks.PARAGRAPH,
                        content: []
                    }
                ];

                spectator.setInput('customRenderers', customRenderers);
                spectator.setInput('content', content);
                spectator.detectChanges();

                // Wait for the Promise to resolve
                tick();
                // Trigger change detection again after the Promise resolves
                spectator.detectChanges();

                const customComponent = spectator.query(DotCMSBlockEditorRendererCustomComponent);
                expect(customComponent).toBeTruthy();
                expect(customComponent?.content).toEqual(content[0]);
            }));

            it('should render unknown block type message', () => {
                const content: ContentNode[] = [
                    {
                        type: 'UNKNOWN_TYPE' as unknown as Blocks,
                        content: []
                    }
                ];
                spectator.setInput('content', content);
                spectator.detectChanges();

                const unknownBlock = spectator.query('div');
                expect(unknownBlock?.textContent).toContain('Unknown Block Type: UNKNOWN_TYPE');
            });
        });
    });
});
