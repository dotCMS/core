import { render, screen } from '@testing-library/react';

import { Row } from './Row';

import { DotPageAssetLayoutColumn, DotPageAssetLayoutRow } from '../../types';

const mockColumn: DotPageAssetLayoutColumn = {
    left: 0,
    width: 6,
    leftOffset: 2,
    preview: false,
    widthPercent: 50,
    styleClass: 'custom-column-class',
    containers: [
        {
            identifier: 'container-1',
            uuid: 'uuid-1',
            historyUUIDs: []
        },
        {
            identifier: 'container-2',
            uuid: 'uuid-2',
            historyUUIDs: []
        }
    ]
};

jest.mock('../../Column/Column', () => ({
    Column: ({ column }: any) => <div data-testid="mock-column">{column?.width}</div>
}));

describe('Row', () => {
    const mockRow: DotPageAssetLayoutRow = {
        identifier: 1,
        columns: [mockColumn]
    };

    it('renders with correct style class', () => {
        render(<Row row={mockRow} />);
        expect(screen.getByRole('generic')).toHaveClass('test-style-class');
    });

    it('renders all columns', () => {
        render(<Row row={mockRow} />);
        const columns = screen.getAllByTestId('mock-column');
        expect(columns).toHaveLength(2);
    });

    it('renders with data-dot attribute', () => {
        render(<Row row={mockRow} />);
        const rowElement = document.querySelector('[data-dot="row"]');
        expect(rowElement).toBeInTheDocument();
    });

    it('renders with container class', () => {
        render(<Row row={mockRow} />);
        expect(screen.getByRole('generic').querySelector('.container')).toBeInTheDocument();
    });
});
