import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';
import { Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';
import { TabPanel, TabView } from 'primeng/tabview';

import {
    DotContentTypeService,
    DotFormatDateService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotWorkflowActionsComponent } from '@dotcms/ui';
import {
    DotFormatDateServiceMock,
    MOCK_MULTIPLE_WORKFLOW_ACTIONS,
    MOCK_SINGLE_WORKFLOW_ACTIONS
} from '@dotcms/utils-testing';

import { DotEditContentFormComponent } from './dot-edit-content-form.component';

import { DotEditContentStore } from '../../feature/edit-content/store/edit-content.store';
import { CONTENT_SEARCH_ROUTE } from '../../models/dot-edit-content-field.constant';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import {
    MOCK_CONTENTLET_1_TAB as MOCK_CONTENTLET_1_OR_2_TABS,
    MOCK_CONTENTTYPE_1_TAB,
    MOCK_CONTENTTYPE_2_TABS,
    MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB
} from '../../utils/edit-content.mock';
import { MockResizeObserver } from '../../utils/mocks';

describe('DotFormComponent', () => {
    let spectator: Spectator<DotEditContentFormComponent>;
    let component: DotEditContentFormComponent;
    let store: InstanceType<typeof DotEditContentStore>;
    let dotContentTypeService: SpyObject<DotContentTypeService>;
    let workflowActionsService: SpyObject<DotWorkflowsActionsService>;
    let workflowActionsFireService: SpyObject<DotWorkflowActionsFireService>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let router: SpyObject<Router>;

    const createComponent = createComponentFactory({
        component: DotEditContentFormComponent,

        providers: [
            DotEditContentStore,
            { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
            // Due using the store directly
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotEditContentService),
            mockProvider(DotContentTypeService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotMessageService),
            mockProvider(Router),
            mockProvider(DotWorkflowService),
            mockProvider(MessageService),
            {
                provide: ActivatedRoute,
                useValue: {
                    // Provide an empty snapshot to bypass the Store's onInit,
                    // allowing direct method calls for testing
                    get snapshot() {
                        return { params: { id: undefined, contentType: undefined } };
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        window.ResizeObserver = MockResizeObserver;

        spectator = createComponent({ detectChanges: false });
        component = spectator.component;
        store = spectator.inject(DotEditContentStore);

        dotContentTypeService = spectator.inject(DotContentTypeService);
        workflowActionsService = spectator.inject(DotWorkflowsActionsService);
        dotEditContentService = spectator.inject(DotEditContentService);
        workflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        router = spectator.inject(Router);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Form creation and validation', () => {
        beforeEach(() => {
            dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_1_TAB));
            workflowActionsService.getByInode.mockReturnValue(
                of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
            );
            dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeExistingContent(MOCK_CONTENTLET_1_OR_2_TABS.inode); // called with the inode of the contentlet

            spectator.detectChanges();
        });

        it('should initialize form with existing content values', () => {
            expect(component.form.get('text1').value).toBe('content text 1');
            expect(component.form.get('text2').value).toBe('content text 2');
            expect(component.form.get('text3').value).toBe('default value modified');
        });

        it('should override default values with content values', () => {
            // text3 had a default value, but it should be overridden
            expect(component.form.get('text3').value).toBe('default value modified');
        });

        it('should maintain required validators for existing content', () => {
            expect(component.form.get('text1').hasValidator(Validators.required)).toBe(true);
        });

        it('should not create form controls for non-field properties', () => {
            expect(component.form.get('modUser')).toBeFalsy();
            expect(component.form.get('modUserName')).toBeFalsy();
            expect(component.form.get('publishDate')).toBeFalsy();
        });
    });

    describe('New Content', () => {
        beforeEach(() => {
            dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_1_TAB));
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeNewContent('TestMock');

            spectator.detectChanges();
        });
        it('should initialize text3 with its default value', () => {
            expect(component.form.get('text3').value).toBe('default value');
        });

        it('should initialize the form with empty values', () => {
            expect(component.form.get('text1').value).toBeNull();
            expect(component.form.get('text2').value).toBeNull();
            expect(component.form.get('text3').value).not.toBeNull(); // has default value
            expect(component.form.get('nonexistentField')).toBeFalsy();
        });

        it('should apply validators correctly', () => {
            expect(component.form.get('text1').hasValidator(Validators.required)).toBe(true);
            expect(component.form.get('text2').hasValidator(Validators.required)).toBe(false);
            expect(component.form.get('text3').hasValidator(Validators.required)).toBe(false);
        });

        it('should render the correct number of rows, columns and fields', () => {
            expect(spectator.queryAll(byTestId('row')).length).toBe(1);
            expect(spectator.queryAll(byTestId('column')).length).toBe(2);
            expect(spectator.queryAll(byTestId('field')).length).toBe(3);
        });
    });

    describe('With multiple tabs and existing content', () => {
        beforeEach(() => {
            dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_2_TABS));
            dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            workflowActionsService.getByInode.mockReturnValue(
                of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
            );
            workflowActionsFireService.fireTo.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeExistingContent(MOCK_CONTENTLET_1_OR_2_TABS.inode); // called with the inode of the contentlet
            spectator.detectChanges();
        });

        describe('UI', () => {
            it('should render two tabs', () => {
                const tabView = spectator.query(TabView);
                expect(tabView).toBeTruthy();

                const tabPanels = spectator.queryAll(TabPanel);
                expect(tabPanels.length).toBe(2);
                expect(tabPanels[0]._header).toBe('Content');
                expect(tabPanels[1]._header).toBe('New Tab');
            });

            it('should have append and prepend areas', () => {
                const prependArea = spectator.query(byTestId('tabview-prepend-content'));
                const appendArea = spectator.query(byTestId('tabview-append-content'));
                expect(prependArea).toBeTruthy();
                expect(appendArea).toBeTruthy();
            });

            it('should render back button in prepend area', () => {
                const backButton = spectator.query(byTestId('back-button'));
                expect(backButton).toBeTruthy();
                expect(backButton.getAttribute('icon')).toBe('pi pi-chevron-left');
            });

            it('should render workflow actions and sidebar toggle in append area', () => {
                const sidebarButton = spectator.query(byTestId('sidebar-toggle-button'));
                const workflowActions = spectator.query(DotWorkflowActionsComponent);

                expect(workflowActions).toBeTruthy();
                expect(sidebarButton).toBeTruthy();
            });

            it('should call toggleSidebar when sidebar button is clicked', () => {
                const sidebarButton = spectator.query(byTestId('sidebar-toggle-button'));
                expect(sidebarButton).toBeTruthy();

                const toggleSidebarSpy = jest.spyOn(store, 'toggleSidebar');

                spectator.click(sidebarButton);

                expect(toggleSidebarSpy).toHaveBeenCalled();
            });

            it('should call fireWorkflowAction when Save action is clicked', () => {
                const fireWorkflowActionSpy = jest.spyOn(component.$store, 'fireWorkflowAction');
                const workflowActions = spectator.query(DotWorkflowActionsComponent);
                expect(workflowActions).toBeTruthy();

                console.log(spectator.debugElement.nativeElement.innerHTML);
                const saveButton = spectator.query('.p-splitbutton-defaultbutton');
                expect(saveButton).toBeTruthy();
                expect(saveButton.textContent.trim()).toBe('Save');

                spectator.click(saveButton);

                expect(fireWorkflowActionSpy).toHaveBeenCalledWith({
                    actionId: MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB[0].id,
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    data: {
                        contentlet: {
                            ...component.form.value,
                            contentType: MOCK_CONTENTTYPE_1_TAB.variable
                        }
                    }
                });
            });

            it('should call toggleSidebar when sidebar toggle button is clicked', () => {
                const toggleSidebarSpy = jest.spyOn(store, 'toggleSidebar');

                const sidebarToggleButton = spectator.query(byTestId('sidebar-toggle-button'));
                expect(sidebarToggleButton).toBeTruthy();

                spectator.click(sidebarToggleButton);

                expect(toggleSidebarSpy).toHaveBeenCalled();

                const backButton = spectator.query(byTestId('back-button'));
                expect(backButton).toBeTruthy();

                spectator.click(backButton);

                expect(router.navigate).toHaveBeenCalledWith([CONTENT_SEARCH_ROUTE], {
                    queryParams: { filter: MOCK_CONTENTTYPE_2_TABS.variable }
                });
            });

            describe('TabView Styling', () => {
                it('should apply single-tab class when only one tab exists', () => {
                    const tabView = spectator.query('.dot-edit-content-tabview');
                    component.$hasSingleTab = signal(true);
                    spectator.detectChanges();

                    expect(tabView).toHaveClass('dot-edit-content-tabview--single-tab');
                });
            });
        });
    });

    describe('Sidebar State', () => {
        beforeEach(() => {
            dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_2_TABS));
            dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            workflowActionsService.getByInode.mockReturnValue(
                of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            store.initializeExistingContent(MOCK_CONTENTLET_1_OR_2_TABS.inode);
            spectator.detectChanges();
        });

        it('should render edit-content-actions element', () => {
            const editContentActions = spectator.query(byTestId('edit-content-actions'));
            expect(editContentActions).toBeTruthy();
        });

        describe('Workflow Actions Component', () => {
            it('should show DotWorkflowActionsComponent when showWorkflowActions is true', () => {
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS) // Single workflow actions trigger the show
                );
                store.initializeExistingContent('inode');
                spectator.detectChanges();

                const workflowActions = spectator.query(DotWorkflowActionsComponent);
                expect(store.showWorkflowActions()).toBe(true);
                expect(workflowActions).toBeTruthy();
            });

            it('should hide DotWorkflowActionsComponent when showWorkflowActions is false', () => {
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_MULTIPLE_WORKFLOW_ACTIONS) // Multiple workflow actions trigger the hide
                );

                store.initializeExistingContent('inode');
                spectator.detectChanges();

                const workflowActions = spectator.query(DotWorkflowActionsComponent);
                expect(store.showWorkflowActions()).toBe(false);
                expect(workflowActions).toBeFalsy();
            });
        });
    });
});
