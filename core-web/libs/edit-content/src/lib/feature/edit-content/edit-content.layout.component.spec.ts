import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent, MockPipe } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotContentTypeService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotFormatDateService, DotMessagePipe } from '@dotcms/ui';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { EditContentLayoutComponent } from './edit-content.layout.component';
import { DotEditContentStore } from './store/edit-content.store';

import { DotEditContentAsideComponent } from '../../components/dot-edit-content-aside/dot-edit-content-aside.component';
import { DotEditContentFormComponent } from '../../components/dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentToolbarComponent } from '../../components/dot-edit-content-toolbar/dot-edit-content-toolbar.component';
import { EditContentPayload } from '../../models/dot-edit-content-form.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import { BINARY_FIELD_CONTENTLET, CONTENT_TYPE_MOCK } from '../../utils/mocks';

const mockData: EditContentPayload = {
    actions: mockWorkflowsActions,
    contentType: CONTENT_TYPE_MOCK.variable,
    layout: CONTENT_TYPE_MOCK.layout,
    fields: CONTENT_TYPE_MOCK.fields,
    contentlet: BINARY_FIELD_CONTENTLET
};

describe('EditContentLayoutComponent', () => {
    let spectator: Spectator<EditContentLayoutComponent>;

    const createComponent = createComponentFactory({
        component: EditContentLayoutComponent,
        imports: [
            HttpClientTestingModule,
            MockPipe(DotMessagePipe),
            MockComponent(DotEditContentFormComponent),
            MockComponent(DotEditContentToolbarComponent),
            MockComponent(DotEditContentAsideComponent)
        ],
        providers: [
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(MessageService),
            mockProvider(DotContentTypeService),
            mockProvider(DotMessageService),
            mockProvider(DotFormatDateService),
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotEditContentService),
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { params: { contentType: undefined, id: '1' } } }
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({
            detectChanges: false,
            providers: [
                {
                    provide: DotEditContentStore,
                    useValue: {
                        vm$: of(mockData)
                    }
                }
            ]
        });

        spectator.detectChanges();
        await spectator.fixture.whenStable();
    });

    it('should pass the data to the DotEditContentForm Component', () => {
        const formComponent = spectator.query(DotEditContentFormComponent);
        expect(formComponent).toExist();
        expect(formComponent.formData).toEqual(mockData);
    });

    it('should pass the actions to the DotEditContentToolbar Component', () => {
        const toolbarComponent = spectator.query(DotEditContentToolbarComponent);
        expect(toolbarComponent).toExist();
        expect(toolbarComponent.actions).toEqual(mockData.actions);
    });

    it('should pass the contentlet and contentType to the DotEditContentAside Component', () => {
        const asideComponent = spectator.query(DotEditContentAsideComponent);
        expect(asideComponent).toExist();
        expect(asideComponent.contentLet).toEqual(mockData.contentlet);
        expect(asideComponent.contentType).toEqual(mockData.contentType);
    });
});
