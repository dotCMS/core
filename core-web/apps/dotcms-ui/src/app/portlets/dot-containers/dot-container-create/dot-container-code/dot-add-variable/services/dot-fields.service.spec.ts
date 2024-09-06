import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DOT_CONTENT_MAP, DotFieldsService } from './dot-fields.service';

import { DotFieldContent, FieldTypes } from '../dot-add-variable.models';

const mockDotMessageService = new MockDotMessageService({
    'Binary-File-Resized': 'Resized',
    'Binary-File-Thumbnail': 'Thumbnail',
    'Binary-File': 'File',
    'Content-Identifier-value': 'Content Identifier Value',
    'Content-Identifier': 'Content Identifier',
    'contenttypes.field.properties.data_type.values.date': 'Date',
    'Date-Database-Format': 'Date Database Format',
    'Date-Long-String': 'Date Long String (mm/dd/yyyy hh:mm:ss)',
    'Date-Short-String': 'Date Short String',
    'File-Extension': 'Extension',
    'File-Identifier': 'Identifier',
    'hh-mm-ss': '(hh:mm:ss)',
    'Host-Folder': 'Host Folder',
    'html-render': 'HTML Render',
    'Image-Extension': 'Extension',
    'Image-Height': 'Height',
    'Image-Identifier': 'Identifier',
    'Image-Title': 'Title',
    'Image-Width': 'Width',
    'Labels-Values': 'Labels & Values, e.g.: USA|US Canada|CA',
    'mm-dd-yyyy': '(mm-dd-yyyy)',
    'Selected-Value': 'Selected Value',
    'Selected-Values': 'Selected Values',
    'yyyy-mm-dd': '(yyyy-mm-dd)',
    Date: 'Date',
    File: 'File',
    Image: 'Image',
    Options: 'Options',
    Time: 'Time',
    values: 'Values'
});
const mockContentTypeField = {
    variable: 'testVariable',
    name: 'test',
    fieldTypeLabel: 'Test Content Type'
};

