import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';

import { DotContentTypeService } from '@dotcms/data-access';

@Component({
    selector: 'dot-ema-form-selector',
    standalone: true,
    imports: [CommonModule, TableModule, ButtonModule],
    templateUrl: './ema-form-selector.component.html',
    styleUrls: ['./ema-form-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DotContentTypeService]
})
export class EmaFormSelectorComponent {
    private readonly contentTypesService = inject(DotContentTypeService);

    data = this.contentTypesService.getByTypes('form');
}
