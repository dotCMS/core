import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import Container from './container';

import { PageContext, PageProviderContext } from '../page-provider/page-provider'; // Adjust the import path based on your file structure.

describe('Container', () => {
    // Mock data for your context and container
    const mockContext: PageProviderContext = {
        layout: {
            header: true,
            footer: true,
            body: {
                rows: [
                    {
                        columns: [
                            {
                                width: 6,
                                leftOffset: 3,
                                containers: [
                                    {
                                        identifier: 'container-1',
                                        uuid: 'uuid-1'
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        },
        containers: {
            'container-1': {
                container: {
                    path: 'path/to/container',
                    identifier: 'container-1'
                },
                containerStructures: [
                    {
                        contentTypeVar: 'content-type-1'
                    }
                ],
                contentlets: {
                    'uuid-1': [
                        {
                            contentType: 'content-type-1',
                            identifier: 'contentlet-1',
                            title: 'Contentlet 1',
                            inode: 'inode-1'
                        }
                    ]
                }
            }
        },
        page: { identifier: 'page-1', title: 'Hello Page' },
        viewAs: { language: { id: 'en' }, persona: { keyTag: 'persona-1' } },
        components: {}
    };

    const mockContainerRef = {
        identifier: 'container-1',
        uuid: '1',
        containers: []
    };

    it('renders NoContent component for unsupported content types', () => {
        const updatedContext = {
            ...mockContext,
            components: {}
        };

        render(
            <PageContext.Provider value={updatedContext}>
                <Container containerRef={mockContainerRef} />
            </PageContext.Provider>
        );

        expect(screen.getByTestId('no-component')).toHaveTextContent(
            'No Component for content-type-1'
        );
    });

    // Add tests for pointer events, dynamic component rendering, and other scenarios...
});
