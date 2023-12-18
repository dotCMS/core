import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { MockComponent, MockPipe } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotFormatDateService, DotMessagePipe } from '@dotcms/ui';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { EditContentLayoutComponent } from './edit-content.layout.component';
import { DotEditContentStore } from './store/edit-content.store';

import { DotEditContentFormComponent } from '../../components/dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import { BINARY_FIELD_CONTENTLET, CONTENT_TYPE_MOCK } from '../../utils/mocks';

const createEditContentLayoutComponent = (params: { contentType?: string; id?: string }) => {
    return createComponentFactory({
        component: EditContentLayoutComponent,
        imports: [
            HttpClientTestingModule,
            MockPipe(DotMessagePipe),
            MockComponent(DotEditContentFormComponent)
        ],
        providers: [
            DotEditContentStore,
            DotWorkflowActionsFireService,
            MessageService,
            mockProvider(DotMessageService),
            mockProvider(DotFormatDateService),
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { params } }
            },
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: jest.fn().mockReturnValue(of(mockWorkflowsActions)),
                    getDefaultActions: jest.fn().mockReturnValue(of(mockWorkflowsActions))
                }
            }
        ]
    });
};

describe('EditContentLayoutComponent with identifier', () => {
    let spectator: Spectator<EditContentLayoutComponent>;
    let dotEditContentStore: SpyObject<DotEditContentStore>;

    const createComponent = createEditContentLayoutComponent({ contentType: undefined, id: '1' });

    beforeEach(async () => {
        spectator = createComponent({
            detectChanges: false,
            providers: [
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
                        getContentById: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK)),
                        saveContentlet: jest.fn().mockReturnValue(of({}))
                    }
                }
            ]
        });

        dotEditContentStore = spectator.inject(DotEditContentStore, true);
        jest.spyOn(dotEditContentStore, 'loadContentEffect');
    });

    it('should set identifier from activatedRoute and contentType undefined', () => {
        expect(spectator.component.contentType).toEqual(undefined);
        expect(spectator.component.identifier).toEqual('1');
    });

    it('should have a [formData] reference on the <dot-edit-content-form>', async () => {
        const data = {
            actions: [],
            contentType: CONTENT_TYPE_MOCK.variable,
            layout: CONTENT_TYPE_MOCK?.layout || [],
            fields: CONTENT_TYPE_MOCK?.fields || [],
            contentlet: BINARY_FIELD_CONTENTLET
        };

        spectator.component.vm$ = of(data);
        spectator.detectChanges();
        const component = spectator.query(DotEditContentFormComponent);
        expect(component).toExist();
        expect(component.formData).toEqual(data);
    });

    it('should call the store with `isNewContent` property being false and the identifier', () => {
        spectator.detectChanges();

        expect(dotEditContentStore.loadContentEffect).toHaveBeenCalledWith({
            isNewContent: false,
            idOrVar: '1'
        });
    });
});

describe('EditContentLayoutComponent without identifier', () => {
    let spectator: Spectator<EditContentLayoutComponent>;
    let dotEditContentStore: SpyObject<DotEditContentStore>;

    const createComponent = createEditContentLayoutComponent({
        contentType: 'test',
        id: undefined
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false,
            providers: [
                {
                    provide: DotEditContentService,
                    useValue: {
                        getContentType: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK)),
                        getContentById: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK))
                    }
                }
            ]
        });

        dotEditContentStore = spectator.inject(DotEditContentStore, true);
        jest.spyOn(dotEditContentStore, 'loadContentEffect');
        spectator.detectChanges();
    });

    it('should set contentType from activatedRoute - Identifier undefined.', () => {
        expect(spectator.component.contentType).toEqual('test');
        expect(spectator.component.identifier).toEqual(undefined);
    });

    it('should call the store with `isNewContent` property being true and the contentType', () => {
        expect(dotEditContentStore.loadContentEffect).toHaveBeenCalledWith({
            isNewContent: true,
            idOrVar: 'test'
        });
    });
});
