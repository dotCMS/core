import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { JsonPipe } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    model,
    Signal,
    signal,
    viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { AutoComplete, AutoCompleteModule, AutoCompleteSelectEvent } from 'primeng/autocomplete';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { TooltipModule } from 'primeng/tooltip';

import { take } from 'rxjs/operators';

import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotWysiwygMonacoComponent } from './components/dot-wysiwyg-monaco/dot-wysiwyg-monaco.component';
import { DotWysiwygTinymceComponent } from './components/dot-wysiwyg-tinymce/dot-wysiwyg-tinymce.component';
import {
    AvailableEditor,
    COMMENT_TINYMCE,
    DEFAULT_EDITOR,
    EditorOptions
} from './dot-edit-content-wysiwyg-field.constant';
import { shouldUseDefaultEditor } from './dot-edit-content-wysiwyg-field.utils';

interface LanguageVariable {
    key: string;
    value: string;
}

// Quantity of language variables to show in the autocomplete
const MAX_LANGUAGES_SUGGESTIONS = 20;

/**
 * Component representing a WYSIWYG (What You See Is What You Get) editor field for editing content in DotCMS.
 * Allows users to edit content using either the TinyMCE or Monaco editor, based on the content type and properties.
 */
@Component({
    selector: 'dot-edit-content-wysiwyg-field',
    standalone: true,
    imports: [
        FormsModule,
        DropdownModule,
        DotWysiwygTinymceComponent,
        DotWysiwygMonacoComponent,
        MonacoEditorModule,
        ConfirmDialogModule,
        AutoCompleteModule,
        DotMessagePipe,
        InputGroupModule,
        InputGroupAddonModule,
        TooltipModule,
        JsonPipe
    ],
    templateUrl: './dot-edit-content-wysiwyg-field.component.html',
    styleUrl: './dot-edit-content-wysiwyg-field.component.scss',
    host: {
        class: 'dot-wysiwyg__wrapper'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentWYSIWYGFieldComponent implements AfterViewInit {
    /**
     * Clear the autocomplete when the overlay is hidden.
     */
    onHideOverlay() {
        this.$autoComplete()?.clear();
    }

    /**
     * Signal to get the TinyMCE component.
     */
    $tinyMCEComponent: Signal<DotWysiwygTinymceComponent | undefined> = viewChild(
        DotWysiwygTinymceComponent
    );

    /**
     * Signal to get the Monaco component.
     */
    $monacoComponent: Signal<DotWysiwygMonacoComponent | undefined> =
        viewChild(DotWysiwygMonacoComponent);

    /**
     * Signal to get the autocomplete component.
     */
    $autoComplete = viewChild<AutoComplete>(AutoComplete);

    #confirmationService = inject(ConfirmationService);
    #dotMessageService = inject(DotMessageService);
    #dotLanguagesService = inject(DotLanguagesService);

    /**
     * This variable represents a required content type field in DotCMS.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * A required input representing a DotCMS contentlet.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });

    /**
     * Representing the currently selected editor.
     */
    $selectedEditorDropdown = model<AvailableEditor>();

    /**
     * A variable representing the currently selected language.
     */
    $selectedLanguageDropdown = model<string>(DEFAULT_EDITOR);

    /**
     * The signal representing the currently displayed editor in the application.
     * It holds an `AvailableEditor` type, which determines which editor is actively shown to the user.
     * The initial value is set to `DEFAULT_EDITOR`.
     */
    $displayedEditor = signal<AvailableEditor>(DEFAULT_EDITOR);

    /**
     * Computed property that determines the default editor used for a contentlet.
     *
     * It inspects the value of a specific field from the contentlet object and returns an
     * appropriate editor based on the content of the field.
     *
     */
    $contentEditorUsed = computed(() => {
        const content = this.$fieldContent();

        if (shouldUseDefaultEditor(content)) {
            return DEFAULT_EDITOR;
        }

        if (content.includes(COMMENT_TINYMCE)) {
            return AvailableEditor.TinyMCE;
        }

        return AvailableEditor.Monaco;
    });

    /**
     * Computed property that returns the content corresponding to the specified field value.
     * It retrieves the `variable` property from `$field` and uses it to access the corresponding
     * value in the `$contentlet`. If the `$contentlet` is null, it returns an empty string.
     *
     * @type {string}
     */
    $fieldContent = computed<string>(() => {
        const fieldValue = this.$field().variable;
        const contentlet = this.$contentlet();

        if (contentlet == null) {
            return '';
        }

        return contentlet[fieldValue] as string;
    });

    readonly editorTypes = AvailableEditor;
    readonly editorOptions = EditorOptions;

    /**
     * Signal to store the language variables, for later use in the autocomplete
     * depending of the search query.
     */
    $languageVariables = signal<LanguageVariable[]>([]);

    /**
     * Signal to store the selected item from the autocomplete.
     */
    $selectedItem = model<string>('');

    /**
     * Computed property to filter the language variables based on the search query.
     */
    $filteredSuggestions = computed(() => {
        const term = this.$selectedItem()?.toLowerCase();
        const languageVariables = this.$languageVariables();

        if (!term) {
            return [];
        }

        return languageVariables
            .filter((variable) => variable.key.toLowerCase().includes(term))
            .slice(0, MAX_LANGUAGES_SUGGESTIONS);
    });

    ngAfterViewInit(): void {
        // Assign the selected editor value
        this.$selectedEditorDropdown.set(this.$contentEditorUsed());
        // Editor showed
        this.$displayedEditor.set(this.$contentEditorUsed());
    }

    /**
     * Handles the editor change event by prompting the user for confirmation.
     * If accepted, the new editor is set. If rejected, the displayed editor remains unchanged.
     *
     * @param {AvailableEditor} newEditor - The editor to switch to.
     * @return {void} - This method does not return a value.
     */
    onEditorChange(newEditor: AvailableEditor) {
        const currentDisplayedEditor = this.$displayedEditor();
        const content = this.$fieldContent();

        if (content?.length > 0 && this.$displayedEditor() !== AvailableEditor.TinyMCE) {
            this.#confirmationService.confirm({
                header: this.#dotMessageService.get(
                    'edit.content.wysiwyg.confirm.switch-editor.header'
                ),
                message: this.#dotMessageService.get(
                    'edit.content.wysiwyg.confirm.switch-editor.message'
                ),
                rejectButtonStyleClass: 'p-button-text',
                acceptIcon: 'none',
                rejectIcon: 'none',
                accept: () => {
                    this.$displayedEditor.set(newEditor);
                },
                reject: () => {
                    this.$selectedEditorDropdown.set(currentDisplayedEditor);
                }
            });
        } else {
            this.$displayedEditor.set(newEditor);
        }
    }

    /**
     * Search for language variables.
     *
     * @return {void}
     */
    loadSuggestions() {
        if (this.$languageVariables().length === 0) {
            this.getLanguageVariables();
        } else {
            /**
             * Set the autocomplete loading to false and show the overlay if there are suggestions.
             * this for handle the bug in the autocomplete and show the loading icon when there are suggestions
             */
            const autocomplete = this.$autoComplete();
            if (autocomplete) {
                autocomplete.loading = false;
                if (this.$filteredSuggestions().length > 0) {
                    autocomplete.overlayVisible = true;
                }

                autocomplete.cd.markForCheck();
            }
        }
    }

    /**
     * Handles the selection of a language variable from the autocomplete.
     *
     * @param {AutoCompleteSelectEvent} $event - The event object containing the selected value.
     * @return {void}
     */
    onSelectLanguageVariable($event: AutoCompleteSelectEvent) {
        const { value } = $event;

        if (this.$displayedEditor() === AvailableEditor.TinyMCE) {
            const tinyMCE = this.$tinyMCEComponent();
            if (tinyMCE) {
                tinyMCE.insertContent(`$text.get('${value.key}')`);
                this.resetAutocomplete();
            } else {
                console.warn('TinyMCE component is not available');
            }
        } else if (this.$displayedEditor() === AvailableEditor.Monaco) {
            const monaco = this.$monacoComponent();
            if (monaco) {
                monaco.insertContent(`$text.get('${value.key}')`);
                this.resetAutocomplete();
            } else {
                console.warn('Monaco component is not available');
            }
        }
    }

    /**
     * Resets the autocomplete state.
     */
    private resetAutocomplete() {
        this.$autoComplete()?.clear();
    }

    /**
     * Fetches language variables from the DotCMS Languages API and formats them for use in the autocomplete.
     */
    getLanguageVariables() {
        // TODO: This is a temporary solution to get the language variables from the DotCMS Languages API.
        // We need a way to get the current language from the contentlet.
        this.#dotLanguagesService
            .getLanguageVariables()
            .pipe(take(1))
            .subscribe({
                next: (variables) => {
                    const formattedVariables = Object.entries(variables)
                        .map(([key, langObj]) => {
                            // Try to get the English value first
                            let value = langObj['en-us']?.value;

                            // If there is no English value, search for it in other languages
                            if (!value) {
                                for (const lang in langObj) {
                                    if (langObj[lang]?.value) {
                                        value = langObj[lang].value;
                                        break;
                                    }
                                }
                            }

                            // If there is no value, use the key
                            if (!value) {
                                value = key;
                            }

                            return { key, value };
                        })
                        .filter(
                            (variable) => variable.value !== null && variable.value !== undefined
                        );

                    this.$languageVariables.set(formattedVariables);
                },
                error: (error) => {
                    console.error('Error fetching language variables:', error);
                }
            });
    }
}
