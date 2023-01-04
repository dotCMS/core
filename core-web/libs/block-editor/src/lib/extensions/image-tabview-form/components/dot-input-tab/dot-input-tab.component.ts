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
import { FormBuilder, FormGroup } from '@angular/forms';

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
                this.input.nativeElement.value = '';
                this.input.nativeElement.focus();
            });
        }
    }

    form: FormGroup;

    constructor(private fb: FormBuilder, private readonly cd: ChangeDetectorRef) {
        this.form = this.fb.group({
            url: ['']
        });
    }

    onSubmit() {
        this.save.emit(this.form.get('url').value);
    }
}
