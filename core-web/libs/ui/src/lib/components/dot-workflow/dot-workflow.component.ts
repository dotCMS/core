import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, model, OnInit, output, input, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectModule } from 'primeng/select';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-workflow',
    imports: [CommonModule, FormsModule, SelectModule],
    templateUrl: './dot-workflow.component.html',
    styleUrl: './dot-workflow.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotWorkflowComponent implements OnInit {
    private contentTypeService = inject(DotContentTypeService);

    disabled = input<boolean>(false);
    placeholder = input<string>('');
    value = model<DotCMSContentType | null>(null);

    onChange = output<DotCMSContentType | null>();

    contentTypes = signal<DotCMSContentType[]>([]);
    loading = signal<boolean>(false);

    ngOnInit(): void {
        this.loadContentTypes();
    }

    onContentTypeChange(contentType: DotCMSContentType | null): void {
        this.value.set(contentType);
        this.onChange.emit(contentType);
    }

    private loadContentTypes(): void {
        if (this.loading()) {
            return;
        }

        this.loading.set(true);
        this.contentTypeService
            .getContentTypes({
                page: 100 // Request a large page size to get all content types
            })
            .subscribe({
                next: (contentTypes) => {
                    this.contentTypes.set(contentTypes);
                    this.loading.set(false);
                },
                error: () => {
                    this.loading.set(false);
                }
            });
    }
}
