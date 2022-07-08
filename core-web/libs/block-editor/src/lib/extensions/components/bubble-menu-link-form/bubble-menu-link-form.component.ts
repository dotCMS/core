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
import { take } from 'rxjs/operators';
import { OnInit } from '@angular/core';

@Component({
    selector: 'dotcms-bubble-menu-link-form',
    templateUrl: './bubble-menu-link-form.component.html',
    styleUrls: ['./bubble-menu-link-form.component.scss']
})
export class BubbleMenuLinkFormComponent implements OnInit {

    @ViewChild('input') input: ElementRef;
    @ViewChild('suggestions', { static: false }) suggestionsComponent: SuggestionsComponent;
    
    @Output() hideForm: EventEmitter<boolean> = new EventEmitter(false);
    @Output() removeLink: EventEmitter<boolean> = new EventEmitter(false);
    @Output() setLink: EventEmitter<{ link: string, blank: boolean }> = new EventEmitter();

    @Input() link = '';
    @Input() props = {
        link: '',
        blank: false
    };

    public items: DotMenuItem[] = [];
    private dotLangs: Languages;

    constructor(private suggestionService: SuggestionsService,  private dotLanguageService: DotLanguageService) {/* */}

    ngOnInit() {
        this.dotLanguageService
        .getLanguages()
        .pipe(take(1))
        .subscribe((dotLang) => (this.dotLangs = dotLang));
    }

    addLink() {
        this.setLink.emit( this.props );
    }

    getContentlets() {
        if(this.props.link.length < 3) {
            return;
        }
        this.suggestionService
            .getContentletsUrlMap({ query: this.props.link })
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

    copyLink() {
        navigator.clipboard
            .writeText(this.link)
            .then(() => this.hideForm.emit(true))
            .catch(() => alert('Could not copy link'));
    }

    focusInput() {
        this.input.nativeElement.focus();
    }

    onSelection({ payload }: SuggestionsCommandProps) {
        this.props.link = payload.url;
        this.addLink();
    }

    onKeyDownEvent(e:KeyboardEvent) {
        switch (e.key) {
            case 'Escape':
                this.hideForm.emit(true);
                break;
            case "ArrowUp":
                this.suggestionsComponent.updateSelection(e);
                break;
            case "ArrowDown":
                this.suggestionsComponent.updateSelection(e);
                break;
        }
    }

    private getContentletLanguage(languageId: number): string {
        const { languageCode, countryCode } = this.dotLangs[languageId];

        if (!languageCode || !countryCode) {
            return '';
        }

        return `${languageCode}-${countryCode}`;
    }
}
