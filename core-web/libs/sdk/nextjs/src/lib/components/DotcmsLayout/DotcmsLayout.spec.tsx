import { render, screen } from '@testing-library/react';
import { ElementRef, ReactNode } from 'react';

import { RowProps } from '@dotcms/react';
import { mockPageContext } from '@dotcms/react/mocks';

import { DotcmsLayout } from './DotcmsLayout';

import '@testing-library/jest-dom';

// Mocking the necessary modules and hooks
jest.mock('next/navigation', () => {
    const router = {
        refresh: jest.fn()
    };

    return {
        useRouter: jest.fn().mockReturnValue(router),
        usePathname: jest.fn().mockReturnValue('/')
    };
});

jest.mock('@dotcms/react', () => {
    const { forwardRef } = jest.requireActual('react');

    const MockRow = forwardRef(
        (props: RowProps, ref: ElementRef<React.ExoticComponent<unknown>>) => (
            <div ref={ref} data-testid="mockRow" {...props}></div>
        )
    );

    const MockPageProvider = ({ children }: { children: ReactNode }) => (
        <div data-testid="mockPageProvider">{children}</div>
    );

    return {
        Row: MockRow,
        usePageEditor: jest.fn().mockReturnValue({
            rowsRef: {
                current: []
            },
            isInsideEditor: true
        }),
        PageProvider: MockPageProvider
    };
});

// Mocking window.postMessage
const postMessageMock = jest.fn();
Object.defineProperty(window, 'parent', {
    value: { postMessage: postMessageMock }
});

describe('DotcmsLayout', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should render correctly with PageProvider and rows', () => {
        render(<DotcmsLayout entity={mockPageContext} />);
        expect(screen.getAllByTestId('mockRow').length).toBe(
            mockPageContext.layout.body.rows.length
        );
    });
});
