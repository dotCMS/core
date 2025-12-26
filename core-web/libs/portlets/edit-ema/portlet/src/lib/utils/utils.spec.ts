import { CurrentUser } from '@dotcms/dotcms-js';
import { DotContainer, DotDevice, DotExperiment, DotExperimentStatus } from '@dotcms/dotcms-models';
import { DotCMSPage, DotCMSViewAsPersona, UVE_MODE } from '@dotcms/types';

import {
    deleteContentletFromContainer,
    insertContentletInContainer,
    sanitizeURL,
    getPersonalization,
    getFullPageURL,
    SDK_EDITOR_SCRIPT_SOURCE,
    getBaseHrefFromPageURI,
    injectBaseTag,
    computeIsPageLocked,
    computeCanEditPage,
    mapContainerStructureToArrayOfContainers,
    mapContainerStructureToDotContainerMap,
    areContainersEquals,
    compareUrlPaths,
    createFullURL,
    getDragItemData,
    createReorderMenuURL,
    getOrientation,
    getWrapperMeasures,
    normalizeQueryParams,
    convertUTCToLocalTime
} from '.';

import { DEFAULT_PERSONA, PERSONA_KEY } from '../shared/consts';
import { dotPageContainerStructureMock } from '../shared/mocks';
import { ContentletDragPayload, ContentTypeDragPayload } from '../shared/models';
import { Orientation } from '../store/models';

const generatePageAndUser = ({ locked, lockedBy, userId }) => ({
    page: {
        locked,
        lockedBy
    } as DotCMSPage,
    currentUser: {
        userId
    } as CurrentUser
});

