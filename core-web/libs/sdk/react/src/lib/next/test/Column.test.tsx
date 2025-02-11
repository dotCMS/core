import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import { Column } from '../components/Column/Column';
import { DotPageAssetLayoutColumn } from '../types';

const MOCK_COLUMN: DotPageAssetLayoutColumn = {
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

jest.mock('../components/Container/Container', () => ({
    Container: ({ container }: any) => (
        <div data-testid="mock-container" data-container-id={container.identifier}>
            Mock Container
        </div>
    )
}));

describe('Column', () => {
    test('should render all containers', () => {
        render(<Column column={MOCK_COLUMN} />);

        const containers = screen.getAllByTestId('mock-container');

        expect(containers).toHaveLength(2);
        expect(containers[0]).toHaveAttribute('data-container-id', 'container-1');
        expect(containers[1]).toHaveAttribute('data-container-id', 'container-2');
    });

    test('should render a column without containers', () => {
        const emptyColumn = {
            ...MOCK_COLUMN,
            containers: []
        };

        render(<Column column={emptyColumn} />);

        const containers = screen.queryAllByTestId('mock-container');
        expect(containers).toHaveLength(0);
    });

    test('should render a container wrapper with correct grid classes', () => {
        const { container } = render(<Column column={MOCK_COLUMN} />);

        const containerWrapper = container.querySelector('[data-dot="column"]');

        const startClass = `col-start-${MOCK_COLUMN.leftOffset}`;
        const endClass = `col-end-${MOCK_COLUMN.width + MOCK_COLUMN.leftOffset}`;

        expect(containerWrapper).toHaveClass(startClass);
        expect(containerWrapper).toHaveClass(endClass);
    });

    test('should render a container with custom style class', () => {
        const { container } = render(<Column column={MOCK_COLUMN} />);

        const containerWrapper = container.querySelector('[data-dot="column"]');
        const columnElement = containerWrapper?.children[0];

        expect(columnElement).toHaveClass('custom-column-class');
    });

    test('should render a container without custom style clas', () => {
        const CUSTOM_MOCK_COLUMN_WITHOUT_STYLE_CLASS = {
            ...MOCK_COLUMN,
            styleClass: ''
        };

        const { container } = render(<Column column={CUSTOM_MOCK_COLUMN_WITHOUT_STYLE_CLASS} />);

        const containerWrapper = container.querySelector('[data-dot="column"]');
        const columnElement = containerWrapper?.children[0];

        expect(columnElement).not.toHaveClass('custom-column-class');
    });
});
