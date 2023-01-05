import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    ViewChild,
    ElementRef
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

    form: FormGroup;

    constructor(private fb: FormBuilder) {
        this.form = this.fb.group({
            url: ['', [Validators.required, Validators.pattern(regexURL)]]
        });

        requestAnimationFrame(() => this.input.nativeElement.focus());
    }

    onSubmit({ url }) {
        this.save.emit(url);
    }
}
