import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { Component, Input } from '@angular/core';

import { BlockEditorNode, DotCMSBasicContentlet, UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

import { DotContentletBlock, NoComponentProvided } from './dot-contentlet.component';

import { CustomRenderer } from '../dotcms-block-editor-renderer.component';

@Component({
    selector: 'test-component',
    template: '<div data-testId="test-component">Test Component</div>'
})
class TestComponent {
    @Input() contentlet!: DotCMSBasicContentlet;
}

// Mock data
const mockData: BlockEditorNode['attrs'] = {
    data: {
        contentType: 'test'
    }
};

const mockCustomRenderers: CustomRenderer = {
    test: Promise.resolve(TestComponent)
};

const MOCK_UVE_STATE_EDIT = {
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

// Test suite
describe('DotContentletBlock', () => {
    const getUVEStateMock = getUVEState as jest.Mock;

    let spectator: Spectator<DotContentletBlock>;
    const createComponent = createComponentFactory({
        component: DotContentletBlock,
        imports: [],
        mocks: [NoComponentProvided],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                attrs: mockData,
                customRenderers: mockCustomRenderers
            }
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should log a message if no data is provided', () => {
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {
            /* empty */
        });
        spectator.setInput('attrs', undefined);
        spectator.detectChanges();
        expect(consoleSpy).toHaveBeenCalledWith(
            '[DotCMSBlockEditorRenderer]: No data provided for Contentlet Block. Try to add a contentlet to the block editor. If the error persists, please contact the DotCMS support team.'
        );
    });

    it('should use NoComponentProvided in dev mode if no component is found', () => {
        jest.spyOn(console, 'error').mockImplementation(() => {
            /* empty */
        });
        jest.spyOn(console, 'warn').mockImplementation(() => {
            /* empty */
        });
        getUVEStateMock.mockReturnValue(MOCK_UVE_STATE_EDIT);
        spectator.setInput('customRenderers', {});
        spectator.detectChanges();
        const unknownContentType = spectator.query(byTestId('no-component-provided'));
        expect(unknownContentType).toBeTruthy();
    });

    it('should log a warning and render nothing if no component is found', () => {
        const consoleSpy = jest.spyOn(console, 'warn').mockImplementation(() => {
            /* empty */
        });
        spectator.setInput('customRenderers', {});
        spectator.detectChanges();
        expect(consoleSpy).toHaveBeenCalledWith(
            '[DotCMSBlockEditorRenderer]: No matching component found for content type: test. Provide a custom renderer for this content type to fix this error.'
        );
        expect(spectator.query('ng-container')).toBeNull();
    });

    it('should render the component if it exists', async () => {
        spectator.detectChanges();

        await spectator.fixture.whenStable();

        const unknownContentType = spectator.query(byTestId('no-component-provided'));
        expect(unknownContentType).toBeFalsy();

        spectator.detectChanges();

        const testComponent = spectator.query(byTestId('test-component'));
        expect(testComponent).toBeTruthy();
    });
});
