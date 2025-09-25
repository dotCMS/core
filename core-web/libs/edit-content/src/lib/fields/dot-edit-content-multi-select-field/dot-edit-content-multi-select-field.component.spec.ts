import { SpectatorHost, createHostFactory } from '@ngneat/spectator/jest';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { MultiSelect, MultiSelectItem } from 'primeng/multiselect';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentMultiSelectFieldComponent } from './dot-edit-content-multi-select-field.component';

import { MULTI_SELECT_FIELD_MOCK } from '../../utils/mocks';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
}

// https://jestjs.io/docs/manual-mocks#mocking-methods-which-are-not-implemented-in-jsdom
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(), // deprecated
        removeListener: jest.fn(), // deprecated
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn()
    }))
});

describe('DotEditContentMultiselectFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentMultiSelectFieldComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentMultiSelectFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false
    });

    describe('test with value', () => {
        it('should render the options selected if the form have value', () => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-multi-select-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [MULTI_SELECT_FIELD_MOCK.variable]: new FormControl(['one', 'two'])
                        }),
                        field: MULTI_SELECT_FIELD_MOCK
                    }
                }
            );
            spectator.detectChanges();

            spectator.query(MultiSelect).show();
            spectator.detectChanges();

            const options = spectator.component.$options();

            spectator.queryAll('.p-multiselect-item').forEach((item, index) => {
                expect(item.textContent).toBe(options[index].label);
            });
        });
    });

    describe('test without value', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-multi-select-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [MULTI_SELECT_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: MULTI_SELECT_FIELD_MOCK
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should render no options selected', () => {
            expect(spectator.query(MultiSelect).valuesAsString).toEqual(undefined);
        });

        it('should render options', () => {
            spectator.query(MultiSelect).show();
            spectator.detectChanges();

            const multiSelectItems = spectator.queryAll(MultiSelectItem);
            expect(multiSelectItems.length).toBe(2);
            expect(multiSelectItems[0].label).toBe('one');
        });
    });

    it('should set the key/value the same when bad formatting options passed', () => {
        const MULTI_SELECT_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL = {
            ...MULTI_SELECT_FIELD_MOCK,
            values: 'one'
        };
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-multi-select-field [field]="field" [formControlName]="field.variable" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [MULTI_SELECT_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL.variable]:
                            new FormControl()
                    }),
                    field: MULTI_SELECT_FIELD_MOCK_WITHOUT_VALUE_AND_LABEL
                }
            }
        );
        spectator.detectChanges();

        const expectedList = [
            {
                label: 'one',
                value: 'one'
            }
        ];
        expect(spectator.query(MultiSelect).options).toEqual(expectedList);
    });
});
