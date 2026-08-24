import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
    selector: 'dot-locales-shell',
    imports: [RouterOutlet],
    templateUrl: './dot-locales-shell.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './dot-locales-shell.component.scss'
})
export class DotLocalesShellComponent {}
