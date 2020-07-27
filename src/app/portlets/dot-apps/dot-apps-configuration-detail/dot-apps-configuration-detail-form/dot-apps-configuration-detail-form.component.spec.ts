import { async, ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '@tests/dot-test-bed';
import { CommonModule } from '@angular/common';
import { DotAppsConfigurationDetailFormComponent } from './dot-apps-configuration-detail-form.component';
import {
    CheckboxModule,
    InputTextareaModule,
    InputTextModule,
    TooltipModule
} from 'primeng/primeng';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { NgxMdModule } from 'ngx-md';

const secrets = [
    {
        dynamic: false,
        name: 'name',
        hidden: false,
        hint: 'This is a Name',
        label: 'Name:',
        required: true,
        type: 'STRING',
        value: 'test'
    },
    {
        dynamic: false,
        name: 'password',
        hidden: true,
        hint: 'This is a password',
        label: 'Password:',
        required: true,
        type: 'STRING',
        value: '****'
    },
    {
        dynamic: false,
        name: 'enabled',
        hidden: false,
        hint: 'This is Enabled!',
        label: 'Enabled:',
        required: false,
        type: 'BOOL',
        value: 'true'
    },
    {
        dynamic: false,
        name: 'select',
        hidden: false,
        hint: 'This is Select!',
        label: 'Select label:',
        options: [
            {
                label: 'uno',
                value: '1'
            },
            {
                label: 'dos',
                value: '2'
            }
        ],
        required: true,
        type: 'SELECT',
        value: '1'
    }
];

const formState = {
    name: secrets[0].value,
    password: secrets[1].value,
    enabled: JSON.parse(secrets[2].value),
    select: secrets[3].options[0].value
};

describe('DotAppsConfigurationDetailFormComponent', () => {
    let component: DotAppsConfigurationDetailFormComponent;
    let fixture: ComponentFixture<DotAppsConfigurationDetailFormComponent>;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [
                CommonModule,
                CheckboxModule,
                DotIconModule,
                InputTextareaModule,
                InputTextModule,
                NgxMdModule,
                ReactiveFormsModule,
                TooltipModule
            ],
            declarations: [DotAppsConfigurationDetailFormComponent],
            providers: []
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotAppsConfigurationDetailFormComponent);
        component = fixture.debugElement.componentInstance;
        component.formFields = secrets;
    });

    describe('Without warnings', () => {
        beforeEach(() => {
            spyOn(component.data, 'emit');
            spyOn(component.valid, 'emit');
            fixture.detectChanges();
        });

        it('should load form components', () => {
            expect(
                fixture.debugElement.queryAll(By.css('.dot-apps-configuration-detail__form-row'))
                    .length
            ).toBe(secrets.length);
        });

        it('should not have warning icon', () => {
            expect(fixture.debugElement.query(By.css('dot-icon'))).toBeFalsy();
        });

        it('should focus on first input when loaded', () => {
            const focusField = component.formContainer.nativeElement.querySelector('#name');
            spyOn(focusField, 'focus');
            fixture.detectChanges();
            fixture.whenStable().then(() => {
                expect(focusField.focus).toHaveBeenCalledTimes(1);
            });
        });

        it('should load Label, Textarea & Hint with right attributes', () => {
            const row = fixture.debugElement.queryAll(
                By.css('.dot-apps-configuration-detail__form-row')
            )[0];
            expect(row.query(By.css('ngx-md'))).toBeTruthy();
            expect(row.query(By.css('label')).nativeElement.textContent).toBe(secrets[0].label);
            expect(
                row.query(By.css('label')).nativeElement.classList.contains('form__label')
            ).toBeTruthy();
            expect(
                row.query(By.css('label')).nativeElement.classList.contains('required')
            ).toBeTruthy();
            expect(row.query(By.css('textarea')).nativeElement.attributes.id.value).toBe(
                secrets[0].name
            );
            expect(row.query(By.css('textarea')).nativeElement.attributes.autoResize.value).toBe(
                'autoResize'
            );
            expect(row.query(By.css('textarea')).nativeElement.value).toBe(secrets[0].value);
            expect(row.query(By.css('.form__group-hint')).nativeElement.textContent).toBe(
                secrets[0].hint
            );
        });

        it('should load Label, Password & Hint with right attributes', () => {
            const row = fixture.debugElement.queryAll(
                By.css('.dot-apps-configuration-detail__form-row')
            )[1];
            expect(row.query(By.css('ngx-md'))).toBeTruthy();
            expect(row.query(By.css('label')).nativeElement.textContent).toBe(secrets[1].label);
            expect(
                row.query(By.css('label')).nativeElement.classList.contains('form__label')
            ).toBeTruthy();
            expect(
                row.query(By.css('label')).nativeElement.classList.contains('required')
            ).toBeTruthy();
            expect(row.query(By.css('input')).nativeElement.attributes.id.value).toBe(
                secrets[1].name
            );
            expect(row.query(By.css('input')).nativeElement.type).toBe('password');
            expect(row.query(By.css('input')).nativeElement.value).toBe(secrets[1].value);
            expect(row.query(By.css('.form__group-hint')).nativeElement.textContent).toBe(
                secrets[1].hint
            );
        });

        it('should load Checkbox & Hint with right attributes', () => {
            const row = fixture.debugElement.queryAll(
                By.css('.dot-apps-configuration-detail__form-row')
            )[2];
            expect(row.query(By.css('ngx-md'))).toBeTruthy();
            expect(row.query(By.css('p-checkbox')).nativeElement.attributes.id.value).toBe(
                secrets[2].name
            );
            expect(row.query(By.css('p-checkbox')).componentInstance.label).toBe(secrets[2].label);
            expect(row.query(By.css('input')).nativeElement.value).toBe(secrets[2].value);
            expect(row.query(By.css('.form__group-hint')).nativeElement.textContent).toBe(
                secrets[2].hint
            );
        });

        it('should load Label, Select & Hint with right attributes', () => {
            const row = fixture.debugElement.queryAll(
                By.css('.dot-apps-configuration-detail__form-row')
            )[3];
            expect(row.query(By.css('ngx-md'))).toBeTruthy();
            expect(row.query(By.css('label')).nativeElement.textContent).toBe(secrets[3].label);
            expect(
                row.query(By.css('label')).nativeElement.classList.contains('form__label')
            ).toBeTruthy();
            expect(
                row.query(By.css('label')).nativeElement.classList.contains('required')
            ).toBeTruthy();
            expect(row.query(By.css('p-dropdown')).nativeElement.id).toBe(secrets[3].name);
            expect(row.query(By.css('p-dropdown')).componentInstance.options).toBe(
                secrets[3].options
            );
            expect(row.query(By.css('p-dropdown')).componentInstance.value).toBe(
                secrets[3].options[0].value
            );
            expect(row.query(By.css('.form__group-hint')).nativeElement.textContent).toBe(
                secrets[3].hint
            );
        });

        it('should emit form state when loaded', () => {
            expect(component.data.emit).toHaveBeenCalledWith(formState);
            expect(component.valid.emit).toHaveBeenCalledWith(true);
        });

        it('should emit form state when value changed', () => {
            component.myFormGroup.get('name').setValue('Test2');
            component.myFormGroup.get('password').setValue('Password2');
            component.myFormGroup.get('enabled').setValue('false');
            expect(component.data.emit).toHaveBeenCalledTimes(4);
            expect(component.valid.emit).toHaveBeenCalledTimes(4);
        });

        it('should emit form state disabled when required field empty', () => {
            component.myFormGroup.get('name').setValue('');
            expect(component.valid.emit).toHaveBeenCalledWith(false);
        });
    });

    describe('With warnings', () => {
        beforeEach(() => {
            component.formFields[0].warnings = ['error A'];
            component.formFields[1].warnings = ['error B'];
            component.formFields[2].warnings = ['error C'];
            fixture.detectChanges();
        });

        it('should have warning icons', () => {
            const warningIcons = fixture.debugElement.queryAll(By.css('dot-icon'));
            expect(warningIcons[0].attributes['name']).toBe('warning');
            expect(warningIcons[0].attributes['size']).toBe('18');
            expect(warningIcons[0].attributes['ng-reflect-text']).toBe(
                component.formFields[0].warnings[0]
            );
            expect(warningIcons[1].attributes['name']).toBe('warning');
            expect(warningIcons[1].attributes['size']).toBe('18');
            expect(warningIcons[1].attributes['ng-reflect-text']).toBe(
                component.formFields[1].warnings[0]
            );
            expect(warningIcons[2].attributes['name']).toBe('warning');
            expect(warningIcons[2].attributes['size']).toBe('18');
            expect(warningIcons[2].attributes['ng-reflect-text']).toBe(
                component.formFields[2].warnings[0]
            );
        });
    });
});
