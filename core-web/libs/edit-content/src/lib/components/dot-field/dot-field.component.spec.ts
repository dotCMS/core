import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
    ControlContainer,
    FormControl,
    FormGroup,
    FormGroupDirective,
    ReactiveFormsModule
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { InputTextModule } from 'primeng/inputtext';

import { DotFieldComponent } from './dot-field.component';

import { DotField } from '../../interfaces/dot-form.interface';
import { DotFormComponent } from '../dot-form/dot-form.component';

describe('DotFieldComponent', () => {
    let component: DotFieldComponent;
    let fixture: ComponentFixture<DotFieldComponent>;

    const field: DotField = {
        id: 'Test id',
        variable: 'testVariable',
        hint: 'Test hind',
        label: 'Test Label',
        required: false,
        type: 'Text'
    };

    const FORM_GROUP_MOCK = new FormGroup({
        testVariable: new FormControl('')
    });
    const FORM_GROUP_DIRECTIVE_MOCK: FormGroupDirective = new FormGroupDirective([], []);
    FORM_GROUP_DIRECTIVE_MOCK.form = FORM_GROUP_MOCK;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                DotFieldComponent,
                CommonModule,
                ReactiveFormsModule,
                InputTextModule,
                DotFormComponent
            ],
            providers: [
                {
                    provide: ControlContainer,
                    useValue: FORM_GROUP_DIRECTIVE_MOCK
                }
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotFieldComponent);
        component = fixture.componentInstance;
        component.field = field;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should render the label', () => {
        fixture.detectChanges();
        const label = fixture.debugElement.query(By.css('label')).nativeElement;
        expect(label.textContent).toContain(field.label);
    });

    it('should render the hint', () => {
        fixture.detectChanges();
        const label = fixture.debugElement.query(By.css('small')).nativeElement;
        expect(label.textContent).toContain(field.hint);
    });
});
