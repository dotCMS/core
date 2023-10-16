import { Spectator, createComponentFactory, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { DotCMSContentType } from '@dotcms/dotcms-models';

import { EditContentLayoutComponent } from './edit-content.layout.component';

import { LAYOUT_MOCK } from '../../components/dot-edit-content-form/dot-edit-content-form.component.spec';
import { DotEditContentService } from '../../services/dot-edit-content.service';

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
    name: 'Test',
    contentType: 'Test',
    system: false,
    systemActionMappings: {},
    variable: 'Test',
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

const createEditContentLayoutComponent = (params: { contentType?: string; id?: string }) => {
    return createComponentFactory({
        component: EditContentLayoutComponent,
        imports: [HttpClientTestingModule],
        componentProviders: [
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { params } }
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
                        getContentTypeFormData: jest.fn().mockReturnValue(of(LAYOUT_MOCK)),
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

    it('should call dotEditContentService.saveContentlet with the correct parameters - Using contentType from getContentById', () => {
        spectator.detectChanges();
        spectator.component.saveContent({ key: 'value' });
        expect(dotEditContentService.saveContentlet).toHaveBeenCalledWith({
            key: 'value',
            inode: '1',
            contentType: 'Test'
        });
    });

    it('should have a [formData] reference on the <dot-edit-content-form>', async () => {
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
                        getContentTypeFormData: jest.fn().mockReturnValue(of(LAYOUT_MOCK)),
                        getContentById: jest.fn().mockReturnValue(of(CONTENT_TYPE_MOCK))
                    }
                }
            ]
        });

        dotEditContentService = spectator.inject(DotEditContentService, true);
    });

    it('should set contentType from activatedRoute - Identifier undefined.', () => {
        expect(spectator.component.contentType).toEqual('test');
        expect(spectator.component.identifier).toEqual(undefined);
    });

    it('should call getContentById and getContentTypeFormData with contentType if identifier is NOT present', () => {
        spectator.detectChanges();
        expect(dotEditContentService.getContentById).not.toHaveBeenCalled();
        expect(dotEditContentService.getContentTypeFormData).toHaveBeenCalledWith('test');
    });
});
