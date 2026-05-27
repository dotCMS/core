import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { BlockEditorNode, UVE_MODE } from '@dotcms/types';
import { BlockEditorState } from '@dotcms/types/internal';
import { getUVEState } from '@dotcms/uve';

import { DotCMSBlockEditorRendererNativeComponent } from './dotcms-block-editor-renderer-native.component';

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

describe('DotCMSBlockEditorRendererNativeComponent', () => {
    const getUVEStateMock = getUVEState as jest.Mock;

    let spectator: Spectator<DotCMSBlockEditorRendererNativeComponent>;
    let component: DotCMSBlockEditorRendererNativeComponent;

    const mockValidBlock: BlockEditorNode = {
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

    const mockInvalidBlock: BlockEditorNode = {
        type: 'invalid',
        content: []
    };

    const createComponent = createComponentFactory({
        component: DotCMSBlockEditorRendererNativeComponent,
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();

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

            expect(component.$blockEditorState()).toEqual({ error: null });
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
        it('should render semantic block content when blocks are valid', () => {
            spectator.setInput('blocks', mockValidBlock);
            spectator.detectChanges();

            // Renders a real <p> with no wrapper element
            expect(spectator.query('p')).toBeTruthy();
            expect(spectator.query('[data-testid="invalid-blocks-message"]')).toBeFalsy();
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

    describe('class / style passthrough', () => {
        it('should apply class and style to the wrapper div', () => {
            spectator.setInput('blocks', mockValidBlock);
            spectator.setInput('class', 'my-class');
            spectator.setInput('style', 'color: red');
            spectator.detectChanges();

            const wrapper = spectator.query('.my-class') as HTMLElement;
            expect(wrapper).toBeTruthy();
            expect(wrapper.style.color).toBe('red');
        });
    });

    describe('asLevel helper', () => {
        it('should stringify a numeric level', () => {
            expect(component.asLevel(6)).toBe('6');
        });

        it('should pass through a string level', () => {
            expect(component.asLevel('2')).toBe('2');
        });

        it('should default to "1" when undefined', () => {
            expect(component.asLevel(undefined)).toBe('1');
        });
    });
});
