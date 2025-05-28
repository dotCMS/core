import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';

import { BlockEditorNode } from '@dotcms/types';

import { DotContent } from '../../components/DotCMSBlockEditorRenderer/components/blocks/DotContent';
import * as hook from '../../hooks/useIsDevMode';

const mockCustomRenderers = {
    KnownContentType: () => <div data-testid="known-content-type">Known Content</div>
};

jest.mock('../../hooks/useIsDevMode', () => ({
    useIsDevMode: jest.fn().mockReturnValue(true)
}));

const mockNode: BlockEditorNode = {
    attrs: {
        data: {
            contentType: 'KnownContentType'
        }
    },
    type: 'contentlet'
};

describe('DotContent Component', () => {
    let useIsDevModeSpy: jest.SpyInstance<boolean>;

    beforeEach(() => {
        useIsDevModeSpy = jest.spyOn(hook, 'useIsDevMode');
    });

    it('should show the no data message when there is no data', () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {
            /* empty function */
        });
        render(<DotContent customRenderers={mockCustomRenderers} node={{} as BlockEditorNode} />);
        expect(consoleErrorSpy).toHaveBeenCalledWith(
            '[DotCMSBlockEditorRenderer]: No data provided for Contentlet Block. Try to add a contentlet to the block editor. If the error persists, please contact the DotCMS support team.'
        );
        consoleErrorSpy.mockRestore();
    });

    it('should call the UnknownContentType component in dev mode when there is no component', () => {
        const { container } = render(<DotContent customRenderers={{}} node={mockNode} />);
        const unknownContentType = container.querySelector(
            "div[data-testid='no-component-provided']"
        );
        expect(unknownContentType).toBeInTheDocument();
    });

    it('should show a warning and render nothing when there is no component in production mode', () => {
        useIsDevModeSpy.mockReturnValue(false);
        const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {
            /* empty function */
        });
        const { container } = render(<DotContent customRenderers={{}} node={mockNode} />);
        expect(consoleWarnSpy).toHaveBeenCalledWith(
            '[DotCMSBlockEditorRenderer]: No matching component found for content type: KnownContentType. Provide a custom renderer for this content type to fix this error.'
        );
        expect(container.firstChild).toBeNull();
        consoleWarnSpy.mockRestore();
    });

    it('should render the component when a matching component is found', () => {
        render(<DotContent customRenderers={mockCustomRenderers} node={mockNode} />);
        expect(screen.getByTestId('known-content-type')).toBeInTheDocument();
    });
});
