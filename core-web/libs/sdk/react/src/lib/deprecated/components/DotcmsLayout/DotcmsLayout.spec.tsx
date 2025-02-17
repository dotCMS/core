import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import { ElementRef, ForwardRefExoticComponent } from 'react';

import { DotcmsLayout } from './DotcmsLayout';

import { mockPageContext } from '../../mocks/mockPageContext';

jest.mock('../Row/Row', () => {
    const { forwardRef } = jest.requireActual('react');

    return {
        Row: forwardRef(
            (
                { children }: { children: JSX.Element },
                ref: ElementRef<ForwardRefExoticComponent<unknown>>
            ) => (
                <div data-testid="mockRow" ref={ref}>
                    {children}
                </div>
            )
        )
    };
});

jest.mock('../PageProvider/PageProvider', () => {
    return {
        PageProvider: ({ children }: { children: JSX.Element }) => (
            <div data-testid="mockPageProvider">{children}</div>
        )
    };
});

describe('DotcmsLayout', () => {
    it('renders correctly with PageProvider and rows', () => {
        render(
            <DotcmsLayout
                pageContext={{ ...mockPageContext, isInsideEditor: true }}
                config={{ pathname: 'some-url' }}
            />
        );
        expect(screen.getAllByTestId('mockRow').length).toBe(
            mockPageContext.pageAsset.layout.body.rows.length
        );
    });
});
