import { render, screen } from '@testing-library/react';

import { Column } from '../components/Column/Column';
import { DotPageAssetLayoutColumn } from '../types';

jest.mock('../../Container/Container', () => ({
    Container: ({ container }: any) => (
        <div data-testid="mock-container" data-container-id={container.identifier}>
            Mock Container
        </div>
    )
}));

describe('Column', () => {
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

    it('renders column with correct grid classes', () => {
        render(<Column column={mockColumn} />);

        const columnElement = screen.getByTestId('column');
        expect(columnElement).toHaveClass('col-start-2');
        expect(columnElement).toHaveClass('col-end-8');
    });

    it('applies custom style class to inner div', () => {
        render(<Column column={mockColumn} />);

        const innerDiv = screen.getByTestId('column').children[0];

        expect(innerDiv).toHaveClass('custom-column-class');
    });

    it('renders all containers', () => {
        render(<Column column={mockColumn} />);

        const containers = screen.getAllByTestId('mock-container');

        expect(containers).toHaveLength(2);
        expect(containers[0]).toHaveAttribute('data-container-id', 'container-1');
        expect(containers[1]).toHaveAttribute('data-container-id', 'container-2');
    });

    it('renders column without containers', () => {
        const emptyColumn = {
            ...mockColumn,
            containers: []
        };

        render(<Column column={emptyColumn} />);

        const containers = screen.queryAllByTestId('mock-container');
        expect(containers).toHaveLength(0);
    });

    it('renders column without style class', () => {
        const columnWithoutStyle = {
            ...mockColumn,
            styleClass: ''
        };

        render(<Column column={columnWithoutStyle} />);

        const innerDiv = screen.getByTestId('column').children[0];
        expect(innerDiv).not.toHaveClass('custom-column-class');
    });
});
