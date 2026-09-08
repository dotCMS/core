import {
    Component,
    EventEmitter,
    NgModule,
    Output,
    ChangeDetectionStrategy,
    input
} from '@angular/core';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'p-splitButton',
    template: `
        <div class="p-splitbutton">
            <button (click)="onClick.emit()"></button>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class SplitButtonMockComponent {
    // eslint-disable-next-line @angular-eslint/no-output-on-prefix
    @Output() onClick = new EventEmitter();
    readonly styleClass = input<string>();
    readonly model = input<[]>();
    readonly label = input<string>();
    readonly disabled = input<boolean>();
}

@NgModule({
    declarations: [SplitButtonMockComponent],
    exports: [SplitButtonMockComponent]
})
export class SplitButtonMockModule {}
