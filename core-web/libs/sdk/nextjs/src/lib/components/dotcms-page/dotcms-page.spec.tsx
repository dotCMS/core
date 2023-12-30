import { render, screen } from '@testing-library/react';
import { ReactNode } from 'react';

import { PageProviderContext } from '@dotcms/react';

import { DotcmsPage } from './dotcms-page';

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

    const MockRow = forwardRef((props, ref) => (
        <div ref={ref} data-testid="mockRow" {...props}></div>
    ));

    const useEventHandlers = jest.fn();

    const MockPageProvider = ({ children }: { children: ReactNode }) => (
        <div data-testid="mockPageProvider">{children}</div>
    );

    return {
        Row: MockRow,
        useEventHandlers: useEventHandlers,
        PageProvider: MockPageProvider
    };
});

// Mocking window.postMessage
const postMessageMock = jest.fn();
Object.defineProperty(window, 'parent', {
    value: { postMessage: postMessageMock }
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
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders correctly with PageProvider and rows', () => {
        render(<DotcmsPage entity={mockEntity} />);
        expect(screen.getAllByTestId('mockRow').length).toBe(mockEntity.layout.body.rows.length);
    });

    // it('sends the correct message to parent window on pathname change', () => {
    //     render(<DotcmsPage entity={mockEntity} />);

    //     // Simulate pathname change
    //     nextRouter.usePathname.mockImplementation(() => '/newpath');
    //     fireEvent.popstate(window, new PopStateEvent('popstate'));

    //     expect(postMessageMock).toHaveBeenCalledWith(
    //         {
    //             action: 'set-url',
    //             payload: {
    //                 url: 'newpath',
    //             },
    //         },
    //         '*'
    //     );
    // });

    // it('adds a ref to rowsRef.current when addRowRef is called', () => {

    //     const { getAllByTestId } = render(<DotcmsPage entity={mockEntity} />);

    //     const renderedRows = getAllByTestId('row'); // Make sure your Row component has a data-testid="row"
    //     expect(renderedRows.length).toBe(mockEntity.layout.body.rows.length);
    // });

    // Add more tests as needed to cover other functionalities and scenarios
});
