import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCommentAndAssignFormComponent } from './dot-comment-and-assign-form.component';
import { DotRolesService } from '@services/dot-roles/dot-roles.service';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { Dropdown, DropdownModule } from 'primeng/dropdown';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from '../../../../../../../projects/dotcms-js/src/lib/core/core-web.service.mock';
import { BaseRequestOptions, ConnectionBackend, Http, RequestOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { FormatDateService } from '@services/format-date-service';
import { Component, DebugElement, Input } from '@angular/core';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { mockProcessedRoles } from '@services/dot-roles/dot-roles.service.spec';

@Component({
    selector: 'dot-test-host-component',
    template:
        '<dot-comment-and-assign-form *ngIf="data" [data]="data"></dot-comment-and-assign-form>'
})
class TestHostComponent {
    @Input() data: any;
}

describe('DotAssigneeFormComponent', () => {
    let component: TestHostComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let dotRolesService: DotRolesService;
    let textArea: DebugElement;
    let dropdownElement: DebugElement;
    let dropdown: Dropdown;

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                declarations: [TestHostComponent, DotCommentAndAssignFormComponent],
                providers: [
                    DotRolesService,
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: ConnectionBackend, useClass: MockBackend },
                    { provide: RequestOptions, useClass: BaseRequestOptions },
                    Http,
                    FormatDateService
                ],
                imports: [
                    DotPipesModule,
                    FormsModule,
                    ReactiveFormsModule,
                    InputTextareaModule,
                    DropdownModule
                ]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.componentInstance;
        dotRolesService = fixture.debugElement.injector.get(DotRolesService);
        spyOn(dotRolesService, 'get').and.returnValue(of(mockProcessedRoles));
    });

    it('should show only commentable field', () => {
        component.data = { commentable: true };
        fixture.detectChanges();
        textArea = fixture.debugElement.query(By.css('textarea'));
        dropdownElement = fixture.debugElement.query(By.css('p-dropdown'));
        expect(textArea).not.toBeNull();
        expect(dropdownElement).toBeNull();
    });

    it('should show only assignable field', () => {
        component.data = { assignable: true, roleId: '123' };
        fixture.detectChanges();
        textArea = fixture.debugElement.query(By.css('textarea'));
        dropdown = fixture.debugElement.query(By.css('p-dropdown')).componentInstance;
        expect(dropdown.options).toEqual([
            { label: mockProcessedRoles[0].name, value: mockProcessedRoles[0].id },
            { label: mockProcessedRoles[1].name, value: mockProcessedRoles[1].id }
        ]);
        expect(textArea).toBeNull();
    });

    describe('both fields', () => {
        beforeEach(() => {
            component.data = { commentable: true, assignable: true, roleId: '123' };
            fixture.detectChanges();
            textArea = fixture.debugElement.query(By.css('textarea'));
            dropdown = fixture.debugElement.query(By.css('p-dropdown')).componentInstance;
        });

        it('should show both fields', () => {
            expect(textArea).not.toBeNull();
            expect(dropdown).not.toBeNull();
        });

        it('should emit value and valid on form change', () => {
            const mockFormValue = {
                assign: mockProcessedRoles[0].id,
                comments: 'test'
            };
            const formComponent: DotCommentAndAssignFormComponent = fixture.debugElement.query(
                By.css('dot-comment-and-assign-form')
            ).componentInstance;
            spyOn(formComponent.valid, 'emit');
            spyOn(formComponent.value, 'emit');

            formComponent.form.setValue(mockFormValue);

            expect(formComponent.valid.emit).toHaveBeenCalledWith(true);
            expect(formComponent.value.emit).toHaveBeenCalledWith(mockFormValue);
        });
    });
});
