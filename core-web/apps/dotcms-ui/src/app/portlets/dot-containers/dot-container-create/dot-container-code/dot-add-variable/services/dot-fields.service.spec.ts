import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFieldsService } from './dot-fields.service';

import { DotFieldContent, FieldTypeWithExtraFields } from '../dot-add-variable.models';

const mockDotMessageService = new MockDotMessageService({
    'Content-Identifier-value': 'Content Identifier Value',
    'Content-Identifier': 'Content Identifier',
    'Image-Identifier': 'Identifier',
    Image: 'Image',
    'Image-Title': 'Title',
    'Image-Extension': 'Extension',
    'Image-Width': 'Width',
    'Image-Height': 'Height'
});
const mockContentTypeField = {
    variable: 'testVariable',
    name: 'test',
    fieldTypeLabel: 'Test Content Type'
};

describe('DotFieldsService', () => {
    let service: DotFieldsService;
    let imageFields: DotFieldContent[];

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotFieldsService,
                {
                    provide: DotMessageService,
                    useValue: mockDotMessageService
                }
            ]
        });
        service = TestBed.inject(DotFieldsService);
    });

    it('should have content identifier value', () => {
        expect(service.contentIdentifierField).toBeTruthy();
        expect(service.contentIdentifierField.name).toBe('Content Identifier Value');
        expect(service.contentIdentifierField.variable).toBe('ContentIdentifier');
        expect(service.contentIdentifierField.fieldTypeLabel).toBe('Content Identifier');
        expect(service.contentIdentifierField.codeTemplate).toBe(
            '$!{dotContentMap.ContentIdentifier}'
        );
    });

    it('should return the default fields', () => {
        const defaultField = service.fields.default(mockContentTypeField)[0];

        expect(defaultField).toBeTruthy();
        expect(defaultField.name).toBe('test');
        expect(defaultField.variable).toBe('testVariable');
        expect(defaultField.fieldTypeLabel).toBe('Test Content Type');
        expect(defaultField.codeTemplate).toBe('$!{dotContentMap.testVariable}');
    });

    describe('Image', () => {
        beforeEach(() => {
            imageFields = service.fields[FieldTypeWithExtraFields.IMAGE](mockContentTypeField);
        });

        it('should return the Image fields', () => {
            expect(imageFields.length).toBe(6);
        });

        it('should return the Image Identifier field', () => {
            expect(imageFields[0]).toBeTruthy();
            expect(imageFields[0].name).toBe('test: Identifier');
            expect(imageFields[0].variable).toBe('testVariableImageIdentifier');
            expect(imageFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[0].codeTemplate).toBe(
                '$!{dotContentMap.testVariableImageIdentifier}'
            );
        });

        // Write the test for all the fields

        it('should return the Image field', () => {
            expect(imageFields[1]).toBeTruthy();
            expect(imageFields[1].name).toBe('test: Image');
            expect(imageFields[1].variable).toBe('testVariableImage');
            expect(imageFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[1].codeTemplate).toBe(
                '#if ($UtilMethods.isSet(${testVariableImageURI}))\n    <img src="$!{dotContentMap.testVariableImageURI}" alt="$!{dotContentMap.testVariableImageTitle}" />\n#end'
            );
        });

        it('should return the Image Title field', () => {
            expect(imageFields[2]).toBeTruthy();
            expect(imageFields[2].name).toBe('test: Title');
            expect(imageFields[2].variable).toBe('testVariableImageTitle');
            expect(imageFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[2].codeTemplate).toBe('$!{dotContentMap.testVariableImageTitle}');
        });

        it('should return the Image Extension field', () => {
            expect(imageFields[3]).toBeTruthy();
            expect(imageFields[3].name).toBe('test: Extension');
            expect(imageFields[3].variable).toBe('testVariableImageExtension');
            expect(imageFields[3].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[3].codeTemplate).toBe(
                '$!{dotContentMap.testVariableImageExtension}'
            );
        });

        it('should return the Image Width field', () => {
            expect(imageFields[4]).toBeTruthy();
            expect(imageFields[4].name).toBe('test: Width');
            expect(imageFields[4].variable).toBe('testVariableImageWidth');
            expect(imageFields[4].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[4].codeTemplate).toBe('$!{dotContentMap.testVariableImageWidth}');
        });

        it('should return the Image Height field', () => {
            expect(imageFields[5]).toBeTruthy();
            expect(imageFields[5].name).toBe('test: Height');
            expect(imageFields[5].variable).toBe('testVariableImageHeight');
            expect(imageFields[5].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[5].codeTemplate).toBe('$!{dotContentMap.testVariableImageHeight}');
        });
    });
});
