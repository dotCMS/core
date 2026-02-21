import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotTag } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe, DotSiteComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-tags-create',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        InputTextModule,
        ButtonModule,
        DotSiteComponent,
        DotMessagePipe,
        DotFieldRequiredDirective
    ],
    templateUrl: './dot-tags-create.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTagsCreateComponent implements OnInit {
    readonly ref = inject(DynamicDialogRef);
    readonly config = inject(DynamicDialogConfig<{ tag?: DotTag }>);

    private readonly fb = inject(FormBuilder);

    readonly form = this.fb.group({
        name: ['', Validators.required],
        siteId: ['']
    });

    isEdit = false;

    ngOnInit(): void {
        const tag = this.config.data?.tag;
        if (tag) {
            this.isEdit = true;
            this.form.patchValue({
                name: tag.label,
                siteId: tag.siteId
            });
        }
    }

    onSubmit(): void {
        if (this.form.valid) {
            this.ref.close(this.form.value);
        }
    }
}
