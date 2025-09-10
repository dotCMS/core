import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { BlockEditorContent, UVE_MODE } from '@dotcms/types';
import { BlockEditorState } from '@dotcms/types/internal';
import { getUVEState } from '@dotcms/uve';

import { DotCMSBlockEditorRendererComponent } from './dotcms-block-editor-renderer.component';
import { DotCMSBlockEditorItemComponent } from './item/dotcms-block-editor-item.component';

const MOCK_UVE_STATE = {
    mode: UVE_MODE.EDIT,
    persona: 'test',
    variantName: 'test',
    experimentId: 'test',
    publishDate: 'test',
    languageId: 'test'
};

jest.mock('@dotcms/uve', () => ({
    getUVEState: jest.fn()
}));

describe('DotCMSBlockEditorRendererComponent', () => {
    const getUVEStateMock = getUVEState as jest.Mock;

    let spectator: Spectator<DotCMSBlockEditorRendererComponent>;
    let component: DotCMSBlockEditorRendererComponent;

    const mockValidBlock: BlockEditorContent = {
        type: 'doc',
        content: [
            {
                type: 'paragraph',
                attrs: {
                    textAlign: 'left'
                },
                content: [
                    {
                        type: 'text',
                        text: 'Im a new paragraph'
                    }
                ]
            }
        ]
    };

    const mockInvalidBlock: BlockEditorContent = {
        type: 'invalid',
        content: []
    };

    const createComponent = createComponentFactory({
        component: DotCMSBlockEditorRendererComponent
    });

    beforeEach(() => {
        // Reset all mocks before each test
        jest.clearAllMocks();

        // Mock getUVEState
        getUVEStateMock.mockReturnValue({ ...MOCK_UVE_STATE, mode: UVE_MODE.EDIT });

        spectator = createComponent({
            props: {
                blocks: mockValidBlock
            }
        });
        component = spectator.component;

        spectator.detectChanges();
    });

    describe('Initialization', () => {
        it('should set initial blockEditorState', () => {
            spectator.detectChanges();

            expect(spectator.component.$blockEditorState()).toEqual({ error: null });
        });

        it('should set isInEditMode based on UVE state', () => {
            expect(component.$isInEditMode()).toBe(true);
        });

        it('should set isInEditMode to false when not in edit mode', () => {
            getUVEStateMock.mockReturnValue({ ...MOCK_UVE_STATE, mode: UVE_MODE.PREVIEW });
            spectator = createComponent();
            expect(spectator.component.$isInEditMode()).toBe(false);
        });
    });

    describe('Block Validation', () => {
        it('should render block content when blocks are valid', () => {
            spectator.setInput('blocks', mockValidBlock);
            spectator.detectChanges();

            const blockRenderer = spectator.query(DotCMSBlockEditorItemComponent);
            expect(blockRenderer).toBeTruthy();
            expect(blockRenderer?.content).toEqual(mockValidBlock.content);
        });

        it('should show error message when blocks are invalid and in edit mode', () => {
            const errorState: BlockEditorState = {
                error: 'Invalid block structure'
            };

            spectator.setInput('blocks', mockInvalidBlock);
            component.$blockEditorState.set(errorState);
            spectator.detectChanges();

            const errorMessage = spectator.query('[data-testid="invalid-blocks-message"]');
            expect(errorMessage).toBeTruthy();
            expect(errorMessage?.textContent).toContain(errorState.error);
        });

        it('should not show error message when not in edit mode', () => {
            getUVEStateMock.mockReturnValue({ ...MOCK_UVE_STATE, mode: UVE_MODE.PREVIEW });
            spectator = createComponent();

            const errorState: BlockEditorState = {
                error: 'Invalid block structure'
            };

            spectator.setInput('blocks', mockInvalidBlock);
            spectator.component.$blockEditorState.set(errorState);
            spectator.detectChanges();

            const errorMessage = spectator.query('[data-testid="invalid-blocks-message"]');
            expect(errorMessage).toBeFalsy();
        });
    });

    describe('Custom Renderers', () => {
        it('should pass custom renderers to block component', () => {
            const customRenderers = {
                'custom-block': Promise.resolve({})
            };

            spectator.setInput('blocks', mockValidBlock);
            spectator.setInput('customRenderers', customRenderers);
            spectator.detectChanges();

            const blockRenderer = spectator.query(DotCMSBlockEditorItemComponent);
            expect(blockRenderer?.customRenderers).toBe(customRenderers);
        });
    });

    describe('Edge Cases', () => {
        it('should handle undefined customRenderers gracefully', () => {
            spectator.setInput('blocks', mockValidBlock);
            spectator.setInput('customRenderers', undefined);
            spectator.detectChanges();

            const blockRenderer = spectator.query(DotCMSBlockEditorItemComponent);
            expect(blockRenderer).toBeTruthy();
            expect(blockRenderer?.customRenderers).toBeUndefined();
        });
    });
});
