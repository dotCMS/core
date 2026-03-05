import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Output, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-uve-form-selector',
    imports: [CommonModule, TableModule, ButtonModule, DotMessagePipe],
    templateUrl: './dot-uve-form-selector.component.html',
    styleUrls: ['./dot-uve-form-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DotContentTypeService]
})
export class DotUveFormSelectorComponent {
    @Output() selected = new EventEmitter<string>();

    private readonly contentTypesService = inject(DotContentTypeService);

    data = this.contentTypesService.getByTypes('form');
}
