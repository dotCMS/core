import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import { Container } from './Container';

import { PageContext } from '../../contexts/PageContext';
import { mockPageContext } from '../../mocks/mockPageContext';

describe('Container', () => {
    // Mock data for your context and container

    const mockContainerRef = {
        identifier: 'container-1',
        uuid: '1',
        containers: []
    };

    it('renders NoContent component for unsupported content types', () => {
        const updatedContext = {
            ...mockPageContext,
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
