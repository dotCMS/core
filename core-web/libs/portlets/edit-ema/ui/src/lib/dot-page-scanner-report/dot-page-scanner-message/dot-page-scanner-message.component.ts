import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
    selector: 'dot-page-scanner-message',
    standalone: true,
    templateUrl: './dot-page-scanner-message.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPageScannerMessageComponent {
    icon = input.required<string>();
    title = input.required<string>();
    description = input.required<string>();
}