describe('DotFieldsService', () => {
    let service: DotFieldsService;
    const variable: string = mockContentTypeField.variable;

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
            expect(imageFields[0].variable).toBe('testVariable.identifier');
            expect(imageFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[0].codeTemplate).toBe('$!{dotContentMap.testVariable.identifier}');
        });

        it('should return the Image field', () => {
            expect(imageFields[1]).toBeTruthy();
            expect(imageFields[1].name).toBe('test: Image');
            expect(imageFields[1].variable).toBe('testVariable.image');
            expect(imageFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[1].codeTemplate).toBe(
                `#if ($!{${DOT_CONTENT_MAP}.${variable}.rawUri})\n    <img src="$!{${DOT_CONTENT_MAP}.${variable}.rawUri}" alt="$!{${DOT_CONTENT_MAP}.${variable}.title}" />\n#elseif($!{${DOT_CONTENT_MAP}.${variable}.identifier})\n    <img src="/dA/\${${DOT_CONTENT_MAP}.${variable}.identifier}" alt="$!{${DOT_CONTENT_MAP}.${variable}.title}"/>\n#end`
            );
        });

        it('should return the Image Title field', () => {
            expect(imageFields[2]).toBeTruthy();
            expect(imageFields[2].name).toBe('test: Title');
            expect(imageFields[2].variable).toBe('testVariable.title');
            expect(imageFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[2].codeTemplate).toBe('$!{dotContentMap.testVariable.title}');
        });

        it('should return the Image Extension field', () => {
            expect(imageFields[3]).toBeTruthy();
            expect(imageFields[3].name).toBe('test: Extension');
            expect(imageFields[3].variable).toBe('testVariable.extension');
            expect(imageFields[3].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[3].codeTemplate).toBe('$!{dotContentMap.testVariable.extension}');
        });

        it('should return the Image Width field', () => {
            expect(imageFields[4]).toBeTruthy();
            expect(imageFields[4].name).toBe('test: Width');
            expect(imageFields[4].variable).toBe('testVariable.width');
            expect(imageFields[4].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[4].codeTemplate).toBe('$!{dotContentMap.testVariable.width}');
        });

        it('should return the Image Height field', () => {
            expect(imageFields[5]).toBeTruthy();
            expect(imageFields[5].name).toBe('test: Height');
            expect(imageFields[5].variable).toBe('testVariable.height');
            expect(imageFields[5].fieldTypeLabel).toBe('Test Content Type');
            expect(imageFields[5].codeTemplate).toBe('$!{dotContentMap.testVariable.height}');
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
            expect(fileFields[0].variable).toBe('testVariable.file');
            expect(fileFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(fileFields[0].codeTemplate).toBe(
                `#if ($!{${DOT_CONTENT_MAP}.${variable}.rawUri})\n    <a href="$!{${DOT_CONTENT_MAP}.${variable}.rawUri}" target="_blank" >$!{${DOT_CONTENT_MAP}.${variable}.title}</a>\n#elseif($!{${DOT_CONTENT_MAP}.${variable}.identifier})\n    <a href="/dA/\${${DOT_CONTENT_MAP}.${variable}.identifier}" target="_blank" >$!{${DOT_CONTENT_MAP}.${variable}.title}</a>\n#end`
            );
        });

        it('should return the File Identifier field', () => {
            expect(fileFields[1]).toBeTruthy();
            expect(fileFields[1].name).toBe('test: Identifier');
            expect(fileFields[1].variable).toBe('testVariable.identifier');
            expect(fileFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(fileFields[1].codeTemplate).toBe('$!{dotContentMap.testVariable.identifier}');
        });

        it('should return the File Extension field', () => {
            expect(fileFields[2]).toBeTruthy();
            expect(fileFields[2].name).toBe('test: Extension');
            expect(fileFields[2].variable).toBe('testVariable.extension');
            expect(fileFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(fileFields[2].codeTemplate).toBe('$!{dotContentMap.testVariable.extension}');
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
            expect(binaryFields[0].variable).toBe('testVariable.file');
            expect(binaryFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(binaryFields[0].codeTemplate).toBe(
                `#if ($!{${DOT_CONTENT_MAP}.${variable}.rawUri})\n    <a href="$!{${DOT_CONTENT_MAP}.${variable}.rawUri}?force_download=1&filename=$!{${DOT_CONTENT_MAP}.${variable}.title}" target="_blank" >$!{${DOT_CONTENT_MAP}.${variable}.title}</a>\n#end`
            );
        });

        it('should return the Binary File Resized', () => {
            expect(binaryFields[1]).toBeTruthy();
            expect(binaryFields[1].name).toBe('test: Resized');
            expect(binaryFields[1].variable).toBe('testVariable.fileResized');
            expect(binaryFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(binaryFields[1].codeTemplate).toBe(
                `#if ($!{${DOT_CONTENT_MAP}.${variable}.rawUri})\n    <img src="/contentAsset/resize-image/\${ContentIdentifier}/${variable}?w=150&h=100&language_id=\${language}" />\n#end`
            );
        });

        it('should return the Binary File Thumbnail', () => {
            expect(binaryFields[2]).toBeTruthy();
            expect(binaryFields[2].name).toBe('test: Thumbnail');
            expect(binaryFields[2].variable).toBe('testVariable.fileThumbnail');
            expect(binaryFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(binaryFields[2].codeTemplate).toBe(
                `#if ($!{${DOT_CONTENT_MAP}.${variable}.rawUri})\n    <img src="/contentAsset/image-thumbnail/\${ContentIdentifier}/${variable}?w=150&h=150&language_id=\${language}" />\n#end`
            );
        });
    });

    describe('Select', () => {
        let selectFields: DotFieldContent[];

        beforeEach(() => {
            selectFields = service.fields[FieldTypes.SELECT](mockContentTypeField);
        });

        it('should return the Select fields', () => {
            expect(selectFields.length).toBe(3);
        });

        it('should return the Selected Value', () => {
            expect(selectFields[0]).toBeTruthy();
            expect(selectFields[0].name).toBe('test: Selected Value');
            expect(selectFields[0].variable).toBe('testVariable.selectValue');
            expect(selectFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(selectFields[0].codeTemplate).toBe('$!{dotContentMap.testVariable.selectValue}');
        });

        it('should return the options', () => {
            expect(selectFields[1]).toBeTruthy();
            expect(selectFields[1].name).toBe('test: Options');
            expect(selectFields[1].variable).toBe('testVariable.options');
            expect(selectFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(selectFields[1].codeTemplate).toBe('$!{dotContentMap.testVariable.options}');
        });

        it('should return the values', () => {
            expect(selectFields[2]).toBeTruthy();
            expect(selectFields[2].name).toBe('test: Values');
            expect(selectFields[2].variable).toBe('testVariable.values');
            expect(selectFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(selectFields[2].codeTemplate).toBe('$!{dotContentMap.testVariable.values}');
        });
    });

    describe('MultiSelect', () => {
        let multiSelectFields: DotFieldContent[];

        beforeEach(() => {
            multiSelectFields = service.fields[FieldTypes.MULTISELECT](mockContentTypeField);
        });

        it('should return the Select fields', () => {
            expect(multiSelectFields.length).toBe(3);
        });

        it('should return the Selected Values', () => {
            expect(multiSelectFields[0]).toBeTruthy();
            expect(multiSelectFields[0].name).toBe('test: Selected Values');
            expect(multiSelectFields[0].variable).toBe('testVariable.selectedValues');
            expect(multiSelectFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(multiSelectFields[0].codeTemplate).toBe(
                '$!{dotContentMap.testVariable.selectedValues}'
            );
        });

        it('should return the options', () => {
            expect(multiSelectFields[1]).toBeTruthy();
            expect(multiSelectFields[1].name).toBe('test: Options');
            expect(multiSelectFields[1].variable).toBe('testVariable.options');
            expect(multiSelectFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(multiSelectFields[1].codeTemplate).toBe(
                '$!{dotContentMap.testVariable.options}'
            );
        });

        it('should return the values', () => {
            expect(multiSelectFields[2]).toBeTruthy();
            expect(multiSelectFields[2].name).toBe('test: Values');
            expect(multiSelectFields[2].variable).toBe('testVariable.values');
            expect(multiSelectFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(multiSelectFields[2].codeTemplate).toBe('$!{dotContentMap.testVariable.values}');
        });
    });

    describe('Radio', () => {
        let radioFields: DotFieldContent[];

        beforeEach(() => {
            radioFields = service.fields[FieldTypes.RADIO](mockContentTypeField);
        });

        it('should return the Select fields', () => {
            expect(radioFields.length).toBe(3);
        });

        it('should return the Selected Value', () => {
            expect(radioFields[0]).toBeTruthy();
            expect(radioFields[0].name).toBe('test: Selected Value');
            expect(radioFields[0].variable).toBe('testVariable.selectValue');
            expect(radioFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(radioFields[0].codeTemplate).toBe('$!{dotContentMap.testVariable.selectValue}');
        });

        it('should return the options', () => {
            expect(radioFields[1]).toBeTruthy();
            expect(radioFields[1].name).toBe('test: Options');
            expect(radioFields[1].variable).toBe('testVariable.options');
            expect(radioFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(radioFields[1].codeTemplate).toBe('$!{dotContentMap.testVariable.options}');
        });

        it('should return the values', () => {
            expect(radioFields[2]).toBeTruthy();
            expect(radioFields[2].name).toBe('test: Values');
            expect(radioFields[2].variable).toBe('testVariable.values');
            expect(radioFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(radioFields[2].codeTemplate).toBe('$!{dotContentMap.testVariable.values}');
        });
    });

    describe('Checkbox', () => {
        let checkboxFields: DotFieldContent[];

        beforeEach(() => {
            checkboxFields = service.fields[FieldTypes.CHECKBOX](mockContentTypeField);
        });

        it('should return the Select fields', () => {
            expect(checkboxFields.length).toBe(3);
        });

        it('should return the Selected Values', () => {
            expect(checkboxFields[0]).toBeTruthy();
            expect(checkboxFields[0].name).toBe('test: Selected Values');
            expect(checkboxFields[0].variable).toBe('testVariable.selectedValues');
            expect(checkboxFields[0].fieldTypeLabel).toBe('Test Content Type');
            expect(checkboxFields[0].codeTemplate).toBe(
                '$!{dotContentMap.testVariable.selectedValues}'
            );
        });

        it('should return the options', () => {
            expect(checkboxFields[1]).toBeTruthy();
            expect(checkboxFields[1].name).toBe('test: Options');
            expect(checkboxFields[1].variable).toBe('testVariable.options');
            expect(checkboxFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(checkboxFields[1].codeTemplate).toBe('$!{dotContentMap.testVariable.options}');
        });

        it('should return the values', () => {
            expect(checkboxFields[2]).toBeTruthy();
            expect(checkboxFields[2].name).toBe('test: Values');
            expect(checkboxFields[2].variable).toBe('testVariable.values');
            expect(checkboxFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(checkboxFields[2].codeTemplate).toBe('$!{dotContentMap.testVariable.values}');
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
            expect(dateFields[0].codeTemplate).toBe(
                `$date.format("M-dd-yyyy", $${DOT_CONTENT_MAP}.${variable})`
            );
        });

        it('should return the Date Database Format field', () => {
            expect(dateFields[1]).toBeTruthy();
            expect(dateFields[1].name).toBe('test: Date Database Format (yyyy-mm-dd)');
            expect(dateFields[1].variable).toBe('testVariable.DBFormat');
            expect(dateFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(dateFields[1].codeTemplate).toBe(
                `$date.format("yyyy-M-dd", $${DOT_CONTENT_MAP}.${variable})`
            );
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
            expect(timeFields[0].codeTemplate).toBe(
                `$date.format("H:m:s", $${DOT_CONTENT_MAP}.${variable})`
            );
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
            expect(dateTimeFields[1].variable).toBe('testVariable.shortFormat');
            expect(dateTimeFields[1].fieldTypeLabel).toBe('Test Content Type');
            expect(dateTimeFields[1].codeTemplate).toBe(
                `$date.format("M-dd-yyyy", $${DOT_CONTENT_MAP}.${variable})`
            );
        });

        it('should return the Date Long String Format', () => {
            expect(dateTimeFields[2]).toBeTruthy();
            expect(dateTimeFields[2].name).toBe('test: Date Long String (mm/dd/yyyy hh:mm:ss)');
            expect(dateTimeFields[2].variable).toBe('testVariable.longFormat');
            expect(dateTimeFields[2].fieldTypeLabel).toBe('Test Content Type');
            expect(dateTimeFields[2].codeTemplate).toBe(
                `$date.format("M-dd-yyyy H:m:s", $${DOT_CONTENT_MAP}.${variable})`
            );
        });

        it('should return the Date Database Format field', () => {
            expect(dateTimeFields[3]).toBeTruthy();
            expect(dateTimeFields[3].name).toBe('test: Date Database Format (yyyy-mm-dd)');
            expect(dateTimeFields[3].variable).toBe('testVariable.DBFormat');
            expect(dateTimeFields[3].fieldTypeLabel).toBe('Test Content Type');
            expect(dateTimeFields[3].codeTemplate).toBe(
                `$date.format("yyyy-M-dd", $${DOT_CONTENT_MAP}.${variable})`
            );
        });
    });
});
