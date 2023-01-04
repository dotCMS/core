import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    Input,
    ViewChild,
    ElementRef,
    ChangeDetectorRef
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

const regexURL =
    '^((http|https)://)[-a-zA-Z0-9@:%._\\+~#?&//=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%._\\+~#?&//=]*)$';

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
            requestAnimationFrame(() => {
                this.resetForm();
                this.setInpuFocus();
            });
        }
    }

    form: FormGroup;

    constructor(private fb: FormBuilder, private cd: ChangeDetectorRef) {
        this.form = this.fb.group({
            url: ['', [Validators.required, Validators.pattern(regexURL)]]
        });
    }

    onSubmit({ url }) {
        this.save.emit(url);
    }

    setInpuFocus() {
        this.input.nativeElement.focus();
        this.cd.markForCheck();
    }

    resetForm() {
        this.form.reset();
        this.form.markAsPristine();
    }
}
