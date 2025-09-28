import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { DotUVENavigationBarComponent } from './components/dot-uve-navigation-bar/dot-uve-navigation-bar.component';

@Component({
    selector: 'dot-uve-shell',
    templateUrl: './dot-uve-shell.component.html',
    styleUrls: ['./dot-uve-shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [RouterOutlet, DotUVENavigationBarComponent]
})
export class DotUVEShellComponent {}
