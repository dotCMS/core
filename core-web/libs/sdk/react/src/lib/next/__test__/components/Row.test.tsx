import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';

import { DotPageAssetLayoutRow } from '@dotcms/types';

import { Row } from '../../components/Row/Row';
import { MOCK_COLUMN } from '../mock';

const MOCK_ROW: DotPageAssetLayoutRow = {
    identifier: 1,
    styleClass: 'test-style-class',
    columns: [MOCK_COLUMN]
};

jest.mock('../../components/Column/Column', () => ({
    Column: ({ column }: any) => <div data-testid="mock-column">{column?.width}</div>
}));

describe('Row', () => {
    test('should render all columns', () => {
        render(<Row row={MOCK_ROW} />);
        const columns = screen.getAllByTestId('mock-column');
        expect(columns).toHaveLength(1);
    });

    test('should render multiple columns', () => {
        const rowWithMultipleColumns: DotPageAssetLayoutRow = {
            ...MOCK_ROW,
            columns: [MOCK_COLUMN, MOCK_COLUMN, MOCK_COLUMN]
        };
        render(<Row row={rowWithMultipleColumns} />);
        const columns = screen.getAllByTestId('mock-column');
        expect(columns).toHaveLength(3);
    });

    test('should render a row without columns', () => {
        const rowWithoutColumns: DotPageAssetLayoutRow = {
            ...MOCK_ROW,
            columns: []
        };
        render(<Row row={rowWithoutColumns} />);
        const columns = screen.queryAllByTestId('mock-column');
        expect(columns).toHaveLength(0);
    });

    test('should have a container div with dot-row-container class', () => {
        const { container } = render(<Row row={MOCK_ROW} />);
        const dotRowContainer = container.querySelector('.dot-row-container') as HTMLElement;
        expect(dotRowContainer).toBeInTheDocument();
        expect(dotRowContainer).toHaveClass('dot-row-container');
    });

    test('should have inner div with data-dot-object attribute', () => {
        const { container } = render(<Row row={MOCK_ROW} />);
        const dotRow = container.querySelector('[data-dot-object="row"]') as HTMLElement;
        expect(dotRow).toBeInTheDocument();
        expect(dotRow).toHaveAttribute('data-dot-object', 'row');
    });

    describe('style class', () => {
        test('should have custom style class on container', () => {
            const { container } = render(<Row row={MOCK_ROW} />);
            const dotRowContainer = container.querySelector('.dot-row-container') as HTMLElement;

            expect(dotRowContainer).toHaveClass('dot-row-container');
            expect(dotRowContainer).toHaveClass('test-style-class');
        });

        test('should have dot-row-container class even when styleClass is empty', () => {
            const rowWithEmptyStyleClass: DotPageAssetLayoutRow = {
                ...MOCK_ROW,
                styleClass: ''
            };
            const { container } = render(<Row row={rowWithEmptyStyleClass} />);
            const dotRowContainer = container.querySelector('.dot-row-container') as HTMLElement;

            expect(dotRowContainer).toHaveClass('dot-row-container');
            expect(dotRowContainer).not.toHaveClass('test-style-class');
        });

        test('should have dot-row-container class even when styleClass is undefined', () => {
            const rowWithoutStyleClass: DotPageAssetLayoutRow = {
                identifier: 1,
                columns: [MOCK_COLUMN]
            };
            const { container } = render(<Row row={rowWithoutStyleClass} />);
            const dotRowContainer = container.querySelector('.dot-row-container') as HTMLElement;

            expect(dotRowContainer).toHaveClass('dot-row-container');
            expect(dotRowContainer).not.toHaveClass('test-style-class');
        });
    });
});
