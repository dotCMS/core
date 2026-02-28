import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { HintPropertyComponent } from './index';

import { FieldProperty } from '../field-properties.model';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.field.properties.hint.label': 'Hint'
});

describe('HintPropertyComponent', () => {
    let spectator: Spectator<HintPropertyComponent>;

    const createComponent = createComponentFactory({
        component: HintPropertyComponent,
        imports: [ReactiveFormsModule, InputTextModule, DotSafeHtmlPipe, DotMessagePipe],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
    });

    it('should have a form', () => {
        const group = new UntypedFormGroup({ name: new UntypedFormControl(null) });
        const property: FieldProperty = {
            name: 'name',
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

    it('should have a input', () => {
        const group = new UntypedFormGroup({
            name: new UntypedFormControl('')
        });
        const property: FieldProperty = {
            name: 'name',
            value: 'value',
            field: { ...dotcmsContentTypeFieldBasicMock }
        };
        spectator.component.group = group;
        spectator.component.property = property;
        spectator.detectChanges();

        const pInput = spectator.query('input[type="text"]');
        expect(pInput).toBeTruthy();
    });
});
