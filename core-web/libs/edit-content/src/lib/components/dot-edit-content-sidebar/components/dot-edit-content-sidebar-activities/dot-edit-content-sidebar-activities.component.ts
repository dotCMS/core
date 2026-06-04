import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    input,
    output,
    viewChildren,
    signal
} from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DataViewModule } from 'primeng/dataview';
import { SkeletonModule } from 'primeng/skeleton';
import { TextareaModule } from 'primeng/textarea';

import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    DotFieldValidationMessageComponent,
    DotGravatarDirective,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import { Activity, DotContentletState } from '../../../../models/dot-edit-content.model';
import { DotEditContentSidebarActivitiesSkeletonComponent } from '../dot-edit-content-sidebar-activities-skeleton/dot-edit-content-sidebar-activities-skeleton.component';

const COMMENT_MAX_LENGTH = 500;

/**
 * Component that displays and manages activities in the content sidebar.
 * Allows users to view activity history and add new comments.
 */
@Component({
    selector: 'dot-edit-content-sidebar-activities',
    imports: [
        ReactiveFormsModule,
        AvatarModule,
        ButtonModule,
        CardModule,
        DataViewModule,
        TextareaModule,
        DotMessagePipe,
        SkeletonModule,
        DotGravatarDirective,
        DotRelativeDatePipe,
        DotEditContentSidebarActivitiesSkeletonComponent,
        DotFieldValidationMessageComponent
    ],
    templateUrl: './dot-edit-content-sidebar-activities.component.html',
    styleUrls: ['./dot-edit-content-sidebar-activities.component.scss'],
    host: {
        class: 'flex flex-col h-full relative'
    },
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
    readonly form: FormGroup = this.#fb.group({
        comment: [
            '',
            [
                // Custom validator: only validate when field has value
                (control: FormControl) => {
                    const value = control.value?.trim() || '';

                    // If empty, no validation needed
                    if (!value) {
                        return null;
                    }

                    // Otherwise apply max length validator
                    if (value.length > COMMENT_MAX_LENGTH) {
                        return { invalid: true };
                    }

                    return null;
                }
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
     * Whether the activities tab is currently the active/visible tab.
     * Needed to trigger the scroll-to-bottom effect when the user switches
     * into this tab after activities were already rendered (otherwise the
     * panel is display:none when `viewChildren` first emits and
     * `scrollIntoView` is a no-op).
     */
    $isActive = input<boolean>(false, { alias: 'isActive' });

    /**
     * View children for the activity items. We read `ElementRef` explicitly so
     * that switching the underlying element to a PrimeNG component (e.g.
     * `<p-card>`) still gives us a DOM element to scroll to.
     */
    activityItems = viewChildren('activityItem', { read: ElementRef });

    /**
     * Effect to scroll to the last activity when the list changes or when the
     * activities tab becomes the active one. Reacting to `$isActive` is needed
     * because the effect first fires while the tab panel is still hidden
     * (display:none) and any scroll on a 0-height element is a no-op.
     */
    #scrollEffect = effect(() => {
        const items = this.activityItems();
        const isActive = this.$isActive();

        if (items.length === 0 || !isActive) {
            return;
        }

        const lastItem = items[items.length - 1].nativeElement;
        // Defer one animation frame so PrimeNG can apply the tabpanel
        // visibility change (display:none -> block) and the scroll container
        // has its real dimensions before we scroll.
        requestAnimationFrame(() => {
            const scrollContainer = lastItem.closest('.overflow-y-auto') as HTMLElement | null;
            if (scrollContainer) {
                // Direct assignment is synchronous and cannot be interrupted by
                // subsequent re-renders, unlike scrollIntoView({ behavior: 'smooth' }).
                scrollContainer.scrollTop = scrollContainer.scrollHeight;
            } else {
                lastItem.scrollIntoView({ block: 'end' });
            }
        });
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
     * Expose the comment max length for template use
     */
    readonly commentMaxLength = COMMENT_MAX_LENGTH;

    // Use writable signals for live updates
    readonly commentLength = signal(0);
    readonly isAtMaxLength = signal(false);

    constructor() {
        // Listen to comment control changes to update the character counter
        this.commentControl.valueChanges.subscribe((value: string) => {
            const length = value ? value.length : 0;
            this.commentLength.set(length);
            this.isAtMaxLength.set(length >= this.commentMaxLength);
        });

        // Effect to disable/enable comment control based on $isSaving
        effect(() => {
            if (this.$isSaving()) {
                this.commentControl.disable({ emitEvent: false });
            } else {
                this.commentControl.enable({ emitEvent: false });
            }
        });
    }

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
        const comment = this.commentControl.value?.trim();

        // Check for empty comment and mark as touched to trigger validation
        if (!comment) {
            this.commentControl.setErrors({ required: true });
            this.commentControl.markAsDirty();
            this.commentControl.markAsTouched();

            return;
        }

        // Check for other validation errors
        if (this.form.invalid) {
            this.commentControl.markAsDirty();
            this.commentControl.markAsTouched();
            this.commentControl.updateValueAndValidity({ onlySelf: true });

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

    /**
     * DataView passthrough (pt) configuration for PrimeNG v21 styling.
     */
    readonly dataViewPt = {
        root: { class: 'bg-transparent border-none' },
        content: { class: 'p-0 border-none flex flex-col gap-4 bg-transparent' },
        emptyMessage: { class: 'bg-transparent p-0' }
    };

    /**
     * Avatar passthrough (pt) configuration for PrimeNG v21 styling.
     */
    readonly avatarPt = {
        root: {
            class: 'w-[21px] h-[21px] text-xs leading-[21px] flex items-center justify-center'
        },
        text: { class: 'text-xs leading-none' }
    };
}
