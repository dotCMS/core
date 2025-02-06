import { Component, EventEmitter, inject, Input, OnInit, Output } from '@angular/core';
import { DotAiService } from '@dotcms/data-access';
import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

@Component({
    selector: 'dot-ai-menu',
    template: `
        <p-menu #menu [model]="items" [popup]="true" [appendTo]="'body'" />
        <p-button
            #btn
            (onClick)="menu.toggle($event)"
            [text]="true"
            severity="secondary"
            class="ai-trigger-button"
            aria-label="AI Actions">
            <svg
                class="ai-icon"
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg">
                <path
                    d="M12.4797 14.2597L8.40384 15.5046C8.29811 15.5368 8.20529 15.6037 8.13926 15.6952C8.07323 15.7868 8.03755 15.8981 8.03755 16.0124C8.03755 16.1268 8.07323 16.238 8.13926 16.3296C8.20529 16.4211 8.29811 16.488 8.40384 16.5203L12.4797 17.7651L13.6792 21.9949C13.7103 22.1046 13.7747 22.2009 13.8629 22.2695C13.9511 22.338 14.0584 22.375 14.1685 22.375C14.2787 22.375 14.3859 22.338 14.4742 22.2695C14.5624 22.2009 14.6268 22.1046 14.6579 21.9949L15.8579 17.7651L19.9338 16.5203C20.0395 16.488 20.1323 16.4211 20.1983 16.3296C20.2644 16.238 20.3 16.1268 20.3 16.0124C20.3 15.8981 20.2644 15.7868 20.1983 15.6952C20.1323 15.6037 20.0395 15.5368 19.9338 15.5046L15.8579 14.2597L14.6579 10.0299C14.6268 9.92018 14.5624 9.82385 14.4742 9.75533C14.386 9.6868 14.2787 9.64977 14.1685 9.64977C14.0584 9.64977 13.9511 9.6868 13.8629 9.75533C13.7747 9.82385 13.7102 9.92018 13.6792 10.0299L12.4797 14.2597Z"
                    fill="currentColor" />
                <path
                    d="M17.466 6.66732L19.6771 5.99138C19.7828 5.95914 19.8756 5.89224 19.9417 5.8007C20.0077 5.70916 20.0434 5.59789 20.0434 5.48355C20.0434 5.3692 20.0077 5.25794 19.9417 5.16639C19.8756 5.07485 19.7828 5.00795 19.6771 4.97571L17.466 4.3003L16.8147 2.00513C16.7836 1.89541 16.7192 1.79908 16.631 1.73056C16.5427 1.66203 16.4355 1.625 16.3253 1.625C16.2151 1.625 16.1079 1.66203 16.0197 1.73056C15.9315 1.79908 15.867 1.89541 15.836 2.00513L15.1851 4.3003L12.9736 4.97571C12.8678 5.00794 12.775 5.07483 12.709 5.16638C12.643 5.25792 12.6073 5.3692 12.6073 5.48355C12.6073 5.5979 12.643 5.70917 12.709 5.80072C12.775 5.89226 12.8678 5.95915 12.9736 5.99138L15.1851 6.66732L15.836 8.96196C15.867 9.07168 15.9315 9.16801 16.0197 9.23653C16.1079 9.30506 16.2151 9.34209 16.3253 9.34209C16.4355 9.34209 16.5427 9.30506 16.631 9.23653C16.7192 9.16801 16.7836 9.07168 16.8147 8.96196L17.466 6.66732Z"
                    fill="currentColor" />
                <path
                    d="M6.24852 7.62823L4.01751 8.57256C3.92365 8.61231 3.84333 8.68016 3.78679 8.76748C3.73025 8.8548 3.70005 8.95761 3.70005 9.06282C3.70005 9.16802 3.73025 9.27083 3.78679 9.35815C3.84333 9.44547 3.92365 9.51333 4.01751 9.55307L6.24852 10.4974L7.15847 12.8127C7.19677 12.9101 7.26217 12.9935 7.34631 13.0522C7.43045 13.1109 7.52953 13.1422 7.6309 13.1422C7.73227 13.1422 7.83135 13.1109 7.91549 13.0522C7.99963 12.9935 8.06502 12.9101 8.10333 12.8127L9.01328 10.4974L11.2438 9.55307C11.3377 9.51332 11.418 9.44546 11.4745 9.35815C11.5311 9.27083 11.5613 9.16801 11.5613 9.06282C11.5613 8.95762 11.5311 8.8548 11.4745 8.76749C11.418 8.68017 11.3377 8.61231 11.2438 8.57256L9.01328 7.62823L8.10333 5.31342C8.06502 5.21602 7.99963 5.13266 7.91549 5.07399C7.83135 5.01531 7.73227 4.98397 7.6309 4.98397C7.52953 4.98397 7.43045 5.01531 7.34631 5.07399C7.26217 5.13266 7.19677 5.21602 7.15847 5.31342L6.24852 7.62823Z"
                    fill="currentColor" />
            </svg>
        </p-button>
    `,
    styles: [
        `
            :host {
                display: inline-flex;
                align-items: center;
            }
            :host ::ng-deep .ai-trigger-button {
                position: absolute;
                right: 0;
                top: 0;
                margin: 0;
                height: 2.5rem;
                border: 0 none;
            }
            :host ::ng-deep .ai-trigger-button .p-button {
                width: 2.5rem;
                height: 100%;
                padding: 0;
                border-top-left-radius: 0;
                border-bottom-left-radius: 0;
                color: var(--text-color-secondary);
                background: transparent;
                border: none;
                display: inline-flex;
                align-items: center;
                justify-content: center;
            }
            :host ::ng-deep .ai-trigger-button .p-button:hover {
                background: var(--surface-hover);
                color: var(--text-color);
            }
            :host ::ng-deep .ai-trigger-button .p-button:focus {
                box-shadow: none;
            }
            .ai-icon {
                width: 20px;
                height: 20px;
                flex-shrink: 0;
                color: var(--color-palette-primary-500);
            }
            :host ::ng-deep .p-menu {
                min-width: 200px;
            }
        `
    ],
    standalone: true,
    imports: [MenuModule, ButtonModule]
})
export class DotAiMenuComponent implements OnInit {
    @Input() text: string;
    items: MenuItem[] | undefined;

