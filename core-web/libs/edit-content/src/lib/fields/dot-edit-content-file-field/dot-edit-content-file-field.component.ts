import { ChangeDetectionStrategy, Component, forwardRef, input, signal } from "@angular/core";
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from "@angular/forms";

import { DotCMSContentTypeField } from "@dotcms/dotcms-models";

@Component({
    selector: 'dot-edit-content-file-field',
    standalone: true,
    imports: [],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentFileFieldComponent)
        }
    ],
    templateUrl: './dot-edit-content-file-field.component.html',
    styleUrls: ['./dot-edit-content-file-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentFileFieldComponent implements ControlValueAccessor {

    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    private onChange: (value: string) => void;
    private onTouched: () => void;

    $value = signal('');

    writeValue(value: string): void {
        this.$value.set(value);
    }
    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }
    
}
