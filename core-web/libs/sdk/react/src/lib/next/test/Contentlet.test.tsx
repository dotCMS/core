import '@testing-library/jest-dom/extend-expect';

import { render, screen } from '@testing-library/react';

import { Contentlet } from '../components/Contentlet/Contentlet';
import { DotCMSPageContext } from '../contexts/DotCMSPageContext';
import { useCheckVisibleContent } from '../hooks/useCheckVisibleContent';
import { getDotContentletAttributes } from '../utils';

jest.mock('./components/FallbackComponent', () => ({
    FallbackComponent: ({ contentlet }: any) => (
        <div data-testid="fallback">Fallback Component: {contentlet.contentType}</div>
    ),
    NoComponentType: () => <div>No Component</div>
}));

jest.mock('../../hooks/useCheckHaveContent', () => ({
    useCheckVisibleContent: jest.fn(() => false)
}));

jest.mock('./utils', () => ({
    getDotContentletAttributes: jest.fn(() => ({ 'data-custom': 'true' }))
}));

describe('Contentlet', () => {
    const dummyContentlet = { contentType: 'test-type', someField: 'value' };
    const renderContentlet = (contextValue: any, contentletProps: any) => {
        return render(
            <DotCMSPageContext.Provider value={contextValue}>
                <Contentlet {...contentletProps} />
            </DotCMSPageContext.Provider>
        );
    };

    beforeEach(() => {
        // Reset the mock value for useCheckVisibleContent before each test.
        (useCheckVisibleContent as jest.Mock).mockReturnValue(false);
        (getDotContentletAttributes as jest.Mock).mockClear();
    });

    it('renders fallback component when no custom component exists', () => {
        const contextValue = {
            isDevMode: true,
            customComponents: {}
        };

        renderContentlet(contextValue, { contentlet: dummyContentlet, container: 'container-1' });
        expect(screen.getByTestId('fallback')).toHaveTextContent('Fallback Component: test-type');

        const containerDiv = screen.getByTestId('fallback').parentElement;
        expect(containerDiv).toHaveAttribute('data-dot-object', 'contentlet');

        expect(containerDiv).toHaveStyle('min-height: 4rem');
        expect(getDotContentletAttributes).toHaveBeenCalledWith(dummyContentlet, 'container-1');
    });

    it('renders custom component when provided', () => {
        const CustomComponentMock = ({ someField }: any) => (
            <div data-testid="custom">Custom Component Rendered with {someField}</div>
        );

        const contextValue = {
            mode: 'development',
            userComponents: {
                'test-type': CustomComponentMock
            }
        };

        renderContentlet(contextValue, { contentlet: dummyContentlet, container: 'container-1' });
        expect(screen.getByTestId('custom')).toHaveTextContent(
            'Custom Component Rendered with value'
        );
    });

    it('applies empty style when isDevMode is false', () => {
        const contextValue = {
            mode: 'production',
            userComponents: {}
        };

        renderContentlet(contextValue, { contentlet: dummyContentlet, container: 'container-1' });
        expect(getDotContentletAttributes).not.toHaveBeenCalled();

        const containerDiv = document.querySelector('div[data-dot-object="contentlet"]');
        expect(containerDiv).toHaveStyle('');
    });

    it('does not apply minHeight style if useCheckVisibleContent returns true', () => {
        (useCheckVisibleContent as jest.Mock).mockReturnValue(true);

        const contextValue = {
            mode: 'development',
            userComponents: {}
        };

        renderContentlet(contextValue, { contentlet: dummyContentlet, container: 'container-1' });

        const containerDiv = document.querySelector('div[data-dot-object="contentlet"]');
        expect(containerDiv).not.toHaveStyle('min-height: 4rem');
    });
});
