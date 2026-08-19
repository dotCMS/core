import { Subject } from 'rxjs';

import {
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    inject,
    ChangeDetectionStrategy
} from '@angular/core';
import {
    FormsModule,
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup,
    Validators
} from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';

import { take, takeUntil } from 'rxjs/operators';

import { DotRolesService } from '@dotcms/data-access';
import { DotRole } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotFormModel } from '../../../../../shared/models/dot-form/dot-form.model';
import { DotPageSelectorComponent } from '../../dot-page-selector/dot-page-selector.component';

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
    styleUrls: ['./dot-comment-and-assign-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        FormsModule,
        ReactiveFormsModule,
        TextareaModule,
        SelectModule,
        DotPageSelectorComponent,
        DotFieldRequiredDirective,
        DotMessagePipe
    ]
})
export class DotCommentAndAssignFormComponent
    implements OnInit, DotFormModel<DotCommentAndAssignData, DotCommentAndAssignValue>
{
    private dotRolesService = inject(DotRolesService);
    private readonly cdr = inject(ChangeDetectorRef);
    fb = inject(UntypedFormBuilder);

    @Input() data!: DotCommentAndAssignData;
    @Output() value = new EventEmitter<DotCommentAndAssignValue>();
    @Output() valid = new EventEmitter<boolean>();
    form!: UntypedFormGroup;
    dotRoles: SelectItem[] = [];

    private destroy$: Subject<boolean> = new Subject<boolean>();

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
            // `?.length`, not truthiness: `[]` is truthy, so this took the first-role branch and
            // read `[0].value` off an empty list. The check has always meant "if there are roles".
            assign: this.dotRoles?.length ? this.dotRoles[0].value : '',
            comments: '',
            pathToMove: this.data[DotActionInputs.MOVEABLE] ? ['', [Validators.required]] : ''
        });
        this.emitValues();
        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.emitValues());
        // Dynamically created inside p-dialog (appendTo="body"); async role load must
        // force CD or the @if (form) template stays empty after Angular 21+/22.
        this.cdr.detectChanges();
    }
}
