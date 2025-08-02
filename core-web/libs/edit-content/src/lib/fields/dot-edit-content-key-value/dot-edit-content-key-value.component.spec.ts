import { SpectatorHost } from '@ngneat/spectator';
import { createHostFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotKeyValueComponent } from '@dotcms/ui';

import { DotEditContentKeyValueComponent } from './dot-edit-content-key-value.component';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
class MockFormComponent {
    form = new FormGroup({
        keyValue: new FormControl<Record<string, string>>({
            key1: 'value1',
            key2: 'value2'
        })
    });
}

describe('DotEditContentKeyValueComponent', () => {
    let spectator: SpectatorHost<DotEditContentKeyValueComponent, MockFormComponent>;
    const createHost = createHostFactory({
        component: DotEditContentKeyValueComponent,
        host: MockFormComponent,
        declarations: [MockComponent(DotKeyValueComponent)],
        imports: [ReactiveFormsModule]
    });

    beforeEach(() => {
        spectator = createHost(` <form [formGroup]="form">
            <dot-edit-content-key-value formControlName="keyValue" />
        </form>`);
    });

    it('should set the correct initial value', () => {
        expect(spectator.component.$initialValue()).toEqual([
            { key: 'key1', value: 'value1' },
            { key: 'key2', value: 'value2' }
        ]);
    });

    it('should set the correct value in the form', (done) => {
        const control = spectator.hostComponent.form.get('keyValue');
        control.valueChanges.subscribe((value) => {
            expect(value).toEqual({ key14: 'value14' });
            done();
        });

        const dotKeyValue = spectator.query(DotKeyValueComponent);
        dotKeyValue.updatedList.emit([{ key: 'key14', hidden: false, value: 'value14' }]);
        expect(control.touched).toBeTruthy();
    });
});
