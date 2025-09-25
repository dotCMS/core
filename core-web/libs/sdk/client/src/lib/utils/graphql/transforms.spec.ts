import { DotCMSGraphQLPage } from '@dotcms/types';

import { graphqlToPageEntity } from './transforms';

const GRAPHQL_RESPONSE_MOCK = {
    page: {
        title: 'test2',
        url: '/test2',
        seodescription: null,
        containers: [
            {
                path: '//demo.dotcms.com/application/containers/default/',
                identifier: '69b3d24d-7e80-4be6-b04a-d352d16493ee',
                containerStructures: [
                    {
                        id: '77ec7720-bad8-431c-a0a3-6443fe87af73'
                    },
                    {
                        id: '1e5372c4-9fff-4793-ad8a-a52558d0d395'
                    },
                    {
                        id: '461b6fad-04a8-49ba-b75b-6634799bc289'
                    },
                    {
                        id: '57b4a9de-4765-4c57-8a05-2a0a317ec546'
                    },
                    {
                        id: '342a83bf-040a-45a2-a4bc-defb0f1bf4eb'
                    },
                    {
                        id: '91a955a0-5c97-4fc3-91dd-f894e2f3dc54'
                    },
                    {
                        id: '71ded7c9deb5b05452824bb42b9aa9d5'
                    },
                    {
                        id: '86a06a2e-ba40-43d4-a3ad-3a870d43bfc1'
                    },
                    {
                        id: '7673e601-26d4-4dbc-b6ad-7028599df656'
                    },
                    {
                        id: '313498a4-5b57-4d9e-8d32-ef2d30f9e867'
                    }
                ],
                containerContentlets: [
                    {
                        uuid: 'uuid-1562770692396',
                        contentlets: [
                            {
                                identifier: 'd9444fa005a10d555318d8b014e474d4'
                            }
                        ]
                    }
                ]
            }
        ],
        host: {
            hostName: 'demo.dotcms.com'
        },
        layout: {
            title: 'default-template'
        },
        template: {
            identifier: '31f4c794-c769-4929-9d5d-7c383408c65c'
        },
        urlContentMap: {
            _map: {
                identifier: '31f4c794-c769-4929-9d5d-7c383408c74d'
            }
        },
        viewAs: {
            mode: 'LIVE'
        },
        runningExperimentId: '123',
        vanityUrl: {
            action: 200,
            uri: '/test2',
            forwardTo: '/test2'
        }
    }
};

const MOCK_PAGE_ENTITY = {
    containers: {
        '//demo.dotcms.com/application/containers/default/': {
            container: {
                identifier: '69b3d24d-7e80-4be6-b04a-d352d16493ee',
                path: '//demo.dotcms.com/application/containers/default/'
            },
            containerStructures: [
                {
                    id: '77ec7720-bad8-431c-a0a3-6443fe87af73'
                },
                {
                    id: '1e5372c4-9fff-4793-ad8a-a52558d0d395'
                },
                {
                    id: '461b6fad-04a8-49ba-b75b-6634799bc289'
                },
                {
                    id: '57b4a9de-4765-4c57-8a05-2a0a317ec546'
                },
                {
                    id: '342a83bf-040a-45a2-a4bc-defb0f1bf4eb'
                },
                {
                    id: '91a955a0-5c97-4fc3-91dd-f894e2f3dc54'
                },
                {
                    id: '71ded7c9deb5b05452824bb42b9aa9d5'
                },
                {
                    id: '86a06a2e-ba40-43d4-a3ad-3a870d43bfc1'
                },
                {
                    id: '7673e601-26d4-4dbc-b6ad-7028599df656'
                },
                {
                    id: '313498a4-5b57-4d9e-8d32-ef2d30f9e867'
                }
            ],
            contentlets: {
                'uuid-1562770692396': [
                    {
                        identifier: 'd9444fa005a10d555318d8b014e474d4'
                    }
                ]
            }
        }
    },
    layout: {
        title: 'default-template'
    },
    page: {
        seodescription: null,
        title: 'test2',
        url: '/test2'
    },
    urlContentMap: {
        identifier: '31f4c794-c769-4929-9d5d-7c383408c74d'
    },
    template: {
        identifier: '31f4c794-c769-4929-9d5d-7c383408c65c'
    },
    viewAs: {
        mode: 'LIVE'
    },
    site: {
        hostName: 'demo.dotcms.com'
    },
    vanityUrl: {
        action: 200,
        uri: '/test2',
        forwardTo: '/test2'
    },
    runningExperimentId: '123'
};

describe('GraphQL Parser', () => {
    it('should return the correct page entity', () => {
        const pageEntity = graphqlToPageEntity(
            GRAPHQL_RESPONSE_MOCK.page as unknown as DotCMSGraphQLPage
        );

        expect(pageEntity).toEqual(MOCK_PAGE_ENTITY);
    });

    it('should transform _map properties correctly', () => {
        const graphqlResponse = {
            page: {
                title: 'map-test',
                url: '/map-test',
                _map: {
                    customField: 'custom value'
                },
                urlContentMap: {
                    _map: {
                        mapField: 'map value',
                        identifier: 'test-id',
                        someNestedField: {
                            nestedField: 'nested value'
                        }
                    }
                },
                containers: [
                    {
                        path: '//test/container/',
                        identifier: 'test-container-id',
                        containerContentlets: [
                            {
                                uuid: 'test-uuid',
                                contentlets: [
                                    {
                                        _map: {
                                            identifier: 'test-contentlet-id',
                                            contentletField: 'contentlet value'
                                        }
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        };

        const expectedResult = {
            page: {
                title: 'map-test',
                url: '/map-test',
                customField: 'custom value'
            },
            urlContentMap: {
                identifier: 'test-id',
                mapField: 'map value',
                someNestedField: {
                    nestedField: 'nested value'
                }
            },
            containers: {
                '//test/container/': {
                    container: {
                        path: '//test/container/',
                        identifier: 'test-container-id'
                    },
                    containerStructures: undefined,
                    contentlets: {
                        'test-uuid': [
                            {
                                identifier: 'test-contentlet-id',
                                contentletField: 'contentlet value'
                            }
                        ]
                    }
                }
            }
        };

        const pageEntity = graphqlToPageEntity(
            graphqlResponse.page as unknown as DotCMSGraphQLPage
        );

        expect(pageEntity?.page).toEqual(expectedResult.page);
        expect(pageEntity?.urlContentMap).toEqual(expectedResult.urlContentMap);
        expect(pageEntity?.containers['//test/container/'].contentlets['test-uuid'][0]).toEqual(
            expectedResult.containers['//test/container/'].contentlets['test-uuid'][0]
        );
    });

    it('should merge urlContentMap keys into _map', () => {
        const graphqlResponse = {
            ...GRAPHQL_RESPONSE_MOCK,
            page: {
                ...GRAPHQL_RESPONSE_MOCK.page,
                urlContentMap: {
                    _map: {
                        customField: 'custom value',
                        someOtherField: 'empty'
                    },
                    someOtherField: 'some other value by relationship'
                }
            }
        };

        const pageEntity = graphqlToPageEntity(
            graphqlResponse.page as unknown as DotCMSGraphQLPage
        );

        expect(pageEntity?.urlContentMap).toEqual({
            customField: 'custom value',
            someOtherField: 'some other value by relationship'
        });
    });
});
