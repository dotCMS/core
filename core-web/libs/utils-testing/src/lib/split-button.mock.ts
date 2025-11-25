import { Component, EventEmitter, Input, NgModule, Output } from '@angular/core';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'p-splitButton',
    template: `
        <div class="p-splitbutton">
            <button (click)="onClick.emit()"></button>
        </div>
    `
})
export class SplitButtonMockComponent {
    // eslint-disable-next-line @angular-eslint/no-output-on-prefix
    @Output() onClick = new EventEmitter();
    @Input() styleClass!: string;
    @Input() model!: [];
    @Input() label!: string;
    @Input() disabled!: boolean;
}

@NgModule({
    declarations: [SplitButtonMockComponent],
    exports: [SplitButtonMockComponent]
})
export class SplitButtonMockModule {}
