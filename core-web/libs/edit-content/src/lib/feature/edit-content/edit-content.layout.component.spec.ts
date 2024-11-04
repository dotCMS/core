import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { MockComponent, MockModule } from 'ng-mocks';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { MessagesModule } from 'primeng/messages';
import { Toast, ToastModule } from 'primeng/toast';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { EditContentLayoutComponent } from './edit-content.layout.component';
import { DotEditContentStore } from './store/edit-content.store';

import { DotEditContentAsideComponent } from '../../components/dot-edit-content-aside/dot-edit-content-aside.component';
import { DotEditContentFormComponent } from '../../components/dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import * as utils from '../../utils/functions.util';
import { CONTENT_TYPE_MOCK } from '../../utils/mocks';

describe('EditContentLayoutComponent', () => {
    let spectator: Spectator<EditContentLayoutComponent>;

    let store: SpyObject<InstanceType<typeof DotEditContentStore>>;
    let dotContentTypeService: SpyObject<DotContentTypeService>;
    let workflowActionsService: SpyObject<DotWorkflowsActionsService>;

    const createComponent = createComponentFactory({
        component: EditContentLayoutComponent,
        imports: [
            MockModule(ToastModule),

            MockModule(MessagesModule),
            MockComponent(DotEditContentFormComponent),
            MockComponent(DotEditContentAsideComponent)
        ],
        componentProviders: [
            DotEditContentStore, // Usign the real DotEditContentStore
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotEditContentService),
            mockProvider(DotContentTypeService)
        ],
        providers: [
            mockProvider(DotHttpErrorManagerService),
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
            },
            mockProvider(Router, {
                navigate: jest.fn().mockReturnValue(Promise.resolve(true)),
                url: '/test-url',
                events: of()
            }),

            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        spectator.detectChanges();

        store = spectator.inject(DotEditContentStore, true);
        dotContentTypeService = spectator.inject(DotContentTypeService, true);

        workflowActionsService = spectator.inject(DotWorkflowsActionsService, true);

        // By default, the local storage is set to true
        jest.spyOn(utils, 'getPersistSidebarState').mockReturnValue(true);
    });

    it('should have p-toast component', () => {
        expect(spectator.query(Toast)).toBeTruthy();
    });

    it('should have p-confirmDialog component', () => {
        expect(spectator.query(ConfirmDialog)).toBeTruthy();
    });

    describe('New Content Editor', () => {
        it('should initialize new content, show layout components and dialogs when new content editor is enabled', fakeAsync(() => {
            dotContentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            workflowActionsService.getDefaultActions.mockReturnValue(of(mockWorkflowsActions));

            store.initializeNewContent('contentTypeName');

            spectator.detectChanges();

            tick(); // Wait for the defer to load

            expect(store.isEnabledNewContentEditor()).toBe(true);
            expect(store.showSidebar()).toBe(true);

            expect(spectator.query(byTestId('edit-content-layout__topBar'))).toBeTruthy();
            expect(spectator.query(byTestId('edit-content-layout__body'))).toBeTruthy();
            expect(spectator.query(byTestId('edit-content-layout__sidebar'))).toBeTruthy();

            expect(spectator.query(Toast)).toBeTruthy();
            expect(spectator.query(ConfirmDialog)).toBeTruthy();
        }));

        it('should not show top bar message when new content editor is disabled', () => {
            const CONTENT_TYPE_MOCK_NO_METADATA = {
                ...CONTENT_TYPE_MOCK,
                metadata: undefined
            };

            dotContentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK_NO_METADATA));
            workflowActionsService.getDefaultActions.mockReturnValue(of(mockWorkflowsActions));

            store.initializeNewContent('contentTypeName');

            spectator.detectChanges();
            expect(store.isEnabledNewContentEditor()).toBe(false);
            expect(spectator.query(byTestId('edit-content-layout__topBar'))).toBeNull();
        });
    });
});
