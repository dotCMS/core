import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';

import { Row } from '@dotcms/react/next/components/Row/Row';
import { DotPageAssetLayoutRow } from '@dotcms/react/next/types';

import { MOCK_COLUMN } from '../mock';

const MOCK_ROW: DotPageAssetLayoutRow = {
    identifier: 1,
    styleClass: 'test-style-class',
    columns: [MOCK_COLUMN]
};

jest.mock('@dotcms/react/next/components/Column/Column', () => ({
    Column: ({ column }: any) => <div data-testid="mock-column">{column?.width}</div>
}));

describe('Row', () => {
    test('should render all columns', () => {
        render(<Row row={MOCK_ROW} />);
        const columns = screen.getAllByTestId('mock-column');
        expect(columns).toHaveLength(1);
    });

    test('should have a container div', () => {
        const { container } = render(<Row row={MOCK_ROW} />);
        expect(container.querySelector('.dot-row-container')).toBeInTheDocument();
    });

    describe('style class', () => {
        test('should have custom style class', () => {
            const { container } = render(<Row row={MOCK_ROW} />);
            const dotRow = container.querySelector('[data-dot-object="row"]') as HTMLElement;

            expect(dotRow).toHaveClass('test-style-class');
        });

        test('should have default style class', () => {
            const { container } = render(<Row row={MOCK_ROW} />);
            const dotRow = container.querySelector('[data-dot-object="row"]') as HTMLElement;

            expect(dotRow).toHaveClass('row');
        });

        test('should have a `dot-row-container` class in the wrapper div', () => {
            const { container } = render(<Row row={MOCK_ROW} />);
            const dotRowContainer = container.querySelector('.dot-row-container') as HTMLElement;
            expect(dotRowContainer).toBeDefined();
        });
    });
});
