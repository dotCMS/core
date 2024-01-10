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

import { take } from 'rxjs/operators';

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
    @Input() readonly: boolean; // ?
    @Output() selected = new EventEmitter<DotLanguage>();
    @HostBinding('class.disabled') disabled: boolean; // ?

    languagesList = signal<DotLanguage[]>([]);

    private dotLanguagesService = inject(DotLanguagesService);

    constructor() {
        effect((onCleanup) => {
            const sub = this.dotLanguagesService
                .getLanguagesUsedPage(this._pageId())
                .pipe(take(1))
                .subscribe((languages: DotLanguage[]) => {
                    this.languagesList.set(languages);
                });

            onCleanup(() => sub.unsubscribe());
        });
    }

    _pageId = signal<string | undefined>(undefined);

    @Input({ required: true }) set pageId(value: string) {
        this._pageId.set(value);
    }

    /**
     * Track changes in the dropdown
     * @param language
     */
    change(language: DotLanguage): void {
        this.selected.emit(language);
    }
}
