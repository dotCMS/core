import { Component, Input } from '@angular/core';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-image-card',
    templateUrl: './dot-image-card.component.html',
    styleUrls: ['./dot-image-card.component.scss']
})
export class DotImageCardComponent {
    @Input() contentlet: DotCMSContentlet;
}
