import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { CLIENT_ACTIONS } from '@dotcms/client';
import { DotMessageService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

import { DotEditorDialogService } from './dot-editor-dialog.service';

import { DotActionUrlService } from '../../../services/dot-action-url/dot-action-url.service';
import { DialogStatus, FormStatus } from '../../../shared/enums';
import { ActionPayload, ContainerPayload, DotPage, PageContainer } from '../../../shared/models';

const mockContainer: ContainerPayload = {
    identifier: 'test-container-id',
    acceptTypes: '*',
    maxContentlets: 100,
    variantId: 'default',
    uuid: 'test-uuid'
};

const mockPageContainer: PageContainer = {
    identifier: 'test-container-id',
    uuid: 'test-uuid',
    contentletsId: ['test-contentlet-id']
};

const mockActionPayload: ActionPayload = {
    container: mockContainer,
    language_id: '1',
    pageContainers: [mockPageContainer],
    pageId: 'test-page-id'
};

const mockPage: DotPage = {
    title: 'Test Page',
    working: true,
    workingInode: 'working-inode',
    inode: 'inode',
    identifier: 'test-identifier',
    canEdit: true,
    canRead: true,
    canSeeRules: true,
    pageURI: '/test-page',
    contentType: 'content',
    live: false
};

describe('DotEditorDialogService', () => {
    let spectator: SpectatorService<DotEditorDialogService>;
    let service: DotEditorDialogService;
    let dotActionUrlServiceMock: { getCreateContentletUrl: jest.Mock };
    let dotMessageServiceMock: { get: jest.Mock };

    const createService = createServiceFactory({
        service: DotEditorDialogService,
        mocks: [DotActionUrlService, DotMessageService]
    });

    beforeEach(() => {
        // Mock window.location
        Object.defineProperty(window, 'location', {
            value: {
                href: 'http://test.com',
                origin: 'http://test.com'
            },
            writable: true
        });

        spectator = createService();
        service = spectator.service;
        dotActionUrlServiceMock = spectator.inject(DotActionUrlService) as {
            getCreateContentletUrl: jest.Mock;
        };
        dotMessageServiceMock = spectator.inject(DotMessageService) as { get: jest.Mock };
        dotActionUrlServiceMock.getCreateContentletUrl.mockReturnValue(
            of('http://test.com/create-contentlet')
        );
        dotMessageServiceMock.get.mockImplementation(
            (key, defaultValue) => `Message: ${key}${defaultValue ? ` - ${defaultValue}` : ''}`
        );
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should have default state', () => {
        expect(service.state()).toEqual({
            header: '',
            url: '',
            type: null,
            status: DialogStatus.IDLE,
            form: {
                status: FormStatus.PRISTINE,
                isTranslation: false
            },
            clientAction: CLIENT_ACTIONS.NOOP
        });
    });

    describe('createContentletFromPalette', () => {
        it('should update the state with create contentlet url', () => {
            service.createContentletFromPalette({
                name: 'Test Content',
                variable: 'testVariable',
                actionPayload: mockActionPayload,
                language_id: '2'
            });

            expect(dotActionUrlServiceMock.getCreateContentletUrl).toHaveBeenCalledWith(
                'testVariable',
                '2'
            );
            expect(service.state().url).toBe('http://test.com/create-contentlet');
            expect(service.state().header).toBe(
                'Message: contenttypes.content.create.contenttype - Test Content'
            );
            expect(service.state().status).toBe(DialogStatus.LOADING);
            expect(service.state().type).toBe('content');
        });

        it('should use default language_id when not provided', () => {
            service.createContentletFromPalette({
                name: 'Test Content',
                variable: 'testVariable',
                actionPayload: mockActionPayload
            });

            expect(dotActionUrlServiceMock.getCreateContentletUrl).toHaveBeenCalledWith(
                'testVariable',
                1
            );
        });
    });

    describe('openDialogOnURL', () => {
        it('should update state with the provided URL and title', () => {
            service.openDialogOnURL({
                url: 'http://test.com/dialog',
                title: 'Test Dialog'
            });

            expect(service.state().url).toBe('http://test.com/dialog');
            expect(service.state().header).toBe('Test Dialog');
            expect(service.state().status).toBe(DialogStatus.LOADING);
            expect(service.state().type).toBe('content');
        });
    });

    describe('createContentlet', () => {
        it('should update state with the provided URL and content type', () => {
            service.createContentlet({
                url: 'http://test.com/create',
                contentType: 'Test Type',
                actionPayload: mockActionPayload
            });

            expect(service.state().url).toBe('http://test.com/create');
            expect(service.state().header).toBe(
                'Message: contenttypes.content.create.contenttype - Test Type'
            );
            expect(service.state().status).toBe(DialogStatus.LOADING);
            expect(service.state().type).toBe('content');
            expect(service.state().actionPayload).toEqual(mockActionPayload);
        });
    });

    describe('loadingIframe', () => {
        it('should set loading state for iframe', () => {
            service.loadingIframe('Loading Frame');

            expect(service.state().header).toBe('Loading Frame');
            expect(service.state().status).toBe(DialogStatus.LOADING);
            expect(service.state().url).toBe('');
            expect(service.state().type).toBe('content');
        });
    });

    describe('editContentlet', () => {
        it('should update state with edit contentlet URL', () => {
            service.editContentlet({
                inode: 'test-inode',
                title: 'Edit Content',
                clientAction: CLIENT_ACTIONS.EDIT_CONTENTLET,
                angularCurrentPortlet: 'test-portlet'
            });

            expect(service.state().header).toBe('Edit Content');
            expect(service.state().status).toBe(DialogStatus.LOADING);
            expect(service.state().type).toBe('content');
            expect(service.state().clientAction).toBe(CLIENT_ACTIONS.EDIT_CONTENTLET);
            expect(service.state().url).toContain('test-inode');
            expect(service.state().url).toContain('test-portlet');
        });

        it('should use default client action when not provided', () => {
            service.editContentlet({
                inode: 'test-inode',
                title: 'Edit Content'
            });

            expect(service.state().clientAction).toBe(CLIENT_ACTIONS.NOOP);
        });
    });

    describe('editUrlContentMapContentlet', () => {
        it('should update state with URL content map URL', () => {
            service.editUrlContentMapContentlet({
                inode: 'test-inode',
                title: 'Edit URL Map'
            });

            expect(service.state().header).toBe('Edit URL Map');
            expect(service.state().status).toBe(DialogStatus.LOADING);
            expect(service.state().type).toBe('content');
            expect(service.state().url).toContain('test-inode');
            expect(service.state().url).toContain('isURLMap=true');
        });
    });

    describe('translatePage', () => {
        it('should update state with translate page URL', () => {
            service.translatePage({
                page: mockPage,
                newLanguage: 2
            });

            expect(service.state().header).toBe('Test Page');
            expect(service.state().status).toBe(DialogStatus.LOADING);
            expect(service.state().type).toBe('content');
            expect(service.state().form.isTranslation).toBe(true);
            expect(service.state().url).toContain('working-inode');
            expect(service.state().url).toContain('lang=2');
        });

        it('should use inode when working is false', () => {
            const nonWorkingPage: DotPage = {
                ...mockPage,
                working: false
            };

            service.translatePage({
                page: nonWorkingPage,
                newLanguage: 2
            });

            expect(service.state().url).toContain('_content_sibbling=inode');
        });
    });

    describe('addWidget', () => {
        it('should update state with widget URL', () => {
            service.addWidget(mockActionPayload);

            const url = service.state().url;
            const decodedUrl = decodeURIComponent(url);

            expect(service.state().header).toBe(
                'Message: edit.ema.page.dialog.header.search.content'
            );
            expect(service.state().status).toBe(DialogStatus.LOADING);
            expect(service.state().type).toBe('content');
            expect(decodedUrl).toContain('container_id=test-container-id');
            expect(decodedUrl).toContain(`add=${DotCMSBaseTypesContentTypes.WIDGET}`);
            expect(decodedUrl).toContain('language_id=1');
        });
    });

    describe('addContentlet', () => {
        it('should update state with contentlet URL using container acceptTypes', () => {
            const customActionPayload: ActionPayload = {
                ...mockActionPayload,
                container: {
                    ...mockContainer,
                    acceptTypes: 'Widget,Form'
                }
            };

            service.addContentlet(customActionPayload);

            const url = service.state().url;
            const decodedUrl = decodeURIComponent(url);
            expect(service.state().header).toBe(
                'Message: edit.ema.page.dialog.header.search.content'
            );
            expect(service.state().status).toBe(DialogStatus.LOADING);
            expect(service.state().type).toBe('content');
            expect(decodedUrl).toContain('container_id=test-container-id');
            expect(decodedUrl).toContain('add=Widget,Form');
            expect(decodedUrl).toContain('language_id=1');
        });

        it('should use wildcard when acceptTypes is not provided', () => {
            const customActionPayload: ActionPayload = {
                ...mockActionPayload,
                container: {
                    ...mockContainer,
                    acceptTypes: undefined
                }
            };

            service.addContentlet(customActionPayload);

            expect(service.state().url).toContain('add=*');
        });
    });

    describe('addForm', () => {
        it('should update state for form selection', () => {
            service.addForm(mockActionPayload);

            expect(service.state().header).toBe('Message: edit.ema.page.dialog.header.search.form');
            expect(service.state().status).toBe(DialogStatus.LOADING);
            expect(service.state().url).toBeNull();
            expect(service.state().type).toBe('form');
            expect(service.state().actionPayload).toEqual(mockActionPayload);
        });
    });

    describe('form state methods', () => {
        it('should set form status to DIRTY', () => {
            service.setDirty();
            expect(service.state().form.status).toBe(FormStatus.DIRTY);
        });

        it('should set form status to SAVED', () => {
            service.setSaved();
            expect(service.state().form.status).toBe(FormStatus.SAVED);
        });
    });

    describe('resetDialog', () => {
        it('should reset dialog state to default values', () => {
            // First set some non-default state
            service.setDirty();
            service.addForm(mockActionPayload);

            // Then reset
            service.resetDialog();

            expect(service.state()).toEqual({
                header: '',
                url: '',
                type: null,
                status: DialogStatus.IDLE,
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                actionPayload: undefined,
                clientAction: CLIENT_ACTIONS.NOOP
            });
        });
    });

    describe('setStatus', () => {
        it('should update dialog status', () => {
            service.setStatus(DialogStatus.LOADING);
            expect(service.state().status).toBe(DialogStatus.LOADING);
        });
    });

    describe('getVariantName', () => {
        it('should get variant name from URL', () => {
            // Mock URL with variantName
            Object.defineProperty(window, 'location', {
                value: {
                    href: 'http://test.com?variantName=test-variant',
                    origin: 'http://test.com'
                },
                writable: true
            });

            expect(service.getVariantName()).toBe('test-variant');
        });

        it('should return default variant ID when variantName is not in URL', () => {
            // Mock URL without variantName
            Object.defineProperty(window, 'location', {
                value: {
                    href: 'http://test.com',
                    origin: 'http://test.com'
                },
                writable: true
            });

            expect(service.getVariantName()).toBe(DEFAULT_VARIANT_ID);
        });
    });
});
