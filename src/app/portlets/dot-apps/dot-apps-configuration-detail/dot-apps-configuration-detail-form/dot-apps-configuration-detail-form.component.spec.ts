import { async, ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '@tests/dot-test-bed';
import { CommonModule } from '@angular/common';
import { DotAppsConfigurationDetailFormComponent } from './dot-apps-configuration-detail-form.component';
import { CheckboxModule, InputTextareaModule, InputTextModule } from 'primeng/primeng';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

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
    }
];

const formState = {
    name: secrets[0].value,
    password: secrets[1].value,
    enabled: JSON.parse(secrets[2].value)
};

describe('DotAppsConfigurationDetailFormComponent', () => {
    let component: DotAppsConfigurationDetailFormComponent;
    let fixture: ComponentFixture<DotAppsConfigurationDetailFormComponent>;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [
                CommonModule,
                CheckboxModule,
                InputTextareaModule,
                InputTextModule,
                ReactiveFormsModule
            ],
            declarations: [DotAppsConfigurationDetailFormComponent],
            providers: []
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotAppsConfigurationDetailFormComponent);
        component = fixture.debugElement.componentInstance;
        component.formFields = secrets;
        spyOn(component.data, 'emit');
        spyOn(component.valid, 'emit');
        fixture.detectChanges();
    });

    it('should load form components', () => {
        expect(
            fixture.debugElement.queryAll(By.css('.dot-apps-configuration-detail__form-row')).length
        ).toBe(secrets.length);
    });

    it('should load Label, Textarea & Hint with right attributes', () => {
        const row = fixture.debugElement.queryAll(
            By.css('.dot-apps-configuration-detail__form-row')
        )[0];
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
        expect(row.query(By.css('textarea')).nativeElement.value).toBe(secrets[0].value);
        expect(row.query(By.css('.form__group-hint')).nativeElement.textContent).toBe(
            secrets[0].hint
        );
    });

    it('should load Label, Password & Hint with right attributes', () => {
        const row = fixture.debugElement.queryAll(
            By.css('.dot-apps-configuration-detail__form-row')
        )[1];
        expect(row.query(By.css('label')).nativeElement.textContent).toBe(secrets[1].label);
        expect(
            row.query(By.css('label')).nativeElement.classList.contains('form__label')
        ).toBeTruthy();
        expect(
            row.query(By.css('label')).nativeElement.classList.contains('required')
        ).toBeTruthy();
        expect(row.query(By.css('input')).nativeElement.attributes.id.value).toBe(secrets[1].name);
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
        expect(row.query(By.css('p-checkbox')).nativeElement.attributes.id.value).toBe(
            secrets[2].name
        );
        expect(row.query(By.css('p-checkbox')).componentInstance.label).toBe(secrets[2].label);
        expect(row.query(By.css('input')).nativeElement.value).toBe(secrets[2].value);
        expect(row.query(By.css('.form__group-hint')).nativeElement.textContent).toBe(
            secrets[2].hint
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
