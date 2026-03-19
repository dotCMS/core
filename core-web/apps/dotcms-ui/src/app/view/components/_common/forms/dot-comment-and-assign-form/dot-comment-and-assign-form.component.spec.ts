/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';

import { DotFormatDateService, DotRolesService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { CoreWebServiceMock, mockProcessedRoles } from '@dotcms/utils-testing';

import { DotCommentAndAssignFormComponent } from './dot-comment-and-assign-form.component';

describe('DotAssigneeFormComponent', () => {
    let spectator: Spectator<DotCommentAndAssignFormComponent>;
    let dotRolesService: DotRolesService;

    const createComponent = createComponentFactory({
        component: DotCommentAndAssignFormComponent,
        imports: [
            HttpClientTestingModule,
            FormsModule,
            ReactiveFormsModule,
            TextareaModule,
            SelectModule,
            DotMessagePipe
        ],
        providers: [
            DotRolesService,
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            DotFormatDateService
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        dotRolesService = spectator.inject(DotRolesService);
        jest.spyOn(dotRolesService, 'get').mockReturnValue(of(mockProcessedRoles));
    });

    it('should show only commentable field', () => {
        spectator.setInput('data', { commentable: true, roleHierarchy: false });
        spectator.detectChanges();

        const textArea = spectator.debugElement.query(By.css('textarea'));
        const dropdownEl = spectator.debugElement.query(By.css('p-select'));
        expect(textArea).toBeTruthy();
        expect(dropdownEl).toBeFalsy();
    });

    it('should show only assignable field', fakeAsync(() => {
        spectator.setInput('data', {
            assignable: true,
            roleId: '123',
            roleHierarchy: false
        });
        spectator.detectChanges();
        tick();
        spectator.detectChanges();

        const textArea = spectator.debugElement.query(By.css('textarea'));
        const dropdownEl = spectator.debugElement.query(By.css('p-select'));
        expect(textArea).toBeFalsy();
        expect(dropdownEl).toBeTruthy();
        expect(spectator.component.dotRoles).toEqual([
            { label: mockProcessedRoles[0].name, value: mockProcessedRoles[0].id },
            { label: mockProcessedRoles[1].name, value: mockProcessedRoles[1].id }
        ]);
    }));

    it('should enable filter on role dropdown when assignable', fakeAsync(() => {
        spectator.setInput('data', {
            assignable: true,
            roleId: '123',
            roleHierarchy: false
        });
        spectator.detectChanges();
        tick();
        spectator.detectChanges();

        const dropdownEl = spectator.debugElement.query(By.css('p-select'));
        expect(dropdownEl).toBeTruthy();
        const selectInstance = dropdownEl?.componentInstance;
        expect(selectInstance?.filter).toBe(true);
        expect(selectInstance?.filterBy).toBe('label');
        expect(selectInstance?.filterPlaceholder).toBeDefined();
    }));

    describe('both fields', () => {
        beforeEach(fakeAsync(() => {
            spectator.setInput('data', {
                commentable: true,
                assignable: true,
                roleId: '123',
                roleHierarchy: false
            });
            spectator.detectChanges();
            tick();
            spectator.detectChanges();
        }));

        it('should show both fields', () => {
            const textArea = spectator.debugElement.query(By.css('textarea'));
            const dropdownEl = spectator.debugElement.query(By.css('p-select'));
            expect(textArea).toBeTruthy();
            expect(dropdownEl).toBeTruthy();
        });

        it('should emit value and valid on form change', () => {
            jest.spyOn(spectator.component.valid, 'emit');
            jest.spyOn(spectator.component.value, 'emit');

            const mockFormValue = {
                assign: mockProcessedRoles[0].id,
                comments: 'test',
                pathToMove: '/path/'
            };
            spectator.component.form.setValue(mockFormValue);

            expect(spectator.component.valid.emit).toHaveBeenCalledWith(true);
            expect(spectator.component.valid.emit).toHaveBeenCalledTimes(1);
            expect(spectator.component.value.emit).toHaveBeenCalledWith(mockFormValue);
            expect(spectator.component.value.emit).toHaveBeenCalledTimes(1);
        });
    });
});
