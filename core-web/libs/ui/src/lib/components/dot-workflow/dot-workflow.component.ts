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
    forwardRef
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
    value = model<DotCMSContentType | null>(null);

    // ControlValueAccessor disabled state (can be set by form control)
    $isDisabled = signal<boolean>(false);

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
    }

    ngOnInit(): void {
        this.loadContentTypes();
    }

    onContentTypeChange(contentType: DotCMSContentType | null): void {
        this.value.set(contentType);
        this.onTouchedCallback();
        this.onChange.emit(contentType);
    }

    // ControlValueAccessor implementation
    writeValue(value: DotCMSContentType | null): void {
        this.value.set(value);
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
                },
                error: () => {
                    this.loading.set(false);
                }
            });
    }
}
