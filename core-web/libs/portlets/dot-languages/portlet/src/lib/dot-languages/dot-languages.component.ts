import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'lib-dot-languages',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-languages.component.html',
    styleUrl: './dot-languages.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLanguagesComponent {}
