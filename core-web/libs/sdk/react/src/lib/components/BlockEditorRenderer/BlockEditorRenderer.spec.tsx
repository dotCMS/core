import '@testing-library/jest-dom';
import { render } from '@testing-library/react';

import { DotCmsClient } from '@dotcms/client';

import { BlockEditorRenderer, BlockEditorItem } from './BlockEditorRenderer';

import { Block, ContentNode } from '../../models/blocks.interface';

describe('BlockEditorRenderer', () => {
    const blocks = {
        content: [
            {
                type: 'paragraph',
                attrs: {},
                content: [
                    {
                        type: 'text',
                        text: 'Hello, World!'
                    }
                ]
            }
        ]
    } as Block;

    it('should render the BlockEditorItem component', () => {
        const { getByText } = render(<BlockEditorRenderer blocks={blocks} />);
        expect(getByText('Hello, World!')).toBeInTheDocument();
    });

    it('should render the custom renderer component', () => {
        const customRenderers = {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            paragraph: ({ content }: { content: any }) => {
                const [{ text }] = content;

                return <p data-testid="custom-paragraph">{text}</p>;
            }
        };
        const { getByTestId } = render(
            <BlockEditorRenderer blocks={blocks} customRenderers={customRenderers} />
        );
        expect(getByTestId('custom-paragraph')).toBeInTheDocument();
    });

    it('should render the property className and style props', () => {
        const { container } = render(
            <BlockEditorRenderer blocks={blocks} className="test-class" style={{ color: 'red' }} />
        );
        expect(container.firstChild).toHaveClass('test-class');
        expect(container.firstChild).toHaveStyle('color: red');
    });
});

