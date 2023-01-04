import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    Input,
    ViewChild,
    ElementRef
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
    selector: 'dot-input-tab',
    templateUrl: './dot-input-tab.component.html',
    styleUrls: ['./dot-input-tab.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotInputTabComponent {
    @ViewChild('input') input!: ElementRef;

    @Output() save = new EventEmitter();
    @Input() set reset(value) {
        if (value) {
            requestAnimationFrame(() => this.resetForm());
        }
    }

    regex =
        '^((http|https)://)[-a-zA-Z0-9@:%._\\+~#?&//=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%._\\+~#?&//=]*)$';
    form: FormGroup;

    constructor(private fb: FormBuilder) {
        this.form = this.fb.group({
            url: ['', [Validators.required, Validators.pattern(this.regex)]]
        });
    }

    onSubmit() {
        if (this.form.invalid) {
            return;
        }

        this.save.emit(this.form.get('url').value);
    }

    resetForm() {
        this.form.reset();
        this.input.nativeElement.focus();
    }
}
