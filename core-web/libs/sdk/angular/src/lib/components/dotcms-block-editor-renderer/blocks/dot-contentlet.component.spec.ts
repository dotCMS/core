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
const mockData: BlockEditorNode = {
    type: 'dotContent',
    attrs: {
        data: {
            contentType: 'test'
        }
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
                node: mockData,
                customRenderers: mockCustomRenderers
            }
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should create the component', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should log a message if no data is provided', () => {
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {
            /* empty */
        });
        spectator.setInput('node', undefined);
        spectator.detectChanges();
        expect(consoleSpy).toHaveBeenCalledWith(
            '[DotCMSBlockEditorRenderer]: No data provided for Contentlet Block. Try to add a contentlet to the block editor. If the error persists, please contact the DotCMS support team.'
        );
    });

    it('should log a message if node has no attrs data', () => {
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {
            /* empty */
        });
        spectator.setInput('node', { type: 'dotContent' });
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

    it('should not show NoComponentProvided in non-dev mode if no component is found', () => {
        jest.spyOn(console, 'warn').mockImplementation(() => {
            /* empty */
        });
        getUVEStateMock.mockReturnValue(null);
        spectator.setInput('customRenderers', {});
        spectator.detectChanges();
        const unknownContentType = spectator.query(byTestId('no-component-provided'));
        expect(unknownContentType).toBeFalsy();
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

    it('should pass contentlet data to the rendered component', async () => {
        const mockContentletData = {
            contentType: 'test',
            identifier: 'test-id',
            title: 'Test Title'
        };

        spectator.setInput('node', {
            type: 'dotContent',
            attrs: {
                data: mockContentletData
            }
        });

        spectator.detectChanges();
        await spectator.fixture.whenStable();
        spectator.detectChanges();

        const testComponent = spectator.query(byTestId('test-component'));
        expect(testComponent).toBeTruthy();
    });

    it('should handle missing contentType gracefully', () => {
        const consoleSpy = jest.spyOn(console, 'warn').mockImplementation(() => {
            /* empty */
        });

        spectator.setInput('node', {
            type: 'dotContent',
            attrs: {
                data: {}
            }
        });

        spectator.detectChanges();

        expect(consoleSpy).toHaveBeenCalledWith(
            '[DotCMSBlockEditorRenderer]: No matching component found for content type: . Provide a custom renderer for this content type to fix this error.'
        );
    });

    it('should update contentComponent when customRenderers change', () => {
        spectator.detectChanges();

        expect(spectator.component.contentComponent).toBe(mockCustomRenderers['test']);

        const newRenderers: CustomRenderer = {
            test: Promise.resolve(TestComponent),
            newType: Promise.resolve(TestComponent)
        };

        spectator.setInput('customRenderers', newRenderers);
        spectator.component.ngOnInit();

        expect(spectator.component.contentComponent).toBe(newRenderers['test']);
    });
});

describe('NoComponentProvided', () => {
    let spectator: Spectator<NoComponentProvided>;
    const createComponent = createComponentFactory({
        component: NoComponentProvided
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create the component', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should render with default contentType', () => {
        spectator.detectChanges();
        const element = spectator.query(byTestId('no-component-provided'));
        expect(element).toBeTruthy();
        expect(element?.textContent).toContain('Unknown');
    });

    it('should render with provided contentType', () => {
        spectator.setInput('contentType', 'MyCustomType');
        spectator.detectChanges();
        const element = spectator.query(byTestId('no-component-provided'));
        expect(element).toBeTruthy();
        expect(element?.textContent).toContain('MyCustomType');
    });

    it('should have correct styling', () => {
        spectator.detectChanges();
        const element = spectator.query(byTestId('no-component-provided')) as HTMLElement;
        expect(element).toBeTruthy();
        expect(element.style.backgroundColor).toBe('rgb(255, 250, 240)');
        // Border color can be returned as hex or rgb depending on browser
        expect(element.style.border).toMatch(/1px solid (#ed8936|rgb\(237, 137, 54\))/);
    });

    it('should contain link to documentation', () => {
        spectator.detectChanges();
        const link = spectator.query('a') as HTMLAnchorElement;
        expect(link).toBeTruthy();
        expect(link.href).toBe('https://dev.dotcms.com/docs/block-editor');
        expect(link.target).toBe('_blank');
        expect(link.rel).toBe('noopener noreferrer');
    });
});
