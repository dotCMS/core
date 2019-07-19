import { of } from 'rxjs';
import { async, ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { ContentTypesFieldsListComponent } from './content-types-fields-list.component';
import { By } from '@angular/platform-browser';
import { FieldService } from '../service';

import { DragulaModule } from 'ng2-dragula';
import { DragulaService } from 'ng2-dragula';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';

const itemsData = [
    {
        label: 'Text',
        clazz: 'text'
    },
    {
        label: 'Date',
        clazz: 'date'
    },
    {
        label: 'Checkbox',
        clazz: 'checkbox'
    },
    {
        label: 'Image',
        clazz: 'image'
    },
    {
        label: 'Tab Divider',
        id: 'tab_divider',
        clazz: 'tab'
    },
    {
        label: 'Line Divider',
        clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField'
    }
];

describe('ContentTypesFieldsListComponent', () => {
    let fixture: ComponentFixture<ContentTypesFieldsListComponent>;
    let de: DebugElement;
    let items: DebugElement[];

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [ContentTypesFieldsListComponent],
            imports: [DragulaModule, DotIconModule],
            providers: [
                DragulaService,
                {
                    provide: FieldService,
                    useValue: {
                        loadFieldTypes() {
                            return of(itemsData);
                        },
                        getIcon() {}
                    }
                }
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesFieldsListComponent);
        de = fixture.debugElement;
        fixture.detectChanges();
        items = de.queryAll(By.css('li span'));
    }));

    it('should filter our tab field', () => {
        items.forEach((el: DebugElement) => {
            expect(el.nativeElement.textContent).not.toBe('Tab Divider');
        });
    });

    it('should add column field at first position', () => {
        expect(items.length).toEqual(6);
        expect(items[0].nativeElement.textContent).toContain('Column');
    });

    it('should move line divider field to second position', () => {
        expect(items[1].nativeElement.textContent).toContain('Line Divider');
    });

    it('should render each field', () => {
        items.forEach((el: DebugElement, index) => {
            if (index > 1) {
                // skipping first 2
                expect(el.nativeElement.textContent).toContain(itemsData[index - 2].label);
            }
        });
    });

    it('should add dragula attr', () => {
        const ulElement = de.query(By.css('ul'));
        expect('fields-bag').toEqual(ulElement.attributes['ng-reflect-dragula']);
        expect('source').toEqual(ulElement.attributes['data-drag-type']);
    });
});
