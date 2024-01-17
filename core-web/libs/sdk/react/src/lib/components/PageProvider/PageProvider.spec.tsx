import { render, screen } from '@testing-library/react';
import React from 'react';

import '@testing-library/jest-dom';
import { PageProvider } from './PageProvider';

import { PageContext } from '../../contexts/PageContext';

const MockChildComponent = () => {
    const context = React.useContext(PageContext);

    return <div data-testid="mockChild">{JSON.stringify(context?.page.title)}</div>;
};

describe('PageProvider', () => {
    const mockEntity = {
        page: {
            title: 'Test Page',
            identifier: 'test-page-1'
        }
        // ... add other context properties as needed
    };

    it('provides the context to its children', () => {
        render(
            <PageProvider entity={mockEntity}>
                <MockChildComponent />
            </PageProvider>
        );
        expect(screen.getByTestId('mockChild')).toHaveTextContent(mockEntity.page.title);
    });

    it('updates context when entity changes', () => {
        const { rerender } = render(
            <PageProvider entity={mockEntity}>
                <MockChildComponent />
            </PageProvider>
        );
        // Change the context
        const newEntity = {
            ...mockEntity,
            page: {
                ...mockEntity.page,
                title: 'Updated Test Page'
            }
        };
        rerender(
            <PageProvider entity={newEntity}>
                <MockChildComponent />
            </PageProvider>
        );
        expect(screen.getByTestId('mockChild')).toHaveTextContent(newEntity.page.title);
    });
});
