import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DotMessagePipe } from '@dotcms/ui';

import { ExistingContentStore } from '../../store/existing-content.store';
import { SearchComponent } from '../search/search.component';

@Component({
    selector: 'dot-relationship-header',
    imports: [DotMessagePipe, SearchComponent, ToggleSwitchModule, FormsModule],
    templateUrl: './header.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    styleUrl: './header.component.scss'
})
export class HeaderComponent {
    readonly store = inject(ExistingContentStore);
}
