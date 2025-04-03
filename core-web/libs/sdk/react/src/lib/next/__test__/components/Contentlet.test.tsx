import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import { getDotContentletAttributes } from '@dotcms/uve/internal';

import { Contentlet } from '../../components/Contentlet/Contentlet';
import { DotCMSPageContext } from '../../contexts/DotCMSPageContext';
import { useCheckVisibleContent } from '../../hooks/useCheckVisibleContent';

jest.mock('../../components/FallbackComponent/FallbackComponent', () => ({
    FallbackComponent: ({ contentlet }: any) => (
        <div data-testid="fallback">Fallback Component: {contentlet.contentType}</div>
    ),
    NoComponentType: () => <div>No Component</div>
}));

jest.mock('../../hooks/useCheckVisibleContent', () => ({
    useCheckVisibleContent: jest.fn(() => false)
}));

jest.mock('@dotcms/uve/internal', () => ({
    getDotContentletAttributes: jest.fn(() => ({ 'data-custom': 'true' })),
    DEVELOPMENT_MODE: 'development',
    PRODUCTION_MODE: 'production'
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

    const useCheckVisibleContentMock = useCheckVisibleContent as jest.Mock;
    const getDotContentletAttributesMock = getDotContentletAttributes as jest.Mock;

    beforeEach(() => {
        useCheckVisibleContentMock.mockReturnValue(false);
        getDotContentletAttributesMock.mockClear();
    });

    test('should render fallback component when no custom component exists', () => {
        const contextValue = {
            mode: 'development',
            userComponents: {}
        };

        renderContentlet(contextValue, { contentlet: dummyContentlet, container: 'container-1' });
        expect(screen.getByTestId('fallback')).toHaveTextContent('Fallback Component: test-type');

        const containerDiv = screen.getByTestId('fallback').parentElement;
        expect(containerDiv).toHaveAttribute('data-dot-object', 'contentlet');

        expect(containerDiv).toHaveStyle('min-height: 4rem');
        expect(getDotContentletAttributesMock).toHaveBeenCalledWith(dummyContentlet, 'container-1');
    });

    test('should render custom component when provided', () => {
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

    test('should apply empty style when isDevMode is false', () => {
        const contextValue = {
            mode: 'production',
            userComponents: {}
        };

        const { container } = renderContentlet(contextValue, {
            contentlet: dummyContentlet,
            container: 'container-1'
        });

        expect(getDotContentletAttributes).not.toHaveBeenCalled();

        const containerDiv = container.querySelector(
            '[data-dot-object="contentlet"]'
        ) as HTMLElement;
        expect(containerDiv.className).toBe('');
    });

    test('should not apply minHeight style if useCheckVisibleContent returns true', () => {
        useCheckVisibleContentMock.mockReturnValue(true);

        const contextValue = {
            mode: 'development',
            userComponents: {}
        };

        renderContentlet(contextValue, { contentlet: dummyContentlet, container: 'container-1' });

        const containerDiv = document.querySelector('div[data-dot-object="contentlet"]');
        expect(containerDiv).not.toHaveStyle('min-height: 4rem');
    });
});
