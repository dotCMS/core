import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { MockPipe } from 'ng-mocks';
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

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { CONTENT_TYPE_MOCK, JUST_FIELDS_MOCKS, LAYOUT_MOCK } from '../../utils/mocks';

const createEditContentLayoutComponent = (params: { contentType?: string; id?: string }) => {
    return createComponentFactory({
        component: EditContentLayoutComponent,
        imports: [HttpClientTestingModule, MockPipe(DotMessagePipe)],
        componentProviders: [
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { params } }
            },
            mockProvider(DotFormatDateService)
        ],
        providers: [
            DotWorkflowActionsFireService,
            MessageService,
            mockProvider(DotMessageService),
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
    let dotEditContentService: SpyObject<DotEditContentService>;

    const createComponent = createEditContentLayoutComponent({ contentType: undefined, id: '1' });

    beforeEach(async () => {
        spectator = createComponent({
            detectChanges: false,
            providers: [
                {
                    provide: DotEditContentService,
                    useValue: {
                        getContentTypeFormData: jest.fn().mockReturnValue(
                            of({
                                layout: LAYOUT_MOCK,
                                fields: JUST_FIELDS_MOCKS
                            })
                        ),
                        getContentById: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK)),
                        saveContentlet: jest.fn().mockReturnValue(of({}))
                    }
                }
            ]
        });

        dotEditContentService = spectator.inject(DotEditContentService, true);
    });

    it('should set identifier from activatedRoute and contentType undefined', () => {
        expect(spectator.component.contentType).toEqual(undefined);
        expect(spectator.component.identifier).toEqual('1');
    });

    it('should call getContentById and getContentTypeFormData with contentType if identifier is present', () => {
        spectator.detectChanges();
        expect(dotEditContentService.getContentById).toHaveBeenCalledWith('1');
    });

    it('should have a [formData] reference on the <dot-edit-content-form>', () => {
        spectator.detectChanges();
        const formElement = spectator.query('dot-edit-content-form');
        expect(formElement.hasAttribute('ng-reflect-form-data')).toBe(true);
        expect(formElement).toBeDefined();
    });
});

describe('EditContentLayoutComponent without identifier', () => {
    let spectator: Spectator<EditContentLayoutComponent>;
    let dotEditContentService: SpyObject<DotEditContentService>;

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
                        getContentTypeFormData: jest.fn().mockReturnValue(
                            of({
                                layout: LAYOUT_MOCK,
                                fields: JUST_FIELDS_MOCKS,
                                contentType: 'test'
                            })
                        ),
                        getContentById: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK))
                    }
                }
            ]
        });

        dotEditContentService = spectator.inject(DotEditContentService, true);
        spectator.detectChanges();
    });

    it('should set contentType from activatedRoute - Identifier undefined.', () => {
        expect(spectator.component.contentType).toEqual('test');
        expect(spectator.component.identifier).toEqual(undefined);
    });

    it('should call getContentById and getContentTypeFormData with contentType if identifier is NOT present', () => {
        expect(dotEditContentService.getContentById).not.toHaveBeenCalled();
        expect(dotEditContentService.getContentTypeFormData).toHaveBeenCalledWith('test');
    });
});
