import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import * as dotcmsClient from '@dotcms/client';

import { DotCMSLayoutBodyRenderer } from '../components/DotCMSLayoutBodyRenderer/DotCMSLayoutBodyRenderer';

const MOCK_PAGE = {
    layout: {
        body: {
            rows: [
                { id: 1, content: 'Row 1 Content' },
                { id: 2, content: 'Row 2 Content' }
            ]
        }
    }
} as any;

jest.mock('../components/Row/Row', () => ({
    Row: ({ row }: { row: any }) => <div data-testid="row">Mocked Row - {row.content}</div>
}));

describe('DotCMSLayoutBodyRenderer', () => {
    describe('With valid layout.body', () => {
        test('should render all rows when the page has a valid layout.body', () => {
            render(<DotCMSLayoutBodyRenderer page={MOCK_PAGE} mode="production" />);

            const rows = screen.getAllByTestId('row');
            expect(rows).toHaveLength(2);
            expect(rows[0]).toHaveTextContent('Row 1 Content');
            expect(rows[1]).toHaveTextContent('Row 2 Content');
        });
    });

    describe('With missing layout.body', () => {
        const MOCK_INVALID_PAGE = {} as any;
        const MESSAGE_WARNING = 'Missing required layout.body property in page';
        let consoleSpy: jest.SpyInstance;
        let isInsideEditorSpy: jest.SpyInstance;
        beforeEach(() => {
            consoleSpy = jest.spyOn(console, 'warn').mockImplementation(() => MESSAGE_WARNING);
            isInsideEditorSpy = jest.spyOn(dotcmsClient, 'isInsideEditor');
        });

        afterEach(() => jest.restoreAllMocks());

        test('should log a warning if the page is missing layout.body', () => {
            render(<DotCMSLayoutBodyRenderer page={MOCK_INVALID_PAGE} mode="production" />);
            expect(consoleSpy).toHaveBeenCalledWith(MESSAGE_WARNING);
        });

        test('should displays an error message in development mode', () => {
            render(<DotCMSLayoutBodyRenderer page={MOCK_INVALID_PAGE} mode="development" />);
            const errorMessage = screen.getByTestId('error-message');
            expect(errorMessage).toBeInTheDocument();
        });

        test('should display an error message in production mode if the page is inside the editor', () => {
            isInsideEditorSpy.mockReturnValue(true);

            render(<DotCMSLayoutBodyRenderer page={MOCK_INVALID_PAGE} mode="production" />);
            const errorMessage = screen.getByTestId('error-message');
            expect(errorMessage).toBeInTheDocument();
        });

        test('should not display an error message in production mode', () => {
            const { container } = render(
                <DotCMSLayoutBodyRenderer page={MOCK_INVALID_PAGE} mode="production" />
            );
            expect(container.innerHTML).toBe('');
        });
    });
});
