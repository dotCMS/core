import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { CheckboxModule } from 'primeng/checkbox';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { CheckboxPropertyComponent } from './checkbox-property.component';

import { FieldProperty } from '../field-properties.model';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.field.properties.required.label': 'required',
    'contenttypes.field.properties.user_searchable.label': 'user searchable.',
    'contenttypes.field.properties.system_indexed.label': 'system indexed',
    'contenttypes.field.properties.listed.label': 'listed',
    'contenttypes.field.properties.unique.label': 'unique'
});

describe('CheckboxPropertyComponent', () => {
    let spectator: Spectator<CheckboxPropertyComponent>;

    const createComponent = createComponentFactory({
        component: CheckboxPropertyComponent,
        imports: [ReactiveFormsModule, CheckboxModule, DotMessagePipe],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
    });

    it('should have a form', () => {
        const group = new UntypedFormGroup({ indexed: new UntypedFormControl(null) });
        const property: FieldProperty = {
            name: 'indexed',
            value: null,
            field: { ...dotcmsContentTypeFieldBasicMock }
        };
        spectator.component.group = group;
        spectator.component.property = property;
        spectator.detectChanges();

        const divForm = spectator.query('div');
        expect(divForm).toBeTruthy();
        expect(spectator.component.group).toEqual(group);
    });

    it('should have a p-checkbox', () => {
        const group = new UntypedFormGroup({
            indexed: new UntypedFormControl('value')
        });
        const property: FieldProperty = {
            name: 'indexed',
            value: 'value',
            field: { ...dotcmsContentTypeFieldBasicMock }
        };

        spectator.component.group = group;
        spectator.component.property = property;
        spectator.detectChanges();

        const pCheckboxDe = spectator.debugElement.query(By.css('p-checkbox'));
        expect(pCheckboxDe).toBeTruthy();
        expect(spectator.query('label')?.textContent?.trim()).toBe('system indexed');
        expect(spectator.component.group.get('indexed')?.value).toBe('value');
    });
});
