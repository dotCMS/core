import { CurrentUser } from '@dotcms/dotcms-js';
import { DotExperiment, DotExperimentStatus } from '@dotcms/dotcms-models';

import {
    deleteContentletFromContainer,
    insertContentletInContainer,
    sanitizeURL,
    getPersonalization,
    createPageApiUrlWithQueryParams,
    SDK_EDITOR_SCRIPT_SOURCE,
    computePageIsLocked,
    computeCanEditPage,
    mapContainerStructureToArrayOfContainers,
    mapContainerStructureToDotContainerMap,
    areContainersEquals,
    compareUrlPaths,
    createFullURL,
    getDragItemData
} from '.';

import { dotPageContainerStructureMock } from '../shared/mocks';
import { ContentletDragPayload, ContentTypeDragPayload, DotPage } from '../shared/models';

const generatePageAndUser = ({ locked, lockedBy, userId }) => ({
    page: {
        locked,
        lockedBy
    } as DotPage,
    currentUser: {
        userId
    } as CurrentUser
});

describe('utils functions', () => {
    describe('SDK Editor Script Source', () => {
        it('should return the correct script source', () => {
            expect(SDK_EDITOR_SCRIPT_SOURCE).toEqual('/html/js/editor-js/sdk-editor.js');
        });
    });

    describe('delete contentlet from container', () => {
        it('should delete', () => {
            // Current page

            const result = deleteContentletFromContainer({
                pageId: 'test',
                language_id: 'test',
                container: {
                    identifier: 'test',
                    acceptTypes: 'test',
                    uuid: 'test',
                    maxContentlets: 1,
                    contentletsId: ['test'],
                    variantId: '1'
                },
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: ['test']
                    }
                ],
                contentlet: {
                    identifier: 'test',
                    inode: 'test',
                    title: 'test',
                    contentType: 'test'
                },
                personaTag: 'test',
                position: 'after'
            });

            expect(result).toEqual({
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: [],
                        personaTag: 'test'
                    }
                ],
                contentletsId: []
            });
        });

        it('should not delete if id not found', () => {
            // Current page
            const pageContainers = [
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
                }
            ];

            // Container where we want to delete the contentlet
            const container = {
                identifier: 'test',
                uuid: 'test',
                contentletsId: ['test'],
                maxContentlets: 1,
                acceptTypes: 'test',
                variantId: '1'
            };

            // Contentlet to delete
            const contentlet = {
                identifier: 'test2',
                inode: 'test',
                title: 'test',
                contentType: 'test'
            };

            const result = deleteContentletFromContainer({
                pageContainers,
                container,
                contentlet,
                pageId: 'test',
                language_id: 'test',
                position: 'after'
            });

            expect(result).toEqual({
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: ['test'],
                        personaTag: undefined
                    }
                ],
                contentletsId: ['test']
            });
        });
    });

    describe('insert contentlet in container', () => {
        it('should insert at the end', () => {
            // Current page
            const pageContainers = [
                {
                    identifier: 'container-identifier-123',
                    uuid: 'container-uui-123',
                    contentletsId: ['contentlet-mark-123']
                }
            ];

            // Container where we want to insert the contentlet
            const container = {
                identifier: 'container-identifier-123',
                acceptTypes: 'test',
                uuid: 'container-uui-123',
                contentletsId: ['contentlet-mark-123'],
                maxContentlets: 1,
                variantId: '1'
            };

            // Contentlet position mark
            const contentlet = {
                identifier: 'contentlet-mark-123',
                inode: 'contentlet-mark-inode-123',
                title: 'test',
                contentType: 'test'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet,
                pageId: 'page-id-123',
                language_id: '1',
                newContentletId: 'new-contentlet-id-123'
            });

            expect(result).toEqual({
                didInsert: true,
                pageContainers: [
                    {
                        identifier: 'container-identifier-123',
                        uuid: 'container-uui-123',
                        contentletsId: ['contentlet-mark-123', 'new-contentlet-id-123'],
                        personaTag: undefined
                    }
                ]
            });
        });

        it('should insert in specific position', () => {
            // Current page
            const pageContainers = [
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['test', 'test123', 'test1234']
                }
            ];

            // Container where we want to insert the contentlet
            const container = {
                identifier: 'test',
                acceptTypes: 'test',
                uuid: 'test',
                contentletsId: ['test'],
                maxContentlets: 1,
                variantId: '1'
            };

            // Contentlet to insert
            const contentlet = {
                identifier: 'test123',
                inode: 'test',
                title: 'test',
                contentType: 'test'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet,
                pageId: 'test',
                language_id: 'test',
                position: 'after',
                newContentletId: '000'
            });

            expect(result).toEqual({
                didInsert: true,
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: ['test', 'test123', '000', 'test1234'],
                        personaTag: undefined
                    }
                ]
            });
        });

        it('should not insert contentlet if already exist', () => {
            // Current Page
            const pageContainers = [
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['test']
                }
            ];

            // Container where we want to insert the contentlet
            const container = {
                identifier: 'test',
                acceptTypes: 'test',
                uuid: 'test',
                contentletsId: ['test'],
                maxContentlets: 1,
                variantId: '1'
            };

            // Contentlet to insert
            const contentlet = {
                identifier: 'test',
                inode: 'test',
                title: 'test',
                contentType: 'test'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet,
                newContentletId: 'test',
                language_id: 'test',
                pageId: 'test'
            });

            expect(result).toEqual({
                didInsert: false,
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: ['test']
                    }
                ]
            });
        });
    });

    describe('url sanitize', () => {
        it('should remove the slash from the start', () => {
            expect(sanitizeURL('/cool')).toEqual('cool');
        });

        it("should remove the slash from the end if it's not the only character", () => {
            expect(sanitizeURL('super-cool/')).toEqual('super-cool');
        });

        it('should remove the slash from the end and the beggining', () => {
            expect(sanitizeURL('/hello-there/')).toEqual('hello-there');
        });

        it('should remove the index if a nested path', () => {
            expect(sanitizeURL('i-have-the-high-ground/index')).toEqual('i-have-the-high-ground');
        });

        it('should remove the index if a nested path with slash', () => {
            expect(sanitizeURL('no-index-please/index/')).toEqual('no-index-please');
        });

        it('should leave as it is for valid url', () => {
            expect(sanitizeURL('this-is-where-the-fun-begins')).toEqual(
                'this-is-where-the-fun-begins'
            );
        });

        it('should leave as it is for a nested valid url', () => {
            expect(sanitizeURL('hello-there/general-kenobi')).toEqual('hello-there/general-kenobi');
        });
    });

    describe('personalization', () => {
        it('should return the correct personalization when persona exists', () => {
            const personalization = getPersonalization({
                contentType: 'persona',
                keyTag: 'adminUser'
            });

            expect(personalization).toBe('dot:persona:adminUser');
        });

        it('should return the correct personalization when persona does not exist', () => {
            const personalization = getPersonalization({});
            expect(personalization).toBe('dot:default');
        });
    });

    describe('createPageApiUrlWithQueryParams', () => {
        it('should return the correct query params', () => {
            const queryParams = {
                variantName: 'test',
                language_id: '20',
                'com.dotmarketing.persona.id': 'the-chosen-one',
                experimentId: '123',
                mode: 'PREVIEW_MODE'
            };
            const result = createPageApiUrlWithQueryParams('test', queryParams);
            expect(result).toBe(
                'test?variantName=test&language_id=20&com.dotmarketing.persona.id=the-chosen-one&experimentId=123&mode=PREVIEW_MODE'
            );
        });

        it('should return url with default query params if no query params', () => {
            const queryParams = {};
            const result = createPageApiUrlWithQueryParams('test', queryParams);
            expect(result).toBe(
                'test?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT'
            );
        });

        it('should ignore the undefined queryParams', () => {
            const queryParams = {
                variantName: 'test',
                experimentId: undefined
            };
            const result = createPageApiUrlWithQueryParams('test', queryParams);
            expect(result).toBe(
                'test?variantName=test&language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona'
            );
        });
    });

    describe('computePageIsLocked', () => {
        it('should return false when the page is unlocked', () => {
            const { page, currentUser } = generatePageAndUser({
                locked: false,
                lockedBy: '123',
                userId: '123'
            });

            const result = computePageIsLocked(page, currentUser);

            expect(result).toBe(false);
        });

        it('should return false when the page is locked and is the same user', () => {
            const { page, currentUser } = generatePageAndUser({
                locked: true,
                lockedBy: '123',
                userId: '123'
            });

            const result = computePageIsLocked(page, currentUser);

            expect(result).toBe(false);
        });

        it('should return true when the page is locked and is not the same user', () => {
            const { page, currentUser } = generatePageAndUser({
                locked: true,
                lockedBy: '123',
                userId: '456'
            });

            const result = computePageIsLocked(page, currentUser);

            expect(result).toBe(true);
        });
    });

    describe('computeCanEditPage', () => {
        it('should return true when the page can be edited, is not locked and does not have experiment', () => {
            const { page, currentUser } = generatePageAndUser({
                locked: false,
                lockedBy: '123',
                userId: '123'
            });

            const result = computeCanEditPage({ ...page, canEdit: true }, currentUser);

            expect(result).toBe(true);
        });

        it('should return true when the page can be edited and does have an experiment that is not running or scheduled', () => {
            const { page, currentUser } = generatePageAndUser({
                locked: false,
                lockedBy: '123',
                userId: '123'
            });

            const experiment = {
                status: DotExperimentStatus.DRAFT
            } as DotExperiment;

            const result = computeCanEditPage({ ...page, canEdit: true }, currentUser, experiment);

            expect(result).toBe(true);
        });

        it('should return false when the page can be edited and does have an experiment that is running', () => {
            const { page, currentUser } = generatePageAndUser({
                locked: false,
                lockedBy: '123',
                userId: '123'
            });

            const experiment = {
                status: DotExperimentStatus.RUNNING
            } as DotExperiment;

            const result = computeCanEditPage({ ...page, canEdit: true }, currentUser, experiment);

            expect(result).toBe(false);
        });

        it('should return false when the page can be edited and does have an experiment that is scheduled', () => {
            const { page, currentUser } = generatePageAndUser({
                locked: false,
                lockedBy: '123',
                userId: '123'
            });

            const experiment = {
                status: DotExperimentStatus.SCHEDULED
            } as DotExperiment;

            const result = computeCanEditPage({ ...page, canEdit: true }, currentUser, experiment);

            expect(result).toBe(false);
        });

        it('should return false when the page can be edited but is locked', () => {
            const { page, currentUser } = generatePageAndUser({
                locked: true,
                lockedBy: '123',
                userId: '456'
            });

            const result = computeCanEditPage({ ...page, canEdit: true }, currentUser);

            expect(result).toBe(false);
        });

        it('should return false when the page cannot be edited', () => {
            const { page, currentUser } = generatePageAndUser({
                locked: true,
                lockedBy: '123',
                userId: '456'
            });

            const result = computeCanEditPage({ ...page, canEdit: false }, currentUser);

            expect(result).toBe(false);
        });
    });

    describe('mapContainerStructureToArrayOfContainers', () => {
        it('should map container structure to array', () => {
            const result = mapContainerStructureToArrayOfContainers(dotPageContainerStructureMock);

            expect(result).toEqual([
                {
                    identifier: '123',
                    uuid: '123',
                    contentletsId: ['123', '456']
                },
                {
                    identifier: '123',
                    uuid: '456',
                    contentletsId: ['123']
                }
            ]);
        });
    });

    describe('mapContainerStructureToDotContainerMap', () => {
        it('should map container structure to dotContainerMap', () => {
            const result = mapContainerStructureToDotContainerMap(dotPageContainerStructureMock);

            expect(result).toEqual({
                '123': dotPageContainerStructureMock['123'].container
            });
        });
    });

    describe('areContainersEquals', () => {
        it('should return true when the containers are equal', () => {
            expect(
                areContainersEquals(
                    {
                        identifier: '123',
                        uuid: '123',
                        contentletsId: ['123', '456']
                    },
                    {
                        identifier: '123',
                        uuid: '123',
                        acceptTypes: 'test',
                        variantId: 'Default',
                        maxContentlets: 1
                    }
                )
            ).toBe(true);
        });
        it('should return false when the containers dont have the same identifier', () => {
            expect(
                areContainersEquals(
                    {
                        identifier: '123',
                        uuid: '123',
                        contentletsId: ['123', '456']
                    },
                    {
                        identifier: '456',
                        uuid: '123',
                        acceptTypes: 'test',
                        variantId: 'Default',
                        maxContentlets: 1
                    }
                )
            ).toBe(false);
        });
        it('should return false when the containers dont have the same uuid', () => {
            expect(
                areContainersEquals(
                    {
                        identifier: '123',
                        uuid: '123',
                        contentletsId: ['123', '456']
                    },
                    {
                        identifier: '123',
                        uuid: '456',
                        acceptTypes: 'test',
                        variantId: 'Default',
                        maxContentlets: 1
                    }
                )
            ).toBe(false);
        });
    });

    describe('compareUrlPaths', () => {
        it('should return true when the paths are equal', () => {
            expect(compareUrlPaths('/test', '/test')).toBe(true);
        });

        it('should return true when the paths are equal without initial slash', () => {
            expect(compareUrlPaths('/test', 'test')).toBe(true);
            expect(compareUrlPaths('test', '/test')).toBe(true);
        });

        it('should return false when the paths are not equal', () => {
            expect(compareUrlPaths('/test', '/test2')).toBe(false);
        });
    });

    describe('createFullURL', () => {
        const expectedURL =
            'http://localhost:4200/page?language_id=1&com.dotmarketing.persona.id=persona&variantName=new&experimentId=1&depth=1';
        const params = {
            url: 'page',
            language_id: '1',
            'com.dotmarketing.persona.id': 'persona',
            variantName: 'new',
            experimentId: '1',
            mode: 'EDIT_MODE',
            clientHost: 'http://localhost:4200/',
            depth: '1'
        };

        it('should return the correct url', () => {
            const result = createFullURL(params);
            expect(result).toBe(expectedURL);
        });

        it('should ignore the double slash in the clientHost or path', () => {
            const result = createFullURL({
                ...params,
                clientHost: 'http://localhost:4200//',
                url: '/page'
            });
            expect(result).toBe(expectedURL);
        });

        it('should add the host_id if the side identifier is passed', () => {
            const result = createFullURL(
                {
                    ...params,
                    clientHost: 'http://localhost:4200//',
                    url: '/page'
                },
                '123'
            );
            expect(result).toBe(`${expectedURL}${'&host_id=123'}`);
        });
    });

    describe('getDragItemData', () => {
        it('should return correct data for content-type', () => {
            const dataset = {
                type: 'content-type',
                item: JSON.stringify({
                    contentType: {
                        baseType: 'base-type-1',
                        variable: 'variable-1',
                        name: 'name-1'
                    },
                    move: true
                })
            };

            const result = getDragItemData(dataset);

            expect(result).toEqual({
                baseType: 'base-type-1',
                contentType: 'variable-1',
                draggedPayload: {
                    item: {
                        variable: 'variable-1',
                        name: 'name-1'
                    },
                    type: 'content-type',
                    move: true
                } as ContentTypeDragPayload
            });
        });

        it('should return correct data for contentlet', () => {
            const dataset = {
                type: 'contentlet',
                item: JSON.stringify({
                    contentlet: {
                        baseType: 'base-type-2',
                        contentType: 'content-type-2'
                    },
                    container: {},
                    move: false
                })
            };
            const draggedPayloadExpected: unknown = {
                item: {
                    contentlet: {
                        baseType: 'base-type-2',
                        contentType: 'content-type-2'
                    },
                    container: {}
                },
                type: 'contentlet',
                move: false
            };

            const result = getDragItemData(dataset);

            expect(result).toEqual({
                baseType: 'base-type-2',
                contentType: 'content-type-2',
                draggedPayload: draggedPayloadExpected as ContentletDragPayload
            });
        });

        it('should return null for invalid JSON data', () => {
            const dataset = {
                type: 'contentlet',
                item: 'invalid-json'
            };

            const result = getDragItemData(dataset);

            expect(result).toBeNull();
        });
    });
});
