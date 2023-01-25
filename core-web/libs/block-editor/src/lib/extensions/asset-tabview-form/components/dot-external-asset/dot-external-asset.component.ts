import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
    ViewChild,
    ElementRef
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

const regexURL =
    '^((http|https)://)[-a-zA-Z0-9@:%._\\+~#?&//=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%._\\+~#?&//=]*)$';

@Component({
    selector: 'dot-external-asset',
    templateUrl: './dot-external-asset.component.html',
    styleUrls: ['./dot-external-asset.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExternalAssetComponent {
    @ViewChild('input') input!: ElementRef;
    @Input() assetType = 'Asset';
    @Output() assetURL = new EventEmitter();

    form: FormGroup;

    constructor(private fb: FormBuilder) {
        this.form = this.fb.group({
            url: ['', [Validators.required, Validators.pattern(regexURL)]]
        });

        requestAnimationFrame(() => this.input.nativeElement.focus());
    }

    onSubmit({ url }) {
        this.assetURL.emit(url);
    }
}
