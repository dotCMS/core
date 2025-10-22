import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-dot-uve-palette',
    imports: [CommonModule],
    templateUrl: './dot-uve-palette.component.html',
    styleUrl: './dot-uve-palette.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteComponent {}
