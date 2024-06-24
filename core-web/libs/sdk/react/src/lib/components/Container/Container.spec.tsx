import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import * as dotcmsClient from '@dotcms/client';

import { Container } from './Container';

import { MockContextRender, mockPageContext } from '../../mocks/mockPageContext';
import { getContainersData } from '../../utils/utils';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const getContainer = ({ containerRef, containers }: { containerRef: any; containers: any }) => {
    const { acceptTypes, maxContentlets, variantId, path } = getContainersData(
        containers,
        containerRef
    );

    return {
        acceptTypes,
        identifier: path ?? containerRef.identifier,
        maxContentlets,
        variantId,
        uuid: containerRef.uuid
    };
};

describe('Container', () => {
    // Mock data for your context and container
    jest.spyOn(dotcmsClient, 'isInsideEditor').mockReturnValue(true);

    describe('with contentlets', () => {
        const mockContainerRef = {
            identifier: 'container-1',
            uuid: '1',
            containers: []
        };

        it('renders NoComponent component for unsupported content types', () => {
            const updatedContext = {
                ...mockPageContext,
                isInsideEditor: true,
                components: {}
            };

            render(
                <MockContextRender mockContext={updatedContext}>
                    <Container containerRef={mockContainerRef} />
                </MockContextRender>
            );

            expect(screen.getByTestId('no-component')).toHaveTextContent(
                'No Component for content-type-1'
            );
        });

        it('render custom NoComponent component for unsetted content types', () => {
            const updatedContext = {
                ...mockPageContext,
                isInsideEditor: true,
                components: {
                    CustomNoComponent: () => (
                        <div data-testid="custom-no-component">Custom No Component</div>
                    )
                }
            };

            render(
                <MockContextRender mockContext={updatedContext}>
                    <Container containerRef={mockContainerRef} />
                </MockContextRender>
            );

            expect(screen.getByTestId('custom-no-component')).toHaveTextContent(
                'Custom No Component'
            );
        });

        it('should render contentlets with the right attributes', () => {
            const mockContainer = {
                identifier: 'container-1',
                uuid: '1',
                containers: []
            };

            const updatedContext = {
                ...mockPageContext,
                components: {},
                isInsideEditor: true
            };

            render(
                <MockContextRender mockContext={updatedContext}>
                    <Container containerRef={mockContainer} />
                </MockContextRender>
            );

            const container = getContainer({
                containerRef: mockContainer,
                containers: updatedContext.pageAsset.containers
            });

            const contentlet = screen.getByTestId('dot-contentlet');
            expect(contentlet).toHaveAttribute('data-dot-identifier', 'contentlet-1');
            expect(contentlet).toHaveAttribute('data-dot-title', 'Contentlet 1');
            expect(contentlet).toHaveAttribute('data-dot-inode', 'inode-1');
            expect(contentlet).toHaveAttribute('data-dot-on-number-of-pages', '1');
            expect(contentlet).toHaveAttribute('data-dot-basetype', 'base-type-1');
            expect(contentlet).toHaveAttribute('data-dot-container', JSON.stringify(container));
        });

        describe('without contentlets', () => {
            const mockContainerRef = {
                identifier: 'container-2',
                uuid: '2',
                containers: []
            };
            it('renders EmptyContainer component in editor mode', () => {
                const updatedContext = {
                    ...mockPageContext,
                    components: {},
                    isInsideEditor: true
                };
                render(
                    <MockContextRender mockContext={updatedContext}>
                        <Container containerRef={mockContainerRef} />
                    </MockContextRender>
                );

                expect(screen.getByTestId('dot-container')).toHaveTextContent(
                    'This container is empty.'
                );
            });

            it('dont render EmptyContainer component outside editor mode', () => {
                jest.spyOn(dotcmsClient, 'isInsideEditor').mockReturnValue(false);

                const updatedContext = {
                    ...mockPageContext,
                    components: {},
                    isInsideEditor: false
                };
                render(
                    <MockContextRender mockContext={updatedContext}>
                        <Container containerRef={mockContainerRef} />
                    </MockContextRender>
                );

                expect(screen.queryByTestId('dot-container')).toBeNull();
            });
        });
    });

    // Add tests for pointer events, dynamic component rendering, and other scenarios...
});
