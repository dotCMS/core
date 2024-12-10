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

import { DotEditContentStore } from '../../feature/edit-content/store/edit-content.store';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import { MOCK_WORKFLOW_STATUS } from '../../utils/edit-content.mock';
import { MockResizeObserver } from '../../utils/mocks';

describe('DotEditContentSidebarComponent', () => {
    let spectator: Spectator<DotEditContentSidebarComponent>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let dotWorkflowService: SpyObject<DotWorkflowService>;

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarComponent,
        declarations: [
            MockComponent(DotEditContentSidebarInformationComponent),
            MockComponent(DotEditContentSidebarWorkflowComponent)
        ],
        providers: [
            DotEditContentStore, // Due using the store directly
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

        dotEditContentService = spectator.inject(DotEditContentService);
        dotWorkflowService = spectator.inject(DotWorkflowService);
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
        const storeSpy = jest.spyOn(spectator.component.store, 'toggleSidebar');

        spectator.click(byTestId('toggle-button'));

        expect(storeSpy).toHaveBeenCalled();
    });
});