    @Output() textChanged = new EventEmitter<string>();

    #dotAiService = inject(DotAiService);

    ngOnInit() {
        this.items = [
            {
                label: 'REWRITE',
                items: [
                    {
                        label: 'Improve writing',
                        command: () => {
                            this.refineText('You are a text refiner. You will be given a text, a tone, and a language. You will need to refine the text to the tone and language.', 'formal', 'en');
                        }
                    },
                    {
                        label: 'Fix spelling and grammar',
                        command: () => {
                            this.refineText('You are an expert proofreader. You will be given a text, and you will need to fix the spelling and grammar.', 'formal', 'en');
                        }
                    },
                    {
                        label: 'Convert to English (US)',
                        command: () => {
                            this.refineText('You are an expert translator. You will be given a text, and you will need to convert it to US English.', 'formal', 'en');
                        }
                    },
                    {
                        label: 'Simplify language',
                        command: () => {
                            this.refineText('You are an expert simplifier. You will be given a text, and you will need to simplify it.', 'formal', 'en');
                        }
                    }
                ]
            },
            {
                label: 'TONE',
                items: [
                    {
                        label: 'Write in friendly tone',
                        command: () => {
                            this.refineText('You are an expert writer. You will be given a text, and you will need to write it in a friendly tone.', 'friendly', 'en');
                        }
                    },
                    {
                        label: 'Write in casual tone',
                        command: () => {
                            this.refineText('You are an expert writer. You will be given a text, and you will need to write it in a casual tone.', 'casual', 'en');
                        }
                    },
                    {
                        label: 'Write in professional tone',
                        command: () => {
                            this.refineText('You are an expert writer. You will be given a text, and you will need to write it in a professional tone.', 'professional', 'en');
                        }
                    }
                ]
            },
            {
                label: 'GENERATE',
                items: [
                    {
                        label: 'Generate SEO friendly title',
                        command: () => {
                            this.refineText('You are an expert SEO writer. You will be given a text, and you will need to generate a SEO friendly title.', 'formal', 'en');
                        }
                    },
                    {
                        label: 'Generate catchy headline',
                        command: () => {
                            this.refineText('You are an expert headline writer. You will be given a text, and you will need to generate a catchy headline.', 'formal', 'en');
                        }
                    },
                    {
                        label: 'Generate URL friendly title',
                        command: () => {
                            this.refineText('You are an expert URL title writer. You will be given a text, and you will need to generate a URL friendly title.', 'formal', 'en');
                        }
                    }
                ]
            }
        ];
    }

    refineText(system: string, tone: string, language: string) {
        this.#dotAiService.refineText({
            system: system,
            text: this.text,
            tone: tone,
            language: language
        })
        .subscribe((response: any) => {
            this.textChanged.emit(response.title);
        });
    }
}
