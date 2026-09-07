import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
    selector: 'dot-auth-shell',
    imports: [RouterOutlet],
    templateUrl: './dot-auth-shell.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotAuthShellComponent {}