describe('utils functions', () => {
    describe('SDK Editor Script Source', () => {
        it('should return the correct script source', () => {
            expect(SDK_EDITOR_SCRIPT_SOURCE).toEqual('/ext/uve/dot-uve.js');
        });
    });

    describe('base tag helpers', () => {
        it('should build base href from pageURI', () => {
            expect(getBaseHrefFromPageURI('/golf-outing-fundraiser', 'https://example.com')).toBe(
                'https://example.com/'
            );
            expect(
                getBaseHrefFromPageURI('/dotAdmin/golf-outing-fundraiser', 'https://example.com')
            ).toBe('https://example.com/dotAdmin/');
            expect(
                getBaseHrefFromPageURI(
                    'https://example.com/news/article-1',
                    'https://irrelevant.com'
                )
            ).toBe('https://example.com/news/');
        });

        it('should inject base tag when missing and head exists', () => {
            const html = '<html><head><title>x</title></head><body><a href="a">a</a></body></html>';
            const out = injectBaseTag({
                html,
                url: '/dotAdmin/golf-outing-fundraiser',
                origin: 'https://example.com'
            });

            expect(out).toContain('<base href="https://example.com/dotAdmin/">');
        });

        it('should not inject base tag when base already exists', () => {
            const html =
                '<html><head><base href="https://example.com/"><title>x</title></head><body></body></html>';
            const out = injectBaseTag({
                html,
                url: '/dotAdmin/golf-outing-fundraiser',
                origin: 'https://example.com'
            });

            // still only one base tag
            expect(out.match(/<base\b/gi)?.length).toBe(1);
        });

        it('should be a no-op when pageURI is missing', () => {
            const html = '<html><head><title>x</title></head><body></body></html>';
            expect(
                injectBaseTag({
                    html,
                    url: undefined,
                    origin: 'https://example.com'
                })
            ).toBe(html);
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
                    },
                    {
                        identifier: 'test-2',
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
                    },
                    {
                        identifier: 'test-2',
                        uuid: 'test',
                        contentletsId: ['test'],
                        personaTag: 'test' // In the last version this was not being added and it lead to saving content to the default persona and messing up with the pages
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

        it('should add container to pageContainers if it does not exist - issue #31790', () => {
            // Current page with no containers
            const pageContainers = [];

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
                identifier: 'test',
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
                personaTag: 'persona-tag'
            });

            expect(result.pageContainers).toEqual([
                {
                    acceptTypes: 'test',
                    maxContentlets: 1,
                    variantId: '1',
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: [],
                    personaTag: 'persona-tag'
                }
            ]);
            expect(result.contentletsId).toEqual([]);
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
                maxContentlets: 2,
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
                maxContentlets: 4,
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
                errorCode: 'DUPLICATE_CONTENT',
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: ['test']
                    }
                ]
            });
        });

        it('should add container to pageContainers if it does not exist - issue #31790', () => {
            // Current page with no containers
            const pageContainers = [];

            // Container where we want to insert the contentlet
            const container = {
                identifier: 'test',
                uuid: 'test',
                contentletsId: [],
                maxContentlets: 1,
                acceptTypes: 'test',
                variantId: '1'
            };

            // Contentlet position mark
            const contentlet = {
                identifier: 'contentlet-id',
                inode: 'contentlet-inode',
                title: 'test',
                contentType: 'test'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet,
                pageId: 'page-id',
                language_id: '1',
                newContentletId: 'new-contentlet-id',
                personaTag: 'persona-tag'
            });

            expect(result).toEqual({
                didInsert: true,
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: ['new-contentlet-id'],
                        personaTag: 'persona-tag',
                        acceptTypes: 'test',
                        maxContentlets: 1,
                        variantId: '1'
                    }
                ]
            });
        });

        it('should add container to pageContainers and insert in specific position - issue #31790', () => {
            // Current page with no containers
            const pageContainers = [];

            // Container where we want to insert the contentlet
            const container = {
                identifier: 'test',
                uuid: 'test',
                contentletsId: ['test123'],
                maxContentlets: 2,
                acceptTypes: 'test',
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
                newContentletId: '000',
                personaTag: 'persona-tag'
            });

            expect(result).toEqual({
                didInsert: true,
                pageContainers: [
                    {
                        identifier: 'test',
                        uuid: 'test',
                        contentletsId: ['test123', '000'],
                        personaTag: 'persona-tag',
                        acceptTypes: 'test',
                        maxContentlets: 2,
                        variantId: '1'
                    }
                ]
            });
        });

        it('should allow inserting into empty container when limit is 1', () => {
            const pageContainers = [
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: [],
                    acceptTypes: 'test',
                    maxContentlets: 1,
                    variantId: '1'
                }
            ];

            const container = {
                identifier: 'test',
                uuid: 'test',
                contentletsId: [],
                maxContentlets: 1,
                acceptTypes: 'test',
                variantId: '1'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet: {
                    identifier: 'contentlet1',
                    inode: 'inode1',
                    title: 'test',
                    contentType: 'test'
                },
                pageId: 'test',
                language_id: 'test',
                newContentletId: 'contentlet1',
                personaTag: 'persona-tag'
            });

            expect(result.didInsert).toBe(true);
            expect(result.pageContainers[0].contentletsId).toEqual(['contentlet1']);
        });

        it('should NOT allow inserting when container with limit 1 already has 1 contentlet', () => {
            const pageContainers = [
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['contentlet1'],
                    acceptTypes: 'test',
                    maxContentlets: 1,
                    variantId: '1'
                }
            ];

            const container = {
                identifier: 'test',
                uuid: 'test',
                contentletsId: ['contentlet1'],
                maxContentlets: 1,
                acceptTypes: 'test',
                variantId: '1'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet: {
                    identifier: 'contentlet1',
                    inode: 'inode1',
                    title: 'test',
                    contentType: 'test'
                },
                pageId: 'test',
                language_id: 'test',
                newContentletId: 'contentlet2',
                personaTag: 'persona-tag'
            });

            expect(result.didInsert).toBe(false);
            expect(result.errorCode).toBe('CONTAINER_LIMIT_REACHED');
            expect(result.pageContainers[0].contentletsId).toEqual(['contentlet1']);
        });

        it('should allow inserting into container with limit 2 that has 1 contentlet', () => {
            const pageContainers = [
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['contentlet1'],
                    acceptTypes: 'test',
                    maxContentlets: 2,
                    variantId: '1'
                }
            ];

            const container = {
                identifier: 'test',
                uuid: 'test',
                contentletsId: ['contentlet1'],
                maxContentlets: 2,
                acceptTypes: 'test',
                variantId: '1'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet: {
                    identifier: 'contentlet1',
                    inode: 'inode1',
                    title: 'test',
                    contentType: 'test'
                },
                pageId: 'test',
                language_id: 'test',
                newContentletId: 'contentlet2',
                personaTag: 'persona-tag'
            });

            expect(result.didInsert).toBe(true);
            expect(result.pageContainers[0].contentletsId).toEqual(['contentlet1', 'contentlet2']);
        });

        it('should NOT allow inserting when container with limit 2 already has 2 contentlets', () => {
            const pageContainers = [
                {
                    identifier: 'test',
                    uuid: 'test',
                    contentletsId: ['contentlet1', 'contentlet2'],
                    acceptTypes: 'test',
                    maxContentlets: 2,
                    variantId: '1'
                }
            ];

            const container = {
                identifier: 'test',
                uuid: 'test',
                contentletsId: ['contentlet1', 'contentlet2'],
                maxContentlets: 2,
                acceptTypes: 'test',
                variantId: '1'
            };

            const result = insertContentletInContainer({
                pageContainers,
                container,
                contentlet: {
                    identifier: 'contentlet1',
                    inode: 'inode1',
                    title: 'test',
                    contentType: 'test'
                },
                pageId: 'test',
                language_id: 'test',
                newContentletId: 'contentlet3',
                personaTag: 'persona-tag'
            });

            expect(result.didInsert).toBe(false);
            expect(result.errorCode).toBe('CONTAINER_LIMIT_REACHED');
            expect(result.pageContainers[0].contentletsId).toEqual(['contentlet1', 'contentlet2']);
        });
    });

    describe('url sanitize', () => {
        it('should left the / as /', () => {
            expect(sanitizeURL('/')).toEqual('/');
        });

        it('should clean multiple slashes', () => {
            expect(sanitizeURL('//////')).toEqual('/');
        });

        it('should clean multiple slashes', () => {
            expect(sanitizeURL('//index////')).toEqual('/index/');
        });

        it('should clean query params from the url', () => {
            expect(sanitizeURL('hello-there/general-kenobi?test=1&test2=2')).toEqual(
                'hello-there/general-kenobi'
            );

            expect(sanitizeURL('/hello-there/general-kenobi?test=1&test2=2')).toEqual(
                '/hello-there/general-kenobi'
            );

            expect(sanitizeURL('/hello-there/general-kenobi/?test=1&test2=2')).toEqual(
                '/hello-there/general-kenobi/'
            );
        });

        describe('nested url', () => {
            it('should leave as it is for a nested valid url', () => {
                expect(sanitizeURL('hello-there/general-kenobi/')).toEqual(
                    'hello-there/general-kenobi/'
                );
            });

            it('should clean multiple slashes in a nested url', () => {
                expect(sanitizeURL('hello-there////general-kenobi//////')).toEqual(
                    'hello-there/general-kenobi/'
                );
            });
        });
    });

    describe('personalization', () => {
        it('should return the correct personalization when persona exists', () => {
            const personalization = getPersonalization({
                contentType: 'persona',
                keyTag: 'adminUser'
            } as DotCMSViewAsPersona);

            expect(personalization).toBe('dot:persona:adminUser');
        });

        it('should return the correct personalization when persona does not exist', () => {
            const personalization = getPersonalization({} as DotCMSViewAsPersona);
            expect(personalization).toBe('dot:default');
        });
    });

    describe('getFullPageURL', () => {
        it('should return the correct query params', () => {
            const params = {
                url: 'test',
                variantName: 'test',
                language_id: '20',
                [PERSONA_KEY]: 'the-chosen-one',
                experimentId: '123',
                mode: UVE_MODE.LIVE
            };
            const result = getFullPageURL({ url: 'test', params });
            expect(result).toBe(
                'test?variantName=test&language_id=20&com.dotmarketing.persona.id=the-chosen-one&experimentId=123&mode=LIVE'
            );
        });

        it('should ignore the undefined queryParams', () => {
            const params = {
                url: 'test',
                language_id: '20',
                [PERSONA_KEY]: 'the-chosen-one',
                variantName: 'test',
                experimentId: undefined
            };
            const result = getFullPageURL({ url: 'test', params });
            expect(result).toBe(
                'test?language_id=20&com.dotmarketing.persona.id=the-chosen-one&variantName=test'
            );
        });

        it('should remove the clientHost if it is passed', () => {
            const params = {
                url: 'test',
                language_id: '20',
                [PERSONA_KEY]: 'the-chosen-one',
                variantName: 'test',
                clientHost: 'http://localhost:4200'
            };

            const result = getFullPageURL({ url: 'test', params });
            expect(result).toBe(
                'test?language_id=20&com.dotmarketing.persona.id=the-chosen-one&variantName=test'
            );
        });
    });

    describe('computeIsPageLocked', () => {
        describe('with legacy behavior (feature flag disabled)', () => {
            it('should return false when the page is unlocked', () => {
                const { page, currentUser } = generatePageAndUser({
                    locked: false,
                    lockedBy: '123',
                    userId: '123'
                });

                const result = computeIsPageLocked(page, currentUser, false);

                expect(result).toBe(false);
            });

            it('should return false when the page is locked and is the same user', () => {
                const { page, currentUser } = generatePageAndUser({
                    locked: true,
                    lockedBy: '123',
                    userId: '123'
                });

                const result = computeIsPageLocked(page, currentUser, false);

                expect(result).toBe(false);
            });

            it('should return true when the page is locked and is not the same user', () => {
                const { page, currentUser } = generatePageAndUser({
                    locked: true,
                    lockedBy: '123',
                    userId: '456'
                });

                const result = computeIsPageLocked(page, currentUser, false);

                expect(result).toBe(true);
            });
        });

        describe('with new behavior (feature flag enabled)', () => {
            it('should return false when the page is unlocked', () => {
                const { page, currentUser } = generatePageAndUser({
                    locked: false,
                    lockedBy: '123',
                    userId: '123'
                });

                const result = computeIsPageLocked(page, currentUser, true);

                expect(result).toBe(false);
            });

            it('should return true when the page is locked by current user', () => {
                const { page, currentUser } = generatePageAndUser({
                    locked: true,
                    lockedBy: '123',
                    userId: '123'
                });

                const result = computeIsPageLocked(page, currentUser, true);

                expect(result).toBe(true);
            });

            it('should return true when the page is locked by another user', () => {
                const { page, currentUser } = generatePageAndUser({
                    locked: true,
                    lockedBy: '123',
                    userId: '456'
                });

                const result = computeIsPageLocked(page, currentUser, true);

                expect(result).toBe(true);
            });
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
                '123': dotPageContainerStructureMock['123'].container as unknown as DotContainer
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
            [PERSONA_KEY]: 'persona',
            variantName: 'new',
            experimentId: '1',
            mode: UVE_MODE.EDIT,
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

    describe('createReorderMenuURL', () => {
        it('should create the correct URL', () => {
            const result = createReorderMenuURL({
                startLevel: 1,
                depth: 1,
                pagePath: '123',
                hostId: '456'
            });

            expect(result).toEqual(
                'http://localhost/c/portal/layout?p_l_id=2df9f117-b140-44bf-93d7-5b10a36fb7f9&p_p_id=site-browser&p_p_action=1&p_p_state=maximized&_site_browser_struts_action=%2Fext%2Ffolders%2Forder_menu&startLevel=1&depth=1&pagePath=123&hostId=456'
            );
        });
    });

    describe('getWrapperMeasures', () => {
        it('should return correct measures for landscape orientation', () => {
            const device: DotDevice = {
                cssHeight: '1200',
                cssWidth: '800',
                inode: 'some-inode'
            } as DotDevice;

            const result = getWrapperMeasures(device, Orientation.LANDSCAPE);
            expect(result).toEqual({ width: '1200px', height: '800px' });
        });

        it('should return correct measures for portrait orientation', () => {
            const device: DotDevice = {
                cssHeight: '800',
                cssWidth: '1200',
                inode: 'some-inode'
            } as DotDevice;

            const result = getWrapperMeasures(device, Orientation.PORTRAIT);
            expect(result).toEqual({ width: '800px', height: '1200px' });
        });

        it('should use percentage unit for default inode', () => {
            const device: DotDevice = {
                cssHeight: '100',
                cssWidth: '100',
                inode: 'default'
            } as DotDevice;

            const result = getWrapperMeasures(device);
            expect(result).toEqual({ width: '100%', height: '100%' });
        });
    });

    describe('getOrientation', () => {
        it('should return PORTRAIT for taller devices', () => {
            const device: DotDevice = {
                cssHeight: '1200',
                cssWidth: '800'
            } as DotDevice;

            const result = getOrientation(device);
            expect(result).toBe(Orientation.PORTRAIT);
        });

        it('should return LANDSCAPE for wider devices', () => {
            const device: DotDevice = {
                cssHeight: '800',
                cssWidth: '1200'
            } as DotDevice;

            const result = getOrientation(device);
            expect(result).toBe(Orientation.LANDSCAPE);
        });
    });

    describe('normalizeQueryParams', () => {
        describe('persona handling', () => {
            it('should remove PERSONA_KEY if it equals DEFAULT_PERSONA.identifier', () => {
                const params = {
                    [PERSONA_KEY]: DEFAULT_PERSONA.identifier,
                    someOtherKey: 'someValue'
                };

                const result = normalizeQueryParams(params);

                expect(result).toEqual({
                    someOtherKey: 'someValue'
                });
            });

            it('should rename PERSONA_KEY to personaId if not default', () => {
                const params = {
                    [PERSONA_KEY]: 'customPersonaId',
                    anotherKey: 'anotherValue'
                };

                const result = normalizeQueryParams(params);

                expect(result).toEqual({
                    personaId: 'customPersonaId',
                    anotherKey: 'anotherValue'
                });
            });
        });

        describe('clientHost handling', () => {
            it('should remove clientHost when it matches baseClientHost exactly', () => {
                const params = {
                    clientHost: 'http://example.com',
                    someKey: 'someValue'
                };

                const result = normalizeQueryParams(params, 'http://example.com');

                expect(result).toEqual({
                    someKey: 'someValue'
                });
            });

            it('should remove clientHost when it matches baseClientHost with trailing slash', () => {
                const params = {
                    clientHost: 'http://example.com/',
                    someKey: 'someValue'
                };

                const result = normalizeQueryParams(params, 'http://example.com');

                expect(result).toEqual({
                    someKey: 'someValue'
                });
            });

            it('should keep clientHost if it differs from baseClientHost', () => {
                const params = {
                    clientHost: 'http://example.com',
                    someKey: 'someValue'
                };

                const result = normalizeQueryParams(params, 'http://different.com');

                expect(result).toEqual({
                    clientHost: 'http://example.com',
                    someKey: 'someValue'
                });
            });

            it('should keep clientHost if baseClientHost is not provided', () => {
                const params = {
                    clientHost: 'http://example.com',
                    someKey: 'someValue'
                };

                const result = normalizeQueryParams(params);

                expect(result).toEqual(params);
            });
        });

        describe('edge cases', () => {
            it('should handle empty params object', () => {
                const params = {};

                const result = normalizeQueryParams(params);

                expect(result).toEqual({});
            });

            it('should handle params with no special keys', () => {
                const params = {
                    someKey: 'someValue',
                    anotherKey: 'anotherValue'
                };

                const result = normalizeQueryParams(params);

                expect(result).toEqual(params);
            });
        });
    });

    describe('convertUTCToLocalTime', () => {
        it('should convert UTC time to local time representation', () => {
            // Create a date representing "2025-11-19T17:13:00.000Z" (5:13 PM UTC)
            const utcDate = new Date('2025-11-19T17:13:00.000Z');

            const result = convertUTCToLocalTime(utcDate);

            // The local time should show 17:13 (5:13 PM) in local timezone
            expect(result.getHours()).toBe(17);
            expect(result.getMinutes()).toBe(13);
            expect(result.getSeconds()).toBe(0);
            expect(result.getFullYear()).toBe(2025);
            expect(result.getMonth()).toBe(10); // November (0-indexed)
            expect(result.getDate()).toBe(19);
        });

        it('should preserve all time components including milliseconds', () => {
            const utcDate = new Date('2025-11-19T14:30:45.123Z');

            const result = convertUTCToLocalTime(utcDate);

            expect(result.getHours()).toBe(14);
            expect(result.getMinutes()).toBe(30);
            expect(result.getSeconds()).toBe(45);
            expect(result.getMilliseconds()).toBe(123);
        });

        it('should handle midnight UTC correctly', () => {
            const utcDate = new Date('2025-11-19T00:00:00.000Z');

            const result = convertUTCToLocalTime(utcDate);

            expect(result.getHours()).toBe(0);
            expect(result.getMinutes()).toBe(0);
            expect(result.getSeconds()).toBe(0);
        });

        it('should handle end of day UTC correctly', () => {
            const utcDate = new Date('2025-11-19T23:59:59.999Z');

            const result = convertUTCToLocalTime(utcDate);

            expect(result.getHours()).toBe(23);
            expect(result.getMinutes()).toBe(59);
            expect(result.getSeconds()).toBe(59);
            expect(result.getMilliseconds()).toBe(999);
        });

        it('should handle leap year dates correctly', () => {
            const utcDate = new Date('2024-02-29T12:00:00.000Z'); // Leap year

            const result = convertUTCToLocalTime(utcDate);

            expect(result.getFullYear()).toBe(2024);
            expect(result.getMonth()).toBe(1); // February (0-indexed)
            expect(result.getDate()).toBe(29);
            expect(result.getHours()).toBe(12);
        });
    });
});
