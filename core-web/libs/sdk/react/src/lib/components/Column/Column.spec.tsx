import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import { Column } from './Column';

jest.mock('../Container/Container', () => {
    return {
        Container: ({ containerRef }) => (
            <div data-testid="mockContainer">{containerRef.identifier}</div>
        )
    };
});

describe('Column', () => {
    const mockColumnData = {
        width: 6, // Adjust as needed
        leftOffset: 3, // Adjust as needed
        containers: [
            { identifier: 'Container1', uuid: 'unique-id-1' },
            { identifier: 'Container2', uuid: 'unique-id-2' }
            // Add more containers as needed for your test
        ]
    };

    beforeEach(() => {
        render(<Column column={mockColumnData} />);
    });

    it('applies the correct width and start classes based on props', () => {
        const columnElement = screen.getByTestId('column');
        expect(columnElement).toHaveClass('col-span-6');
        expect(columnElement).toHaveClass('col-start-3');
    });

    it('applies the correct data attr', () => {
        expect(screen.getByTestId('column')).toHaveAttribute('data-dot', 'column');
    });

    it('renders the correct number of containers', () => {
        const containers = screen.getAllByTestId('mockContainer');
        expect(containers.length).toBe(mockColumnData.containers.length);
    });

    it('passes the correct props to each Container', () => {
        mockColumnData.containers.forEach((container) => {
            expect(screen.getByText(container.identifier)).toBeInTheDocument();
        });
    });
});
