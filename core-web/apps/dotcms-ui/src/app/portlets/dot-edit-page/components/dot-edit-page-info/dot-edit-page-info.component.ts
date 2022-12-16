import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/**
 * Basic page information for edit mode
 *
 * @export
 * @class DotEditPageInfoComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-edit-page-info',
    templateUrl: './dot-edit-page-info.component.html',
    styleUrls: ['./dot-edit-page-info.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditPageInfoComponent {
    @Input() title: string;
    @Input() url: string;
    @Input() apiLink: string;
}
