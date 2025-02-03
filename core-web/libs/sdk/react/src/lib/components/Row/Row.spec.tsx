import { render, screen } from '@testing-library/react';

import { UVE_MODE } from '@dotcms/uve';

import { Row } from './Row';

import { MockContextRender } from '../../mocks/mockPageContext';
import { DotCMSPageContext } from '../../models';
import { ColumnProps } from '../Column/Column';

import '@testing-library/jest-dom';

jest.mock('../Column/Column', () => {
    return {
        Column: ({ column }: ColumnProps) => (
            <div data-testid="mockColumn">{JSON.stringify(column)}</div>
        )
    };
});

describe('Row', () => {
    const mockRowData: DotCMSPageContext['pageAsset']['layout']['body']['rows'][0] = {
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

    describe('row is on EDIT mode editor', () => {
        beforeEach(() => {
            render(
                <MockContextRender mockContext={{ UVEState: { mode: UVE_MODE.EDIT } }}>
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
    describe('row is on LIVE mode editor', () => {
        beforeEach(() => {
            render(
                <MockContextRender
                    mockContext={{
                        UVEState: {
                            mode: UVE_MODE.LIVE
                        }
                    }}>
                    <Row row={mockRowData} />
                </MockContextRender>
            );
        });

        it('should not have dot attr', () => {
            expect(screen.queryByTestId('row')).toBeNull();
        });
    });

    describe('row is on PREVIEW mode editor', () => {
        beforeEach(() => {
            render(
                <MockContextRender
                    mockContext={{
                        UVEState: {
                            mode: UVE_MODE.PREVIEW
                        }
                    }}>
                    <Row row={mockRowData} />
                </MockContextRender>
            );
        });

        it('should not have dot attr', () => {
            expect(screen.queryByTestId('row')).toBeNull();
        });
    });
});
