import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component, Input, SimpleChange } from '@angular/core';
import { ContentTypeFieldsDropZoneComponent } from './';
import { By } from '@angular/platform-browser';
import { Field, FieldRow } from '../';
import { DragulaModule } from 'ng2-dragula';
import { FieldDragDropService } from '../service';

@Component({
    selector: 'content-type-fields-row',
    template: ''
})
class TestContentTypeFieldsRow {
    @Input() fieldRow: FieldRow;
}

describe('ContentTypeFieldsDropZoneComponent', () => {
    let comp: ContentTypeFieldsDropZoneComponent;
    let fixture: ComponentFixture<ContentTypeFieldsDropZoneComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(async(() => {

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsDropZoneComponent,
                TestContentTypeFieldsRow
            ],
            imports: [
                DragulaModule
            ],
            providers: [
                FieldDragDropService
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsDropZoneComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should has a fields container', () => {

        fixture.detectChanges();

        let fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));

        expect(fieldsContainer).not.toBeNull();

        expect('target').toEqual(fieldsContainer.attributes['data-drag-type']);
        expect('fields-row-bag').toEqual(fieldsContainer.attributes['dragula']);
    });

    it('should has a fields container', () => {

        let fields: Field[] = [
            {
                clazz: 'text',
                name: 'field 1'
            },
            {
                clazz: 'text',
                name: 'field 2'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                name: 'field 3'
            },
            {
                clazz: 'text',
                name: 'field 4'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
                name: 'field 5'
            },
            {
                clazz: 'text',
                name: 'field 6'
            },
        ];

        comp.ngOnChanges({
            fields: new SimpleChange(null, fields, true),
        });

        fixture.detectChanges();

        let fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));

        expect(fieldsContainer).not.toBeNull();

        expect('target').toEqual(fieldsContainer.attributes['data-drag-type']);
        expect('fields-row-bag').toEqual(fieldsContainer.attributes['dragula']);

        let fieldRows = fieldsContainer.queryAll(By.css('content-type-fields-row'));
        expect(2).toEqual(fieldRows.length);

        expect(2).toEqual(fieldRows[0].componentInstance.fieldRow.columns.length);
        expect(2).toEqual(fieldRows[0].componentInstance.fieldRow.columns[0].fields.length);
        expect(1).toEqual(fieldRows[0].componentInstance.fieldRow.columns[1].fields.length);

        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns.length);
        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns[0].fields.length);
    });
});