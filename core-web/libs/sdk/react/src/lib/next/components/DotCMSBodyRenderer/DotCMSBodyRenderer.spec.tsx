import { render, screen } from '@testing-library/react';
import { isInsideEditor } from '@dotcms/client';

import { DotCMSBodyRenderer } from './DotCMSBodyRenderer';
import { DotCMSPageAsset } from '../../types';

// Mock the @dotcms/client module
jest.mock('@dotcms/client', () => ({ isInsideEditor: jest.fn()}));

// Mock the Row component
jest.mock('./components/Row/Row', () => ({
    Row: ({ row }: { row: any }) => <div data-testid="mock-row">{row.identifier}</div>
}));

describe('DotCMSBodyRenderer', () => {
    const mockPageAsset = {
        layout: {
            body: {
                rows: [
                    { identifier: 'row1' },
                    { identifier: 'row2' }
                ]
            }
        }
    } as unknown as DotCMSPageAsset;

    beforeEach(() => {
        (isInsideEditor as jest.Mock).mockReset();
        (isInsideEditor as jest.Mock).mockReturnValue(false);
    });

    it('renders rows when page body is provided', () => {
        render(<DotCMSBodyRenderer dotCMSPageAsset={mockPageAsset} />);
        const rows = screen.getAllByTestId('mock-row');
        expect(rows).toHaveLength(2);
    });

    it('passes custom components through context', () => {
        const customComponents = {
            CustomComponent: () => <div>Custom Component</div>
        };
        render(
            <DotCMSBodyRenderer 
                dotCMSPageAsset={mockPageAsset}
                customComponents={customComponents}
            />
        );
        const rows = screen.getAllByTestId('mock-row');
        expect(rows).toHaveLength(2);
    });

    describe("when the page body is not defined", () => {
        const invalidPageAsset = {} as unknown as DotCMSPageAsset;

        it('shows warning message in dev mode when page body is not defined', () => {
            render(<DotCMSBodyRenderer dotCMSPageAsset={invalidPageAsset} devMode={true} />);
            expect(screen.getByText(/The page body is not defined/)).toBeInTheDocument();
        });

        it('shows warning message when inside editor and page body is not defined', () => {
            (isInsideEditor as jest.Mock).mockReturnValue(true);
            render(<DotCMSBodyRenderer dotCMSPageAsset={invalidPageAsset} />);
            expect(screen.getByText(/The page body is not defined/)).toBeInTheDocument();
        });

        it('returns null when page body is not defined and not in dev mode', () => {
            const { container } = render(<DotCMSBodyRenderer dotCMSPageAsset={invalidPageAsset} />);
            expect(container.firstChild).toBeNull();
        });

    });
});