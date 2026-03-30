import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import { getDotContentletAttributes } from '@dotcms/uve/internal';

import { Contentlet, CONTENTLET_CLASS } from '../../components/Contentlet/Contentlet';
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
    CUSTOM_NO_COMPONENT: 'CustomNoComponent',
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
        expect(containerDiv).toHaveClass(CONTENTLET_CLASS);

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

        // UVE attributes should always be called
        expect(getDotContentletAttributesMock).toHaveBeenCalledWith(dummyContentlet, 'container-1');

        const containerDiv = container.querySelector(
            '[data-dot-object="contentlet"]'
        ) as HTMLElement;
        expect(containerDiv.className).toBe(CONTENTLET_CLASS);
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

    test('should always apply UVE attributes in production', () => {
        const contextValue = {
            mode: 'production',
            userComponents: {}
        };

        renderContentlet(contextValue, { contentlet: dummyContentlet, container: 'container-1' });

        // UVE attributes should always be called
        expect(getDotContentletAttributesMock).toHaveBeenCalledWith(dummyContentlet, 'container-1');
    });

    test('should always apply UVE attributes even when isDevMode is false', () => {
        const contextValue = {
            mode: 'production',
            userComponents: {}
        };

        renderContentlet(contextValue, { contentlet: dummyContentlet, container: 'container-1' });

        // UVE attributes should always be called
        expect(getDotContentletAttributesMock).toHaveBeenCalledWith(dummyContentlet, 'container-1');
    });

    test('should always apply UVE attributes in development mode (UVE)', () => {
        const contextValue = {
            mode: 'development',
            userComponents: {}
        };

        renderContentlet(contextValue, { contentlet: dummyContentlet, container: 'container-1' });

        // UVE attributes should always be called
        expect(getDotContentletAttributesMock).toHaveBeenCalledWith(dummyContentlet, 'container-1');
    });

    test('should render slot node when present for contentlet identifier', () => {
        const slotNode = <div data-testid="slot-node">Server rendered slot</div>;
        const contextValue = {
            mode: 'production',
            userComponents: {},
            slots: { 'slot-id': slotNode }
        };

        renderContentlet(contextValue, {
            contentlet: { ...dummyContentlet, identifier: 'slot-id' },
            container: 'container-1'
        });

        expect(screen.getByTestId('slot-node')).toBeInTheDocument();
        expect(screen.getByTestId('slot-node')).toHaveTextContent('Server rendered slot');
    });

    test('should fall back to userComponents when no slot exists for identifier', () => {
        const CustomComponentMock = () => <div data-testid="custom">Custom</div>;
        const contextValue = {
            mode: 'production',
            userComponents: { 'test-type': CustomComponentMock },
            slots: { 'other-id': <div>Other slot</div> }
        };

        renderContentlet(contextValue, {
            contentlet: { ...dummyContentlet, identifier: 'no-slot-id' },
            container: 'container-1'
        });

        expect(screen.getByTestId('custom')).toBeInTheDocument();
    });

    test('should not crash when slots is omitted from context', () => {
        const contextValue = {
            mode: 'production',
            userComponents: {}
        };

        expect(() =>
            renderContentlet(contextValue, {
                contentlet: { ...dummyContentlet, identifier: 'some-id' },
                container: 'container-1'
            })
        ).not.toThrow();

        expect(screen.getByTestId('fallback')).toBeInTheDocument();
    });
});
