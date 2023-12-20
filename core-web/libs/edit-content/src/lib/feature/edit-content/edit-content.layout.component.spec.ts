import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent, MockPipe } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotContentTypeService,
    DotMessageService,
    DotRenderMode,
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
import { DotEditContentService } from '../../services/dot-edit-content.service';
import { BINARY_FIELD_CONTENTLET, CONTENT_TYPE_MOCK } from '../../utils/mocks';

// const mockData: EditContentPayload = {
//     actions: mockWorkflowsActions,
//     contentType: CONTENT_TYPE_MOCK.variable,
//     layout: CONTENT_TYPE_MOCK.layout,
//     fields: CONTENT_TYPE_MOCK.fields,
//     contentlet: BINARY_FIELD_CONTENTLET
// };

describe('EditContentLayoutComponent', () => {
    let spectator: Spectator<EditContentLayoutComponent>;
    let dotEditContentService: DotEditContentService;
    let dotWorkflowsActionsService: DotWorkflowsActionsService;

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
            mockProvider(MessageService),
            mockProvider(DotContentTypeService),
            mockProvider(DotMessageService),
            mockProvider(DotFormatDateService),
            mockProvider(DotEditContentStore),
            mockProvider(DotWorkflowActionsFireService),

            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: jest.fn().mockReturnValue(of(mockWorkflowsActions)),
                    getDefaultActions: jest.fn().mockReturnValue(of(mockWorkflowsActions))
                }
            },
            {
                provide: DotEditContentService,
                useValue: {
                    getContentType: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK)),
                    getContentById: jest.fn().mockReturnValue(of(BINARY_FIELD_CONTENTLET))
                }
            },
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { params: { contentType: undefined, id: '1' } } }
            }
        ]
    });

    describe('Existing content', () => {
        beforeEach(async () => {
            spectator = createComponent({
                detectChanges: false
            });

            dotEditContentService = spectator.inject(DotEditContentService, true);
            dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService, true);
        });

        it('should get content data', () => {
            const spyContent = jest.spyOn(dotEditContentService, 'getContentById');
            const spyContentType = jest.spyOn(dotEditContentService, 'getContentType');
            const spyWorkflow = jest.spyOn(dotWorkflowsActionsService, 'getByInode');

            spectator.detectChanges();

            expect(spyContent).toHaveBeenCalledWith('1'); // It's been called
            expect(spyContentType).toHaveBeenCalledWith(BINARY_FIELD_CONTENTLET.contentType); // It's not been called
            expect(spyWorkflow).toHaveBeenCalledWith('1', DotRenderMode.EDITING); // It's not been called
        });
    });

    describe('New content', () => {
        beforeEach(async () => {
            spectator = createComponent({
                detectChanges: false
            });

            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });
    });
});
