import { Subject } from 'rxjs';

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';

import { SelectItem } from 'primeng/api';

import { take, takeUntil } from 'rxjs/operators';

import { DotRolesService } from '@dotcms/data-access';
import { DotRole } from '@dotcms/dotcms-models';
import { DotFormModel } from '@models/dot-form/dot-form.model';

enum DotActionInputs {
    ASSIGNABLE = 'assignable',
    MOVEABLE = 'moveable'
}

interface DotCommentAndAssignData {
    commentable?: boolean;
    assignable?: boolean;
    moveable?: boolean;
    roleId?: string;
    roleHierarchy: boolean;
}

interface DotCommentAndAssignValue {
    assign: string;
    comments: string;
    pathToMove: string;
}

@Component({
    selector: 'dot-comment-and-assign-form',
    templateUrl: './dot-comment-and-assign-form.component.html',
    styleUrls: ['./dot-comment-and-assign-form.component.scss']
})
export class DotCommentAndAssignFormComponent
    implements OnInit, DotFormModel<DotCommentAndAssignData, DotCommentAndAssignValue>
{
    form: UntypedFormGroup;
    @Input() data: DotCommentAndAssignData;
    @Output() value = new EventEmitter<DotCommentAndAssignValue>();
    @Output() valid = new EventEmitter<boolean>();
    dotRoles: SelectItem[];

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(private dotRolesService: DotRolesService, public fb: UntypedFormBuilder) {}

    ngOnInit() {
        if (this.data) {
            if (this.data[DotActionInputs.ASSIGNABLE]) {
                this.dotRolesService
                    .get(this.data.roleId, this.data.roleHierarchy)
                    .pipe(take(1))
                    .subscribe((items: DotRole[]) => {
                        this.dotRoles = items.map((role) => {
                            return { label: role.name, value: role.id };
                        });
                        this.initForm();
                    });
            } else {
                this.initForm();
            }
        }
    }

    /**
     * Emit if form is valid and the value.
     * @memberof DotCommentAndAssignFormComponent
     */
    emitValues(): void {
        this.valid.emit(this.form.valid);
        this.value.emit(this.form.value);
    }

    private initForm(): void {
        this.form = this.fb.group({
            assign: this.dotRoles ? this.dotRoles[0].value : '',
            comments: '',
            pathToMove: this.data[DotActionInputs.MOVEABLE] ? ['', [Validators.required]] : ''
        });
        this.emitValues();
        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.emitValues());
    }
}
