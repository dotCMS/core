import { Params } from '@angular/router';

import { CurrentUser } from '@dotcms/dotcms-js';
import { DotDevice, DotExperiment, DotExperimentStatus } from '@dotcms/dotcms-models';

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
    getDragItemData,
    createReorderMenuURL,
    getAllowedPageParams,
    getOrientation,
    getWrapperMeasures
} from '.';

import { DotPageApiParams } from '../services/dot-page-api.service';
import { PERSONA_KEY } from '../shared/consts';
import { PAGE_MODE } from '../shared/enums';
import { dotPageContainerStructureMock } from '../shared/mocks';
import { ContentletDragPayload, ContentTypeDragPayload, DotPage } from '../shared/models';
import { Orientation } from '../store/models';

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

        it('should leave as it is for valid url', () => {
            expect(sanitizeURL('this-is-where-the-fun-begins')).toEqual(
                'this-is-where-the-fun-begins'
            );
        });

        it('should return index if the url is index without nested path', () => {
            expect(sanitizeURL('index')).toEqual('index');
            expect(sanitizeURL('index/')).toEqual('index');
            expect(sanitizeURL('/index/')).toEqual('index');
        });

        describe('nested url', () => {
            it('should leave as it is for a nested valid url', () => {
                expect(sanitizeURL('hello-there/general-kenobi')).toEqual(
                    'hello-there/general-kenobi'
                );
            });

            it('should remove index from the end of the url but keep the slash if is a nested path', () => {
                expect(sanitizeURL('my-nested-path/index/')).toEqual('my-nested-path/');
            });

            it('should remove the index if a nested path', () => {
                expect(sanitizeURL('i-have-the-high-ground/index')).toEqual(
                    'i-have-the-high-ground/'
                );
            });

            it('should remove the index if a nested path with slash', () => {
                expect(sanitizeURL('no-index-please/index/')).toEqual('no-index-please/');
            });
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
                [PERSONA_KEY]: 'the-chosen-one',
                experimentId: '123',
                mode: PAGE_MODE.LIVE
            };
            const result = createPageApiUrlWithQueryParams('test', queryParams);
            expect(result).toBe(
                'test?variantName=test&language_id=20&com.dotmarketing.persona.id=the-chosen-one&experimentId=123&mode=LIVE'
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
            [PERSONA_KEY]: 'persona',
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

    describe('getAllowedPageParams', () => {
        it('should filter and return only allowed page params', () => {
            const expected = {
                url: 'some-url',
                mode: 'edit',
                depth: '2',
                clientHost: 'localhost',
                variantName: 'variant',
                language_id: '1',
                experimentId: 'exp123',
                [PERSONA_KEY]: 'persona123'
            } as DotPageApiParams;

            const params: Params = {
                ...expected,
                invalidParam: 'invalid'
            };

            const result = getAllowedPageParams(params);

            expect(result).toEqual(expected);
        });

        it('should return an empty object if no allowed params are present', () => {
            const params: Params = {
                invalidParam1: 'invalid1',
                invalidParam2: 'invalid2'
            };

            const expected = {} as DotPageApiParams;

            const result = getAllowedPageParams(params);

            expect(result).toEqual(expected);
        });

        it('should return an empty object if params is empty', () => {
            const params: Params = {};

            const expected = {} as DotPageApiParams;

            const result = getAllowedPageParams(params);

            expect(result).toEqual(expected);
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
});
