import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { ContentTypesFieldsListComponent } from './content-types-fields-list.component';
import { By } from '@angular/platform-browser';
import { Field, FieldTypesService } from '../service';

import { DragulaModule } from 'ng2-dragula';
import { DragulaService } from 'ng2-dragula';
import { Observable } from 'rxjs/Observable';

describe('ContentTypesFieldsListComponent', () => {
    let comp: ContentTypesFieldsListComponent;
    let fixture: ComponentFixture<ContentTypesFieldsListComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(async(() => {

        let fieldTypesService = {};

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypesFieldsListComponent
            ],
            imports: [
                DragulaModule
            ],
            providers: [
                { provide: FieldTypesService, useValue:  fieldTypesService },
                DragulaService
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesFieldsListComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should renderer each items', () => {
        let fieldTypesService = fixture.debugElement.injector.get(FieldTypesService);
        let itemsData = [
            {
                name: 'Text'
            },
            {
                name: 'Date'
            },
            {
                name: 'Checkbox'
            },
            {
                name: 'Image'
            }
        ];

        spyOn(fieldTypesService, 'loadFieldTypes').and.returnValue(Observable.of(itemsData));

        let dragulaService = fixture.debugElement.injector.get(DragulaService);
        spyOn(dragulaService, 'setOptions');

        comp.ngOnInit();

        fixture.detectChanges();

        let itemsElements = de.queryAll(By.css('li'));

        expect(itemsData.length).toEqual(itemsElements.length);
        itemsData.forEach((fieldType, index) => expect(fieldType.name).toEqual(itemsElements[index].nativeElement.textContent));

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

        let fieldTypesService = fixture.debugElement.injector.get(FieldTypesService);
        spyOn(fieldTypesService, 'loadFieldTypes').and.returnValue(Observable.of([]));

        let dragulaService = fixture.debugElement.injector.get(DragulaService);
        spyOn(dragulaService, 'setOptions').and.callFake((name, opts) => {
            this.opts = opts;
            this.name = name;
        });

        comp.ngOnInit();

        fixture.detectChanges();
        expect(dragulaOptions.copy).toEqual(this.opts.copy);
        expect(dragulaName).toEqual(this.name);
    });
});