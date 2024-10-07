import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';

import {
    DotContentTypeService,
    DotFireActionOptions,
    DotHttpErrorManagerService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSWorkflowAction
} from '@dotcms/dotcms-models';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { DotEditContentStore } from './edit-content.store';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { CONTENT_TYPE_MOCK } from '../../../utils/mocks';

describe('DotEditContentStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotEditContentStore>>;
    let store: InstanceType<typeof DotEditContentStore>;

    let contentTypeService: SpyObject<DotContentTypeService>;

    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;
    let dotEditContentService: SpyObject<DotEditContentService>;

    let mockActivatedRouteParams: { [key: string]: unknown };
    let router: SpyObject<Router>;

    let workflowActionsService: SpyObject<DotWorkflowsActionsService>;
    let workflowActionsFireService: SpyObject<DotWorkflowActionsFireService>;

    const createService = createServiceFactory({
        service: DotEditContentStore,
        mocks: [
            DotWorkflowActionsFireService,
            DotContentTypeService,
            DotEditContentService,
            DotHttpErrorManagerService,
            DotWorkflowsActionsService
        ],
        providers: [
            {
                provide: ActivatedRoute,
                useValue: {
                    get snapshot() {
                        return { params: mockActivatedRouteParams };
                    }
                }
            },

            mockProvider(Router, {
                navigate: jest.fn().mockReturnValue(Promise.resolve(true))
            })
        ]
    });

    beforeEach(() => {
        mockActivatedRouteParams = {};

        spectator = createService();

        store = spectator.service;
        contentTypeService = spectator.inject(DotContentTypeService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        workflowActionsService = spectator.inject(DotWorkflowsActionsService);
        workflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        dotEditContentService = spectator.inject(DotEditContentService);

        router = spectator.inject(Router);
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should create the store', () => {
        expect(spectator.service).toBeDefined();
    });

    describe('initializeNewContent', () => {
        it('should initialize new content successfully', () => {
            const testContentType = 'testContentType';

            contentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionsService.getDefaultActions.mockReturnValue(of(mockWorkflowsActions));

            store.initializeNewContent(testContentType);

            // use the proper contentType for get the data
            expect(contentTypeService.getContentType).toHaveBeenCalledWith(testContentType);
            expect(workflowActionsService.getDefaultActions).toHaveBeenCalledWith(testContentType);

            expect(store.contentType()).toEqual(CONTENT_TYPE_MOCK);
            expect(store.actions()).toEqual(mockWorkflowsActions);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.error()).toBeNull();
        });

        it('should handle error when initializing new content', fakeAsync(() => {
            const mockError = new HttpErrorResponse({ status: 404, statusText: 'Not Found' });

            contentTypeService.getContentType.mockReturnValue(throwError(() => mockError));
            workflowActionsService.getDefaultActions.mockReturnValue(of(mockWorkflowsActions));

            store.initializeNewContent('testContentType');

            expect(store.error()).toBe('Error initializing content');
            expect(store.status()).toBe(ComponentStatus.ERROR);
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
        }));
    });

    describe('initializeExistingContent', () => {
        const testInode = '123-test-inode';
        it('should initialize existing content successfully', () => {
            const mockContentlet = {
                inode: testInode,
                contentType: 'testContentType'
            } as DotCMSContentlet;

            const mockContentType = {
                id: '1',
                name: 'Test Content Type'
            } as DotCMSContentType;

            const mockActions = [{ id: '1', name: 'Test Action' }] as DotCMSWorkflowAction[];

            dotEditContentService.getContentById.mockReturnValue(of(mockContentlet));
            contentTypeService.getContentType.mockReturnValue(of(mockContentType));
            workflowActionsService.getByInode.mockReturnValue(of(mockActions));

            store.initializeExistingContent(testInode);

            expect(dotEditContentService.getContentById).toHaveBeenCalledWith(testInode);
            expect(contentTypeService.getContentType).toHaveBeenCalledWith(
                mockContentlet.contentType
            );
            expect(workflowActionsService.getByInode).toHaveBeenCalledWith(
                testInode,
                expect.anything()
            );

            expect(store.contentlet()).toEqual(mockContentlet);
            expect(store.contentType()).toEqual(mockContentType);
            expect(store.actions()).toEqual(mockActions);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.error()).toBe(null);
        });

        it('should handle error when initializing existing content', fakeAsync(() => {
            const mockError = new HttpErrorResponse({ status: 404, statusText: 'Not Found' });

            dotEditContentService.getContentById.mockReturnValue(throwError(() => mockError));

            store.initializeExistingContent(testInode);
            tick();

            expect(dotEditContentService.getContentById).toHaveBeenCalledWith(testInode);
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
            expect(router.navigate).toHaveBeenCalledWith(['/c/content']);

            expect(store.status()).toBe(ComponentStatus.ERROR);
        }));
    });

    describe('fireWorkflowAction', () => {
        const mockOptions: DotFireActionOptions<{ [key: string]: string | object }> = {
            inode: '123',
            actionId: 'publish'
        };

        it('should fire workflow action successfully', fakeAsync(() => {
            const mockContentlet = { inode: '456', contentType: 'testType' } as DotCMSContentlet;
            const mockActions = [{ id: '1', name: 'Test Action' }] as DotCMSWorkflowAction[];

            workflowActionsFireService.fireTo.mockReturnValue(of(mockContentlet));
            workflowActionsService.getByInode.mockReturnValue(of(mockActions));

            store.fireWorkflowAction(mockOptions);
            tick();

            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.contentlet()).toEqual(mockContentlet);
            expect(store.actions()).toEqual(mockActions);
            expect(store.error()).toBeNull();

            expect(workflowActionsFireService.fireTo).toHaveBeenCalledWith(mockOptions);
            expect(workflowActionsService.getByInode).toHaveBeenCalledWith(
                mockContentlet.inode,
                DotRenderMode.EDITING
            );
            expect(router.navigate).toHaveBeenCalledWith(['/content', mockContentlet.inode], {
                replaceUrl: true,
                queryParamsHandling: 'preserve'
            });
        }));

        it('should handle error when firing workflow action', fakeAsync(() => {
            const mockError = new HttpErrorResponse({
                status: 500,
                statusText: 'Internal Server Error'
            });

            workflowActionsFireService.fireTo.mockReturnValue(throwError(() => mockError));

            store.fireWorkflowAction(mockOptions);
            tick();

            expect(store.status()).toBe(ComponentStatus.ERROR);
            expect(store.error()).toBe('Error firing workflow action');
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
        }));

        it('should navigate to content list if contentlet has no inode', fakeAsync(() => {
            const mockContentletWithoutInode = { contentType: 'testType' } as DotCMSContentlet;

            workflowActionsFireService.fireTo.mockReturnValue(of(mockContentletWithoutInode));

            store.fireWorkflowAction(mockOptions);
            tick();

            expect(router.navigate).toHaveBeenCalledWith(['/c/content']);
        }));
    });

    describe('toggleSidebar', () => {
        it('should toggle sidebar state', () => {
            expect(store.showSidebar()).toBe(true);
            store.toggleSidebar();
            expect(store.showSidebar()).toBe(false);
            store.toggleSidebar();
            expect(store.showSidebar()).toBe(true);
        });
    });
});
