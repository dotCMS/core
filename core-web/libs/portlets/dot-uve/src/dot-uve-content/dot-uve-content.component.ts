import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotUVEToolbarComponent } from './components/dot-uve-toolbar/dot-uve-toolbar.component';

@Component({
    selector: 'dot-uve-content',
    templateUrl: './dot-uve-content.component.html',
    styleUrls: ['./dot-uve-content.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotUVEToolbarComponent]
})
export class DotUVEContentComponent {}
