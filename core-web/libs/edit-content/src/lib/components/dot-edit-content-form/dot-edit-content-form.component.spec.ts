import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentFormComponent } from './dot-edit-content-form.component';

import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';

describe('DotFormComponent', () => {
    let component: DotEditContentFormComponent;
    let fixture: ComponentFixture<DotEditContentFormComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                CommonModule,
                ReactiveFormsModule,
                DotEditContentFieldComponent,
                ButtonModule,
                DotMessagePipe
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentFormComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
