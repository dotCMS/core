import { render, screen } from '@testing-library/react';

import { Container } from '@dotcms/react/next/components/Container/Container';
import { DotCMSPageContext } from '@dotcms/react/next/contexts/DotCMSPageContext';
import * as utils from '@dotcms/react/next/utils';

import { EMPTY_PAGE_ASSET, MOCK_CONTAINER, MOCK_PAGE_ASSET, MOCK_CONTAINER_DATA } from '../mock';

jest.mock('@dotcms/react/next/components/Contentlet/Contentlet', () => ({
    Contentlet: ({ contentlet }: { contentlet: any }) => (
        <div data-testid="mock-contentlet">{contentlet.identifier}</div>
    )
}));

jest.mock('@dotcms/react/next/utils', () => ({
    getContainersData: jest.fn(),
    getDotContainerAttributes: jest.fn(),
    getContentletsInContainer: jest.fn()
}));

describe('Container', () => {
    const getContainersDataMock = utils.getContainersData as jest.Mock;
    const getContentletsInContainerMock = utils.getContentletsInContainer as jest.Mock;

    const renderWithContext = (component: React.ReactNode, contextValue = {}) => {
        return render(
            <DotCMSPageContext.Provider
                value={{
                    pageAsset: MOCK_PAGE_ASSET,
                    mode: 'production',
                    ...contextValue
                }}>
                {component}
            </DotCMSPageContext.Provider>
        );
    };

    beforeEach(() => {
        jest.spyOn(utils, 'getDotContainerAttributes').mockReturnValue({
            'data-dot-object': 'container',
            'data-dot-identifier': 'test-container-id',
            'data-dot-accept-types': 'test-accept-types',
            'data-max-contentlets': '10',
            'data-dot-uuid': 'test-uuid',
            'data-testid': 'dot-container'
        });
        getContainersDataMock.mockReturnValue(MOCK_CONTAINER_DATA);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('WITH CONTENT', () => {
        beforeEach(() =>
            getContentletsInContainerMock.mockReturnValue([
                { identifier: 'contentlet-1' },
                { identifier: 'contentlet-2' }
            ])
        );

        test('should contentlets when container has content', () => {
            renderWithContext(<Container container={MOCK_CONTAINER} />);
            const contentlets = screen.getAllByTestId('mock-contentlet');
            expect(contentlets).toHaveLength(2);
        });
    });

    describe('EMPTY CONTAINER', () => {
        beforeEach(() => getContentletsInContainerMock.mockReturnValue([]));

        test('should show empty message when container has no contentlets', () => {
            const { container } = renderWithContext(<Container container={MOCK_CONTAINER} />, {
                dotCMSPageAsset: EMPTY_PAGE_ASSET
            });

            const emptyContainerMessage = container.querySelector(
                '[data-testid="empty-container-message"]'
            );

            expect(emptyContainerMessage).toBeDefined();
        });

        test('should show empty container with styles when container has no contentlets and is in dev mode', () => {
            const { container } = renderWithContext(<Container container={MOCK_CONTAINER} />, {
                pageAsset: EMPTY_PAGE_ASSET,
                mode: 'development'
            });

            const emptyContainerMessage = container.querySelector(
                '[data-testid="empty-container-message"]'
            );

            const parentElement = emptyContainerMessage?.parentElement;

            expect(emptyContainerMessage).toBeDefined();
            expect(parentElement?.style.backgroundColor).toBe('rgb(236, 240, 253)');
            expect(parentElement?.style.display).toBe('flex');
            expect(parentElement?.style.justifyContent).toBe('center');
            expect(parentElement?.style.alignItems).toBe('center');
        });

        test('should not show empty container with styles when container has no contentlets and is in production mode', () => {
            const { container } = renderWithContext(<Container container={MOCK_CONTAINER} />, {
                pageAsset: EMPTY_PAGE_ASSET,
                mode: 'production'
            });

            const emptyContainerMessage = container.querySelector(
                '[data-testid="empty-container-message"]'
            );

            expect(emptyContainerMessage).toBeNull();
        });
    });

    describe('CONTAINER NOT FOUND', () => {
        beforeEach(() => {
            getContainersDataMock.mockReturnValue(null);
        });

        test('should show ContainerNotFound in dev mode when container is not found', () => {
            const { container } = renderWithContext(<Container container={MOCK_CONTAINER} />, {
                pageAsset: MOCK_PAGE_ASSET,
                mode: 'development'
            });

            const containerNotFound = container.querySelector(
                '[data-testid="container-not-found"]'
            );
            expect(containerNotFound).toBeDefined();
        });

        test('should not render ContainerNotFound in production when container is not found', () => {
            const { container } = renderWithContext(<Container container={MOCK_CONTAINER} />, {
                pageAsset: MOCK_PAGE_ASSET,
                mode: 'production'
            });

            const containerNotFound = container.querySelector(
                '[data-testid="container-not-found"]'
            );
            expect(containerNotFound).toBeNull();
        });
    });
});
