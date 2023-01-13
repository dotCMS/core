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
    selector: 'dot-insert-external-image',
    templateUrl: './dot-insert-external-image.component.html',
    styleUrls: ['./dot-insert-external-image.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotInsertExternalImageComponent {
    @ViewChild('input') input!: ElementRef;
    @Output() addImage = new EventEmitter();

    form: FormGroup;

    constructor(private fb: FormBuilder) {
        this.form = this.fb.group({
            url: ['', [Validators.required, Validators.pattern(regexURL)]]
        });

        requestAnimationFrame(() => this.input.nativeElement.focus());
    }

    onSubmit({ url }) {
        this.addImage.emit(url);
    }
}
