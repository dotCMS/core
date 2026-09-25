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

import { catchError, finalize, take } from 'rxjs/operators';

import { DotContentTypeService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DOT_TOOLS_BASE_TYPES, DOT_TOOLS_DATA_VIEW_MODES } from '../constants/dot-tools.constants';
import {
    DotToolsCatalogEntry,
    DotToolsCustomToolConfig,
    DotToolsDataViewMode
} from '../models/dot-tools.models';
import { DotToolsService } from '../services/dot-tools.service';

interface DotToolsToolDialogData {
    tool?: DotToolsCatalogEntry;
    /** Optional prefill; tests inject one, production fetches it via the service. */
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
        ButtonModule,
        DotMessagePipe
    ],
    templateUrl: './dot-tools-tool-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotToolsToolDialogComponent implements OnInit {
    readonly ref = inject(DynamicDialogRef);
    readonly config = inject<DynamicDialogConfig<DotToolsToolDialogData>>(DynamicDialogConfig);

    readonly #fb = inject(FormBuilder);
    readonly #contentTypeService = inject(DotContentTypeService);
    readonly #toolsService = inject(DotToolsService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    protected readonly form = this.#fb.nonNullable.group({
        portletName: ['', Validators.required],
        portletId: ['', Validators.required],
        baseTypes: [<DotCMSBaseTypesContentTypes[]>[DotCMSBaseTypesContentTypes.CONTENT]],
        contentTypes: [<string[]>[]],
        dataViewMode: [<DotToolsDataViewMode>'list', Validators.required]
    });

    protected readonly baseTypes = DOT_TOOLS_BASE_TYPES;
    protected readonly viewModes = DOT_TOOLS_DATA_VIEW_MODES;
    protected readonly $contentTypes = signal<ContentTypeOption[]>([]);
    protected readonly $contentTypesLoading = signal(true);
    protected readonly $prefillLoading = signal(false);
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
        const injectedPrefill = this.config.data?.prefill;

        if (tool) {
            this.isEdit = true;
            this.#idTouched = true;
            // Seed with what the catalog already knows so the dialog renders
            // immediately; the fetched prefill patches over on arrival.
            this.form.patchValue({
                portletName: injectedPrefill?.portletName ?? tool.title,
                portletId: injectedPrefill?.portletId ?? tool.id,
                baseTypes: injectedPrefill?.baseTypes ?? [DotCMSBaseTypesContentTypes.CONTENT],
                contentTypes: injectedPrefill?.contentTypes ?? [],
                dataViewMode: injectedPrefill?.dataViewMode ?? 'list'
            });

            if (!injectedPrefill && tool.isCustom) {
                this.#loadPrefill(tool.id);
            }
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

    #loadPrefill(portletId: string): void {
        this.$prefillLoading.set(true);

        this.#toolsService
            .getCustomTool(portletId)
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);

                    return EMPTY;
                }),
                finalize(() => this.$prefillLoading.set(false))
            )
            .subscribe((config) => {
                this.form.patchValue({
                    portletName: config.portletName,
                    portletId: config.portletId,
                    baseTypes: config.baseTypes,
                    contentTypes: config.contentTypes,
                    dataViewMode: config.dataViewMode
                });
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
