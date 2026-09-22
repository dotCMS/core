import { EMPTY } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    computed,
    inject,
    signal
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';

import { catchError, finalize, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DOT_TOOLS_SECTION_ICONS } from '../constants/dot-tools.constants';
import { DotToolsStore } from '../dot-tools-page/store/dot-tools.store';
import { DotToolsSection } from '../models/dot-tools.models';

interface DotToolsSectionDialogData {
    section?: DotToolsSection;
}

interface IconOption {
    id: string;
    label: string;
}

/**
 * Section create / edit dialog.
 *
 * Owns the submit rather than closing with the form value. The section-create
 * endpoint can reject a duplicate name with 400 and a message the screen
 * should display next to the Name field — that only works if the dialog is
 * still there when the response arrives. See `libs/portlets/CLAUDE.md ›
 * Dialogs whose submit can fail into the form`.
 */
@Component({
    selector: 'dot-tools-section-dialog',
    standalone: true,
    imports: [ReactiveFormsModule, InputTextModule, SelectModule, ButtonModule, DotMessagePipe],
    templateUrl: './dot-tools-section-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotToolsSectionDialogComponent implements OnInit {
    readonly ref = inject(DynamicDialogRef);
    readonly config = inject<DynamicDialogConfig<DotToolsSectionDialogData>>(DynamicDialogConfig);

    readonly #fb = inject(FormBuilder);
    readonly #store = inject(DotToolsStore);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    protected readonly form = this.#fb.nonNullable.group({
        name: ['', Validators.required],
        icon: ['widgets', Validators.required]
    });

    protected readonly icons: IconOption[] = DOT_TOOLS_SECTION_ICONS.map((id) => ({
        id,
        label: id.replace(/_/g, ' ')
    }));

    protected readonly $submitted = signal(false);
    protected readonly $submitting = signal(false);

    /**
     * Message the server sent back on a rejected write — typically the
     * duplicate-name / blank / over-255 message from FR-005. Non-null keeps
     * the dialog open and renders inline under the name field.
     */
    protected readonly $submitError = signal<string | null>(null);

    protected readonly $nameShowsError = computed(() => {
        const control = this.form.controls.name;

        return control.invalid && (control.touched || this.$submitted());
    });

    protected isEdit = false;

    ngOnInit(): void {
        const section = this.config.data?.section;
        if (section) {
            this.isEdit = true;
            this.form.patchValue({ name: section.name, icon: section.icon });
        }
    }

    protected onSubmit(): void {
        this.$submitted.set(true);
        this.$submitError.set(null);

        if (this.form.invalid) {
            this.form.markAllAsTouched();

            return;
        }

        const value = this.form.getRawValue();
        const section = this.config.data?.section;
        const request$ = section
            ? this.#store.updateSection(section.id, value)
            : this.#store.createSection(value);

        this.$submitting.set(true);

        request$
            .pipe(
                take(1),
                catchError((error: unknown) => {
                    if (this.#isValidationError(error)) {
                        // Duplicate name / blank / over-length — display inline
                        // so the user can correct without losing the input.
                        this.$submitError.set(this.#extractMessage(error));
                    } else {
                        // Anything else is a genuine server error; route to
                        // the global handler and close so the user isn't stuck.
                        this.#httpErrorManager.handle(error as HttpErrorResponse);
                        this.ref.close();
                    }

                    return EMPTY;
                }),
                finalize(() => this.$submitting.set(false))
            )
            .subscribe(() => this.ref.close(true));
    }

    protected onCancel(): void {
        this.ref.close();
    }

    protected onNameInput(): void {
        // Typing away from the offending name clears the server-side error;
        // the request will re-validate on next submit.
        if (this.$submitError()) {
            this.$submitError.set(null);
        }
    }

    #isValidationError(error: unknown): boolean {
        return error instanceof HttpErrorResponse && error.status === 400;
    }

    /**
     * Best-effort message extraction. dotCMS wraps errors as
     * `{ errors: [{ message, ... }], ... }` when JAX-RS maps them and as a
     * plain string when the exception bubbles up raw. We accept both, and
     * fall back to the response's own text if the body isn't recognisable.
     */
    #extractMessage(error: unknown): string {
        if (error instanceof HttpErrorResponse) {
            const body = error.error as
                | string
                | { message?: string; errors?: Array<{ message?: string }> }
                | null;

            if (typeof body === 'string' && body.length > 0) {
                return body;
            }

            if (body && typeof body === 'object') {
                if (typeof body.message === 'string' && body.message.length > 0) {
                    return body.message;
                }
                const first = body.errors?.[0]?.message;
                if (typeof first === 'string' && first.length > 0) {
                    return first;
                }
            }

            if (typeof error.statusText === 'string' && error.statusText.length > 0) {
                return error.statusText;
            }
        }

        return 'The section could not be saved. Please try again.';
    }
}
