import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { FieldType } from '../fields/models';

@Component({
    selector: 'dot-convert-to-block-info',
    templateUrl: './dot-convert-to-block-info.component.html',
    standalone: true,
    host: {
        class: 'flex justify-center items-center gap-1 px-6 py-2 bg-primary-100/50 text-primary-900 rounded-sm'
    },
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [ButtonModule, DotMessagePipe]
})
export class DotConvertToBlockInfoComponent {
    // `input()` takes the initial value first and options second. These were written as
    // `input({ alias: '...' })`, which made the options object the *default value* and left the
    // inputs unaliased — so `[currentFieldType]`/`[currentField]` bound to nothing and
    // `$currentField()?.id` read `.id` off the options object, hiding the convert button.
    readonly $currentFieldType = input<FieldType | undefined>(undefined, {
        alias: 'currentFieldType'
    });
    readonly $action = output<MouseEvent>();
    readonly $currentField = input<DotCMSContentTypeField | undefined>(undefined, {
        alias: 'currentField'
    });
}
