import { expect } from '@jest/globals';
import { byTestId } from '@ngneat/spectator';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent, MockPipe } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';
import { MessagesModule } from 'primeng/messages';

import {
    DotContentTypeService,
    DotMessageService,
    DotRenderMode,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotFormatDateService
} from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { EditContentLayoutComponent } from './edit-content.layout.component';
import { DotEditContentStore } from './store/edit-content.store';

import { DotEditContentAsideComponent } from '../../components/dot-edit-content-aside/dot-edit-content-aside.component';
import { DotEditContentFormComponent } from '../../components/dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentToolbarComponent } from '../../components/dot-edit-content-toolbar/dot-edit-content-toolbar.component';
import { EditContentPayload } from '../../models/dot-edit-content-form.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import { BINARY_FIELD_CONTENTLET, CONTENT_TYPE_MOCK } from '../../utils/mocks';

describe('EditContentLayoutComponent', () => {
    let spectator: Spectator<EditContentLayoutComponent>;
    let dotEditContentStore: DotEditContentStore;
    let dotEditContentService: DotEditContentService;
    let dotWorkflowsActionsService: DotWorkflowsActionsService;

    const createComponent = createComponentFactory({
        component: EditContentLayoutComponent,
        imports: [
            HttpClientTestingModule,
            MessagesModule,
            MockPipe(DotMessagePipe),
            MockComponent(DotEditContentFormComponent),
            MockComponent(DotEditContentToolbarComponent),
            MockComponent(DotEditContentAsideComponent)
        ],
        providers: [
            mockProvider(MessageService),
            mockProvider(DotContentTypeService),
            mockProvider(DotMessageService),
            mockProvider(DotFormatDateService),
            mockProvider(DotEditContentStore),
            mockProvider(DotWorkflowActionsFireService)
        ]
    });

    describe('Existing content', () => {
        const mockData: EditContentPayload = {
            actions: mockWorkflowsActions,
            contentType: CONTENT_TYPE_MOCK,
            contentlet: BINARY_FIELD_CONTENTLET,
            loading: false,
            layout: {
                showSidebar: true
            }
        };

        beforeEach(async () => {
            spectator = createComponent({
                detectChanges: false,
                providers: [
                    {
                        provide: DotEditContentService,
                        useValue: {
                            getContentById: jest.fn().mockReturnValue(of(BINARY_FIELD_CONTENTLET)),
                            getContentType: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK))
                        }
                    },
                    {
                        provide: DotWorkflowsActionsService,
                        useValue: {
                            getByInode: jest.fn().mockReturnValue(of(mockWorkflowsActions)),
                            getDefaultActions: jest.fn().mockReturnValue(of(mockWorkflowsActions))
                        }
                    },
                    {
                        provide: ActivatedRoute,
                        useValue: { snapshot: { params: { contentType: undefined, id: '1' } } }
                    }
                ]
            });

            dotEditContentService = spectator.inject(DotEditContentService, true);
            dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService, true);
            dotEditContentStore = spectator.inject(DotEditContentStore, true);
        });

        it('should get content data', () => {
            const spyContent = jest.spyOn(dotEditContentService, 'getContentById');
            const spyContentType = jest.spyOn(dotEditContentService, 'getContentType');
            const spyWorkflow = jest.spyOn(dotWorkflowsActionsService, 'getByInode');

            spectator.detectChanges();

            expect(spyContent).toHaveBeenCalledWith('1');
            expect(spyContentType).toHaveBeenCalledWith(BINARY_FIELD_CONTENTLET.contentType);
            expect(spyWorkflow).toHaveBeenCalledWith('1', DotRenderMode.EDITING);
        });

        it('should pass the data to the DotEditContentForm Component', () => {
            spectator.detectChanges();
            const formComponent = spectator.query(DotEditContentFormComponent);
            expect(formComponent).toBeDefined();
            expect(formComponent.formData).toEqual(mockData);
        });

        it('should pass the actions to the DotEditContentToolbar Component', () => {
            spectator.detectChanges();
            const toolbarComponent = spectator.query(DotEditContentToolbarComponent);
            expect(toolbarComponent).toBeDefined();
            expect(toolbarComponent.$actions).toEqual(mockData.actions);
        });

        it('should pass the contentlet and contentType to the DotEditContentAside Component', () => {
            spectator.detectChanges();
            const asideComponent = spectator.query(DotEditContentAsideComponent);
            expect(asideComponent).toBeDefined();
            expect(asideComponent.$contentlet).toEqual(mockData.contentlet);
            expect(asideComponent.$contentType).toEqual(mockData.contentType);
        });

        it('should fire workflow action', () => {
            const spyStore = jest.spyOn(dotEditContentStore, 'fireWorkflowActionEffect');
            spectator.detectChanges();
            const toolbarComponent = spectator.query(DotEditContentToolbarComponent);
            toolbarComponent.$actionFired.emit(mockWorkflowsActions[0]);

            expect(spyStore).toHaveBeenCalledWith({
                actionId: mockWorkflowsActions[0].id,
                inode: BINARY_FIELD_CONTENTLET.inode,
                data: {
                    contentlet: expect.any(Object) // Expect any object
                }
            });
        });

        it('should hide the beta topBar if metadata is not present', () => {
            spectator.detectChanges();

            const betaTopbar = spectator.query(byTestId('topBar'));
            expect(betaTopbar).toBeNull();
        });

        it('should show the beta topBar when the metadata is present', () => {
            spectator.detectChanges();
            const metadata = {};
            metadata[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] = true;

            dotEditContentStore.patchState({ contentType: { ...CONTENT_TYPE_MOCK, metadata } });
            spectator.detectChanges();
            const betaTopbar = spectator.query(byTestId('topBar'));
            expect(betaTopbar).not.toBeNull();
        });

        it('should toggle the sidebar when the toggle button is clicked', () => {
            spectator.detectChanges();
            const toggleBtn = spectator.query(byTestId('sidebar-toggle'));

            expect(toggleBtn.classList).toContain('showSidebar');

            spectator.click(toggleBtn);

            expect(toggleBtn.classList).not.toContain('showSidebar');
            spectator.component.toggleSidebar();
        });
    });

    describe('New content', () => {
        const mockData: EditContentPayload = {
            actions: mockWorkflowsActions,
            contentType: CONTENT_TYPE_MOCK,
            contentlet: null,
            loading: false,
            layout: {
                showSidebar: true
            }
        };

        beforeEach(async () => {
            spectator = createComponent({
                detectChanges: false,
                providers: [
                    {
                        provide: DotEditContentService,
                        useValue: {
                            getContentById: jest.fn().mockReturnValue(of(BINARY_FIELD_CONTENTLET)),
                            getContentType: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK))
                        }
                    },
                    {
                        provide: DotWorkflowsActionsService,
                        useValue: {
                            getByInode: jest.fn().mockReturnValue(of(mockWorkflowsActions)),
                            getDefaultActions: jest.fn().mockReturnValue(of(mockWorkflowsActions))
                        }
                    },
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            snapshot: { params: { contentType: mockData.contentType, id: null } }
                        }
                    }
                ]
            });

            dotEditContentService = spectator.inject(DotEditContentService, true);
            dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService, true);
        });

        it('should get new content data', () => {
            const spyContent = jest.spyOn(dotEditContentService, 'getContentById');
            const spyContentType = jest.spyOn(dotEditContentService, 'getContentType');
            const spyWorkflow = jest.spyOn(dotWorkflowsActionsService, 'getDefaultActions');

            spectator.detectChanges();

            expect(spyContentType).toHaveBeenCalledWith(mockData.contentType);
            expect(spyWorkflow).toHaveBeenCalledWith(mockData.contentType);
            expect(spyContent).not.toHaveBeenCalledWith();
        });

        it('should pass the data to the DotEditContentForm Component', () => {
            spectator.detectChanges();
            const formComponent = spectator.query(DotEditContentFormComponent);
            expect(formComponent).toBeDefined();
            expect(formComponent.formData).toEqual(mockData);
        });

        it('should pass the actions to the DotEditContentToolbar Component', () => {
            spectator.detectChanges();
            const toolbarComponent = spectator.query(DotEditContentToolbarComponent);
            expect(toolbarComponent).toBeDefined();
            expect(toolbarComponent.$actions).toEqual(mockData.actions);
        });

        it('should pass the contentlet and contentType to the DotEditContentAside Component', () => {
            spectator.detectChanges();
            const asideComponent = spectator.query(DotEditContentAsideComponent);
            expect(asideComponent).toBeDefined();
            expect(asideComponent.$contentlet).toEqual(mockData.contentlet);
            expect(asideComponent.$contentType).toEqual(mockData.contentType);
        });
    });
});
