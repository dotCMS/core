import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFieldsService } from './dot-fields.service';

import { DotFieldContent, FieldTypes } from '../dot-add-variable.models';

const mockDotMessageService = new MockDotMessageService({
    'Content-Identifier-value': 'Content Identifier Value',
    'Content-Identifier': 'Content Identifier',
    'Image-Identifier': 'Identifier',
    Image: 'Image',
    'Image-Title': 'Title',
    'Image-Extension': 'Extension',
    'Image-Width': 'Width',
    'Image-Height': 'Height',
    'Host-Folder': 'Host Folder',
    File: 'File',
    'File-Identifier': 'Identifier',
    'File-Extension': 'Extension',
    'html-render': 'HTML Render',
    'Binary-File': 'File',
    'Binary-File-Resized': 'Resized',
    'Binary-File-Thumbnail': 'Thumbnail',
    'Selected-Value': 'Selected Value',
    'Labels-Values': 'Labels & Values, e.g.: USA|US Canada|CA',
    'contenttypes.field.properties.data_type.values.date': 'Date',
    'mm-dd-yyyy': '(mm-dd-yyyy)',
    'Date-Database-Format': 'Date Database Format',
    'yyyy-mm-dd': '(yyyy-mm-dd)',
    Time: 'Time',
    'hh-mm-ss': '(hh:mm:ss)',
    Date: 'Date',
    'Date-Short-String': 'Date Short String',
    'Date-Long-String': 'Date Long String (mm/dd/yyyy hh:mm:ss)'
});
const mockContentTypeField = {
    variable: 'testVariable',
    name: 'test',
    fieldTypeLabel: 'Test Content Type'
};

