import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
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
import { DotGravatarDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { Activity, DotContentletState } from '../../../../models/dot-edit-content.model';
import { DotEditContentSidebarActivitiesSkeletonComponent } from '../dot-edit-content-sidebar-activities-skeleton/dot-edit-content-sidebar-activities-skeleton.component';

/**
 * Component that displays a list of activities in the content sidebar
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
        DotEditContentSidebarActivitiesSkeletonComponent
    ],
    templateUrl: './dot-edit-content-sidebar-activities.component.html',
    styleUrls: ['./dot-edit-content-sidebar-activities.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarActivitiesComponent {
    private readonly fb = inject(FormBuilder);

    readonly form: FormGroup<{
        comment: FormControl<string>;
    }> = this.fb.group({
        comment: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(500)]]
    });

    /**
     * The activities to display
     */
    $activities = input<Activity[]>([], { alias: 'activities' });

    /**
     * The status of the activities
     */
    $status = input<ComponentStatus>(ComponentStatus.LOADING, { alias: 'status' });

    /**
     * The initial contentlet state
     */
    $initialContentletState = input<DotContentletState>('new', { alias: 'initialContentletState' });

    /**
     * Event emitted when a new comment is submitted
     */
    commentSubmitted = output<string>();

    /**
     * Whether the activities are saving
     */
    $isSaving = computed(() => this.$status() === ComponentStatus.SAVING);

    /**
     * Whether the activities are loading
     */
    $isLoading = computed(() => this.$status() === ComponentStatus.LOADING);

    /**
     * Whether the form should be hidden
     */
    $hideForm = computed(() => {
        const initialContentletState = this.$initialContentletState();

        return initialContentletState === 'new';
    });

    /**
     * Whether the form is valid and dirty
     */
    protected readonly canSubmit = computed(() => {
        const commentValue = this.form.get('comment')?.value;

        return (
            this.form.valid &&
            this.form.dirty &&
            typeof commentValue === 'string' &&
            commentValue.trim().length > 0
        );
    });

    /**
     * Clear the comment form
     */
    clearComment(): void {
        this.form.reset();
    }

    /**
     * Submit the comment form
     */
    onSubmit(): void {
        if (this.canSubmit()) {
            const comment = this.form.get('comment')?.value?.trim();
            if (comment) {
                this.commentSubmitted.emit(comment);
                this.form.reset();
            }
        }
    }
}
