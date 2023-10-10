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

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentFieldComponent } from './dot-edit-content-field.component';

describe('DotFieldComponent', () => {
    let component: DotEditContentFieldComponent;
    let fixture: ComponentFixture<DotEditContentFieldComponent>;

    const field: DotCMSContentTypeField = {
        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
        contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
        dataType: 'TEXT',
        fieldType: 'Text',
        fieldTypeLabel: 'Text',
        fieldVariables: [],
        fixed: false,
        iDate: 1696896882000,
        id: 'c3b928bc2b59fc22c67022de4dd4b5c4',
        indexed: false,
        listed: false,
        hint: 'A helper text',
        modDate: 1696896882000,
        name: 'testVariable',
        readOnly: false,
        required: false,
        searchable: false,
        sortOrder: 2,
        unique: false,
        variable: 'testVariable'
    };

    const FORM_GROUP_MOCK = new FormGroup({
        testVariable: new FormControl('')
    });
    const FORM_GROUP_DIRECTIVE_MOCK: FormGroupDirective = new FormGroupDirective([], []);
    FORM_GROUP_DIRECTIVE_MOCK.form = FORM_GROUP_MOCK;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                DotEditContentFieldComponent,
                CommonModule,
                ReactiveFormsModule,
                InputTextModule
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
        fixture = TestBed.createComponent(DotEditContentFieldComponent);
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
        expect(label.textContent).toContain(field.fieldTypeLabel);
    });

    it('should render the hint', () => {
        fixture.detectChanges();
        const label = fixture.debugElement.query(By.css('small')).nativeElement;
        expect(label.textContent).toContain(field.hint);
    });
});
