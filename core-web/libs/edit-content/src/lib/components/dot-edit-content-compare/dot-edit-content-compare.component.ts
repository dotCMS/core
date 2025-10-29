import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotContentCompareModule } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentStore } from '../../store/edit-content.store';

@Component({
    selector: 'dot-edit-content-compare',
    standalone: true,
    imports: [ButtonModule, DotContentCompareModule, DotMessagePipe],
    templateUrl: './dot-edit-content-compare.component.html',
    styleUrls: ['./dot-edit-content-compare.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentCompareComponent {
    readonly $store = inject(DotEditContentStore);
}
