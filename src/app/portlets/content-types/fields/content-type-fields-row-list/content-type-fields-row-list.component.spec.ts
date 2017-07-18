import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { ContentTypeFieldsRowListComponent } from './';
import { By } from '@angular/platform-browser';
import { FieldDragDropService } from '../service';
import { Field } from '../';
import { DragulaModule } from 'ng2-dragula';
import { Observable } from 'rxjs/Observable';

describe('ContentTypesFieldDragabbleItemComponent', () => {
    let comp: ContentTypeFieldsRowListComponent;
    let fixture: ComponentFixture<ContentTypeFieldsRowListComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(async(() => {

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsRowListComponent
            ],
            imports: [
                DragulaModule
            ],
            providers: [
                FieldDragDropService
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsRowListComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should has a ul', () => {
        let ul = de.query(By.css('ul'));

        expect('fields-row-bag').toEqual(ul.attributes['dragula']);
        expect('source').toEqual(ul.attributes['data-drag-type']);
    });

    it('should set drag and drop options', () => {
        let fieldDragDropService = fixture.debugElement.injector.get(FieldDragDropService);
        spyOn(fieldDragDropService, 'setFieldRowBagOptions');

        comp.ngOnInit();

        expect(fieldDragDropService.setFieldRowBagOptions).toHaveBeenCalled();
    });

    it('should has a list of items', () => {
        let fieldDragDropService = fixture.debugElement.injector.get(FieldDragDropService);
        spyOn(fieldDragDropService, 'setFieldRowBagOptions');

        comp.ngOnInit();
        fixture.detectChanges();

        let lis = de.queryAll(By.css('li'));
        expect(4).toEqual(lis.length);
    });
});