describe('DotFieldsService', () => {
    let service: DotFieldsService;

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
        let imageFields: DotFieldContent[];

        beforeEach(() => {
            imageFields = service.fields[FieldTypes.IMAGE](mockContentTypeField);
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

    describe('Host', () => {
        let hostFields: DotFieldContent[];

        beforeEach(() => {
            hostFields = service.fields[FieldTypes.HOST](mockContentTypeField);
        });

        it('should return the Host fields', () => {
            expect(hostFields.length).toBe(1);
        });

        it('should return the Host field', () => {
            expect(hostFields[0]).toBeTruthy();
            expect(hostFields[0].name).toBe('test');
            expect(hostFields[0].variable).toBe('testVariable');
            expect(hostFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(hostFields[0].codeTemplate).toBe('$!{dotContentMap.ConHostFolder}');
        });
    });

    describe('File', () => {
        let fileFields: DotFieldContent[];

        beforeEach(() => {
            fileFields = service.fields[FieldTypes.FILE](mockContentTypeField);
        });

        it('should return the File fields', () => {
            expect(fileFields.length).toBe(3);
        });

        it('should return the File field', () => {
            expect(fileFields[0]).toBeTruthy();
            expect(fileFields[0].name).toBe('test: File');
            expect(fileFields[0].variable).toBe('testVariableFile');
            expect(fileFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(fileFields[0].codeTemplate).toBe(
                '#if (${testVariableFileURI})\n    <a href="$!{dotContentMap.testVariableFileURI}">$!{dotContentMap.testVariableFileTitle}</a>\n#end'
            );
        });

        it('should return the File Identifier field', () => {
            expect(fileFields[1]).toBeTruthy();
            expect(fileFields[1].name).toBe('test: Identifier');
            expect(fileFields[1].variable).toBe('testVariableFileIdentifier');
            expect(fileFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(fileFields[1].codeTemplate).toBe('$!{dotContentMap.testVariableFileIdentifier}');
        });

        it('should return the File Extension field', () => {
            expect(fileFields[2]).toBeTruthy();
            expect(fileFields[2].name).toBe('test: Extension');
            expect(fileFields[2].variable).toBe('testVariableFileExtension');
            expect(fileFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(fileFields[2].codeTemplate).toBe('$!{dotContentMap.testVariableFileExtension}');
        });
    });

    describe('Story-Block (Block Editor)', () => {
        let blockEditorFields: DotFieldContent[];

        beforeEach(() => {
            blockEditorFields = service.fields[FieldTypes.BLOCK_EDITOR](mockContentTypeField);
        });

        it('should return the Block Editor fields', () => {
            expect(blockEditorFields.length).toBe(1);
        });

        it('should return the Block Editor field', () => {
            expect(blockEditorFields[0]).toBeTruthy();
            expect(blockEditorFields[0].name).toBe('test: HTML Render');
            expect(blockEditorFields[0].variable).toBe('testVariable');
            expect(blockEditorFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(blockEditorFields[0].codeTemplate).toBe(
                "$dotContentMap.get('testVariable').toHtml()"
            );
        });
    });

    describe('Binary', () => {
        let binaryFields: DotFieldContent[];

        beforeEach(() => {
            binaryFields = service.fields[FieldTypes.BINARY](mockContentTypeField);
        });

        it('should return the Binary fields', () => {
            expect(binaryFields.length).toBe(4);
        });

        it('should return the Binary File', () => {
            expect(binaryFields[0]).toBeTruthy();
            expect(binaryFields[0].name).toBe('test: File');
            expect(binaryFields[0].variable).toBe('testVariableBinaryFile');
            expect(binaryFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(binaryFields[0].codeTemplate).toBe(
                '#if ($UtilMethods.isSet(${testVariableBinaryFileURI}))\n    <a href="$!{dotContentMap.testVariableBinaryFileURI}?force_download=1&filename=$!{dotContentMap.testVariableBinaryFileTitle}">$!{dotContentMap.testVariableBinaryFileTitle}</a>\n#end'
            );
        });

        it('should return the Binary File Resized', () => {
            expect(binaryFields[1]).toBeTruthy();
            expect(binaryFields[1].name).toBe('test: Resized');
            expect(binaryFields[1].variable).toBe('testVariableBinaryFileResized');
            expect(binaryFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(binaryFields[1].codeTemplate).toBe(
                '#if ($UtilMethods.isSet(${testVariableBinaryFileURI}))\n    <img src="/contentAsset/resize-image/${ContentIdentifier}/testVariable?w=150&h=100&language_id=${language}" />\n#end'
            );
        });

        it('should return the Binary File Thumbnail', () => {
            expect(binaryFields[2]).toBeTruthy();
            expect(binaryFields[2].name).toBe('test: Thumbnail');
            expect(binaryFields[2].variable).toBe('testVariableBinaryFileThumbnail');
            expect(binaryFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(binaryFields[2].codeTemplate).toBe(
                '#if ($UtilMethods.isSet(${testVariableBinaryFileURI}))\n    <img src="/contentAsset/image-thumbnail/${ContentIdentifier}/testVariable?w=150&h=150&language_id=${language}" />\n#end'
            );
        });
    });

    describe('Select', () => {
        let selectFields: DotFieldContent[];

        beforeEach(() => {
            selectFields = service.fields[FieldTypes.SELECT](mockContentTypeField);
        });

        it('should return the Select fields', () => {
            expect(selectFields.length).toBe(2);
        });

        it('should return the Selected Value', () => {
            expect(selectFields[0]).toBeTruthy();
            expect(selectFields[0].name).toBe('test: Selected Value');
            expect(selectFields[0].variable).toBe('testVariable');
            expect(selectFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(selectFields[0].codeTemplate).toBe('$!{dotContentMap.testVariable}');
        });

        it('should return the labels and values', () => {
            expect(selectFields[1]).toBeTruthy();
            expect(selectFields[1].name).toBe('test: Labels & Values, e.g.: USA|US Canada|CA');
            expect(selectFields[1].variable).toBe('testVariableSelectLabelsValues');
            expect(selectFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(selectFields[1].codeTemplate).toBe(
                '$!{dotContentMap.testVariableSelectLabelsValues}'
            );
        });
    });

    describe('MultiSelect', () => {
        let multiSelectFields: DotFieldContent[];

        beforeEach(() => {
            multiSelectFields = service.fields[FieldTypes.MULTISELECT](mockContentTypeField);
        });

        it('should return the Select fields', () => {
            expect(multiSelectFields.length).toBe(2);
        });

        it('should return the Selected Value', () => {
            expect(multiSelectFields[0]).toBeTruthy();
            expect(multiSelectFields[0].name).toBe('test: Selected Value');
            expect(multiSelectFields[0].variable).toBe('testVariable');
            expect(multiSelectFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(multiSelectFields[0].codeTemplate).toBe('$!{dotContentMap.testVariable}');
        });

        it('should return the labels and values', () => {
            expect(multiSelectFields[1]).toBeTruthy();
            expect(multiSelectFields[1].name).toBe('test: Labels & Values, e.g.: USA|US Canada|CA');
            expect(multiSelectFields[1].variable).toBe('testVariableSelectLabelsValues');
            expect(multiSelectFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(multiSelectFields[1].codeTemplate).toBe(
                '$!{dotContentMap.testVariableSelectLabelsValues}'
            );
        });
    });

    describe('Radio', () => {
        let radioFields: DotFieldContent[];

        beforeEach(() => {
            radioFields = service.fields[FieldTypes.RADIO](mockContentTypeField);
        });

        it('should return the Select fields', () => {
            expect(radioFields.length).toBe(2);
        });

        it('should return the Selected Value', () => {
            expect(radioFields[0]).toBeTruthy();
            expect(radioFields[0].name).toBe('test: Selected Value');
            expect(radioFields[0].variable).toBe('testVariable');
            expect(radioFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(radioFields[0].codeTemplate).toBe('$!{dotContentMap.testVariable}');
        });

        it('should return the labels and values', () => {
            expect(radioFields[1]).toBeTruthy();
            expect(radioFields[1].name).toBe('test: Labels & Values, e.g.: USA|US Canada|CA');
            expect(radioFields[1].variable).toBe('testVariableRadioLabelsValues');
            expect(radioFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(radioFields[1].codeTemplate).toBe(
                '$!{dotContentMap.testVariableRadioLabelsValues}'
            );
        });
    });

    describe('Checkbox', () => {
        let checkboxFields: DotFieldContent[];

        beforeEach(() => {
            checkboxFields = service.fields[FieldTypes.CHECKBOX](mockContentTypeField);
        });

        it('should return the Select fields', () => {
            expect(checkboxFields.length).toBe(2);
        });

        it('should return the Selected Value', () => {
            expect(checkboxFields[0]).toBeTruthy();
            expect(checkboxFields[0].name).toBe('test: Selected Value');
            expect(checkboxFields[0].variable).toBe('testVariable');
            expect(checkboxFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(checkboxFields[0].codeTemplate).toBe('$!{dotContentMap.testVariable}');
        });

        it('should return the labels and values', () => {
            expect(checkboxFields[1]).toBeTruthy();
            expect(checkboxFields[1].name).toBe('test: Labels & Values, e.g.: USA|US Canada|CA');
            expect(checkboxFields[1].variable).toBe('testVariableCheckboxLabelsValues');
            expect(checkboxFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(checkboxFields[1].codeTemplate).toBe(
                '$!{dotContentMap.testVariableCheckboxLabelsValues}'
            );
        });
    });

    describe('Date', () => {
        let dateFields: DotFieldContent[];

        beforeEach(() => {
            dateFields = service.fields[FieldTypes.DATE](mockContentTypeField);
        });

        it('should return the Date fields', () => {
            expect(dateFields.length).toBe(2);
        });

        it('should return the Date field', () => {
            expect(dateFields[0]).toBeTruthy();
            expect(dateFields[0].name).toBe('test: Date (mm-dd-yyyy)');
            expect(dateFields[0].variable).toBe('testVariable');
            expect(dateFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(dateFields[0].codeTemplate).toBe('$!{dotContentMap.testVariable}');
        });

        it('should return the Date Database Format field', () => {
            expect(dateFields[1]).toBeTruthy();
            expect(dateFields[1].name).toBe('test: Date Database Format (yyyy-mm-dd)');
            expect(dateFields[1].variable).toBe('testVariableDBFormat');
            expect(dateFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(dateFields[1].codeTemplate).toBe('$!{dotContentMap.testVariableDBFormat}');
        });
    });

    describe('Time', () => {
        let timeFields: DotFieldContent[];

        beforeEach(() => {
            timeFields = service.fields[FieldTypes.TIME](mockContentTypeField);
        });

        it('should return the Time fields', () => {
            expect(timeFields.length).toBe(1);
        });

        it('should return the Time field', () => {
            expect(timeFields[0]).toBeTruthy();
            expect(timeFields[0].name).toBe('test: Time (hh:mm:ss)');
            expect(timeFields[0].variable).toBe('testVariable');
            expect(timeFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(timeFields[0].codeTemplate).toBe('$!{dotContentMap.testVariable}');
        });
    });

    describe('Date and Time', () => {
        let dateTimeFields: DotFieldContent[];

        beforeEach(() => {
            dateTimeFields = service.fields[FieldTypes.DATE_AND_TIME](mockContentTypeField);
        });

        it('should return the Date and Time fields', () => {
            expect(dateTimeFields.length).toBe(4);
        });

        it('should return the Date field', () => {
            expect(dateTimeFields[0]).toBeTruthy();
            expect(dateTimeFields[0].name).toBe('test: Date');
            expect(dateTimeFields[0].variable).toBe('testVariable');
            expect(dateTimeFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(dateTimeFields[0].codeTemplate).toBe('$!{dotContentMap.testVariable}');
        });

        it('should return the Date Short String Format', () => {
            expect(dateTimeFields[1]).toBeTruthy();
            expect(dateTimeFields[1].name).toBe('test: Date Short String (mm-dd-yyyy)');
            expect(dateTimeFields[1].variable).toBe('testVariableShortFormat');
            expect(dateTimeFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(dateTimeFields[1].codeTemplate).toBe(
                '$!{dotContentMap.testVariableShortFormat}'
            );
        });

        it('should return the Date Long String Format', () => {
            expect(dateTimeFields[2]).toBeTruthy();
            expect(dateTimeFields[2].name).toBe('test: Date Long String (mm/dd/yyyy hh:mm:ss)');
            expect(dateTimeFields[2].variable).toBe('testVariableLongFormat');
            expect(dateTimeFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(dateTimeFields[2].codeTemplate).toBe('$!{dotContentMap.testVariableLongFormat}');
        });

        it('should return the Date Database Format field', () => {
            expect(dateTimeFields[3]).toBeTruthy();
            expect(dateTimeFields[3].name).toBe('test: Date Database Format (yyyy-mm-dd)');
            expect(dateTimeFields[3].variable).toBe('testVariableDBFormat');
            expect(dateTimeFields[3].fieldTypeLabel).toBe('Test Content Type');
            expect(dateTimeFields[3].codeTemplate).toBe('$!{dotContentMap.testVariableDBFormat}');
        });
    });
});
