import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { TabView } from 'primeng/tabview';

import {
    DotContentletService,
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';

import { DotEditContentSidebarInformationComponent } from './components/dot-edit-content-sidebar-information/dot-edit-content-sidebar-information.component';
import { DotEditContentSidebarWorkflowComponent } from './components/dot-edit-content-sidebar-workflow/dot-edit-content-sidebar-workflow.component';
import { DotEditContentSidebarComponent } from './dot-edit-content-sidebar.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { DotEditContentStore } from '../../store/edit-content.store';
import { MOCK_WORKFLOW_STATUS } from '../../utils/edit-content.mock';
import * as utils from '../../utils/functions.util';
import { MockResizeObserver } from '../../utils/mocks';

describe('DotEditContentSidebarComponent', () => {
    let spectator: Spectator<DotEditContentSidebarComponent>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let dotWorkflowService: SpyObject<DotWorkflowService>;
    let store: SpyObject<InstanceType<typeof DotEditContentStore>>;

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarComponent,
        declarations: [
            MockComponent(DotEditContentSidebarInformationComponent),
            MockComponent(DotEditContentSidebarWorkflowComponent)
        ],
        providers: [
            DotEditContentStore,
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotEditContentService),
            mockProvider(DotContentTypeService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotMessageService),
            mockProvider(Router),
            mockProvider(DotWorkflowService),
            mockProvider(MessageService),
            mockProvider(DotContentletService),
            mockProvider(DotLanguagesService),
            mockProvider(DialogService),
            {
                provide: ActivatedRoute,
                useValue: {
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

        store = spectator.inject(DotEditContentStore, true);
        dotEditContentService = spectator.inject(DotEditContentService);
        dotWorkflowService = spectator.inject(DotWorkflowService);

        // Mock the initial UI state
        jest.spyOn(utils, 'getStoredUIState').mockReturnValue({
            activeTab: 0,
            isSidebarOpen: true,
            activeSidebarTab: 0
        });

        dotEditContentService.getReferencePages.mockReturnValue(of(1));
        dotWorkflowService.getWorkflowStatus.mockReturnValue(of(MOCK_WORKFLOW_STATUS));

        spectator.detectChanges();
    });

    it('should create the component', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should render DotEditContentSidebarInformationComponent', () => {
        const informationComponent = spectator.query(DotEditContentSidebarInformationComponent);
        expect(informationComponent).toBeTruthy();
    });

    it('should render DotEditContentSidebarWorkflowComponent', () => {
        const workflowComponent = spectator.query(DotEditContentSidebarWorkflowComponent);
        expect(workflowComponent).toBeTruthy();
    });

    it('should render PrimeNG TabView', () => {
        const tabView = spectator.query(TabView);
        expect(tabView).toBeTruthy();
    });

    it('should render toggle button', () => {
        const toggleButton = spectator.query('[data-testId="toggle-button"]');
        expect(toggleButton).toBeTruthy();
    });

    it('should render append content in TabView', () => {
        const tabViewElement = spectator.query('p-tabview');
        const appendContent = tabViewElement.querySelector(
            '[data-testid="tabview-append-content"]'
        );
        expect(appendContent).toBeTruthy();
    });

    it('should call toggleSidebar when toggle button is clicked', () => {
        const storeSpy = jest.spyOn(store, 'toggleSidebar');

        spectator.click(byTestId('toggle-button'));

        expect(storeSpy).toHaveBeenCalled();
    });

    describe('UI State', () => {
        it('should initialize with correct UI state', () => {
            expect(store.isSidebarOpen()).toBe(true);
            expect(store.activeSidebarTab()).toBe(0);
        });

        it('should update active tab when changed', () => {
            store.setActiveSidebarTab(1);
            expect(store.activeSidebarTab()).toBe(1);
        });

        it('should toggle sidebar visibility', () => {
            const initialState = store.isSidebarOpen();
            store.toggleSidebar();
            expect(store.isSidebarOpen()).toBe(!initialState);
        });
    });
});
