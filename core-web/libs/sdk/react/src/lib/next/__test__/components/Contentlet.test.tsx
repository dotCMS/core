import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import {
    getAnalyticsContentletAttributes,
    getDotContentletAttributes,
    isDotAnalyticsActive
} from '@dotcms/uve/internal';

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
    getDotContentletAttributes: jest.fn(() => ({ 'data-dot-identifier': 'editor-id' })),
    getAnalyticsContentletAttributes: jest.fn(() => ({ 'data-dot-identifier': 'analytics-id' })),
    isDotAnalyticsActive: jest.fn(() => false),
    ANALYTICS_READY_EVENT: 'dotcms:analytics:ready',
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
    const getAnalyticsContentletAttributesMock = getAnalyticsContentletAttributes as jest.Mock;
    const isDotAnalyticsActiveMock = isDotAnalyticsActive as jest.Mock;

    beforeEach(() => {
        useCheckVisibleContentMock.mockReturnValue(false);
        isDotAnalyticsActiveMock.mockReturnValue(false);
        getDotContentletAttributesMock.mockClear();
        getAnalyticsContentletAttributesMock.mockClear();
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

    test('should not apply minHeight style if useCheckVisibleContent returns true', () => {
        useCheckVisibleContentMock.mockReturnValue(true);

        const contextValue = {
            mode: 'development',
            userComponents: {}
        };

        renderContentlet(contextValue, { contentlet: dummyContentlet, container: 'container-1' });

        const containerDiv = document.querySelector(`.${CONTENTLET_CLASS}`);
        expect(containerDiv).not.toHaveStyle('min-height: 4rem');
    });

    describe('edit mode (UVE)', () => {
        const contextValue = { mode: 'development', userComponents: {} };

        test('should emit the full editor attribute set and data-dot-object', () => {
            const { container } = renderContentlet(contextValue, {
                contentlet: dummyContentlet,
                container: 'container-1'
            });

            const el = container.querySelector(`.${CONTENTLET_CLASS}`) as HTMLElement;

            expect(getDotContentletAttributesMock).toHaveBeenCalledWith(
                dummyContentlet,
                'container-1'
            );
            expect(getAnalyticsContentletAttributesMock).not.toHaveBeenCalled();
            expect(el).toHaveAttribute('data-dot-object', 'contentlet');
            expect(el).toHaveAttribute('data-dot-identifier', 'editor-id');
        });
    });

    describe('live mode without analytics', () => {
        const contextValue = { mode: 'production', userComponents: {} };

        test('should not emit any data-dot-* attributes but keep the class', () => {
            const { container } = renderContentlet(contextValue, {
                contentlet: dummyContentlet,
                container: 'container-1'
            });

            const el = container.querySelector(`.${CONTENTLET_CLASS}`) as HTMLElement;

            expect(el).toBeInTheDocument();
            expect(el).toHaveClass(CONTENTLET_CLASS);
            expect(getDotContentletAttributesMock).not.toHaveBeenCalled();
            expect(getAnalyticsContentletAttributesMock).not.toHaveBeenCalled();

            const dotAttrs = el.getAttributeNames().filter((name) => name.startsWith('data-dot'));
            expect(dotAttrs).toEqual([]);
        });
    });

    describe('live mode with analytics active', () => {
        const contextValue = { mode: 'production', userComponents: {} };

        beforeEach(() => {
            isDotAnalyticsActiveMock.mockReturnValue(true);
        });

        test('should emit only the minimal analytics attributes, not the editor set', () => {
            const { container } = renderContentlet(contextValue, {
                contentlet: dummyContentlet,
                container: 'container-1'
            });

            const el = container.querySelector(`.${CONTENTLET_CLASS}`) as HTMLElement;

            expect(getAnalyticsContentletAttributesMock).toHaveBeenCalledWith(dummyContentlet);
            expect(getDotContentletAttributesMock).not.toHaveBeenCalled();
            expect(el).toHaveAttribute('data-dot-identifier', 'analytics-id');
            expect(el).not.toHaveAttribute('data-dot-object');
        });
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
