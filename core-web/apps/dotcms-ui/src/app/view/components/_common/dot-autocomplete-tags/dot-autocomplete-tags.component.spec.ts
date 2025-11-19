/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AutoComplete } from 'primeng/autocomplete';

import { DotMessageService, DotTagsService } from '@dotcms/data-access';
import { DotTag } from '@dotcms/dotcms-models';
import { createFakeEvent, MockDotMessageService } from '@dotcms/utils-testing';

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
            imports: [DotAutocompleteTagsComponent, BrowserAnimationsModule],
            providers: [
                { provide: DotTagsService, useClass: DotTagsServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(DotAutocompleteTagsComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        fixture.detectChanges();
        autoComplete = de.query(By.directive(AutoComplete)).componentInstance as AutoComplete;
    });

    it('should set options when load', () => {
        expect(component.filteredOptions).toEqual(mockResponse);
    });

    describe('autoComplete', () => {
        beforeEach(() => {
            component.placeholder = 'Custom Placeholder';
            fixture.detectChanges();
        });

        it('should set all properties correctly', () => {
            expect(autoComplete.optionLabel).toEqual('label');
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
                jest.spyOn(component, 'propagateChange');
                component.value = [...preLoadedTags];
            });

            describe('onKeyUp', () => {
                const enterEvent = {
                    key: 'Enter',
                    currentTarget: { value: 'enterEvent' }
                } as unknown as KeyboardEvent;
                const newEnterEvent = {
                    key: 'Enter',
                    currentTarget: { value: 'newTag' }
                } as unknown as KeyboardEvent;
                const backspaceEvent = { key: 'Backspace' } as unknown as KeyboardEvent;
                const qEvent = {
                    key: 'q',
                    currentTarget: { value: 'qEvent' }
                } as unknown as KeyboardEvent;

                beforeEach(() => {
                    //
                });

                it('should NOT add the tag because user dint hit enter', () => {
                    autoComplete.onKeyUp.emit(qEvent);
                    expect(component.value.length).toEqual(2);
                });

                it('should NOT add the tag because label is just white spaces', () => {
                    const mockEvent = {
                        key: 'Enter',
                        currentTarget: { value: '        ' }
                    } as unknown as KeyboardEvent;
                    autoComplete.onKeyUp.emit(mockEvent);
                    expect(component.value.length).toEqual(2);
                });

                it('should NOT add the tag because is duplicate if the user hit enter', () => {
                    autoComplete.onKeyUp.emit({ ...enterEvent });
                    expect(component.value[1].label).toEqual(preLoadedTags[1].label);
                    expect(component.value.length).toEqual(2);
                });

                it('should call checkForTag if user hit enter should add the tag and clear input value', () => {
                    jest.spyOn(component, 'checkForTag');
                    jest.spyOn(autoComplete, 'hide');
                    autoComplete.onKeyUp.emit(newEnterEvent);

                    expect(component.checkForTag).toHaveBeenCalledWith(newEnterEvent);
                    expect(component.checkForTag).toHaveBeenCalledTimes(1);
                    expect(component.value[0].label).toEqual('newTag');
                    // expect(newEnterEvent.currentTarget.value).toBeNull();
                    expect(component.propagateChange).toHaveBeenCalledWith(
                        'newTag,enterEvent,Dotcms'
                    );
                    expect(autoComplete.hide).toHaveBeenCalled();
                });

                it('should put back last deleted item by the p-autoComplete', () => {
                    autoComplete.onUnselect.emit({
                        originalEvent: createFakeEvent('click'),
                        value: { label: 'qEvent' }
                    });
                    autoComplete.onKeyUp.emit({ ...backspaceEvent });
                    expect(component.value.length).toEqual(3);
                    expect(component.value[2].label).toEqual('qEvent');
                });

                it('should not do nothing on backspace if there is not a previous deleted element', () => {
                    component.value = [];
                    autoComplete.onKeyUp.emit({ ...backspaceEvent });
                    expect(component.value.length).toEqual(0);
                });
            });

            it('should call filterTags on completeMethod and remove already selected', () => {
                jest.spyOn(component, 'filterTags');
                component.value.push({
                    label: 'test',
                    siteId: '',
                    siteName: '',
                    persona: null
                });
                const fakeEvent = createFakeEvent('click');
                autoComplete.completeMethod.emit({
                    originalEvent: fakeEvent,
                    query: 'test'
                });

                expect(component.filterTags).toHaveBeenCalledWith({
                    query: 'test',
                    originalEvent: fakeEvent
                });
                expect(component.filteredOptions.length).toBe(1);
            });

            it('should call addItem on onSelect event and place last element as first', () => {
                jest.spyOn(component, 'addItem');
                autoComplete.onSelect.emit();

                expect(component.addItem).toHaveBeenCalledTimes(1);
                expect(component.propagateChange).toHaveBeenCalledWith('Dotcms,enterEvent');
                expect(component.propagateChange).toHaveBeenCalledTimes(1);
            });

            it('should call removeItem on onUnselect event and ', () => {
                jest.spyOn(component, 'removeItem');
                autoComplete.onUnselect.emit();

                expect(component.removeItem).toHaveBeenCalledTimes(1);
                expect(component.propagateChange).toHaveBeenCalledWith('enterEvent,Dotcms');
                expect(component.propagateChange).toHaveBeenCalledTimes(1);
            });
        });
    });
});
