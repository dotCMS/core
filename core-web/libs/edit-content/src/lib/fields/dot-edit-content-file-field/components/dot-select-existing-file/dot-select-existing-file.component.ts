import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotDataViewComponent } from './components/dot-dataview/dot-dataview.component';
import { DotSideBarComponent } from './components/dot-sidebar/dot-sidebar.component';

@Component({
    selector: 'dot-select-existing-file',
    standalone: true,
    imports: [DotSideBarComponent, DotDataViewComponent, ButtonModule],
    templateUrl: './dot-select-existing-file.component.html',
    styleUrls: ['./dot-select-existing-file.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSelectExistingFileComponent {}