describe('BlockEditorItem', () => {
    const content = [
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
    ] as ContentNode[];

    it('should render the paragraph block', () => {
        const { getByText } = render(<BlockEditorItem content={content} />);
        expect(getByText('Hello, World!')).toBeInTheDocument();
    });

    it('should render the heading block', () => {
        const { container } = render(<BlockEditorItem content={content} />);
        const heading = container.querySelector('h4');
        expect(heading).toBeInTheDocument();
    });

    describe('should render the text block', () => {
        it('should render the text block', () => {
            const { getByText } = render(<BlockEditorItem content={content} />);
            expect(getByText('Hello, World!')).toBeInTheDocument();
        });

        it('should render a link', () => {
            const content = [
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
            ] as unknown as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const link = container.querySelector('a');
            expect(link).toBeInTheDocument();
            expect(link).toHaveTextContent('Link text');
            expect(link).toHaveAttribute('href', 'https://dotcms.com');
        });

        it('should render a bold text', () => {
            const content = [
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
            ] as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const bold = container.querySelector('strong');
            expect(bold).toBeInTheDocument();
            expect(bold).toHaveTextContent('Bold text');
        });

        it('should render an italic text', () => {
            const content = [
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
            ] as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const italic = container.querySelector('em');
            expect(italic).toBeInTheDocument();
            expect(italic).toHaveTextContent('Italic text');
        });

        it('should render a strike text', () => {
            const content = [
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
            ] as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const strike = container.querySelector('s');
            expect(strike).toBeInTheDocument();
            expect(strike).toHaveTextContent('Strike text');
        });

        it('should render an underline text', () => {
            const content = [
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
            ] as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const underline = container.querySelector('u');
            expect(underline).toBeInTheDocument();
            expect(underline).toHaveTextContent('Underline text');
        });

        it('should render a superscript text', () => {
            const content = [
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
            ] as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const superscript = container.querySelector('sup');
            expect(superscript).toBeInTheDocument();
            expect(superscript).toHaveTextContent('Superscript text');
        });

        it('should render a subscript text', () => {
            const content = [
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
            ] as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const subscript = container.querySelector('sub');
            expect(subscript).toBeInTheDocument();
            expect(subscript).toHaveTextContent('Subscript text');
        });
    });

    describe('Lists', () => {
        it('should render a list item', () => {
            const content = [
                {
                    type: 'listItem',
                    content: [
                        {
                            type: 'text',
                            text: 'Item 1'
                        }
                    ]
                }
            ] as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const listItem = container.querySelector('li');
            expect(listItem).toBeInTheDocument();
            expect(listItem).toHaveTextContent('Item 1');
        });

        it('should render a bullet list', () => {
            const content = [
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
            ] as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const list = container.querySelector('ul');
            expect(list).toBeInTheDocument();
            expect(list).toHaveTextContent('Item 1');
            expect(list).toHaveTextContent('Item 2');
        });

        it('should render an ordered list', () => {
            const content = [
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
            ] as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const list = container.querySelector('ol');
            expect(list).toBeInTheDocument();
            expect(list).toHaveTextContent('Item 1');
            expect(list).toHaveTextContent('Item 2');
        });
    });

    it('should render a blockquote', () => {
        const content = [
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
        ] as ContentNode[];
        const { container } = render(<BlockEditorItem content={content} />);
        const blockquote = container.querySelector('blockquote');
        expect(blockquote).toBeInTheDocument();
        expect(blockquote).toHaveTextContent('Blockquote text');
    });

    it('should render a code block', () => {
        const content = [
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
        ] as unknown as ContentNode[];
        const { container } = render(<BlockEditorItem content={content} />);
        const codeBlock = container.querySelector('pre');
        expect(codeBlock).toBeInTheDocument();
        expect(codeBlock).toHaveAttribute('data-language', 'javascript');
        expect(codeBlock).toHaveTextContent('console.log("Hello, World!")');
    });

    describe('Separators', () => {
        it('should render a horizontal rule', () => {
            const content = [
                {
                    type: 'horizontalRule',
                    content: []
                }
            ] as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const hr = container.querySelector('hr');
            expect(hr).toBeInTheDocument();
        });

        it('should render a hard break', () => {
            const content = [
                {
                    type: 'hardBreak',
                    content: []
                }
            ] as ContentNode[];
            const { container } = render(<BlockEditorItem content={content} />);
            const br = container.querySelector('br');
            expect(br).toBeInTheDocument();
        });
    });

    describe('Assets', () => {
        it('should render a dotImage using internal src', () => {
            const content = [
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
            ] as unknown as ContentNode[];

            // Mock DotCmsClient
            DotCmsClient.instance = {
                dotcmsUrl: 'https://some.dotcms.com'
            } as unknown as DotCmsClient;

            const { container } = render(<BlockEditorItem content={content} />);
            const image = container.querySelector('img');
            expect(image).toBeInTheDocument();
            expect(image).toHaveAttribute('src', 'https://some.dotcms.com/image.jpg');
        });

        it('should render a dotImage using external src', () => {
            const content = [
                {
                    type: 'dotImage',
                    attrs: {
                        src: 'https://external.com/image.jpg',
                        data: {}
                    }
                }
            ] as unknown as ContentNode[];

            // Mock DotCmsClient
            DotCmsClient.instance = {
                dotcmsUrl: 'https://some.dotcms.com'
            } as unknown as DotCmsClient;

            const { container } = render(<BlockEditorItem content={content} />);
            const image = container.querySelector('img');
            expect(image).toBeInTheDocument();
            expect(image).toHaveAttribute('src', 'https://external.com/image.jpg');
        });

        it('should render a dotVideo', () => {
            const content = [
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
            ] as unknown as ContentNode[];

            // Mock DotCmsClient
            DotCmsClient.instance = {
                dotcmsUrl: 'https://some.dotcms.com'
            } as unknown as DotCmsClient;

            const { container } = render(<BlockEditorItem content={content} />);
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
        const content = [
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
        ] as unknown as ContentNode[];
        const { container } = render(<BlockEditorItem content={content} />);
        const table = container.querySelector('table');
        expect(table).toBeInTheDocument();
        
        const rows = table?.querySelectorAll('tr');
        expect(rows).toHaveLength(2);

        const cells = rows?.[1].querySelectorAll('td');
        expect(cells).toHaveLength(2);
        expect(cells?.[0]).toHaveTextContent('Content 1');
    });

    it('should render a dotContent from customRenderers', () => {
        const content = [
            {
                type: 'dotContent',
                attrs: {
                    identifier: '1234',
                    title: 'My activity'
                }
            }
        ] as unknown as ContentNode[];

        const customRenderers = {
            dotContent: ({ title }: { title: string }) => {
                return <div data-testid="custom-dot-content">{title}</div>;
            }
        };

        const { getByTestId } = render(
            <BlockEditorItem content={content} customRenderers={customRenderers} />
        );
        expect(getByTestId('custom-dot-content')).toHaveTextContent('My activity');
    })
});
