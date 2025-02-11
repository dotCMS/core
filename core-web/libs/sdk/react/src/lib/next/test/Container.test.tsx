import { render, screen } from '@testing-library/react';

import { Container } from '../components/Container/Container';
import { DotCMSPageContext } from '../contexts/DotCMSPageContext';
import { DotCMSColumnContainer, DotCMSPageAsset } from '../types';
import * as utils from '../utils';

jest.mock('../../Contentlet/Contentlet', () => ({
    Contentlet: ({ contentlet }: { contentlet: any }) => (
        <div data-testid="mock-contentlet">{contentlet.identifier}</div>
    )
}));

const mockContainer: DotCMSColumnContainer = {
    identifier: 'test-container-id',
    uuid: 'test-uuid',
    historyUUIDs: []
};

const mockPageAsset = {
    containers: {
        'test-container-id': {
            identifier: 'test-container-id',
            title: 'Test Container'
        }
    },
    contentlets: {
        'test-container-id': [{ identifier: 'contentlet-1' }, { identifier: 'contentlet-2' }]
    }
} as unknown as DotCMSPageAsset;

describe('Container', () => {
    const renderWithContext = (component: React.ReactNode, contextValue = {}) => {
        return render(
            <DotCMSPageContext.Provider
                value={{
                    pageAsset: mockPageAsset,
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
            'data-dot-identifier': 'test-container-id'
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    test('renders contentlets when container has content', () => {
        renderWithContext(<Container container={mockContainer} />);

        const contentlets = screen.getAllByTestId('mock-contentlet');
        expect(contentlets).toHaveLength(2);
        expect(contentlets[0]).toHaveTextContent('contentlet-1');
        expect(contentlets[1]).toHaveTextContent('contentlet-2');
    });

    test('shows empty message when container has no contentlets', () => {
        const emptyPageAsset = {
            ...mockPageAsset,
            contentlets: { 'test-container-id': [] }
        };

        renderWithContext(<Container container={mockContainer} />, {
            dotCMSPageAsset: emptyPageAsset
        });

        expect(screen.getByText('This container is empty.')).toBeInTheDocument();
    });

    test('renders ContainerNotFound in dev mode when container is not found', () => {
        const pageAssetWithoutContainer = {
            containers: {},
            contentlets: {}
        };

        renderWithContext(<Container container={mockContainer} />, {
            pageAsset: pageAssetWithoutContainer,
            mode: 'development'
        });

        expect(
            screen.getByText(/This container with identifier test-container-id was not found/)
        ).toBeInTheDocument();
    });

    test('does not render ContainerNotFound in production when container is not found', () => {
        const pageAssetWithoutContainer = {
            containers: {},
            contentlets: {}
        };

        renderWithContext(<Container container={mockContainer} />, {
            pageAsset: pageAssetWithoutContainer,
            mode: 'production'
        });

        expect(screen.queryByText(/This container with identifier/)).not.toBeInTheDocument();
    });

    test('applies empty container styles when container has no contentlets', () => {
        const emptyPageAsset = {
            ...mockPageAsset,
            contentlets: { 'test-container-id': [] }
        };

        renderWithContext(<Container container={mockContainer} />, {
            pageAsset: emptyPageAsset
        });

        const containerElement = screen.getByText('This container is empty.');
        expect(containerElement.parentElement).toHaveStyle({
            backgroundColor: '#ECF0FD',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center'
        });
    });
});
