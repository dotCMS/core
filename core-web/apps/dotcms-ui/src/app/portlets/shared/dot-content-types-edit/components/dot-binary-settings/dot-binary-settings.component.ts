import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-dot-binary-settings',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-binary-settings.component.html',
    styleUrl: './dot-binary-settings.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinarySettingsComponent {}
