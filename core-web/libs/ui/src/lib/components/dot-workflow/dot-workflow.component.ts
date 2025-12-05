import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    model,
    OnInit,
    output,
    input,
    inject,
    signal,
    effect,
    forwardRef,
    computed
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';

import { SelectModule } from 'primeng/select';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-workflow',
    imports: [CommonModule, FormsModule, SelectModule],
    templateUrl: './dot-workflow.component.html',
    styleUrl: './dot-workflow.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotWorkflowComponent),
            multi: true
        }
    ]
})
export class DotWorkflowComponent implements OnInit, ControlValueAccessor {
    private contentTypeService = inject(DotContentTypeService);

    placeholder = input<string>('');
    disabled = input<boolean>(false);
    value = model<DotCMSContentType | null>(null);

    // ControlValueAccessor disabled state (can be set by form control)
    $isDisabled = signal<boolean>(false);

    // Combined disabled state (input disabled OR form control disabled)
    $disabled = computed(() => this.disabled() || this.$isDisabled());

    // Custom output for explicit change events
    onChange = output<DotCMSContentType | null>();

    contentTypes = signal<DotCMSContentType[]>([]);
    loading = signal<boolean>(false);

    // ControlValueAccessor callback functions
    private onChangeCallback = (_value: DotCMSContentType | null) => {
        // Implementation provided by registerOnChange
    };

    private onTouchedCallback = () => {
        // Implementation provided by registerOnTouched
    };

    constructor() {
        // Sync model signal changes with ControlValueAccessor
        effect(() => {
            const currentValue = this.value();
            this.onChangeCallback(currentValue);
        });

        // Watch for disabled state changes - if field becomes enabled, load all content types
        effect(() => {
            const isDisabled = this.$disabled();
            const hasContentTypes = this.contentTypes().length > 0;

            // If field becomes enabled and we don't have content types loaded, load them
            if (!isDisabled && !hasContentTypes && !this.loading()) {
                this.loadContentTypes();
            }
        });
    }

    ngOnInit(): void {
        // Only load all content types on init if not disabled
        // If disabled, we'll load them when the field becomes enabled (via effect)
        if (!this.$disabled()) {
            this.loadContentTypes();
        }
    }

    onContentTypeChange(contentType: DotCMSContentType | null): void {
        this.value.set(contentType);
        this.onTouchedCallback();
        this.onChange.emit(contentType);
    }

    // ControlValueAccessor implementation
    writeValue(value: DotCMSContentType | null): void {
        this.value.set(value);

        // If we have a value, ensure it's in the contentTypes array
        // This is especially important when the field is disabled
        if (value) {
            this.ensureContentTypeInList(value);
        }
    }

    /**
     * Ensures the given content type is in the contentTypes list.
     * Since writeValue receives the full DotCMSContentType object, we can just add it directly.
     *
     * @private
     * @param contentType The content type to ensure is in the list
     */
    private ensureContentTypeInList(contentType: DotCMSContentType): void {
        const currentContentTypes = this.contentTypes();
        const exists = currentContentTypes.some(
            (ct) => ct.id === contentType.id || ct.variable === contentType.variable
        );

        if (!exists) {
            // We have the full object, so just add it directly to the list
            this.contentTypes.set([...currentContentTypes, contentType]);
        }
    }

    registerOnChange(fn: (value: DotCMSContentType | null) => void): void {
        this.onChangeCallback = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.onTouchedCallback = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }

    private loadContentTypes(): void {
        if (this.loading()) {
            return;
        }

        this.loading.set(true);
        this.contentTypeService
            .getContentTypes({
                page: 100 // Request a large page size to get all content types
            })
            .subscribe({
                next: (contentTypes) => {
                    this.contentTypes.set(contentTypes);
                    this.loading.set(false);

                    // After loading, ensure current value is in the list
                    const currentValue = this.value();
                    if (currentValue) {
                        this.ensureContentTypeInList(currentValue);
                    }
                },
                error: () => {
                    this.loading.set(false);
                }
            });
    }
}
