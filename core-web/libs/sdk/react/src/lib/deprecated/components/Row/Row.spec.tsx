import { render, screen } from '@testing-library/react';

import { Row } from './Row';

import { MockContextRender } from '../../mocks/mockPageContext';
import { ColumnProps } from '../Column/Column';
import { DotCMSPageContext } from '../PageProvider/PageProvider';

import '@testing-library/jest-dom';

jest.mock('../Column/Column', () => {
    return {
        Column: ({ column }: ColumnProps) => (
            <div data-testid="mockColumn">{JSON.stringify(column)}</div>
        )
    };
});

describe('Row', () => {
    const mockRowData: DotCMSPageContext['layout']['body']['rows'][0] = {
        columns: [
            {
                width: 60,
                leftOffset: 2,
                containers: [
                    {
                        identifier: '123',
                        uuid: '1'
                    }
                ],
                styleClass: ''
            },
            {
                width: 20,
                leftOffset: 0,
                containers: [
                    {
                        identifier: '456',
                        uuid: '2'
                    }
                ],
                styleClass: ''
            }
        ],
        styleClass: ''
    };

    describe('row is inside editor', () => {
        beforeEach(() => {
            render(
                <MockContextRender mockContext={{ isInsideEditor: true }}>
                    <Row row={mockRowData} />
                </MockContextRender>
            );
        });

        it('should set the data-dot attribute', () => {
            expect(screen.getByTestId('row')).toHaveAttribute('data-dot', 'row');
        });

        it('renders the correct number of mock columns', () => {
            const mockColumns = screen.getAllByTestId('mockColumn');
            expect(mockColumns.length).toBe(mockRowData.columns.length);
        });

        it('passes the correct props to each mock Column', () => {
            mockRowData.columns.forEach((column, index) => {
                expect(screen.getAllByTestId('mockColumn')[index].innerHTML).toBe(
                    JSON.stringify(column)
                );
            });
        });

        it('renders the correct number of columns', () => {
            const columns = screen.getAllByTestId('mockColumn');
            expect(columns.length).toBe(mockRowData.columns.length);
        });
    });
    describe('row is not inside editor', () => {
        beforeEach(() => {
            render(
                <MockContextRender mockContext={{ isInsideEditor: false }}>
                    <Row row={mockRowData} />
                </MockContextRender>
            );
        });

        it('should not have dot attr', () => {
            expect(screen.queryByTestId('row')).toBeNull();
        });
    });
});
