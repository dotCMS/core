/* eslint-disable @typescript-eslint/no-unused-vars */
import { Spectator, createComponentFactory, SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { DotContentTypeService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

import { EditContentLayoutComponent } from './edit-content.layout.component';

import { DotEditContentFormComponent } from '../../components/dot-edit-content-form/dot-edit-content-form.component';
import { LAYOUT_MOCK } from '../../components/dot-edit-content-form/dot-edit-content-form.component.spec';
import { DotEditContentService } from '../../services/dot-edit-content.service';

const EDIT_CONTENT_LAYOUT_PROVIDERS_MOCK = [
    // DotEditContentService,
    // {
    //     provide: DotEditContentService,
    //     useValue: {
    //         getContentById: (id: string) => of({ contentType: 'test' }),
    //         getContentTypeFormData: (idOrVar: string) => () => of(LAYOUT_MOCK),
    //         saveContentlet: (data: { [key: string]: string }) => of({})
    //     }
    // },
    // {
    //     provide: DotContentTypeService,
    //     useValue: { getContentType: () => of(LAYOUT_MOCK) }
    // },
    // {
    //     provide: DotWorkflowActionsFireService,
    //     useValue: { saveContentlet: () => of({}) }
    // },
    // {
    //     provide: ActivatedRoute,
    //     useValue: { snapshot: { params: { contentType: 'test', id: '1' } } }
    // },
    // {
    //     provide: HttpClient,
    //     useValue: { get: () => of({ entity: { contentType: 'test' } }) }
    // }
];

export const CONTENT_TYPE_MOCK: DotCMSContentType = {
    baseType: 'CONTENT',
    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    defaultType: false,
    fields: [
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'SYSTEM',
            fieldType: 'Row',
            fieldTypeLabel: 'Row',
            fieldVariables: [],
            fixed: false,
            iDate: 1697051073000,
            id: 'a31ea895f80eb0a3754e4a2292e09a52',
            indexed: false,
            listed: false,
            modDate: 1697051077000,
            name: 'fields-0',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 0,
            unique: false,
            variable: 'fields0'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'SYSTEM',
            fieldType: 'Column',
            fieldTypeLabel: 'Column',
            fieldVariables: [],
            fixed: false,
            iDate: 1697051073000,
            id: 'd4c32b4b9fb5b11c58c245d4a02bef47',
            indexed: false,
            listed: false,
            modDate: 1697051077000,
            name: 'fields-1',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 1,
            unique: false,
            variable: 'fields1'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'TEXT',
            defaultValue: 'Placeholder',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            hint: 'A hint Text',
            iDate: 1697051093000,
            id: '1d1505a4569681b923769acb785fd093',
            indexed: false,
            listed: false,
            modDate: 1697051093000,
            name: 'name1',
            readOnly: false,
            required: true,
            searchable: false,
            sortOrder: 2,
            unique: false,
            variable: 'name1'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'TEXT',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            iDate: 1697051107000,
            id: 'fc776c45044f2d043f5e98eaae36c9ff',
            indexed: false,
            listed: false,
            modDate: 1697051107000,
            name: 'text2',
            readOnly: false,
            required: true,
            searchable: false,
            sortOrder: 3,
            unique: false,
            variable: 'text2'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'SYSTEM',
            fieldType: 'Column',
            fieldTypeLabel: 'Column',
            fieldVariables: [],
            fixed: false,
            iDate: 1697051077000,
            id: '848fc78a11e7290efad66eb39333ae2b',
            indexed: false,
            listed: false,
            modDate: 1697051107000,
            name: 'fields-2',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 4,
            unique: false,
            variable: 'fields2'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
            dataType: 'TEXT',
            fieldType: 'Text',
            fieldTypeLabel: 'Text',
            fieldVariables: [],
            fixed: false,
            hint: 'A hint text2',
            iDate: 1697051118000,
            id: '1f6765de8d4ad069ff308bfca56b9255',
            indexed: false,
            listed: false,
            modDate: 1697051118000,
            name: 'text3',
            readOnly: false,
            required: false,
            searchable: false,
            sortOrder: 5,
            unique: false,
            variable: 'text3'
        }
    ],
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    iDate: 1697051073000,
    icon: 'event_note',
    id: 'd46d6404125ac27e6ab68fad09266241',
    layout: LAYOUT_MOCK,
    modDate: 1697051118000,
    multilingualable: false,
    name: 'Test-K',
    system: false,
    systemActionMappings: {},
    variable: 'TestK',
    versionable: true,
    workflows: [
        {
            archived: false,
            creationDate: new Date(1697047303976),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            mandatory: false,
            modDate: new Date(1697047292887),
            name: 'System Workflow',
            system: true
        }
    ],
    nEntries: 0
};

describe('EditContentLayoutComponent', () => {
    let spectator: Spectator<EditContentLayoutComponent>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let dotContentTypeService: SpyObject<DotContentTypeService>;
    const createComponent = createComponentFactory({
        component: EditContentLayoutComponent,
        // imports: [CommonModule, DotEditContentFormComponent],
        imports: [HttpClientTestingModule],
        // mocks: [DotWorkflowActionsFireService, DotEditContentService],
        // componentMocks: [DotEditContentService]
        providers: [
            mockProvider(DotEditContentService),
            mockProvider(DotContentTypeService, { getContentType: () => of(LAYOUT_MOCK) }),
            mockProvider(DotWorkflowActionsFireService),
            // {
            //     provide: DotContentTypeService,
            //     useValue: { getContentType: () => of(LAYOUT_MOCK) }
            // },
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { params: { contentType: 'test', id: '1' } } }
            }
            // {
            //     provide: HttpClient,
            //     useValue: { get: () => of({ entity: { contentType: 'test' } }) }
            // }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        dotEditContentService = spectator.inject(DotEditContentService, true);

        dotContentTypeService = spectator.inject(DotContentTypeService);
    });

    it('should set contentType and identifier from activatedRoute', () => {
        // TODO: Ask why this dont work
        // const activatedRoute = spectator.inject(ActivatedRoute);
        // activatedRoute.snapshot.params.mockReturnValue({ contentType: 'test', id: '1' });
        // spectator.detectChanges();
        expect(spectator.component.contentType).toEqual('test');
        expect(spectator.component.identifier).toEqual('1');
    });

    describe('Data from form', () => {
        it('should call getContentById and getContentTypeFormData with contentType if identifier is present', () => {
            // TODO: Ask why this dont work
            // // dotEditContentService.getContentById.mockReturnValue(of(CONTENT_TYPE_MOCK));
            // const httpService = spectator.inject(HttpClient);
            // httpService.get.mockReturnValue(of({ entity: CONTENT_TYPE_MOCK }));
            // dotEditContentService.getContentTypeFormData.mockReturnValue(of(LAYOUT_MOCK));

            // TODO: Ask why this dont work as a spy mock.

            // dotContentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));
            spectator.detectChanges();

            expect(dotEditContentService.getContentTypeFormData).toHaveBeenCalledWith('test');
            // expect(dotEditContentService.getContentTypeFormData).toHaveBeenCalledWith('test');
        });

        // it('should call getContentTypeFormData with contentType if identifier is not present', () => {
        //     // const createComponentWithoutId = createComponentFactory({
        //     //     component: EditContentLayoutComponent,
        //     //     imports: [CommonModule, DotEditContentFormComponent],
        //     //     providers: EDIT_CONTENT_LAYOUT_PROVIDERS_MOCK
        //     // });
        //     // spectator = createComponentWithoutId();
        //     spectator.component.identifier = undefined;
        //     spectator.detectChanges();
        //     // dotEditContentService = spectator.inject(DotEditContentService);
        //     jest.spyOn(dotEditContentService, 'getContentTypeFormData');
        //     dotEditContentService;
        //     expect(dotEditContentService.getContentTypeFormData).toHaveBeenCalledWith('test');
        // });
    });

    // describe('saveContent', () => {
    //     it('should call dotEditContentService.saveContentlet with the correct parameters', () => {
    //         jest.spyOn(dotEditContentService, 'saveContentlet')
    //         spectator.component.saveContent({ key: 'value' });
    //         expect(dotEditContentService.saveContentlet).toHaveBeenCalledWith({
    //             key: 'value',
    //             inode: '1',
    //             contentType: 'test'
    //         });
    //     });

    //     it('should set isContentSaved to true and then false after 3 seconds', fakeAsync(() => {
    //         jest.spyOn(dotEditContentService, 'saveContentlet').and.returnValue(of({}));
    //         spectator.component.saveContent({ key: 'value' });
    //         expect(spectator.component.isContentSaved).toBe(true);
    //         tick(3000);
    //         expect(spectator.component.isContentSaved).toBe(false);
    //     }));
    // });
});
