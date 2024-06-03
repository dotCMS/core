import { PageProvider } from '../components/PageProvider/PageProvider';
import { DotCMSPageContext } from '../models';

export const mockPageContext: DotCMSPageContext = {
    pageAsset: {
        layout: {
            header: true,
            footer: true,
            body: {
                rows: [
                    {
                        styleClass: 'row',
                        columns: [
                            {
                                styleClass: 'col-md-12',
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
                    identifier: 'container-1',
                    maxContentlets: 100,
                    parentPermissionable: {}
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
                            inode: 'inode-1',
                            onNumberOfPages: 1,
                            baseType: 'base-type-1'
                        }
                    ]
                }
            },
            'container-2': {
                container: {
                    path: 'path/to/container',
                    identifier: 'container-2',
                    maxContentlets: 100,
                    parentPermissionable: {}
                },
                containerStructures: [
                    {
                        contentTypeVar: 'content-type-2'
                    }
                ],
                contentlets: {
                    'uuid-2': []
                }
            }
        },
        page: { identifier: 'page-1', title: 'Hello Page' },
        viewAs: { language: { id: 'en' }, persona: { keyTag: 'persona-1' }, variantId: 'variant-1' }
    },
    components: {},
    isInsideEditor: false
};

export const MockContextRender = ({
    children,
    mockContext
}: {
    children: JSX.Element;
    mockContext: Partial<DotCMSPageContext>;
}) => {
    return <PageProvider pageContext={mockContext}>{children}</PageProvider>;
};
