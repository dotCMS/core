import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    output
} from '@angular/core';
import {
    FormBuilder,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { SkeletonModule } from 'primeng/skeleton';

import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    DotFieldValidationMessageComponent,
    DotGravatarDirective,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import { Activity, DotContentletState } from '../../../../models/dot-edit-content.model';
import { DotEditContentSidebarActivitiesSkeletonComponent } from '../dot-edit-content-sidebar-activities-skeleton/dot-edit-content-sidebar-activities-skeleton.component';

const COMMENT_MIN_LENGTH = 3;
const COMMENT_MAX_LENGTH = 500;

/**
 * Component that displays and manages activities in the content sidebar.
 * Allows users to view activity history and add new comments.
 */
@Component({
    selector: 'dot-edit-content-sidebar-activities',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        AvatarModule,
        ButtonModule,
        DataViewModule,
        InputTextareaModule,
        DotMessagePipe,
        SkeletonModule,
        DotGravatarDirective,
        DotRelativeDatePipe,
        DotEditContentSidebarActivitiesSkeletonComponent,
        DotFieldValidationMessageComponent
    ],
    templateUrl: './dot-edit-content-sidebar-activities.component.html',
    styleUrls: ['./dot-edit-content-sidebar-activities.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarActivitiesComponent {
    /**
     * Form builder for the comment field
     */
    #fb = inject(FormBuilder);

    /**
     * Form group for the comment field
     */
    readonly form: FormGroup<{
        comment: FormControl<string>;
    }> = this.#fb.group({
        comment: [
            '',
            [
                Validators.required,
                Validators.minLength(COMMENT_MIN_LENGTH),
                Validators.maxLength(COMMENT_MAX_LENGTH)
            ]
        ]
    });

    /**
     * List of activities to display in the timeline
     * @readonly
     */
    $activities = input<Activity[]>([], { alias: 'activities' });

    /**
     * Current status of the activities component
     * Used to control loading and saving states
     * @readonly
     */
    $status = input<ComponentStatus>(ComponentStatus.LOADING, { alias: 'status' });

    /**
     * Effect to scroll to bottom when activities change
     */
    #scrollEffect = effect(() => {
        const activities = this.$activities();
        if (activities.length) {
            setTimeout(() => {
                const lastActivity = document.querySelector('.activities__item-wrapper:last-child');
                if (lastActivity) {
                    lastActivity.scrollIntoView({
                        behavior: 'smooth',
                        block: 'end'
                    });
                }
            });
        }
    });

    /**
     * Initial state of the contentlet
     * Used to determine if the comment form should be displayed
     * @readonly
     */
    $initialContentletState = input<DotContentletState>('new', {
        alias: 'initialContentletState'
    });

    /**
     * Event emitted when a new comment is submitted
     */
    commentSubmitted = output<string>();

    /**
     * Determines if the activities are in a loading state
     */
    protected readonly $isLoading = computed(() => this.$status() === ComponentStatus.LOADING);

    /**
     * Determines if the activities are in a saving state
     */
    protected readonly $isSaving = computed(() => this.$status() === ComponentStatus.SAVING);

    /**
     * Determines if the comment form should be hidden
     */
    protected readonly $hideForm = computed(() => this.$initialContentletState() === 'new');

    /**
     * Resets the comment form to its initial state
     */
    clearComment(): void {
        this.form.reset();
        this.form.markAsPristine();
        this.commentControl.markAsUntouched();
    }

    /**
     * Handles the submission of a new comment
     * Validates the form and emits the comment if valid
     */
    onSubmit(): void {
        if (this.form.invalid) {
            this.commentControl.markAsDirty();
            this.commentControl.markAsTouched();
            this.commentControl.updateValueAndValidity({ onlySelf: true });

            return;
        }

        const comment = this.commentControl.value?.trim();
        if (!comment) {
            return;
        }

        this.commentSubmitted.emit(comment);
        this.form.reset();
        this.form.markAsPristine();
    }

    /**
     * Get the comment control
     */
    protected get commentControl(): FormControl<string> {
        return this.form.get('comment') as FormControl<string>;
    }
}
