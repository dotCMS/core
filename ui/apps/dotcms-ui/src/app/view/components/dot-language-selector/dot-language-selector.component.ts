import {
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    HostBinding,
    SimpleChanges,
    OnChanges
} from '@angular/core';
import { take } from 'rxjs/operators';
import { DotLanguagesService } from '@services/dot-languages/dot-languages.service';
import { DotLanguage } from '@models/dot-language/dot-language.model';

@Component({
    selector: 'dot-language-selector',
    templateUrl: './dot-language-selector.component.html',
    styleUrls: ['./dot-language-selector.component.scss']
})
export class DotLanguageSelectorComponent implements OnInit, OnChanges {
    @Input() value: DotLanguage;
    @Output() selected = new EventEmitter<DotLanguage>();
    @HostBinding('class.disabled') disabled: boolean;

    options: DotLanguage[] = [];

    constructor(private dotLanguagesService: DotLanguagesService) {}

    ngOnInit() {
        this.loadOptions();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.contentInode && !changes.contentInode.firstChange) {
            this.loadOptions();
        }
    }

    /**
     * Track changes in the dropdown
     * @param DotLanguage language
     */
    change(language: DotLanguage): void {
        this.selected.emit(language);
    }

    private decorateLabels(languages: DotLanguage[]): DotLanguage[] {
        return languages.map((language: DotLanguage) => {
            const countryCodeLabel = language.countryCode ? ` (${language.countryCode})` : '';

            return {
                ...language,
                language: `${language.language}${countryCodeLabel}`
            };
        });
    }

    private loadOptions(): void {
        this.dotLanguagesService
            .get()
            .pipe(take(1))
            .subscribe(
                (languages: DotLanguage[]) => {
                    this.options = this.decorateLabels(languages);
                    this.disabled = this.options.length === 0;
                },
                () => {
                    this.disabled = true;
                }
            );
    }
}
