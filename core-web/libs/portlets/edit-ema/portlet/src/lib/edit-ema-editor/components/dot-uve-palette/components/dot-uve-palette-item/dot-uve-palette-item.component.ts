import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-dot-uve-palette-item',
    imports: [CommonModule],
    templateUrl: './dot-uve-palette-item.component.html',
    styleUrl: './dot-uve-palette-item.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteItemComponent {}
