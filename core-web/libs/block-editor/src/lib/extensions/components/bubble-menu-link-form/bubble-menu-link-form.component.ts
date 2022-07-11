import {
    Component,
    ViewChild,
    ElementRef,
    EventEmitter,
    Output,
    Input
} from '@angular/core';
import { SuggestionsService } from '../../services/suggestions/suggestions.service';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMenuItem, SuggestionsCommandProps, SuggestionsComponent } from '../suggestions/suggestions.component';
import { DotLanguageService, Languages } from '../../services/dot-language/dot-language.service';
import { take, debounceTime } from 'rxjs/operators';
import { OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';

@Component({
    selector: 'dotcms-bubble-menu-link-form',
    templateUrl: './bubble-menu-link-form.component.html',
    styleUrls: ['./bubble-menu-link-form.component.scss']
})
export class BubbleMenuLinkFormComponent implements OnInit {
    
    @ViewChild('input') input: ElementRef;
    @ViewChild('suggestions', { static: false }) suggestionsComponent: SuggestionsComponent;
    
    @Output() hide: EventEmitter<boolean> = new EventEmitter(false);
    @Output() removeLink: EventEmitter<boolean> = new EventEmitter(false);
    @Output() submitForm: EventEmitter<{ link: string, blank: boolean }> = new EventEmitter();
    
    @Input() formPristine = true;
    @Input() initialValues: Record<string, unknown> = {
        link: '',
        blank: false
    };
    
    public items: DotMenuItem[] = [];
    public form: FormGroup;
    private dotLangs: Languages;

    constructor(
        private suggestionService: SuggestionsService,
        private dotLanguageService: DotLanguageService,
        private fb: FormBuilder
    ) {/* */}

    ngOnInit() {
        this.form = this.fb.group({...this.initialValues});

        this.dotLanguageService
        .getLanguages()
        .pipe(take(1))
        .subscribe((dotLang) => (this.dotLangs = dotLang));

        this.form.valueChanges
            .pipe(debounceTime(500))
            .subscribe( ({ link }) => {
                if(link.length < 3 || this.formPristine) {
                    this.formPristine = false;
                    return
                }
                this.setContentlets({ link });
            });
    }

    onSubmit(form) {
        this.submitForm.emit( form.value );
    }

    setContentlets({ link = ''}) {
        this.suggestionService
            .getContentletsUrlMap({ query: link })
            .subscribe((contentlets: DotCMSContentlet[]) => {
                this.items = contentlets.map((contentlet) => {
                    const { languageId } = contentlet;
                    contentlet.language = this.getContentletLanguage(languageId);
                    return {
                        label: contentlet.title,
                        icon: 'contentlet/image',
                        data: {
                            contentlet: contentlet
                        },
                        command: () => {
                            this.onSelection({
                                payload: contentlet,
                                type: {
                                    name: 'dotContent'
                                }
                            });
                        }
                    };
            })
        })

    }

    setFormValue({link, blank}: Record<string, unknown>) {
        this.form.setValue({ link, blank });
    }

    focusInput() {
        this.input.nativeElement.focus();
    }

    copyLink() {
        navigator.clipboard
            .writeText(this.initialValues.link as string)
            .then(() => this.hide.emit(true))
            .catch(() => alert('Could not copy link'));
    }

    onKeyDownEvent(e:KeyboardEvent) {
        switch (e.key) {
            case 'Escape':
                this.hide.emit(true);
                break;
            case 'Enter':
                this.suggestionsComponent?.execCommand();
                // prevent submit form
                return false;
            case "ArrowUp":
                this.suggestionsComponent?.updateSelection(e);
                break;
            case "ArrowDown":
                this.suggestionsComponent?.updateSelection(e);
                break;
        }
    }

    private onSelection({ payload: { url } }: SuggestionsCommandProps) {
        this.setFormValue({ ...this.form.value, link: url});
        this.submitForm.emit( this.form.value );
    }

    private getContentletLanguage(languageId: number): string {
        const { languageCode, countryCode } = this.dotLangs[languageId];

        if (!languageCode || !countryCode) {
            return '';
        }

        return `${languageCode}-${countryCode}`;
    }
}
