import { NgIf } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    effect,
    EventEmitter,
    HostBinding,
    inject,
    Input,
    Output,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

@Component({
    standalone: true,
    selector: 'dot-language-selector',
    templateUrl: './dot-language-selector.component.html',
    imports: [DropdownModule, FormsModule, NgIf],
    providers: [DotLanguagesService],
    styleUrls: ['./dot-language-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLanguageSelectorComponent {
    @Input() value: DotLanguage;
    @Input() readonly: boolean;
    @Output() selected = new EventEmitter<DotLanguage>();
    @HostBinding('class.disabled') disabled: boolean;

    languagesList = signal<DotLanguage[]>([]);

    private dotLanguagesService = inject(DotLanguagesService);

    constructor() {
        // Todo: move all of this with Angular 17.1 to ue fromEffect and signal input
        effect(
            (onCleanup) => {
                const sub = this.dotLanguagesService
                    .getLanguagesUsedPage(this._pageId())
                    .subscribe((languages: DotLanguage[]) => {
                        this.languagesList.set(languages);
                    });

                onCleanup(() => sub.unsubscribe());
            },
            { allowSignalWrites: true }
        );
    }

    _pageId = signal<string | undefined>(undefined);

    @Input({ required: true }) set pageId(value: string) {
        this._pageId.set(value);
    }

    selectedItem(value: DotLanguage) {
        this.selected.emit(value);
    }
}
