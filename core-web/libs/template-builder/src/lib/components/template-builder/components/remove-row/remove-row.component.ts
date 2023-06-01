import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dotcms-remove-row',
    templateUrl: './remove-row.component.html',
    styleUrls: ['./remove-row.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true
})
export class RemoveRowComponent {}
