import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';
//

@Component({
    selector: 'dot-input-tab',
    templateUrl: './dot-input-tab.component.html',
    styleUrls: ['./dot-input-tab.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotInputTabComponent {
    @Output() save = new EventEmitter();
    public value = '';

    onSubmit() {
        this.save.emit(this.value);
        this.value = '';
    }
}
