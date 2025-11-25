import '@testing-library/jest-dom';
import { render } from '@testing-library/react';

import { DotCmsClient } from '@dotcms/client';

import { BlockEditorBlock } from './BlockEditorBlock';

import { ContentNode } from '../../../models/content-node.interface';

const BLOCKS_MOCKS = {
    PARAGRAPH: [
        {
            type: 'paragraph',
            attrs: {},
            content: [
                {
                    type: 'text',
                    text: 'Hello, World!'
                }
            ]
        },
        {
            type: 'heading',
            attrs: { level: '4' },
            content: [
                {
                    type: 'text',
                    text: 'Heading!!'
                }
            ]
        }
    ],
    LINK: [
        {
            type: 'paragraph',
            content: [
                {
                    type: 'text',
                    text: 'Link text',
                    marks: [
                        {
                            type: 'link',
                            attrs: {
                                href: 'https://dotcms.com'
                            }
                        }
                    ]
                }
            ]
        }
    ],
    BOLD: [
        {
            type: 'paragraph',
            content: [
                {
                    type: 'text',
                    text: 'Bold text',
                    marks: [
                        {
                            type: 'bold',
                            attrs: {}
                        }
                    ]
                }
            ]
        }
    ],
    ITALIC: [
        {
            type: 'paragraph',
            content: [
                {
                    type: 'text',
                    text: 'Italic text',
                    marks: [
                        {
                            type: 'italic',
                            attrs: {}
                        }
                    ]
                }
            ]
        }
    ],
    STRIKE: [
        {
            type: 'paragraph',
            content: [
                {
                    type: 'text',
                    text: 'Strike text',
                    marks: [
                        {
                            type: 'strike',
                            attrs: {}
                        }
                    ]
                }
            ]
        }
    ],
    UNDERLINE: [
        {
            type: 'paragraph',
            content: [
                {
                    type: 'text',
                    text: 'Underline text',
                    marks: [
                        {
                            type: 'underline',
                            attrs: {}
                        }
                    ]
                }
            ]
        }
    ],
    SUPSCRIPT: [
        {
            type: 'paragraph',
            content: [
                {
                    type: 'text',
                    text: 'Superscript text',
                    marks: [
                        {
                            type: 'superscript',
                            attrs: {}
                        }
                    ]
                }
            ]
        }
    ],
    SUBSCRIPT: [
        {
            type: 'paragraph',
            content: [
                {
                    type: 'text',
                    text: 'Subscript text',
                    marks: [
                        {
                            type: 'subscript',
                            attrs: {}
                        }
                    ]
                }
            ]
        }
    ],
    LIST: [
        {
            type: 'listItem',
            content: [
                {
                    type: 'text',
                    text: 'Item 1'
                }
            ]
        }
    ],
    BULLET_LIST: [
        {
            type: 'bulletList',
            content: [
                {
                    type: 'listItem',
                    content: [
                        {
                            type: 'text',
                            text: 'Item 1'
                        }
                    ]
                },
                {
                    type: 'listItem',
                    content: [
                        {
                            type: 'text',
                            text: 'Item 2'
                        }
                    ]
                }
            ]
        }
    ],
    ORDERED_LIST: [
        {
            type: 'orderedList',
            content: [
                {
                    type: 'listItem',
                    content: [
                        {
                            type: 'text',
                            text: 'Item 1'
                        }
                    ]
                },
                {
                    type: 'listItem',
                    content: [
                        {
                            type: 'text',
                            text: 'Item 2'
                        }
                    ]
                }
            ]
        }
    ],
    BLOCKQUOTE: [
        {
            type: 'blockquote',
            content: [
                {
                    type: 'paragraph',
                    content: [
                        {
                            type: 'text',
                            text: 'Blockquote text'
                        }
                    ]
                }
            ]
        }
    ],
    CODE_BLOCK: [
        {
            type: 'codeBlock',
            attrs: {
                language: 'javascript'
            },
            content: [
                {
                    type: 'text',
                    text: 'console.log("Hello, World!")'
                }
            ]
        }
    ],
    HARDBREAK: [
        {
            type: 'hardBreak',
            content: []
        }
    ],
    HORIZONTAL_RULE: [
        {
            type: 'horizontalRule',
            content: []
        }
    ],
    DOT_IMAGE: [
        {
            type: 'dotImage',
            attrs: {
                src: '/image.jpg',
                data: {
                    title: 'Image title',
                    baseType: 'Image',
                    inode: '1234',
                    archived: false,
                    working: true,
                    locked: false,
                    contentType: 'Image',
                    live: true,
                    identifier: 'image-identifier',
                    image: 'image.jpg',
                    imageContentAsset: 'image-content-asset',
                    urlTitle: 'image-url-title',
                    url: 'image-url',
                    titleImage: 'title-image',
                    urlMap: 'image-url-map',
                    hasLiveVersion: true,
                    hasTitleImage: true,
                    sortOrder: 1,
                    modUser: 'admin',
                    __icon__: 'image-icon',
                    contentTypeIcon: 'image-content-type-icon'
                }
            }
        }
    ],
    DOT_IMAGE_EXTERNAL: [
        {
            type: 'dotImage',
            attrs: {
                src: 'https://external.com/image.jpg',
                data: {}
            }
        }
    ],
    DOT_VIDEO: [
        {
            type: 'dotVideo',
            attrs: {
                src: '/video.mp4',
                width: '640',
                height: '360',
                mimeType: 'video/mp4',
                data: {
                    title: 'Video title',
                    baseType: 'Video',
                    inode: '1234',
                    archived: false,
                    working: true,
                    locked: false,
                    contentType: 'Video',
                    live: true,
                    identifier: 'video-identifier',
                    image: 'video.jpg',
                    imageContentAsset: 'video-content-asset',
                    urlTitle: 'video-url-title',
                    url: 'video-url',
                    titleImage: 'title-image',
                    urlMap: 'video-url-map',
                    hasLiveVersion: true,
                    hasTitleImage: true,
                    sortOrder: 1,
                    modUser: 'admin',
                    __icon__: 'video-icon',
                    contentTypeIcon: 'video-content-type-icon',
                    thumbnail: '/thumbnail.jpg'
                }
            }
        }
    ],
    TABLE: [
        {
            type: 'table',
            content: [
                {
                    type: 'tableRow',
                    content: [
                        {
                            type: 'tableHeader',
                            content: [
                                [
                                    {
                                        type: 'paragraph',
                                        attrs: {
                                            textAlign: 'left'
                                        },
                                        content: [
                                            {
                                                type: 'text',
                                                text: 'Header 1'
                                            }
                                        ]
                                    }
                                ]
                            ]
                        },
                        {
                            type: 'tableHeader',
                            content: [
                                [
                                    {
                                        type: 'paragraph',
                                        attrs: {
                                            textAlign: 'left'
                                        },
                                        content: [
                                            {
                                                type: 'text',
                                                text: 'Header 1'
                                            }
                                        ]
                                    }
                                ]
                            ]
                        }
                    ]
                },
                {
                    type: 'tableRow',
                    content: [
                        {
                            type: 'tableCell',
                            attrs: {
                                colspan: 1,
                                rowspan: 1,
                                colwidth: null
                            },
                            content: [
                                {
                                    type: 'paragraph',
                                    attrs: {
                                        textAlign: 'left'
                                    },
                                    content: [
                                        {
                                            type: 'text',
                                            text: 'Content 1'
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            type: 'tableCell',
                            attrs: {
                                colspan: 1,
                                rowspan: 1,
                                colwidth: null
                            },
                            content: [
                                {
                                    type: 'paragraph',
                                    attrs: {
                                        textAlign: 'left'
                                    },
                                    content: [
                                        {
                                            type: 'text',
                                            text: 'Content 2'
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ],
    DOT_CONTENT: [
        {
            type: 'dotContent',
            attrs: {
                identifier: '1234',
                title: 'My activity'
            }
        }
    ]
} as unknown as { [key: string]: ContentNode[] };

describe('BlockEditorItem', () => {
    it('should render the paragraph block', () => {
        const { getByText } = render(<BlockEditorBlock content={BLOCKS_MOCKS.PARAGRAPH} />);
        expect(getByText('Hello, World!')).toBeInTheDocument();
    });

    it('should render the heading block', () => {
        const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.PARAGRAPH} />);
        const heading = container.querySelector('h4');
        expect(heading).toBeInTheDocument();
    });

    describe('should render the text block', () => {
        it('should render the text block', () => {
            const { getByText } = render(<BlockEditorBlock content={BLOCKS_MOCKS.PARAGRAPH} />);
            expect(getByText('Hello, World!')).toBeInTheDocument();
        });

        it('should render a link', () => {
            const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.LINK} />);
            const link = container.querySelector('a');
            expect(link).toBeInTheDocument();
            expect(link).toHaveTextContent('Link text');
            expect(link).toHaveAttribute('href', 'https://dotcms.com');
        });

        it('should render a bold text', () => {
            const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.BOLD} />);
            const bold = container.querySelector('strong');
            expect(bold).toBeInTheDocument();
            expect(bold).toHaveTextContent('Bold text');
        });

        it('should render an italic text', () => {
            const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.ITALIC} />);
            const italic = container.querySelector('em');
            expect(italic).toBeInTheDocument();
            expect(italic).toHaveTextContent('Italic text');
        });

        it('should render a strike text', () => {
            const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.STRIKE} />);
            const strike = container.querySelector('s');
            expect(strike).toBeInTheDocument();
            expect(strike).toHaveTextContent('Strike text');
        });

        it('should render an underline text', () => {
            const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.UNDERLINE} />);
            const underline = container.querySelector('u');
            expect(underline).toBeInTheDocument();
            expect(underline).toHaveTextContent('Underline text');
        });

        it('should render a supscript text', () => {
            const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.SUPSCRIPT} />);
            const superscript = container.querySelector('sup');
            expect(superscript).toBeInTheDocument();
            expect(superscript).toHaveTextContent('Superscript text');
        });

        it('should render a subscript text', () => {
            const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.SUBSCRIPT} />);
            const subscript = container.querySelector('sub');
            expect(subscript).toBeInTheDocument();
            expect(subscript).toHaveTextContent('Subscript text');
        });
    });

    describe('Lists', () => {
        const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.LIST} />);
        const listItem = container.querySelector('li');
        expect(listItem).toBeInTheDocument();
        expect(listItem).toHaveTextContent('Item 1');
    });

    it('should render a bullet list', () => {
        const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.BULLET_LIST} />);
        const list = container.querySelector('ul');
        expect(list).toBeInTheDocument();
        expect(list).toHaveTextContent('Item 1');
        expect(list).toHaveTextContent('Item 2');
    });

    it('should render an ordered list', () => {
        const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.ORDERED_LIST} />);
        const list = container.querySelector('ol');
        expect(list).toBeInTheDocument();
        expect(list).toHaveTextContent('Item 1');
        expect(list).toHaveTextContent('Item 2');
    });
});

