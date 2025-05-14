import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import { UVE_MODE } from '@dotcms/types';
import * as dotcmsUVE from '@dotcms/uve';

import { DotCMSLayoutBody } from '../../components/DotCMSLayoutBody/DotCMSLayoutBody';
import { MOCK_PAGE_ASSET } from '../mock';

jest.mock('../../components/Row/Row', () => ({
    Row: ({ row }: { row: any }) => <div data-testid="row">Mocked Row - {row.content}</div>
}));

jest.mock('@dotcms/uve/internal', () => ({
    ...jest.requireActual('@dotcms/uve/internal'),
    DEVELOPMENT_MODE: 'development',
    PRODUCTION_MODE: 'production'
}));

describe('DotCMSLayoutBody', () => {
    describe('With valid layout.body', () => {
        test('should render all rows when the page has a valid layout.body', () => {
            render(<DotCMSLayoutBody page={MOCK_PAGE_ASSET} components={{}} mode="production" />);

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
        let getUVEStateSpy: jest.SpyInstance;
        beforeEach(() => {
            consoleSpy = jest.spyOn(console, 'warn').mockImplementation(() => MESSAGE_WARNING);
            getUVEStateSpy = jest.spyOn(dotcmsUVE, 'getUVEState');
        });

        afterEach(() => jest.restoreAllMocks());

        test('should log a warning if the page is missing layout.body', () => {
            render(<DotCMSLayoutBody page={MOCK_INVALID_PAGE} components={{}} mode="production" />);
            expect(consoleSpy).toHaveBeenCalledWith(MESSAGE_WARNING);
        });

        test('should displays an error message in development mode', () => {
            render(
                <DotCMSLayoutBody page={MOCK_INVALID_PAGE} components={{}} mode="development" />
            );

            const errorMessage = screen.getByTestId('error-message');
            expect(errorMessage).toBeInTheDocument();
        });

        test('should display an error message in production mode if the page is inside the editor', () => {
            getUVEStateSpy.mockReturnValue({ mode: UVE_MODE.EDIT });

            render(<DotCMSLayoutBody page={MOCK_INVALID_PAGE} components={{}} mode="production" />);
            const errorMessage = screen.getByTestId('error-message');
            expect(errorMessage).toBeInTheDocument();
        });

        test('should not display an error message in production mode', () => {
            const { container } = render(
                <DotCMSLayoutBody page={MOCK_INVALID_PAGE} components={{}} mode="production" />
            );
            expect(container.innerHTML).toBe('');
        });
    });
});
