import { render, screen } from '@testing-library/react';
import { ReactNode } from 'react';

import { PageProviderContext } from '@dotcms/react';

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

    const MockRow = forwardRef((props, ref) => (
        <div ref={ref} data-testid="mockRow" {...props}></div>
    ));

    const MockPageProvider = ({ children }: { children: ReactNode }) => (
        <div data-testid="mockPageProvider">{children}</div>
    );

    return {
        Row: MockRow,
        usePageEditor: jest.fn().mockReturnValue({ current: [] }),
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

describe('DotcmsLayout', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders correctly with PageProvider and rows', () => {
        render(<DotcmsLayout entity={mockEntity} />);
        expect(screen.getAllByTestId('mockRow').length).toBe(mockEntity.layout.body.rows.length);
    });
});
