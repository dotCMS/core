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

import { DOT_TOOLS_SECTION_ICONS } from '../constants/dot-tools.constants';
import { DotToolsSection } from '../models/dot-tools.models';

interface DotToolsSectionDialogData {
    section?: DotToolsSection;
}

interface IconOption {
    id: string;
    label: string;
}

@Component({
    selector: 'dot-tools-section-dialog',
    standalone: true,
    imports: [ReactiveFormsModule, InputTextModule, SelectModule, ButtonModule],
    templateUrl: './dot-tools-section-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotToolsSectionDialogComponent implements OnInit {
    readonly ref = inject(DynamicDialogRef);
    readonly config = inject<DynamicDialogConfig<DotToolsSectionDialogData>>(DynamicDialogConfig);

    readonly #fb = inject(FormBuilder);

    protected readonly form = this.#fb.nonNullable.group({
        name: ['', Validators.required],
        icon: ['widgets', Validators.required]
    });

    protected readonly icons: IconOption[] = DOT_TOOLS_SECTION_ICONS.map((id) => ({
        id,
        label: id.replace(/_/g, ' ')
    }));

    // Flipped by onSubmit; drives when errors become visible on untouched
    // fields (per-field messages, red outlines, and the footer warning).
    protected readonly $submitted = signal(false);

    // Field-level guard used by both the red-outline binding and the inline
    // "A name is required." message, so both flip on together.
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

        if (this.form.invalid) {
            this.form.markAllAsTouched();

            return;
        }

        this.ref.close(this.form.getRawValue());
    }

    protected onCancel(): void {
        this.ref.close();
    }
}