it('should render a blockquote', () => {
    const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.BLOCKQUOTE} />);
    const blockquote = container.querySelector('blockquote');
    expect(blockquote).toBeInTheDocument();
    expect(blockquote).toHaveTextContent('Blockquote text');
});

it('should render a code block', () => {
    const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.CODE_BLOCK} />);
    const codeBlock = container.querySelector('pre');
    expect(codeBlock).toBeInTheDocument();
    expect(codeBlock).toHaveAttribute('data-language', 'javascript');
    expect(codeBlock).toHaveTextContent('console.log("Hello, World!")');
});

describe('Separators', () => {
    it('should render a horizontal rule', () => {
        const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.HORIZONTAL_RULE} />);
        const hr = container.querySelector('hr');
        expect(hr).toBeInTheDocument();
    });

    it('should render a hard break', () => {
        const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.HARDBREAK} />);
        const br = container.querySelector('br');
        expect(br).toBeInTheDocument();
    });
});

describe('Assets', () => {
    it('should render a dotImage using internal src', () => {
        // Mock DotCmsClient
        DotCmsClient.instance = {
            dotcmsUrl: 'https://some.dotcms.com'
        } as unknown as DotCmsClient;

        const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.DOT_IMAGE} />);
        const image = container.querySelector('img');
        expect(image).toBeInTheDocument();
        expect(image).toHaveAttribute('src', 'https://some.dotcms.com/image.jpg');
    });

    it('should render a dotImage using external src', () => {
        // Mock DotCmsClient
        DotCmsClient.instance = {
            dotcmsUrl: 'https://some.dotcms.com'
        } as unknown as DotCmsClient;

        const { container } = render(
            <BlockEditorBlock content={BLOCKS_MOCKS.DOT_IMAGE_EXTERNAL} />
        );
        const image = container.querySelector('img');
        expect(image).toBeInTheDocument();
        expect(image).toHaveAttribute('src', 'https://external.com/image.jpg');
    });

    it('should render a dotVideo', () => {
        // Mock DotCmsClient
        DotCmsClient.instance = {
            dotcmsUrl: 'https://some.dotcms.com'
        } as unknown as DotCmsClient;

        const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.DOT_VIDEO} />);
        const video = container.querySelector('video');
        expect(video).toBeInTheDocument();
        expect(video).toHaveAttribute('width', '640');
        expect(video).toHaveAttribute('height', '360');
        expect(video).toHaveAttribute('poster', 'https://some.dotcms.com/thumbnail.jpg');

        const source = video?.querySelector('source');
        expect(source).toHaveAttribute('src', 'https://some.dotcms.com/video.mp4');
        expect(source).toHaveAttribute('type', 'video/mp4');
    });
});

it('should render a table', () => {
    const { container } = render(<BlockEditorBlock content={BLOCKS_MOCKS.TABLE} />);
    const table = container.querySelector('table');
    expect(table).toBeInTheDocument();

    const rows = table?.querySelectorAll('tr');
    expect(rows).toHaveLength(2);

    const cells = rows?.[1].querySelectorAll('td');
    expect(cells).toHaveLength(2);
    expect(cells?.[0]).toHaveTextContent('Content 1');
});

it('should render a dotContent from customRenderers', () => {
    const customRenderers = {
        dotContent: (props: ContentNode) => {
            return <div data-testid="custom-dot-content">{props.attrs?.title}</div>;
        }
    };

    const { getByTestId } = render(
        <BlockEditorBlock content={BLOCKS_MOCKS.DOT_CONTENT} customRenderers={customRenderers} />
    );
    expect(getByTestId('custom-dot-content')).toHaveTextContent('My activity');
});
