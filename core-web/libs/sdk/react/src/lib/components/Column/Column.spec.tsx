import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

import * as uve from '@dotcms/uve';

import { Column } from './Column';

import { MockContextRender } from '../../mocks/mockPageContext';
import { ContainerProps } from '../Container/Container';

jest.mock('../Container/Container', () => {
    return {
        Container: ({ containerRef }: Partial<ContainerProps>) => (
            <div data-testid="mockContainer">{containerRef?.identifier}</div>
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
        ],
        styleClass: ''
    };

    describe('Column is on EDIT mmode editor', () => {
        beforeEach(() => {
            jest.spyOn(uve, 'getUVEState').mockReturnValue({
                mode: uve.UVE_MODE.EDIT
            });
            render(
                <MockContextRender
                    mockContext={{
                        UVEState: {
                            mode: uve.UVE_MODE.EDIT
                        }
                    }}>
                    <Column column={mockColumnData} />
                </MockContextRender>
            );
        });

        it('applies the correct width and start classes based on props', () => {
            const columnElement = screen.getByTestId('column');
            expect(columnElement).toHaveClass('col-end-9');
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

    describe('Column is on LIVE mode editor', () => {
        beforeEach(() => {
            jest.spyOn(uve, 'getUVEState').mockReturnValue(undefined);
            render(
                <MockContextRender
                    mockContext={{
                        UVEState: {
                            mode: uve.UVE_MODE.LIVE
                        }
                    }}>
                    <Column column={mockColumnData} />
                </MockContextRender>
            );
        });

        it('should not have dot attrs', () => {
            const columnElement = screen.queryByTestId('column');
            expect(columnElement).toBeNull();
        });
    });
    describe('Column is PREVIEW mode editor', () => {
        beforeEach(() => {
            jest.spyOn(uve, 'getUVEState').mockReturnValue(undefined);
            render(
                <MockContextRender
                    mockContext={{
                        UVEState: {
                            mode: uve.UVE_MODE.PREVIEW
                        }
                    }}>
                    <Column column={mockColumnData} />
                </MockContextRender>
            );
        });

        it('should not have dot attrs', () => {
            const columnElement = screen.queryByTestId('column');
            expect(columnElement).toBeNull();
        });
    });
});
