import { render, screen } from '@testing-library/react';
import React from 'react';

import '@testing-library/jest-dom';
import { PageProvider } from './PageProvider';

import { PageContext } from '../../contexts/PageContext';

const MockChildComponent = () => {
    const context = React.useContext(PageContext);

    return <div data-testid="mockChild">{JSON.stringify(context?.pageAsset.page.title)}</div>;
};

describe('PageProvider', () => {
    const mockEntity = {
        pageAsset: {
            page: {
                title: 'Test Page',
                identifier: 'test-page-1'
            }
            // ... add other context properties as needed
        }
    };

    it('provides the context to its children', () => {
        render(
            <PageProvider pageContext={mockEntity}>
                <MockChildComponent />
            </PageProvider>
        );
        expect(screen.getByTestId('mockChild')).toHaveTextContent(mockEntity.pageAsset.page.title);
    });

    it('updates context when entity changes', () => {
        const { rerender } = render(
            <PageProvider pageContext={mockEntity}>
                <MockChildComponent />
            </PageProvider>
        );
        // Change the context
        const newEntity = {
            ...mockEntity,
            pageAsset: {
                ...mockEntity.pageAsset,
                page: {
                    ...mockEntity.pageAsset.page,
                    title: 'Updated Test Page'
                }
            }
        };
        rerender(
            <PageProvider pageContext={newEntity}>
                <MockChildComponent />
            </PageProvider>
        );
        expect(screen.getByTestId('mockChild')).toHaveTextContent(newEntity.pageAsset.page.title);
    });
});
