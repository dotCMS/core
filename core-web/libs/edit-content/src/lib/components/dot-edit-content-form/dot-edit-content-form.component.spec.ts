import { expect } from '@jest/globals';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';
import { fakeAsync, flush, tick } from '@angular/core/testing';
import { Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { InputSwitch, InputSwitchChangeEvent } from 'primeng/inputswitch';
import { TabPanel, TabView } from 'primeng/tabview';

import {
    DotContentletService,
    DotContentTypeService,
    DotCurrentUserService,
    DotFormatDateService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotSystemConfigService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCMSWorkflowAction,
    DotContentletCanLock,
    DotContentletDepths
} from '@dotcms/dotcms-models';
import { DotWorkflowActionsComponent } from '@dotcms/ui';
import {
    DotFormatDateServiceMock,
    MOCK_MULTIPLE_WORKFLOW_ACTIONS,
    MOCK_SINGLE_WORKFLOW_ACTIONS
} from '@dotcms/utils-testing';

import { DotEditContentFormComponent } from './dot-edit-content-form.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { DotEditContentStore } from '../../store/edit-content.store';
import {
    MOCK_CONTENTLET_1_TAB as MOCK_CONTENTLET_1_OR_2_TABS,
    MOCK_CONTENTLET_WITHOUT_DISABLED_WYSIWYG,
    MOCK_CONTENTTYPE_1_TAB,
    MOCK_CONTENTTYPE_2_TABS,
    MOCK_WORKFLOW_ACTIONS_NEW_ITEMNTTYPE_1_TAB,
    MOCK_WORKFLOW_STATUS
} from '../../utils/edit-content.mock';
import { generatePreviewUrl } from '../../utils/functions.util';

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
            mockProvider(DialogService),
            mockProvider(DotWorkflowEventHandlerService),
            mockProvider(DotWizardService),
            mockProvider(DotMessageService),
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
            })
        ]
    });

    beforeEach(() => {
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
            dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_1_TAB));
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
            dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_1_TAB));
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

        describe('UI', () => {
            it('should render two tabs', () => {
                const tabView = spectator.query(TabView);
                expect(tabView).toBeTruthy();

                const tabPanels = spectator.queryAll(TabPanel);
                expect(tabPanels.length).toBe(2);
                expect(tabPanels[0]._header).toBe('Content');
                expect(tabPanels[1]._header).toBe('New Tab');
            });

            it('should have append area', () => {
                const appendArea = spectator.query(byTestId('tabview-append-content'));
                expect(appendArea).toBeTruthy();
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

            describe('TabView Styling', () => {
                it('should apply single-tab class when only one tab exists', () => {
                    const tabView = spectator.query('.dot-edit-content-tabview');
                    component.$hasSingleTab = signal(true);
                    spectator.detectChanges();

                    expect(tabView.classList.contains('dot-edit-content-tabview--single-tab')).toBe(
                        true
                    );
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

        describe('Workflow Actions Component', () => {
            it('should show DotWorkflowActionsComponent when showWorkflowActions is true', () => {
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS) // Single workflow actions trigger the show
                );
                store.initializeExistingContent({
                    inode: 'inode',
                    depth: DotContentletDepths.ONE
                });
                spectator.detectChanges();

                const workflowActions = spectator.query(DotWorkflowActionsComponent);
                expect(store.showWorkflowActions()).toBe(true);
                expect(workflowActions).toBeTruthy();
            });

            it('should hide DotWorkflowActionsComponent when showWorkflowActions is false', () => {
                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_MULTIPLE_WORKFLOW_ACTIONS) // Multiple workflow actions trigger the hide
                );

                store.initializeExistingContent({
                    inode: 'inode',
                    depth: DotContentletDepths.ONE
                });
                spectator.detectChanges();

                const workflowActions = spectator.query(DotWorkflowActionsComponent);
                expect(store.showWorkflowActions()).toBe(false);
                expect(workflowActions).toBeFalsy();
            });

            it('should send the correct parameters when firing an action', () => {
                const spy = jest.spyOn(store, 'fireWorkflowAction');

                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                store.initializeExistingContent({
                    inode: 'inode',
                    depth: DotContentletDepths.ONE
                });
                spectator.detectChanges();

                const workflowActions = spectator.query(DotWorkflowActionsComponent);
                workflowActions.actionFired.emit({ id: '1' } as DotCMSWorkflowAction);

                expect(spy).toHaveBeenCalledWith({
                    actionId: '1',
                    inode: 'cc120e84-ae80-49d8-9473-36d183d0c1c9',
                    data: {
                        contentlet: {
                            contentType: 'TestMock',
                            text1: 'content text 1',
                            text11: 'Tab 2 input content',
                            text2: 'content text 2',
                            text3: 'default value modified',
                            multiselect: 'A,B,C',
                            languageId: '',
                            identifier: null,
                            disabledWYSIWYG: ['wysiwygField1', 'wysiwygField2']
                        }
                    }
                });
            });

            it('should call the wizard service when the workflow action is fired', () => {
                const wizardService = spectator.inject(DotWizardService);

                workflowActionsService.getWorkFlowActions.mockReturnValue(
                    of(MOCK_SINGLE_WORKFLOW_ACTIONS)
                );
                store.initializeExistingContent({
                    inode: 'inode',
                    depth: DotContentletDepths.ONE
                });
                spectator.detectChanges();

                const workflowActions = spectator.query(DotWorkflowActionsComponent);
                workflowActions.actionFired.emit({
                    id: '1',
                    actionInputs: [{ id: 'move', body: {} }]
                } as DotCMSWorkflowAction);

                expect(wizardService.open).toHaveBeenCalled();
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

                dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_2_TABS));
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

                dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_2_TABS));
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
    });

    describe('Lock functionality', () => {
        beforeEach(() => {
            dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_1_TAB));
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
                    of({ inode: '123' } as DotCMSContentlet)
                );

                dotContentletService.unlockContent.mockReturnValue(
                    of({ inode: '123' } as DotCMSContentlet)
                );

                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                });

                spectator.detectChanges();
            });

            it('should call lockContent when switch is turned on', () => {
                const lockSwitch = spectator.query(InputSwitch);

                lockSwitch.onChange.emit({ checked: true } as InputSwitchChangeEvent);

                expect(dotContentletService.lockContent).toHaveBeenCalled();
            });

            it('should call unlockContent when switch is turned off', () => {
                const lockSwitch = spectator.query(InputSwitch);

                lockSwitch.onChange.emit({ checked: false } as InputSwitchChangeEvent);

                expect(dotContentletService.unlockContent).toHaveBeenCalled();
            });
        });

        describe('cant lock', () => {
            beforeEach(() => {
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: false } as DotContentletCanLock)
                );

                store.initializeExistingContent({
                    inode: MOCK_CONTENTLET_1_OR_2_TABS.inode,
                    depth: DotContentletDepths.ONE
                }); // called with the inode of the contentlet

                spectator.detectChanges();
            });

            it('should hide the lock switch when user can not lock', () => {
                const lockSwitch = spectator.query(InputSwitch);
                expect(lockSwitch).toBe(null);
            });
        });
    });

    describe('disabledWYSIWYG functionality', () => {
        describe('onDisabledWYSIWYGChange', () => {
            beforeEach(() => {
                dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_1_TAB));
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
                dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_1_TAB));
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
                dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_1_TAB));
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
            dotContentTypeService.getContentType.mockReturnValue(of(MOCK_CONTENTTYPE_1_TAB));
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
    });
});
