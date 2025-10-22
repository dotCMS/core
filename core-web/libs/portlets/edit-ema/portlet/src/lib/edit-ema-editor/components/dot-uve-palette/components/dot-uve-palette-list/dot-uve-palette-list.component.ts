import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-dot-uve-palette-list',
    imports: [CommonModule],
    templateUrl: './dot-uve-palette-list.component.html',
    styleUrl: './dot-uve-palette-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteListComponent {}
