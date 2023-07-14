/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';
import { ChipsModule } from 'primeng/chips';

import { DotMessageService, DotTagsService } from '@dotcms/data-access';
import { DotTag } from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotAutocompleteTagsComponent } from './dot-autocomplete-tags.component';

const mockResponse = [
    { label: 'test', siteId: '1', siteName: 'Site', persona: false },
    { label: 'united', siteId: '1', siteName: 'Site', persona: false }
];

class DotTagsServiceMock {
    getSuggestions(_name?: string): Observable<DotTag[]> {
        return of(mockResponse);
    }
}

const messageServiceMock = new MockDotMessageService({
    'dot.common.press': 'Press'
});

describe('DotAutocompleteTagsComponent', () => {
    let component: DotAutocompleteTagsComponent;
    let fixture: ComponentFixture<DotAutocompleteTagsComponent>;
    let de: DebugElement;
    let autoComplete: AutoComplete;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotAutocompleteTagsComponent],
            imports: [
                BrowserAnimationsModule,
                ChipsModule,
                AutoCompleteModule,
                FormsModule,
                DotIconModule,
                DotPipesModule,
                DotMessagePipe
            ],
            providers: [
                { provide: DotTagsService, useClass: DotTagsServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(DotAutocompleteTagsComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        fixture.detectChanges();
        autoComplete = de.query(By.css('p-autoComplete')).componentInstance as AutoComplete;
    });

    it('should set options when load', () => {
        expect(component.filteredOptions).toEqual(mockResponse);
    });

    it('should hide add helper if value is null', () => {
        const helper = de.query(By.css('.autocomplete-helper'));
        expect(helper).toBeNull();
    });

    describe('autoComplete', () => {
        beforeEach(() => {
            component.placeholder = 'Custom Placeholder';
            fixture.detectChanges();
        });

        it('should set all properties correctly', () => {
            expect(autoComplete.field).toEqual('label');
            expect(autoComplete.dataKey).toEqual('label');
            expect(autoComplete.multiple).toBe(true);
            expect(autoComplete.placeholder).toEqual('Custom Placeholder');
        });

        describe('events', () => {
            const preLoadedTags = [
                {
                    label: 'enterEvent',
                    siteId: '',
                    siteName: '',
                    persona: null
                },
                {
                    label: 'Dotcms',
                    siteId: '',
                    siteName: '',
                    persona: null
                }
            ];

            beforeEach(() => {
                spyOn(component, 'propagateChange').and.callThrough();
                component.value = [...preLoadedTags];
            });

            describe('onKeyUp', () => {
                const enterEvent = { key: 'Enter', currentTarget: { value: 'enterEvent' } };
                const newEnterEvent = { key: 'Enter', currentTarget: { value: 'newTag' } };
                const backspaceEvent = { key: 'Backspace' };
                const qEvent = { key: 'q', currentTarget: { value: 'qEvent' } };

                beforeEach(() => {
                    //
                });

                it('should show the helper when input has value', () => {
                    autoComplete.onKeyup({ ...qEvent });
                    fixture.detectChanges();
                    const helper = de.query(By.css('.autocomplete-helper'));
                    expect(helper).not.toBeNull();
                });

                it('should NOT add the tag because user dint hit enter', () => {
                    autoComplete.onKeyup({ ...qEvent });
                    expect(component.value.length).toEqual(2);
                });

                it('should NOT add the tag because label is just white spaces', () => {
                    autoComplete.onKeyup({ key: 'Enter', currentTarget: { value: '        ' } });
                    expect(component.value.length).toEqual(2);
                });

                it('should NOT add the tag because is duplicate if the user hit enter', () => {
                    autoComplete.onKeyup({ ...enterEvent });
                    expect(component.value[1].label).toEqual(preLoadedTags[1].label);
                    expect(component.value.length).toEqual(2);
                });

                it('should call checkForTag if user hit enter should add the tag and clear input value', () => {
                    spyOn(component, 'checkForTag').and.callThrough();
                    spyOn(autoComplete, 'hide').and.callThrough();
                    autoComplete.onKeyup(newEnterEvent);

                    expect<any>(component.checkForTag).toHaveBeenCalledWith(newEnterEvent);
                    expect(component.value[0].label).toEqual('newTag');
                    expect(newEnterEvent.currentTarget.value).toBeNull();
                    expect(component.propagateChange).toHaveBeenCalledWith(
                        'newTag,enterEvent,Dotcms'
                    );
                    expect(autoComplete.hide).toHaveBeenCalled();
                });

                it('should put back last deleted item by the p-autoComplete', () => {
                    autoComplete.onUnselect.emit({ label: qEvent.currentTarget.value });
                    autoComplete.onKeyup({ ...backspaceEvent });
                    expect(component.value.length).toEqual(3);
                    expect(component.value[2].label).toEqual(qEvent.currentTarget.value);
                });

                it('should not do nothing on backspace if there is not a previous deleted element', () => {
                    component.value = [];
                    autoComplete.onKeyup({ ...backspaceEvent });
                    expect(component.value.length).toEqual(0);
                });
            });

            it('should call filterTags on completeMethod and remove already selected', () => {
                spyOn(component, 'filterTags').and.callThrough();
                component.value.push({
                    label: 'test',
                    siteId: '',
                    siteName: '',
                    persona: null
                });
                autoComplete.completeMethod.emit({ query: 'test' });

                expect(component.filterTags).toHaveBeenCalledWith({ query: 'test' });
                expect(component.filteredOptions.length).toBe(1);
            });

            it('should call addItem on onSelect event and place last element as first', () => {
                spyOn(component, 'addItem').and.callThrough();
                autoComplete.onSelect.emit();

                expect(component.addItem).toHaveBeenCalledTimes(1);
                expect(component.propagateChange).toHaveBeenCalledWith('Dotcms,enterEvent');
            });

            it('should call removeItem on onUnselect event and ', () => {
                spyOn(component, 'removeItem').and.callThrough();
                autoComplete.onUnselect.emit();

                expect(component.removeItem).toHaveBeenCalledTimes(1);
                expect(component.propagateChange).toHaveBeenCalledWith('enterEvent,Dotcms');
            });
        });
    });
});
