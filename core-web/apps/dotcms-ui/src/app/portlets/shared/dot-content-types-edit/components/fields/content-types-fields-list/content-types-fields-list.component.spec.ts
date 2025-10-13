/* eslint-disable @typescript-eslint/no-empty-function */

import { DragulaModule, DragulaService } from 'ng2-dragula';
import { of } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotIconModule } from '@dotcms/ui';

import { ContentTypesFieldsListComponent } from './content-types-fields-list.component';

import { FieldService } from '../service';

const itemsData = [
    {
        label: 'Text',
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField'
    },
    {
        label: 'Date',
        clazz: 'com.dotcms.contenttype.model.field.ImmutableDateField'
    },
    {
        label: 'Checkbox',
        clazz: 'com.dotcms.contenttype.model.field.ImmutableCheckboxField'
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
        label: 'Relationships Legacy',
        id: 'relationships_tab',
        clazz: 'relationships_tab'
    },
    {
        label: 'Permission',
        id: 'permissions_tab',
        clazz: 'permissions_tab'
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

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [ContentTypesFieldsListComponent, DragulaModule, DotIconModule],
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
        }).compileComponents();

        fixture = TestBed.createComponent(ContentTypesFieldsListComponent);
        de = fixture.debugElement;
    }));

    describe('with empty Content baseType ', () => {
        beforeEach(() => {
            fixture.detectChanges();
            items = de.queryAll(By.css('li span'));
        });

        it('should filter black listed fields', () => {
            const backListFields = ['relationships_tab', 'permissions_tab', 'tab_divider'];

            items.forEach((el: DebugElement) => {
                expect(backListFields).not.toContain(el.nativeElement.textContent);
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

    describe('with FORM Content baseType ', () => {
        let component: ContentTypesFieldsListComponent;

        beforeEach(() => {
            component = fixture.componentInstance;
            component.baseType = 'FORM';
            fixture.detectChanges();
            items = de.queryAll(By.css('li span'));
        });

        it('should filter fields that are only allowed by FORM', () => {
            expect(items.length).toEqual(5);
        });
    });
});
