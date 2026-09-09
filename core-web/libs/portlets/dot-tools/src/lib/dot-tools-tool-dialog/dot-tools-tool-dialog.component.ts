import { EMPTY } from 'rxjs';

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
import { MultiSelectModule } from 'primeng/multiselect';
import { SelectButtonModule } from 'primeng/selectbutton';

import { catchError, take } from 'rxjs/operators';

import { DotContentTypeService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

import { DOT_TOOLS_BASE_TYPES, DOT_TOOLS_DATA_VIEW_MODES } from '../constants/dot-tools.constants';
import {
    DotToolsCatalogEntry,
    DotToolsCustomToolConfig,
    DotToolsDataViewMode
} from '../models/dot-tools.models';

interface DotToolsToolDialogData {
    tool?: DotToolsCatalogEntry;
    prefill?: DotToolsCustomToolConfig;
}

interface ContentTypeOption {
    variable: string;
    label: string;
}

@Component({
    selector: 'dot-tools-tool-dialog',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        InputTextModule,
        MultiSelectModule,
        SelectButtonModule,
        ButtonModule
    ],
    templateUrl: './dot-tools-tool-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotToolsToolDialogComponent implements OnInit {
    readonly ref = inject(DynamicDialogRef);
    readonly config = inject<DynamicDialogConfig<DotToolsToolDialogData>>(DynamicDialogConfig);

    readonly #fb = inject(FormBuilder);
    readonly #contentTypeService = inject(DotContentTypeService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    protected readonly form = this.#fb.nonNullable.group({
        portletName: ['', Validators.required],
        portletId: ['', Validators.required],
        baseTypes: [<DotCMSBaseTypesContentTypes[]>[DotCMSBaseTypesContentTypes.CONTENT]],
        contentTypes: [<string[]>[]],
        dataViewMode: [<DotToolsDataViewMode>'List', Validators.required]
    });

    protected readonly baseTypes = DOT_TOOLS_BASE_TYPES;
    protected readonly viewModes = DOT_TOOLS_DATA_VIEW_MODES;
    protected readonly $contentTypes = signal<ContentTypeOption[]>([]);
    protected readonly $contentTypesLoading = signal(true);

    // See sibling comment in dot-tools-section-dialog: flipped by onSubmit
    // and drives error visibility on untouched fields.
    protected readonly $submitted = signal(false);

    protected readonly $nameShowsError = computed(() => {
        const control = this.form.controls.portletName;

        return control.invalid && (control.touched || this.$submitted());
    });

    protected readonly $idShowsError = computed(() => {
        const control = this.form.controls.portletId;

        return control.invalid && (control.touched || this.$submitted());
    });

    protected isEdit = false;

    // Auto-slug the portlet id from the name until the user types their own.
    // Once they touch the id field, we stop overwriting it — a specific id may
    // be a hard integration dependency, per the design's help text.
    #idTouched = false;

    ngOnInit(): void {
        const tool = this.config.data?.tool;
        const prefill = this.config.data?.prefill;

        if (tool) {
            this.isEdit = true;
            this.form.patchValue({
                portletName: prefill?.portletName ?? tool.title,
                portletId: prefill?.portletId ?? tool.id,
                baseTypes: prefill?.baseTypes ?? [DotCMSBaseTypesContentTypes.CONTENT],
                contentTypes: prefill?.contentTypes ?? [],
                dataViewMode: prefill?.dataViewMode ?? 'List'
            });
            this.#idTouched = true;
        }

        this.#loadContentTypes();
    }

    protected onNameChange(name: string): void {
        if (this.#idTouched) {
            return;
        }

        this.form.controls.portletId.setValue(this.#slugify(name), { emitEvent: false });
    }

    protected onIdTouched(): void {
        this.#idTouched = true;
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

    #loadContentTypes(): void {
        this.#contentTypeService
            .getContentTypes({ per_page: 1000 })
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    this.$contentTypesLoading.set(false);

                    return EMPTY;
                })
            )
            .subscribe((types) => {
                const options = types
                    .map((type) => ({ variable: type.variable, label: type.name }))
                    .sort((a, b) => a.label.localeCompare(b.label));
                this.$contentTypes.set(options);
                this.$contentTypesLoading.set(false);
            });
    }

    #slugify(value: string): string {
        return value
            .toLowerCase()
            .trim()
            .replace(/[^a-z0-9]+/g, '-')
            .replace(/^-|-$/g, '');
    }
}
