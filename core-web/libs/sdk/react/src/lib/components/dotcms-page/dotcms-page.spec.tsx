import { render, screen } from '@testing-library/react';

import '@testing-library/jest-dom';
import { DotcmsPage } from './dotcms-page';

import { PageProviderContext } from '../page-provider/page-provider';

// Mock the custom hook and components
jest.mock('../../hooks/useEventHandlers', () => ({
    useEventHandlers: jest.fn()
}));

jest.mock('../row/row', () => {
    const { forwardRef } = jest.requireActual('react');

    return forwardRef((props, ref) => <div ref={ref} data-testid="mockRow"></div>);
});

jest.mock('../page-provider/page-provider', () => {
    return ({ children }) => <div data-testid="mockPageProvider">{children}</div>;
});

const mockEntity: PageProviderContext = {
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

describe('DotcmsPage', () => {
    it('renders correctly with PageProvider and rows', () => {
        render(<DotcmsPage entity={mockEntity} />);
        expect(screen.getAllByTestId('mockRow').length).toBe(mockEntity.layout.body.rows.length);
    });

    it('populates rowsRef with row elements', () => {
        // This test ensures that the addRowRef function is working as expected
        // Since useRef is a hook, you might need to mock it or use a custom renderer to access its current value.
        // ...
    });

    it('calls useEventHandlers with the correct parameters', () => {
        // This test ensures that useEventHandlers is called with the correct rowsRef
        // You will need to mock useEventHandlers and check that it's called with the expected arguments.
        // ...
    });

    // Add more tests as needed...
});
