import { async, ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { ContentTypeFieldsRowListComponent } from './';
import { By } from '@angular/platform-browser';
import { FieldDragDropService } from '../service';
import { DragulaModule, DragulaService } from 'ng2-dragula';

describe('ContentTypeFieldsRowListComponent', () => {
    let comp: ContentTypeFieldsRowListComponent;
    let fixture: ComponentFixture<ContentTypeFieldsRowListComponent>;
    let de: DebugElement;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [ContentTypeFieldsRowListComponent],
            imports: [DragulaModule],
            providers: [FieldDragDropService, DragulaService]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsRowListComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
    }));

    it('should has a ul', () => {
        const ul = de.query(By.css('ul'));

        expect('fields-row-bag').toEqual(ul.attributes['dragula']);
        expect('source').toEqual(ul.attributes['data-drag-type']);
    });

    it('should has a list of items', () => {
        const fieldDragDropService = fixture.debugElement.injector.get(FieldDragDropService);
        spyOn(fieldDragDropService, 'setFieldRowBagOptions');

        comp.ngOnInit();
        fixture.detectChanges();

        const lis = de.queryAll(By.css('li'));
        expect(4).toEqual(lis.length);
    });
});
