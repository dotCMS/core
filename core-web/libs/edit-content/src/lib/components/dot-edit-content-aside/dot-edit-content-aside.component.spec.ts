import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService, dotcmsContentTypeBasicMock } from '@dotcms/utils-testing';

import { DotContentAsideInformationComponent } from './components/dot-content-aside-information/dot-content-aside-information.component';
import { DotContentAsideWorkflowComponent } from './components/dot-content-aside-workflow/dot-content-aside-workflow.component';
import { DotEditContentAsideComponent } from './dot-edit-content-aside.component';

import { CONTENT_FORM_DATA_MOCK } from '../../utils/mocks';

describe('DotEditContentAsideComponent', () => {
    let spectator: Spectator<DotEditContentAsideComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentAsideComponent,
        detectChanges: false,
        imports: [
            HttpClientTestingModule,
            DotContentAsideInformationComponent,
            DotContentAsideWorkflowComponent
        ],
        declarations: [
            MockComponent(DotContentAsideInformationComponent),
            MockComponent(DotContentAsideWorkflowComponent)
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

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet: CONTENT_FORM_DATA_MOCK.contentlet,
                contentType: dotcmsContentTypeBasicMock
            }
        });
    });

    it('should render aside information data', () => {
        spectator.detectChanges();
        const dotContentAsideInformationComponent = spectator.query(
            DotContentAsideInformationComponent
        );

        expect(dotContentAsideInformationComponent).toBeTruthy();
        expect(dotContentAsideInformationComponent.contentType).toEqual(dotcmsContentTypeBasicMock);
        expect(dotContentAsideInformationComponent.contentlet).toEqual(
            CONTENT_FORM_DATA_MOCK.contentlet
        );
    });

    it('should render aside workflow data', () => {
        spectator.detectChanges();
        const dotContentAsideWorkflowComponent = spectator.query(DotContentAsideWorkflowComponent);

        expect(dotContentAsideWorkflowComponent).toBeTruthy();
        expect(dotContentAsideWorkflowComponent.inode).toEqual(
            CONTENT_FORM_DATA_MOCK.contentlet.inode
        );
        expect(dotContentAsideWorkflowComponent.contentType).toEqual(dotcmsContentTypeBasicMock);
    });
});
