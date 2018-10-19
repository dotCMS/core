import { of as observableOf } from 'rxjs';
import { async, ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { ContentTypesFieldsListComponent } from './content-types-fields-list.component';
import { By } from '@angular/platform-browser';
import { FieldService, FieldDragDropService } from '../service';

import { DragulaModule } from 'ng2-dragula';
import { DragulaService } from 'ng2-dragula';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';

describe('ContentTypesFieldsListComponent', () => {
    let fixture: ComponentFixture<ContentTypesFieldsListComponent>;
    let de: DebugElement;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [ContentTypesFieldsListComponent],
            imports: [DragulaModule, DotIconModule],
            providers: [DragulaService, FieldDragDropService, FieldService]
        });

        fixture = DOTTestBed.createComponent(ContentTypesFieldsListComponent);
        de = fixture.debugElement;
    }));

    it('should renderer each items', () => {
        const fieldService = de.injector.get(FieldService);
        const itemsData = [
            {
                label: 'Text'
            },
            {
                label: 'Date'
            },
            {
                label: 'Checkbox'
            },
            {
                label: 'Image'
            },
            {
                label: 'Tab Divider',
                id: 'tab_divider'
            }
        ];

        spyOn(fieldService, 'loadFieldTypes').and.returnValue(observableOf(itemsData));

        const fieldDragDropService = de.injector.get(FieldDragDropService);
        spyOn(fieldDragDropService, 'setFieldBagOptions');

        fixture.detectChanges();

        const itemsElements = de.queryAll(By.css('li'));

        expect(itemsElements.length).toEqual((itemsData.length - 1));
        itemsData.filter(item => item.id !== 'tab_divider').forEach((fieldType, index) =>
            expect(itemsElements[index].nativeElement.textContent).toContain(fieldType.label)
        );

        const ulElement = de.query(By.css('ul'));

        expect('fields-bag').toEqual(ulElement.attributes['ng-reflect-dragula']);
        expect('source').toEqual(ulElement.attributes['data-drag-type']);
    });
});
