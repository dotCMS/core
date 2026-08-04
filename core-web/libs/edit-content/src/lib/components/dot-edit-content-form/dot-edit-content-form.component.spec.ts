import { expect } from '@jest/globals';
import { patchState } from '@ngrx/signals';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, flush, tick } from '@angular/core/testing';
import { Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { Tab, Tabs } from 'primeng/tabs';

import {
    DotContentletService,
    DotContentTypeService,
    DotCurrentUserService,
    DotFormatDateService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotSystemConfigService,
    DotVersionableService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSWorkflowAction,
    DotContentletCanLock,
    DotContentletDepths
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotContentletStatusBadgeComponent } from '@dotcms/ui';
import {
    DotFormatDateServiceMock,
    MOCK_SINGLE_WORKFLOW_ACTIONS,
    mockMatchMedia
} from '@dotcms/utils-testing';

import { DotEditContentFormComponent } from './dot-edit-content-form.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { EDIT_CONTENT_HOST } from '../../services/host/edit-content-host.model';
import { DotEditContentStore } from '../../store/edit-content.store';
import {
    MOCK_CONTENTLET_1_TAB as MOCK_CONTENTLET_1_OR_2_TABS,
    MOCK_CONTENTLET_WITHOUT_DISABLED_WYSIWYG,
    MOCK_CONTENTTYPE_1_TAB,
    MOCK_CONTENTTYPE_2_TABS,
    MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB,
    MOCK_WORKFLOW_STATUS
} from '../../utils/edit-content.mock';
import { generatePageEditUrl, generatePreviewUrl } from '../../utils/functions.util';

describe('DotFormComponent', () => {
    let spectator: Spectator<DotEditContentFormComponent>;
    let component: DotEditContentFormComponent;
    let store: InstanceType<typeof DotEditContentStore>;
    let dotContentTypeService: SpyObject<DotContentTypeService>;
    let workflowActionsService: SpyObject<DotWorkflowsActionsService>;
    let workflowActionsFireService: SpyObject<DotWorkflowActionsFireService>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let dotWorkflowService: SpyObject<DotWorkflowService>;
    let dotContentletService: SpyObject<DotContentletService>;

    const createComponent = createComponentFactory({
        component: DotEditContentFormComponent,

        providers: [
            DotEditContentStore,
            {
                provide: EDIT_CONTENT_HOST,
                useValue: {
                    setContentTitle: jest.fn(),
                    addBreadcrumb: jest.fn(),
                    goToSavedContent: jest.fn(),
                    goToRestoredVersion: jest.fn()
                }
            },
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
            mockProvider(DotContentletService),
            mockProvider(MessageService),
            ConfirmationService,
            mockProvider(DialogService),
            mockProvider(DotWorkflowEventHandlerService),
            mockProvider(DotWizardService, {
                open: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotMessageService),
            mockProvider(DotVersionableService),
            mockProvider(GlobalStore, {
                loadCurrentSite: jest.fn(),
                siteDetails: jest.fn().mockReturnValue(null),
                addNewBreadcrumb: jest.fn()
            }),
            {
                provide: ActivatedRoute,
                useValue: {
                    // Provide an empty snapshot to bypass the Store's onInit,
                    // allowing direct method calls for testing
                    get snapshot() {
                        return { params: { id: undefined, contentType: undefined } };
                    }
                }
            },
            {
                provide: DotCurrentUserService,
                useValue: {
                    getCurrentUser: () =>
                        of({
                            userId: '123',
                            userName: 'John Doe'
                        })
                }
            },
            mockProvider(DotSystemConfigService, {
                getSystemConfig: jest.fn().mockReturnValue(
                    of({
                        logos: { loginScreen: '/assets/logo.png', navBar: 'NA' },
                        colors: { primary: '#000000', secondary: '#FFFFFF', background: '#F5F5F5' },
                        releaseInfo: { buildDate: 'Jan 01, 2025', version: 'test' },
                        systemTimezone: {
                            id: 'UTC',
                            label: 'Coordinated Universal Time',
                            offset: 0
                        },
                        languages: [
                            {
                                country: 'United States',
                                countryCode: 'US',
                                id: 1,
                                isoCode: 'en-us',
                                language: 'English',
                                languageCode: 'en'
                            }
                        ],
                        license: {
                            displayServerId: 'serverId',
                            isCommunity: true,
                            level: 100,
                            levelName: 'COMMUNITY'
                        },
                        cluster: { clusterId: 'cluster-id', companyKeyDigest: 'digest' }
                    })
                )
            }),
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        mockMatchMedia();
        spectator = createComponent({ detectChanges: false });
        component = spectator.component;
        store = spectator.inject(DotEditContentStore);

        dotContentTypeService = spectator.inject(DotContentTypeService);
        workflowActionsService = spectator.inject(DotWorkflowsActionsService);
        dotEditContentService = spectator.inject(DotEditContentService);
        workflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        dotWorkflowService = spectator.inject(DotWorkflowService);
        dotContentletService = spectator.inject(DotContentletService);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Form creation and validation', () => {
        beforeEach(() => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                of(MOCK_CONTENTTYPE_1_TAB)
            );
            workflowActionsService.getByInode.mockReturnValue(
                of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
            );
            dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
            dotContentletService.canLock.mockReturnValue(
                of({ canLock: true } as DotContentletCanLock)
            );

            store.initializeExistingContent({
                inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                depth: DotContentletDepths.ONE
            }); // called with the inode of the contentlet

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

        it('should create disabledWYSIWYG form control with existing contentlet values', () => {
            const disabledWYSIWYGControl = component.form.get('disabledWYSIWYG');
            expect(disabledWYSIWYGControl).toBeTruthy();
            expect(disabledWYSIWYGControl?.value).toEqual(['wysiwygField1', 'wysiwygField2']);
        });
    });

    describe('New Content', () => {
        beforeEach(() => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                of(MOCK_CONTENTTYPE_1_TAB)
            );
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );

            dotContentletService.canLock.mockReturnValue(
                of({ canLock: true } as DotContentletCanLock)
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

        it('should create disabledWYSIWYG form control with empty array for new content', () => {
            const disabledWYSIWYGControl = component.form.get('disabledWYSIWYG');
            expect(disabledWYSIWYGControl).toBeTruthy();
            expect(disabledWYSIWYGControl?.value).toEqual([]);
        });

        it('should render the correct number of rows, columns and fields', fakeAsync(() => {
            // First, ensure the component is fully initialized
            spectator.detectChanges();

            // Give time for Angular to process any pending tasks
            tick();

            // Find the first tab content
            const form = spectator.query(byTestId('edit-content-form'));
            expect(form).toBeTruthy();

            // If we can directly query the elements even though they are in a tab
            const rows = spectator.queryAll(byTestId('row'));
            const columns = spectator.queryAll(byTestId('column'));
            const fields = spectator.queryAll(byTestId('field'));

            expect(rows.length).toBe(1);
            expect(columns.length).toBe(2);
            expect(fields.length).toBe(3);

            // Clean up any pending async operations
            flush();
        }));
    });

    describe('Field padding and form max-width (issue #36615)', () => {
        // MOCK_CONTENTTYPE_1_TAB has a single row with two columns, i.e. a multi-column tab.
        it('should apply the wider max-width and gap classes for a multi-column layout', () => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                of(MOCK_CONTENTTYPE_1_TAB)
            );
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotContentletService.canLock.mockReturnValue(
                of({ canLock: true } as DotContentletCanLock)
            );

            store.initializeNewContent('TestMock');
            spectator.detectChanges();

            const container = spectator.query(byTestId('tab-layout-container'));
            expect(container?.classList.contains('max-w-343')).toBe(true);
            expect(container?.classList.contains('max-w-206')).toBe(false);
            expect(container?.classList.contains('mx-auto')).toBe(true);

            const row = spectator.query(byTestId('row'));
            const column = spectator.query(byTestId('column'));
            expect(row?.classList.contains('gap-9')).toBe(true);
            expect(row?.classList.contains('mb-5')).toBe(true);
            expect(column?.classList.contains('gap-8')).toBe(true);
        });

        it('should apply the narrower max-width for a single-column layout (every row has exactly one column)', () => {
            const singleColumnRow = MOCK_CONTENTTYPE_1_TAB.layout[0];
            const singleColumnContentType: DotCMSContentType = {
                ...MOCK_CONTENTTYPE_1_TAB,
                layout: [
                    { divider: singleColumnRow.divider, columns: [singleColumnRow.columns[0]] },
                    { divider: singleColumnRow.divider, columns: [singleColumnRow.columns[1]] }
                ]
            };

            dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                of(singleColumnContentType)
            );
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotContentletService.canLock.mockReturnValue(
                of({ canLock: true } as DotContentletCanLock)
            );

            store.initializeNewContent('TestMock');
            spectator.detectChanges();

            const container = spectator.query(byTestId('tab-layout-container'));
            expect(container?.classList.contains('max-w-206')).toBe(true);
            expect(container?.classList.contains('max-w-286')).toBe(false);
            expect(container?.classList.contains('mx-auto')).toBe(true);

            const rows = spectator.queryAll(byTestId('row'));
            expect(rows.length).toBe(2);
        });
    });

    describe('With multiple tabs and existing content', () => {
        beforeEach(() => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                of(MOCK_CONTENTTYPE_2_TABS)
            );
            dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            workflowActionsService.getByInode.mockReturnValue(
                of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
            );
            workflowActionsFireService.fireTo.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
            dotContentletService.canLock.mockReturnValue(
                of({ canLock: true } as DotContentletCanLock)
            );

            store.initializeExistingContent({
                inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                depth: DotContentletDepths.ONE
            }); // called with the inode of the contentlet
            spectator.flushEffects(); // Wait for async store effects to complete
            spectator.detectChanges();
        });

        describe('UI', () => {
            it('should render two tabs', () => {
                const tabView = spectator.query(Tabs);
                expect(tabView).toBeTruthy();

                const tabPanels = spectator.queryAll(Tab);
                expect(tabPanels.length).toBe(2);
                // PrimeNG v21 uses .p-tab for tab headers
                const tabHeaders = spectator.queryAll('.p-tab');
                expect(tabHeaders.length).toBe(2);
                expect(tabHeaders[0]?.textContent?.trim()).toBe('Content');
                expect(tabHeaders[1]?.textContent?.trim()).toBe('New Tab');
            });

            it('should have append area', () => {
                const appendArea = spectator.query(byTestId('tabview-append-content'));
                expect(appendArea).toBeTruthy();
            });

            it('should render the status tag, command bar actions and sidebar toggle in append area', () => {
                const sidebarToggle = spectator.query(byTestId('sidebar-toggle'));
                const sidebarButton =
                    spectator.query(byTestId('sidebar-toggle-button')) ??
                    sidebarToggle?.querySelector('button');
                const statusTag = spectator.query(byTestId('content-status-tag'));
                const commandBar = spectator.query(byTestId('command-bar-actions'));

                expect(statusTag).toBeTruthy();
                expect(commandBar).toBeTruthy();
                expect(sidebarToggle).toBeTruthy();
                expect(sidebarButton).toBeTruthy();
            });

            it('should not render the lock toggle or workflow actions in the append area', () => {
                expect(spectator.query(byTestId('content-lock-controls'))).toBeFalsy();
                expect(spectator.query(byTestId('content-lock-switch'))).toBeFalsy();
                expect(spectator.query(byTestId('workflow-actions'))).toBeFalsy();
            });

            it('should call toggleSidebar when sidebar button is clicked', () => {
                const sidebarToggle = spectator.query(byTestId('sidebar-toggle'));
                const sidebarButton =
                    spectator.query(byTestId('sidebar-toggle-button')) ??
                    sidebarToggle?.querySelector('button');
                expect(sidebarToggle).toBeTruthy();
                expect(sidebarButton).toBeTruthy();

                const toggleSidebarSpy = jest.spyOn(store, 'toggleSidebar');

                spectator.click(sidebarButton);

                expect(toggleSidebarSpy).toHaveBeenCalled();
            });

            it('should render both open and close sidebar icons, hiding one per state', () => {
                const openIcon = spectator.query(byTestId('sidebar-open-icon'));
                const closeIcon = spectator.query(byTestId('sidebar-close-icon'));
                expect(openIcon).toBeTruthy();
                expect(closeIcon).toBeTruthy();
                // One of the two icons must be hidden at any given time
                const openHidden = openIcon?.classList.contains('hidden');
                const closeHidden = closeIcon?.classList.contains('hidden');
                expect(openHidden).not.toBe(closeHidden);
            });

            describe('TabView Styling', () => {
                it('should apply single-tab class when only one tab exists', () => {
                    dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                        of(MOCK_CONTENTTYPE_1_TAB)
                    );
                    store.initializeExistingContent({
                        inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                        depth: DotContentletDepths.ONE
                    });
                    spectator.flushEffects();
                    spectator.detectChanges();

                    const tabView = spectator.query('.dot-edit-content-tabview');
                    expect(
                        tabView?.classList.contains('dot-edit-content-tabview--single-tab')
                    ).toBe(true);
                });
            });
        });
    });

    describe('Sidebar State', () => {
        beforeEach(() => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                of(MOCK_CONTENTTYPE_2_TABS)
            );
            dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            workflowActionsService.getByInode.mockReturnValue(
                of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
            workflowActionsFireService.fireTo.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            dotContentletService.canLock.mockReturnValue(
                of({ canLock: true } as DotContentletCanLock)
            );

            store.initializeExistingContent({
                inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                depth: DotContentletDepths.ONE
            });
            spectator.detectChanges();
        });

        it('should render edit-content-actions element', () => {
            const editContentActions = spectator.query(byTestId('edit-content-actions'));
            expect(editContentActions).toBeTruthy();
        });

        // The workflow actions UI now lives in the sidebar; the form keeps the public
        // fireWorkflowAction() method (called by the layout). These tests exercise it
        // directly to preserve the parameter, wizard and validation regression coverage.
        describe('fireWorkflowAction (programmatic)', () => {
            const baseParams = {
                inode: 'inode',
                contentType: 'TestMock',
                languageId: '1',
                identifier: 'identifier'
            };

            beforeEach(() => {
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                store.initializeExistingContent({
                    inode: 'inode',
                    depth: DotContentletDepths.ONE
                });
                spectator.detectChanges();
            });

            it('should fire the action on the store when it has no inputs', () => {
                const spy = jest.spyOn(store, 'fireWorkflowAction');

                component.fireWorkflowAction({
                    workflow: { id: '1' } as DotCMSWorkflowAction,
                    ...baseParams
                });

                expect(spy).toHaveBeenCalledWith({
                    actionId: '1',
                    inode: 'inode',
                    data: {
                        contentlet: {
                            contentType: 'TestMock',
                            text1: 'content text 1',
                            text11: 'Tab 2 input content',
                            text2: 'content text 2',
                            text3: 'default value modified',
                            multiselect: 'A,B,C',
                            languageId: '1',
                            identifier: 'identifier',
                            disabledWYSIWYG: ['wysiwygField1', 'wysiwygField2']
                        }
                    }
                });
            });

            it('should call the wizard service when the workflow action has inputs', () => {
                const wizardService = spectator.inject(DotWizardService);

                component.fireWorkflowAction({
                    workflow: {
                        id: '1',
                        actionInputs: [{ id: 'move', body: {} }]
                    } as DotCMSWorkflowAction,
                    ...baseParams
                });

                expect(wizardService.open).toHaveBeenCalled();
            });

            it('should validate and not fire when the form is invalid (regression)', () => {
                const fireSpy = jest.spyOn(store, 'fireWorkflowAction');
                const setFormStatusSpy = jest.spyOn(store, 'setFormStatus');
                const markAllAsTouchedSpy = jest.spyOn(component.form, 'markAllAsTouched');

                // Force the form invalid via a required control.
                component.form.get('text1')?.setValidators(Validators.required);
                component.form.get('text1')?.setValue('');
                component.form.get('text1')?.updateValueAndValidity();
                expect(component.form.invalid).toBe(true);

                component.fireWorkflowAction({
                    workflow: { id: '1' } as DotCMSWorkflowAction,
                    ...baseParams
                });

                expect(markAllAsTouchedSpy).toHaveBeenCalled();
                expect(setFormStatusSpy).toHaveBeenCalledWith('invalid');
                expect(fireSpy).not.toHaveBeenCalled();
            });

            describe('commentable and assignable dialog', () => {
                let wizardService: DotWizardService;

                beforeEach(() => {
                    wizardService = spectator.inject(DotWizardService);
                    (wizardService.open as jest.Mock).mockClear();
                });

                it('should open wizard when action has commentable input', () => {
                    component.fireWorkflowAction({
                        workflow: {
                            id: '1',
                            actionInputs: [{ id: 'commentable', body: {} }]
                        } as DotCMSWorkflowAction,
                        ...baseParams
                    });

                    expect(wizardService.open).toHaveBeenCalled();
                });

                it('should open wizard when action has assignable input', () => {
                    component.fireWorkflowAction({
                        workflow: {
                            id: '1',
                            actionInputs: [{ id: 'assignable', body: {} }]
                        } as DotCMSWorkflowAction,
                        ...baseParams
                    });

                    expect(wizardService.open).toHaveBeenCalled();
                });

                it('should open wizard when action has both commentable and assignable inputs', () => {
                    component.fireWorkflowAction({
                        workflow: {
                            id: '1',
                            actionInputs: [
                                { id: 'commentable', body: {} },
                                { id: 'assignable', body: {} }
                            ]
                        } as DotCMSWorkflowAction,
                        ...baseParams
                    });

                    expect(wizardService.open).toHaveBeenCalled();
                });

                it('should not open wizard when action has no inputs', () => {
                    component.fireWorkflowAction({
                        workflow: { id: '1' } as DotCMSWorkflowAction,
                        ...baseParams
                    });

                    expect(wizardService.open).not.toHaveBeenCalled();
                });
            });
        });
    });

    describe('Preview Button', () => {
        let windowOpenSpy: jest.SpyInstance;

        afterEach(() => {
            // Restore the original implementation of window.open
            windowOpenSpy.mockRestore();
        });

        describe('With URL Map', () => {
            beforeEach(() => {
                // Mock window.open
                windowOpenSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

                dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                    of(MOCK_CONTENTTYPE_2_TABS)
                );
                dotEditContentService.getContentById.mockReturnValue(
                    of({
                        ...MOCK_CONTENTLET_1_OR_2_TABS,
                        URL_MAP_FOR_CONTENT: '/blog/post/5-snow-sports-to-try-this-winter'
                    })
                );
                workflowActionsService.getByInode.mockReturnValue(
                    of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true } as DotContentletCanLock)
                );

                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });
                spectator.detectChanges();
            });
            it('should render the preview button when $showPreviewLink is true', () => {
                const previewButton = spectator.query(byTestId('preview-button'));
                expect(previewButton).toBeTruthy();
            });

            it('should call showPreview when the preview button is clicked', () => {
                const showPreviewSpy = jest.spyOn(component, 'showPreview');
                const previewButton = spectator.query(byTestId('preview-button'));

                spectator.click(previewButton);

                expect(showPreviewSpy).toHaveBeenCalled();
            });

            it('should call window.open when showPreview is executed', () => {
                const expectedUrl = generatePreviewUrl({
                    ...MOCK_CONTENTLET_1_OR_2_TABS,
                    URL_MAP_FOR_CONTENT: '/blog/post/5-snow-sports-to-try-this-winter'
                });
                component.showPreview();
                expect(windowOpenSpy).toHaveBeenCalledWith(expectedUrl, '_blank');
            });
        });

        describe('Without URL Map', () => {
            beforeEach(() => {
                // Mock window.open
                windowOpenSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

                dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                    of(MOCK_CONTENTTYPE_2_TABS)
                );
                dotEditContentService.getContentById.mockReturnValue(
                    of(MOCK_CONTENTLET_1_OR_2_TABS)
                );
                workflowActionsService.getByInode.mockReturnValue(
                    of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true } as DotContentletCanLock)
                );

                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });
                spectator.detectChanges();
            });

            it('should not render the preview button when $showPreviewLink is false', () => {
                const previewButton = spectator.query(byTestId('preview-button'));
                expect(previewButton).toBeFalsy();
            });
        });

        describe('HTML Page', () => {
            const pageContentlet = {
                ...MOCK_CONTENTLET_1_OR_2_TABS,
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE
            } as DotCMSContentlet;

            beforeEach(() => {
                windowOpenSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

                dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                    of(MOCK_CONTENTTYPE_2_TABS)
                );
                dotEditContentService.getContentById.mockReturnValue(of(pageContentlet));
                workflowActionsService.getByInode.mockReturnValue(
                    of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true } as DotContentletCanLock)
                );

                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });
                spectator.detectChanges();
            });

            it('should render the preview button for an HTML page', () => {
                const previewButton = spectator.query(byTestId('preview-button'));
                expect(previewButton).toBeTruthy();
            });

            it('should use generatePageEditUrl when showPreview runs for an HTML page', () => {
                const expectedUrl = generatePageEditUrl(pageContentlet);
                expect(expectedUrl).toBeTruthy();

                component.showPreview();

                expect(windowOpenSpy).toHaveBeenCalledWith(expectedUrl, '_blank');
            });
        });

        describe('New content', () => {
            beforeEach(() => {
                windowOpenSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

                dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                    of(MOCK_CONTENTTYPE_1_TAB)
                );
                workflowActionsService.getDefaultActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true } as DotContentletCanLock)
                );

                store.initializeNewContent('TestMock');
                spectator.detectChanges();
            });

            it('should not render the preview button for new content', () => {
                const previewButton = spectator.query(byTestId('preview-button'));
                expect(previewButton).toBeFalsy();
            });
        });
    });

    describe('Command Bar', () => {
        beforeEach(() => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                of(MOCK_CONTENTTYPE_2_TABS)
            );
            dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            workflowActionsService.getByInode.mockReturnValue(
                of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
            );
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
            dotContentletService.canLock.mockReturnValue(
                of({ canLock: true } as DotContentletCanLock)
            );
        });

        it('should render the status tag for existing content', () => {
            store.initializeExistingContent({
                inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                depth: DotContentletDepths.ONE
            });
            spectator.detectChanges();

            const statusTag = spectator.query(byTestId('content-status-tag'));
            expect(statusTag).toBeTruthy();
        });

        it('should render the command bar actions for existing content', () => {
            store.initializeExistingContent({
                inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                depth: DotContentletDepths.ONE
            });
            spectator.detectChanges();

            const commandBar = spectator.query(byTestId('command-bar-actions'));
            expect(commandBar).toBeTruthy();
        });

        it('should not render the command bar actions for new content', () => {
            store.initializeNewContent('TestMock');
            spectator.detectChanges();

            const commandBar = spectator.query(byTestId('command-bar-actions'));
            expect(commandBar).toBeFalsy();
        });

        describe('status badge', () => {
            it('should pass the contentlet as the badge state for existing content', () => {
                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });
                spectator.detectChanges();

                const badge = spectator.query(DotContentletStatusBadgeComponent);
                expect(badge).toBeTruthy();
                expect(badge?.state()).toEqual(store.contentlet());
            });

            it('should pass null as the badge state for new content', () => {
                store.initializeNewContent('TestMock');
                spectator.detectChanges();

                const badge = spectator.query(DotContentletStatusBadgeComponent);
                expect(badge).toBeTruthy();
                expect(badge?.state()).toBeNull();
            });
        });
    });

    describe('Lock functionality', () => {
        beforeEach(() => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                of(MOCK_CONTENTTYPE_1_TAB)
            );
            workflowActionsService.getByInode.mockReturnValue(
                of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
            );
            dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
        });

        describe('Locked / Unlocked State', () => {
            beforeEach(() => {
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true } as DotContentletCanLock)
                );

                dotContentletService.lockContent.mockReturnValue(
                    of({
                        ...MOCK_CONTENTLET_1_OR_2_TABS,
                        locked: true,
                        lockedBy: 'dotcms.org.1',
                        lockedByName: 'Admin User',
                        lockedOn: new Date()
                    } as DotCMSContentlet)
                );

                dotContentletService.unlockContent.mockReturnValue(
                    of({
                        ...MOCK_CONTENTLET_1_OR_2_TABS,
                        locked: false,
                        lockedBy: null,
                        lockedByName: null,
                        lockedOn: null
                    } as DotCMSContentlet)
                );

                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });

                spectator.detectChanges();
            });

            // The lock toggle UI moved out of this component; the form still reacts to
            // lock-state changes coming from the store, so we drive those directly.
            it('should call lockContent on the store when locking', () => {
                store.lockContent();

                expect(dotContentletService.lockContent).toHaveBeenCalled();
            });

            it('should call unlockContent on the store when unlocking', () => {
                store.unlockContent();

                expect(dotContentletService.unlockContent).toHaveBeenCalled();
            });

            it('should not reinitialize the form when only lock state changes', () => {
                const initFormSpy = jest.spyOn(
                    component as DotEditContentFormComponent & { initializeForm(): void },
                    'initializeForm'
                );

                store.lockContent();
                spectator.detectChanges();

                // identifier/inode/modDate did not change — form must not rebuild,
                // otherwise in-flight field state (e.g. category selections) is lost.
                expect(initFormSpy).not.toHaveBeenCalled();
            });
        });

        describe('lock controls UI', () => {
            beforeEach(() => {
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true } as DotContentletCanLock)
                );

                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                }); // called with the inode of the contentlet

                spectator.detectChanges();
            });

            it('should never render the lock toggle in the command bar', () => {
                expect(spectator.query(byTestId('content-lock-controls'))).toBeFalsy();
                expect(spectator.query(byTestId('content-lock-switch'))).toBeFalsy();
            });
        });

        describe('Form dirty state after lock toggle', () => {
            // Mirrors the fallback timer in #scheduleMarkPristineAfterInit
            // (race(appRef.isStable, timer(500))). Named so the coupling is explicit
            // and breaks loudly here if the production debounce changes.
            const PRISTINE_RESET_DEBOUNCE_MS = 500;

            beforeEach(() => {
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true } as DotContentletCanLock)
                );

                dotContentletService.lockContent.mockReturnValue(
                    of({
                        ...MOCK_CONTENTLET_1_OR_2_TABS,
                        locked: true,
                        lockedBy: 'dotcms.org.1',
                        lockedByName: 'Admin User',
                        lockedOn: new Date()
                    } as DotCMSContentlet)
                );

                dotContentletService.unlockContent.mockReturnValue(
                    of({
                        ...MOCK_CONTENTLET_1_OR_2_TABS,
                        locked: false,
                        lockedBy: null,
                        lockedByName: null,
                        lockedOn: null
                    } as DotCMSContentlet)
                );

                // tick(500) triggers downstream effects (history feature loads versions)
                // that hit dotEditContentService — mock these so the fakeAsync flush
                // doesn't crash with "Cannot read properties of undefined".
                dotEditContentService.getVersions.mockReturnValue(
                    of({
                        entity: [],
                        pagination: null,
                        errors: [],
                        i18nMessagesMap: {},
                        messages: [],
                        permissions: []
                    })
                );
                dotEditContentService.getPushPublishHistory.mockReturnValue(
                    of({
                        entity: [],
                        pagination: null,
                        errors: [],
                        i18nMessagesMap: {},
                        messages: [],
                        permissions: []
                    })
                );
            });

            it('should keep the form pristine when content opens locked (AC1, AC2, AC7)', fakeAsync(() => {
                dotEditContentService.getContentById.mockReturnValue(
                    of({
                        ...MOCK_CONTENTLET_1_OR_2_TABS,
                        locked: true,
                        lockedBy: 'dotcms.org.1',
                        lockedByName: 'Admin User',
                        lockedOn: new Date()
                    } as DotCMSContentlet)
                );

                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });

                spectator.detectChanges();

                // Drain the 500ms fallback timer in #scheduleMarkPristineAfterInit
                tick(PRISTINE_RESET_DEBOUNCE_MS);
                spectator.detectChanges();

                expect(component.form.dirty).toBe(false);
                expect(component.form.pristine).toBe(true);

                flush();
            }));

            it('should not mark form dirty when the content is locked (AC2 regression guard)', fakeAsync(() => {
                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });

                spectator.detectChanges();

                tick(PRISTINE_RESET_DEBOUNCE_MS);
                spectator.detectChanges();

                expect(component.form.pristine).toBe(true);

                store.lockContent();
                spectator.detectChanges();

                // Locking patches the contentlet reference but must not dirty the form.
                expect(dotContentletService.lockContent).toHaveBeenCalled();
                expect(component.form.dirty).toBe(false);

                flush();
            }));

            it('should mark form dirty when a real field is edited (AC5, AC8)', fakeAsync(() => {
                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });

                spectator.detectChanges();

                tick(PRISTINE_RESET_DEBOUNCE_MS);
                spectator.detectChanges();

                expect(component.form.pristine).toBe(true);

                // Real user edit routed through the rendered field input, so Angular's forms
                // machinery (not a manual markAsDirty) is what dirties the control.
                const input = spectator.query(byTestId('text2')) as HTMLInputElement;
                spectator.typeInElement('edited via DOM', input);
                spectator.detectChanges();

                expect(component.form.get('text2')?.dirty).toBe(true);
                expect(component.form.dirty).toBe(true);

                flush();
            }));

            it('should mark form dirty when locked content is unlocked and then a field is edited (AC6)', fakeAsync(() => {
                const lockedMock = {
                    ...MOCK_CONTENTLET_1_OR_2_TABS,
                    locked: true,
                    lockedBy: 'dotcms.org.1',
                    lockedByName: 'Admin User',
                    lockedOn: new Date()
                } as DotCMSContentlet;
                dotEditContentService.getContentById.mockReturnValue(of(lockedMock));

                store.initializeExistingContent({
                    inode: lockedMock.inode,
                    depth: DotContentletDepths.ONE
                });

                spectator.detectChanges();

                tick(PRISTINE_RESET_DEBOUNCE_MS); // drain #scheduleMarkPristineAfterInit timer
                spectator.detectChanges();

                expect(component.form.pristine).toBe(true);
                expect(component.form.dirty).toBe(false);

                store.unlockContent();

                expect(dotContentletService.unlockContent).toHaveBeenCalled();

                spectator.detectChanges();

                // Real user edit routed through the rendered field input after unlocking.
                const input = spectator.query(byTestId('text2')) as HTMLInputElement;
                spectator.typeInElement('edited after unlock', input);
                spectator.detectChanges();

                expect(component.form.get('text2')?.dirty).toBe(true);
                expect(component.form.dirty).toBe(true);

                flush();
            }));

            it('should not re-enable the form when toggling lock so field CVAs do not re-emit (#35754, AC2)', fakeAsync(() => {
                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });

                spectator.detectChanges();

                tick(PRISTINE_RESET_DEBOUNCE_MS);
                spectator.detectChanges();

                expect(component.form.enabled).toBe(true);
                expect(component.form.pristine).toBe(true);

                const enableSpy = jest.spyOn(component.form, 'enable');

                store.lockContent();
                spectator.detectChanges();

                expect(dotContentletService.lockContent).toHaveBeenCalled();

                // The lock patch replaces the contentlet reference, so the enable/disable
                // effect re-runs — but the form is already enabled, so the idempotent guard
                // must skip enable(). A redundant enable() would make async field CVAs (e.g.
                // the date field) re-emit and wrongly dirty the form.
                expect(enableSpy).not.toHaveBeenCalled();
                expect(component.form.dirty).toBe(false);
                expect(component.form.pristine).toBe(true);

                flush();
            }));

            it('should preserve a real unsaved edit when toggling lock (#35754, AC6 regression)', fakeAsync(() => {
                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });

                spectator.detectChanges();

                tick(PRISTINE_RESET_DEBOUNCE_MS);
                spectator.detectChanges();

                expect(component.form.pristine).toBe(true);

                // Real user edit before locking.
                const control = component.form.get('disabledWYSIWYG');
                control?.setValue(['user-edit']);
                control?.markAsDirty();
                expect(component.form.dirty).toBe(true);

                store.lockContent();
                spectator.detectChanges();

                // Locking must not clobber the user's real unsaved changes.
                expect(component.form.dirty).toBe(true);

                flush();
            }));
        });
    });

    describe('disabledWYSIWYG functionality', () => {
        describe('onDisabledWYSIWYGChange', () => {
            beforeEach(() => {
                dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                    of(MOCK_CONTENTTYPE_1_TAB)
                );
                workflowActionsService.getByInode.mockReturnValue(
                    of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );
                dotEditContentService.getContentById.mockReturnValue(
                    of(MOCK_CONTENTLET_1_OR_2_TABS)
                );
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true } as DotContentletCanLock)
                );

                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });

                spectator.detectChanges();
            });

            it('should update disabledWYSIWYG form control when onDisabledWYSIWYGChange is called', () => {
                const newDisabledWYSIWYG = ['newField1', 'newField2'];
                const disabledWYSIWYGControl = component.form.get('disabledWYSIWYG');

                // Verify initial value
                expect(disabledWYSIWYGControl?.value).toEqual(['wysiwygField1', 'wysiwygField2']);

                // Call the method
                component.onDisabledWYSIWYGChange(newDisabledWYSIWYG);

                // Verify the control was updated
                expect(disabledWYSIWYGControl?.value).toEqual(newDisabledWYSIWYG);
            });

            it('should not throw error when onDisabledWYSIWYGChange is called and form does not exist', () => {
                // Set form to null to simulate case where form doesn't exist
                component.form = null;

                expect(() => {
                    component.onDisabledWYSIWYGChange(['test']);
                }).not.toThrow();
            });

            it('should not throw error when onDisabledWYSIWYGChange is called and disabledWYSIWYG control does not exist', () => {
                // Remove the disabledWYSIWYG control
                component.form.removeControl('disabledWYSIWYG');

                expect(() => {
                    component.onDisabledWYSIWYGChange(['test']);
                }).not.toThrow();
            });

            it('should emit event when disabledWYSIWYG form control value changes', () => {
                const disabledWYSIWYGControl = component.form.get('disabledWYSIWYG');
                const spy = jest.spyOn(disabledWYSIWYGControl, 'setValue');

                component.onDisabledWYSIWYGChange(['newField']);

                expect(spy).toHaveBeenCalledWith(['newField'], { emitEvent: true });
            });
        });

        describe('with different contentlet scenarios', () => {
            it('should handle contentlet without disabledWYSIWYG property', () => {
                dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                    of(MOCK_CONTENTTYPE_1_TAB)
                );
                workflowActionsService.getByInode.mockReturnValue(
                    of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );
                dotEditContentService.getContentById.mockReturnValue(
                    of(MOCK_CONTENTLET_WITHOUT_DISABLED_WYSIWYG)
                );
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true } as DotContentletCanLock)
                );

                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_WITHOUT_DISABLED_WYSIWYG.inode,
                    depth: DotContentletDepths.ONE
                });

                spectator.detectChanges();

                const disabledWYSIWYGControl = component.form.get('disabledWYSIWYG');
                expect(disabledWYSIWYGControl).toBeTruthy();
                expect(disabledWYSIWYGControl?.value).toEqual([]);
            });

            it('should include disabledWYSIWYG in form values when form is processed', () => {
                dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                    of(MOCK_CONTENTTYPE_1_TAB)
                );
                workflowActionsService.getByInode.mockReturnValue(
                    of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
                );
                dotEditContentService.getContentById.mockReturnValue(
                    of(MOCK_CONTENTLET_1_OR_2_TABS)
                );
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true } as DotContentletCanLock)
                );

                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });

                spectator.detectChanges();

                const formValues = component.form.value;
                expect(formValues.disabledWYSIWYG).toEqual(['wysiwygField1', 'wysiwygField2']);
            });
        });
    });

    describe('Form value processing', () => {
        beforeEach(() => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                of(MOCK_CONTENTTYPE_1_TAB)
            );
            workflowActionsService.getDefaultActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotContentletService.canLock.mockReturnValue(
                of({ canLock: true } as DotContentletCanLock)
            );

            store.initializeNewContent('TestMock');
            spectator.detectChanges();
        });

        it('should emit changeValue when form values change', () => {
            const testValues = {
                text1: 'test string',
                text2: 'another string'
            };

            // Spy on the changeValue output
            const changeValueSpy = jest.fn();
            spectator.output('changeValue').subscribe(changeValueSpy);

            // Call onFormChange
            component.onFormChange(testValues);

            // Check that the event was emitted
            expect(changeValueSpy).toHaveBeenCalledWith(expect.objectContaining(testValues));
        });

        it('should convert non-array category values to empty array in processed form values', () => {
            // Add a Category field to $formFields
            const categoryField = {
                fieldType: 'Category',
                variable: 'categories',
                readOnly: false,
                required: false
            } as unknown as DotCMSContentTypeField;

            const originalFormFields = component.$formFields();
            jest.spyOn(component, '$formFields').mockReturnValue([
                ...originalFormFields,
                categoryField
            ]);

            const changeValueSpy = jest.fn();
            spectator.output('changeValue').subscribe(changeValueSpy);

            // Simulate the translation scenario where categories is an empty string
            component.onFormChange({ text1: 'value', categories: '' });

            expect(changeValueSpy).toHaveBeenCalledWith(
                expect.objectContaining({ categories: [] })
            );
        });

        it('should preserve array category values in processed form values', () => {
            const categoryField = {
                fieldType: 'Category',
                variable: 'categories',
                readOnly: false,
                required: false
            } as unknown as DotCMSContentTypeField;

            const originalFormFields = component.$formFields();
            jest.spyOn(component, '$formFields').mockReturnValue([
                ...originalFormFields,
                categoryField
            ]);

            const changeValueSpy = jest.fn();
            spectator.output('changeValue').subscribe(changeValueSpy);

            component.onFormChange({
                text1: 'value',
                categories: ['inode1', 'inode2']
            });

            expect(changeValueSpy).toHaveBeenCalledWith(
                expect.objectContaining({ categories: ['inode1', 'inode2'] })
            );
        });
    });

    describe('Historical Version Functionality', () => {
        let historicalContentlet: DotCMSContentlet;

        beforeEach(() => {
            dotContentTypeService.getContentTypeWithRender.mockReturnValue(
                of(MOCK_CONTENTTYPE_2_TABS)
            );
            dotEditContentService.getContentById.mockReturnValue(of(MOCK_CONTENTLET_1_OR_2_TABS));
            workflowActionsService.getByInode.mockReturnValue(
                of(MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB)
            );
            workflowActionsService.getWorkFlowActions.mockReturnValue(
                of(MOCK_SINGLE_WORKFLOW_ACTIONS)
            );
            dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));
            dotContentletService.canLock.mockReturnValue(
                of({ canLock: true } as DotContentletCanLock)
            );

            // Setup mock for historical content - used across multiple tests.
            // Use a different inode so the form reinitialize effect detects a
            // real identity change (matches real historical-version responses).
            historicalContentlet = {
                ...MOCK_CONTENTLET_1_OR_2_TABS,
                inode: 'historical-inode',
                text1: 'historical content'
            };
            dotContentletService.getContentletByInode.mockReturnValue(of(historicalContentlet));

            store.initializeExistingContent({
                inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                depth: DotContentletDepths.ONE
            });
            spectator.detectChanges();
        });

        describe('Form State Management', () => {
            xit('should disable / enable form when exiting historical version view', () => {
                // Start by simulating historical version state
                store.loadVersionContent('historical-inode');
                spectator.detectChanges();
                expect(component.form.disabled).toBe(true);

                // Exit historical version using the store's public method
                store.exitHistoricalView();
                spectator.detectChanges();

                // Form should be enabled again
                expect(component.form.enabled).toBe(true);
            });

            it('should reinitialize form when contentlet changes', () => {
                const initFormSpy = jest.spyOn(
                    component as DotEditContentFormComponent & { initializeForm(): void },
                    'initializeForm'
                );
                const initListenerSpy = jest.spyOn(
                    component as DotEditContentFormComponent & { initializeFormListener(): void },
                    'initializeFormListener'
                );

                // Simulate contentlet change by loading a historical version (triggers the effect)
                store.loadVersionContent('new-inode');
                spectator.detectChanges();

                expect(initFormSpy).toHaveBeenCalled();
                expect(initListenerSpy).toHaveBeenCalled();
            });
        });

        describe('Historical Version UI Elements', () => {
            it('should hide the status tag and command bar when viewing historical version', () => {
                // Initially the normal-view command bar should be visible
                expect(spectator.query(byTestId('content-status-tag'))).toBeTruthy();
                expect(spectator.query(byTestId('command-bar-actions'))).toBeTruthy();

                // Simulate loading a historical version using the store's public method
                store.loadVersionContent('historical-inode');
                spectator.detectChanges();

                // The status tag and command bar should be hidden
                expect(spectator.query(byTestId('content-status-tag'))).toBeFalsy();
                expect(spectator.query(byTestId('command-bar-actions'))).toBeFalsy();
            });

            it('should show previewing label when viewing historical version', () => {
                // Initially previewing label should not be visible
                const previewingLabel = spectator.query(byTestId('previewing-label'));
                expect(previewingLabel).toBeFalsy();

                // Simulate loading a historical version using the store's public method
                store.loadVersionContent('historical-inode');
                spectator.detectChanges();

                // Previewing label should be visible (restore/close live in the sidebar banner)
                const previewingLabelAfter = spectator.query(byTestId('previewing-label'));
                expect(previewingLabelAfter).toBeTruthy();
            });
        });

        describe('State Transitions', () => {
            it('should properly transition from normal to historical view', () => {
                // Initial state - normal view
                expect(store.isViewingHistoricalVersion()).toBe(false);
                //TODO: enable this when all fields have disable state expect(component.form.enabled).toBe(true);

                const statusTag = spectator.query(byTestId('content-status-tag'));
                const commandBar = spectator.query(byTestId('command-bar-actions'));
                const previewingLabel = spectator.query(byTestId('previewing-label'));

                expect(statusTag).toBeTruthy();
                expect(commandBar).toBeTruthy();
                expect(previewingLabel).toBeFalsy();

                // Simulate loading a historical version using the store's public method
                store.loadVersionContent('historical-inode');
                spectator.detectChanges();

                // Check historical view state
                //TODO: enable this when all fields have disable state expect(component.form.disabled).toBe(true);

                const statusTagAfter = spectator.query(byTestId('content-status-tag'));
                const commandBarAfter = spectator.query(byTestId('command-bar-actions'));
                const previewingLabelAfter = spectator.query(byTestId('previewing-label'));

                expect(statusTagAfter).toBeFalsy();
                expect(commandBarAfter).toBeFalsy();
                expect(previewingLabelAfter).toBeTruthy();
            });

            it('should properly transition from historical to normal view', () => {
                // Start in historical view using the store's public method
                store.loadVersionContent('historical-inode');
                spectator.detectChanges();

                //TODO: enable this when all fields have disable state expect(component.form.disabled).toBe(true);
                expect(spectator.query(byTestId('previewing-label'))).toBeTruthy();

                // Transition back to normal view using the store's public method
                store.exitHistoricalView();
                spectator.detectChanges();

                // Check normal view state
                expect(component.form.enabled).toBe(true);

                const statusTag = spectator.query(byTestId('content-status-tag'));
                const commandBar = spectator.query(byTestId('command-bar-actions'));
                const previewingLabel = spectator.query(byTestId('previewing-label'));

                expect(statusTag).toBeTruthy();
                expect(commandBar).toBeTruthy();
                expect(previewingLabel).toBeFalsy();
            });
        });
    });

    describe('Manual translation — $shouldRenderFields', () => {
        beforeEach(() => {
            // Prevent form rebuilding from creating a FormGroup and triggering
            // extra change detection cycles that cause NG0101 inside fakeAsync.
            type PrivateFormMethods = {
                initializeForm: () => void;
                initializeFormListener: () => void;
            };
            jest.spyOn(
                component as unknown as PrivateFormMethods,
                'initializeForm'
            ).mockReturnValue(undefined);
            jest.spyOn(
                component as unknown as PrivateFormMethods,
                'initializeFormListener'
            ).mockReturnValue(undefined);
            spectator.detectChanges();
        });

        it('should toggle $shouldRenderFields false then back to true when isManualTranslation is true', fakeAsync(() => {
            patchState(store, { initialContentletState: 'copy', isManualTranslation: true });
            spectator.detectChanges();

            expect(component.$shouldRenderFields()).toBe(false);

            tick(); // advance past the setTimeout(0)

            expect(component.$shouldRenderFields()).toBe(true);
        }));

        it('should toggle $shouldRenderFields false then back to true when isManualTranslation is false (populate)', fakeAsync(() => {
            patchState(store, { initialContentletState: 'copy', isManualTranslation: false });
            spectator.detectChanges();

            expect(component.$shouldRenderFields()).toBe(false);

            tick();

            expect(component.$shouldRenderFields()).toBe(true);
        }));
    });
});
