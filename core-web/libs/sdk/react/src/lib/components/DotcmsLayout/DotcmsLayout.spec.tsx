import { render, screen } from '@testing-library/react';

import '@testing-library/jest-dom';

import { DotcmsLayout } from './DotcmsLayout';

import { PageProviderContext } from '../PageProvider/PageProvider';

// Mock the custom hook and components
jest.mock('../../hooks/useEventHandlers', () => ({
    useEventHandlers: jest.fn()
}));

jest.mock('../Row/Row', () => {
    const { forwardRef } = jest.requireActual('react');

    return {
        Row: forwardRef(({ children }, ref) => (
            <div data-testid="mockRow" ref={ref}>
                {children}
            </div>
        ))
    };
});

jest.mock('../PageProvider/PageProvider', () => {
    return {
        PageProvider: ({ children }) => <div data-testid="mockPageProvider">{children}</div>
    };
});

export const mockEntity: PageProviderContext = {
    layout: {
        header: true,
        footer: true,
        body: {
            rows: [
                {
                    columns: [
                        {
                            width: 6,
                            leftOffset: 3,
                            containers: [
                                {
                                    identifier: 'container-1',
                                    uuid: 'uuid-1'
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    },
    containers: {
        'container-1': {
            container: {
                path: 'path/to/container',
                identifier: 'container-1'
            },
            containerStructures: [
                {
                    contentTypeVar: 'content-type-1'
                }
            ],
            contentlets: {
                'uuid-1': [
                    {
                        contentType: 'content-type-1',
                        identifier: 'contentlet-1',
                        title: 'Contentlet 1',
                        inode: 'inode-1'
                    }
                ]
            }
        }
    },
    page: { identifier: 'page-1', title: 'Hello Page' },
    viewAs: { language: { id: 'en' }, persona: { keyTag: 'persona-1' } },
    components: {}
};

describe('DotcmsLayout', () => {
    it('renders correctly with PageProvider and rows', () => {
        render(<DotcmsLayout entity={mockEntity} />);
        expect(screen.getAllByTestId('mockRow').length).toBe(mockEntity.layout.body.rows.length);
    });
});
