import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputSwitchModule } from 'primeng/inputswitch';

import { DotMessagePipe } from '@dotcms/ui';

import { ExistingContentStore } from '../../store/existing-content.store';
import { SearchComponent } from '../search/search.component';

@Component({
    selector: 'dot-relationship-header',
    imports: [DotMessagePipe, SearchComponent, InputSwitchModule, FormsModule],
    templateUrl: './header.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    styleUrl: './header.component.scss'
})
export class HeaderComponent {
    readonly store = inject(ExistingContentStore);
}
