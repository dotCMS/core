import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    HostBinding,
    inject,
    Input,
    OnChanges,
    Output,
    signal,
    SimpleChanges
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-language-selector',
    templateUrl: './dot-language-selector.component.html',
    imports: [DropdownModule, FormsModule],
    styleUrls: ['./dot-language-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLanguageSelectorComponent implements OnChanges {
    @Input() value: DotLanguage;
    @Input() readonly: boolean;
    @Output() selected = new EventEmitter<DotLanguage>();
    @HostBinding('class.disabled') disabled: boolean;

    languagesList = signal<DotLanguage[]>([]);

    private dotLanguagesService = inject(DotLanguagesService);

    _pageId = signal<string | undefined>(undefined);

    @Input({ required: true }) set pageId(value: string) {
        this._pageId.set(value);
    }

    selectedItem(value: DotLanguage) {
        this.selected.emit(value);
    }

    ngOnChanges(changes: SimpleChanges): void {
        const { value } = changes;
        if (value && value.currentValue) {
            this.dotLanguagesService
                .getLanguagesUsedPage(this._pageId())
                .subscribe((languages: DotLanguage[]) => {
                    this.languagesList.set(languages);
                });
        }
    }
}
