import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService, dotcmsContentTypeBasicMock } from '@dotcms/utils-testing';

import { DotContentSidebarInformationComponent } from './components/dot-edit-content-sidebar-information/dot-content-sidebar-information.component';
import { DotContentSidebarWorkflowComponent } from './components/dot-edit-content-sidebar-workflow/dot-edit-content-sidebar-workflow.component';
import { DotEditContentSidebarComponent } from './dot-edit-content-sidebar.component';

import { CONTENT_FORM_DATA_MOCK, MockResizeObserver } from '../../utils/mocks';

describe('DotEditContentSidebarComponent', () => {
    let spectator: Spectator<DotEditContentSidebarComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentSidebarComponent,
        imports: [
            HttpClientTestingModule,
            DotContentSidebarInformationComponent,
            DotContentSidebarWorkflowComponent
        ],
        declarations: [
            MockComponent(DotContentSidebarInformationComponent),
            MockComponent(DotContentSidebarWorkflowComponent)
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    Information: 'Information',
                    Workflow: 'Workflow',
                    'show-all': 'Show all'
                })
            }
        ]
    });
    beforeAll(() => {
        window.ResizeObserver = MockResizeObserver;
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet: CONTENT_FORM_DATA_MOCK.contentlet,
                contentType: dotcmsContentTypeBasicMock,
                loading: false,
                collapsed: false
            } as unknown
        });
    });

    it('should render aside information data', () => {
        spectator.detectChanges();
        const dotContentSidebarInformationComponent = spectator.query(
            DotContentSidebarInformationComponent
        );

        expect(dotContentSidebarInformationComponent).toBeTruthy();
        expect(dotContentSidebarInformationComponent.contentType).toEqual(
            dotcmsContentTypeBasicMock
        );
        expect(dotContentSidebarInformationComponent.contentlet).toEqual(
            CONTENT_FORM_DATA_MOCK.contentlet
        );
    });

    it('should render aside workflow data', () => {
        spectator.detectChanges();
        const dotContentSidebarWorkflowComponent = spectator.query(
            DotContentSidebarWorkflowComponent
        );

        expect(dotContentSidebarWorkflowComponent).toBeTruthy();
        expect(dotContentSidebarWorkflowComponent.inode).toEqual(
            CONTENT_FORM_DATA_MOCK.contentlet.inode
        );
        expect(dotContentSidebarWorkflowComponent.contentType).toEqual(dotcmsContentTypeBasicMock);
    });

    it('should emit toggle event on button click', () => {
        const spy = jest.spyOn(spectator.component.$toggle, 'emit');

        const toggleBtn = spectator.query('[data-testId="toggle-button"]');

        spectator.click(toggleBtn);

        expect(spy).toHaveBeenCalled();
    });
});
