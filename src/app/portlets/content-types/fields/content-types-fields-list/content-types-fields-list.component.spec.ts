import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { ContentTypesFieldsListComponent } from './content-types-fields-list.component';
import { By } from '@angular/platform-browser';
import { FieldService, FieldDragDropService } from '../service';
import { Field } from '../';

import { DragulaModule } from 'ng2-dragula';
import { DragulaService } from 'ng2-dragula';
import { Observable } from 'rxjs/Observable';

describe('ContentTypesFieldsListComponent', () => {
    let comp: ContentTypesFieldsListComponent;
    let fixture: ComponentFixture<ContentTypesFieldsListComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(async(() => {

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypesFieldsListComponent
            ],
            imports: [
                DragulaModule
            ],
            providers: [
                DragulaService,
                FieldDragDropService,
                FieldService
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesFieldsListComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should renderer each items', () => {
        let fieldService = fixture.debugElement.injector.get(FieldService);
        let itemsData = [
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
            }
        ];

        spyOn(fieldService, 'loadFieldTypes').and.returnValue(Observable.of(itemsData));

        let fieldDragDropService = fixture.debugElement.injector.get(FieldDragDropService);
        spyOn(fieldDragDropService, 'setFieldBagOptions');

        comp.ngOnInit();

        fixture.detectChanges();

        let itemsElements = de.queryAll(By.css('li'));

        expect(itemsData.length).toEqual(itemsElements.length);
        itemsData.forEach((fieldType, index) => expect(fieldType.label).toEqual(itemsElements[index].nativeElement.textContent));

        let ulElement = de.query(By.css('ul'));

        expect('fields-bag').toEqual(ulElement.attributes['ng-reflect-dragula']);
        expect('source').toEqual(ulElement.attributes['data-drag-type']);
    });

    it('should set the Dragula options', () => {
        let dragulaName = 'fields-bag';
        let dragulaOptions = {
                copy: true,
                moves: (el, target, source, sibling) => {
                    return target.dataset.dragType === 'source';
                },
            };

        let fieldTypesService = fixture.debugElement.injector.get(FieldService);
        spyOn(fieldTypesService, 'loadFieldTypes').and.returnValue(Observable.of([]));

        let fieldDragDropService = fixture.debugElement.injector.get(FieldDragDropService);
        spyOn(fieldDragDropService, 'setFieldBagOptions');

        comp.ngOnInit();

        fixture.detectChanges();

        expect(fieldDragDropService.setFieldBagOptions).toHaveBeenCalled();

    });
});