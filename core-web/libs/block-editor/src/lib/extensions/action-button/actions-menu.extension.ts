import { PluginKey } from 'prosemirror-state';
import { Observable, Subject } from 'rxjs';
import tippy, { GetReferenceClientRect, Instance } from 'tippy.js';

import { ComponentRef, ViewContainerRef } from '@angular/core';

import { filter, take } from 'rxjs/operators';

import { Editor, Extension, Range } from '@tiptap/core';
import { FloatingMenuPluginProps } from '@tiptap/extension-floating-menu';
import { Level } from '@tiptap/extension-heading';
import Suggestion, { SuggestionOptions, SuggestionProps } from '@tiptap/suggestion';

import { Action, RemoteCustomExtensions } from '@dotcms/dotcms-models';

import {
    clearFilter,
    CONTENT_SUGGESTION_ID,
    DotMenuItem,
    findParentNode,
    FLOATING_ACTIONS_MENU_KEYBOARD,
    FloatingActionsKeydownProps,
    FloatingActionsPlugin,
    FloatingActionsProps,
    getEditorElement,
    ItemsType,
    suggestionOptions,
    SuggestionPopperModifiers,
    SuggestionsCommandProps,
    SuggestionsComponent
} from '../../shared';
import { NodeTypes } from '../../shared/utils';
import { AI_CONTENT_PROMPT_EXTENSION_NAME } from '../ai-content-prompt/ai-content-prompt.extension';
import { AI_IMAGE_PROMPT_EXTENSION_NAME } from '../ai-image-prompt/ai-image-prompt.extension';
import { BubbleFormValue, BubbleFormValues } from '../bubble-form/model';

const AI_BLOCK_EXTENSIONS_IDS = [AI_CONTENT_PROMPT_EXTENSION_NAME, AI_IMAGE_PROMPT_EXTENSION_NAME];
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        actionsMenu: {
            /**
             * Add Heading
             */
            addHeading: (attr: {
                range: Range;
                type: { name: string; level?: number };
            }) => ReturnType;
            addContentletBlock: ({
                range,
                payload
            }: {
                range: Range;
                payload: unknown;
            }) => ReturnType;
            addNextLine: () => ReturnType;
        };
    }
}

export type FloatingMenuOptions = Omit<FloatingMenuPluginProps, 'editor' | 'element'> & {
    element: HTMLElement | null;
    suggestion: Omit<SuggestionOptions, 'editor'>;
};

/**
 * A menu entry contributed by a remote custom block. `DotMenuItem` leaves `id` and
 * `commandKey` optional, but `mapCustomActions` always fills both and the dispatch below
 * indexes on them, so they are required here.
 */
type CustomActionItem = DotMenuItem & { id: string; commandKey: string };

function getTippyInstance({
    element,
    content,
    rect,
    onHide
}: {
    element: Element;
    content: Element;
    rect: GetReferenceClientRect;
    onHide?: () => void;
}) {
    return tippy(element, {
        content: content,
        placement: 'bottom',
        popperOptions: {
            modifiers: SuggestionPopperModifiers
        },
        getReferenceClientRect: rect,
        showOnCreate: true,
        interactive: true,
        offset: [120, 10],
        trigger: 'manual',
        maxWidth: 'none',
        onHide
    });
}

function execCommand({
    editor,
    range,
    props,
    customBlocks
}: {
    editor: Editor;
    range: Range;
    props: SuggestionsCommandProps;
    customBlocks: RemoteCustomExtensions;
}) {
    const { type, payload } = props;
    // Indexed by node name and by the ids remote blocks contribute, so it is a lookup table
    // rather than a fixed-shape object. The built-in entries below are still exhaustive.
    const whatToDo: Record<string, () => void> = {
        dotContent: () => {
            editor.chain().addContentletBlock({ range, payload }).addNextLine().run();
        },
        heading: () => {
            editor.chain().addHeading({ range, type }).run();
        },
        table: () => {
            // `openForm` is declared `any` because a Subject-returning command cannot satisfy
            // TipTap's command contract; narrow it here, where the values are actually read.
            const tableForm$: Observable<BubbleFormValue> = editor.commands.openForm(
                [
                    {
                        key: 'rows',
                        label: 'Rows',
                        required: true,
                        value: '3',
                        controlType: 'number',
                        type: 'number',
                        min: 1
                    },
                    {
                        key: 'columns',
                        label: 'Columns',
                        required: true,
                        value: '3',
                        controlType: 'number',
                        type: 'number',
                        min: 1
                    },
                    {
                        key: 'header',
                        label: 'Add Row Header',
                        required: false,
                        value: true,
                        controlType: 'text',
                        type: 'checkbox'
                    }
                ],
                { customClass: 'dotTableForm' }
            );

            tableForm$
                .pipe(
                    take(1),
                    filter((value): value is BubbleFormValues => !!value)
                )
                .subscribe((value) => {
                    requestAnimationFrame(() => {
                        editor
                            .chain()
                            .insertTable({
                                // The number controls emit their value as a string.
                                rows: Number(value['rows']),
                                cols: Number(value['columns']),
                                withHeaderRow: !!value['header']
                            })
                            .focus()
                            .run();
                    });
                });
        },
        orderedList: () => {
            editor.chain().deleteRange(range).toggleOrderedList().focus().run();
        },
        bulletList: () => {
            editor.chain().deleteRange(range).toggleBulletList().focus().run();
        },
        blockquote: () => {
            editor.chain().deleteRange(range).setBlockquote().focus().run();
        },
        codeBlock: () => {
            editor.chain().deleteRange(range).setCodeBlock().focus().run();
        },
        horizontalRule: () => {
            editor.chain().deleteRange(range).setHorizontalRule().focus().run();
        },
        image: () => editor.commands.openAssetForm({ type: 'image' }),
        subscript: () => editor.chain().setSubscript().focus().run(),
        superscript: () => editor.chain().setSuperscript().focus().run(),
        video: () => editor.commands.openAssetForm({ type: 'video' }),
        audio: () => editor.commands.openAssetForm({ type: 'audio' }),
        aiContentPrompt: () => editor.commands.openAIPrompt(),
        aiContent: () => editor.commands.insertAINode(),
        aiImagePrompt: () => editor.commands.openImagePrompt(),
        gridBlock: () => {
            editor.chain().deleteRange(range).insertGridBlock().focus().run();
        }
    };

    getCustomActions(customBlocks).forEach((option) => {
        whatToDo[option.id] = () => {
            try {
                // Remote blocks register their commands at runtime, so the key is not in
                // `SingleCommands`. The try/catch below is what handles a missing one.
                (editor.commands as unknown as Record<string, () => void>)[option.commandKey]();
            } catch {
                console.warn(`Custom command ${option.commandKey} does not exists.`);
            }
        };
    });

    whatToDo[type.name]
        ? whatToDo[type.name]()
        : editor.chain().setTextSelection(range).focus().run();
}

function mapCustomActions(actions: Action[]): CustomActionItem[] {
    return actions.map((action) => ({
        icon: action.icon,
        label: action.menuLabel,
        commandKey: action.command,
        id: `${action.command}-id`
    }));
}

function getCustomActions(customBlocks: RemoteCustomExtensions): CustomActionItem[] {
    return customBlocks.extensions
        .map((extension) => mapCustomActions(extension.actions || []))
        .flat();
}

export const ActionsMenu = (
    viewContainerRef: ViewContainerRef,
    customBlocks: RemoteCustomExtensions,
    disabledExtensions: { shouldShowAIExtensions: boolean | unknown }
) => {
    let myTippy: Instance | undefined;
    // Null between `onExit` and the next `onStart`; every read below is either inside a
    // callback that only runs while the menu is open, or guarded.
    let suggestionsComponent: ComponentRef<SuggestionsComponent> | null = null;
    const suggestionKey = new PluginKey('suggestionPlugin');
    const destroy$: Subject<boolean> = new Subject<boolean>();
    let shouldShow = true;

    /**
     * Get's called on button click or suggestion char
     *
     * @param {(SuggestionProps | FloatingActionsProps)} { editor, range, clientRect }
     */
    function onStart({ editor, range, clientRect }: SuggestionProps | FloatingActionsProps): void {
        if (shouldShow) {
            setUpSuggestionComponent(editor, range);

            const element = getEditorElement(editor)?.parentElement;

            if (!element || !clientRect || !suggestionsComponent) {
                return;
            }

            myTippy = getTippyInstance({
                element,
                content: suggestionsComponent.location.nativeElement,
                // TipTap's `clientRect` may return null; tippy's `GetReferenceClientRect` does
                // not allow that. Cast rather than substitute a rect, so runtime is unchanged.
                rect: clientRect as GetReferenceClientRect,
                onHide: () => {
                    editor.commands.focus();
                    const queryRange = updateQueryRange({ editor, range });
                    const text = editor.state.doc.textBetween(queryRange.from, queryRange.to, ' ');
                    if (text === '/') {
                        editor.commands.deleteRange(queryRange);
                    }

                    const transaction = editor.state.tr.setMeta(FLOATING_ACTIONS_MENU_KEYBOARD, {
                        open: false
                    });
                    editor.view.dispatch(transaction);
                    editor.commands.freezeScroll(false);
                }
            });
        }
    }

    function onBeforeStart({ editor }: { editor: Editor }): void {
        editor.commands.freezeScroll(true);

        const isCodeBlock =
            findParentNode(editor.view.state.selection.$from, [NodeTypes.CODE_BLOCK])?.type.name ===
            NodeTypes.CODE_BLOCK;

        shouldShow = !isCodeBlock;
    }

    function setUpSuggestionComponent(editor: Editor, range: Range) {
        const { allowedBlocks, allowedContentTypes, lang, contentletIdentifier } =
            editor.storage.dotConfig;

        const editorAllowedBlocks = allowedBlocks.length > 1 ? allowedBlocks : [];
        const items = getItems({ allowedBlocks: editorAllowedBlocks, editor, range });

        suggestionsComponent = viewContainerRef.createComponent(SuggestionsComponent);

        // Setting Inputs
        suggestionsComponent.instance.items = items;
        suggestionsComponent.instance.currentLanguage = lang;
        suggestionsComponent.instance.allowedContentTypes = allowedContentTypes;
        suggestionsComponent.instance.contentletIdentifier = contentletIdentifier;

        suggestionsComponent.instance.onSelectContentlet = (props) => {
            clearFilter({ type: ItemsType.CONTENT, editor, range, suggestionKey, ItemsType });
            onSelection({ editor, range, props });
        };

        // Needs to be called after settings the component Inputs
        // To avoid calling the `onInit` hook before the Inputs are initialized
        suggestionsComponent.changeDetectorRef.detectChanges();

        if (allowedBlocks.length <= 1 || allowedBlocks.includes(CONTENT_SUGGESTION_ID)) {
            suggestionsComponent.instance.addContentletItem();
        }
    }

    /**
     * Retrieves the items for the given parameters.
     *
     * @param {object} options - The options for retrieving the items.
     * @param {string[]} options.allowedBlocks - The array of allowed block IDs.
     * @param {object} options.editor - The editor object.
     * @param {object} options.range - The range object.
     * @return {DotMenuItem[]} - The array of DotMenuItem objects.
     */
    function getItems({
        allowedBlocks = [],
        editor,
        range
    }: {
        allowedBlocks?: string[];
        editor: Editor;
        range: Range;
    }): DotMenuItem[] {
        let filteredSuggestionOptions: DotMenuItem[] = [...suggestionOptions];

        if (!disabledExtensions?.shouldShowAIExtensions) {
            filteredSuggestionOptions = suggestionOptions.filter(
                (item) => !item.id || !AI_BLOCK_EXTENSIONS_IDS.includes(item.id)
            );
        }

        const items = allowedBlocks.length
            ? filteredSuggestionOptions.filter(
                  (item) => !!item.id && allowedBlocks.includes(item.id)
              )
            : filteredSuggestionOptions;

        const customItems = [...items, ...getCustomActions(customBlocks)];

        customItems.forEach((item) => (item.command = () => onCommand({ item, editor, range })));

        return customItems;
    }

    function onCommand({
        item,
        editor,
        range
    }: {
        item: DotMenuItem;
        editor: Editor;
        range: Range;
    }) {
        const { id = '', attributes } = item;
        const props: SuggestionsCommandProps = {
            type: { name: id.includes('heading') ? 'heading' : id, ...attributes }
        };

        clearFilter({ type: ItemsType.BLOCK, editor, range, suggestionKey, ItemsType });
        onSelection({ editor, range, props });
    }

    function onSelection({
        editor,
        range,
        props
    }: {
        editor: Editor;
        range: Range;
        props: SuggestionsCommandProps;
    }) {
        const newRange = updateQueryRange({ editor, range });
        execCommand({ editor: editor, range: newRange, props, customBlocks });
    }

    /**
     * Returns a new range based on a query start and length
     *
     * @param editor {Editor}
     * @param range {Range}
     *
     * @return range {Range}
     */
    function updateQueryRange({ editor, range }: { editor: Editor; range: Range }): Range {
        const suggestionQuery = suggestionKey.getState(editor.view.state).query?.length || 0;
        range.to = range.to + suggestionQuery;

        return range;
    }

    /* End new Functions */

    /**
     * Handle the keyboard events when the suggestion are opened
     *
     * @param {FloatingActionsKeydownProps} { event }
     * @return {*}  {boolean}
     */
    function onKeyDown({ event }: FloatingActionsKeydownProps): boolean {
        const { key } = event;

        if (key === 'Escape') {
            event.stopImmediatePropagation();
            myTippy?.hide();

            return true;
        }

        if (key === 'Enter') {
            suggestionsComponent?.instance.execCommand();

            return true;
        }

        if (key === 'ArrowDown' || key === 'ArrowUp') {
            suggestionsComponent?.instance.updateSelection(event);

            return true;
        }

        return false;
    }

    function onExit({ editor }: { editor: Editor }): void {
        myTippy?.destroy();
        editor.commands.freezeScroll(false);
        suggestionsComponent?.destroy();
        suggestionsComponent = null;
        destroy$.next(true);
        destroy$.complete();
    }

    return Extension.create<FloatingMenuOptions>({
        name: 'actionsMenu',
        priority: 1000, // If open, give priority on events

        addOptions() {
            return {
                pluginKey: 'actionsMenu',
                element: null,
                suggestion: {
                    char: '/',
                    pluginKey: suggestionKey,
                    allowSpaces: true,
                    startOfLine: true,
                    render: () => {
                        return {
                            onBeforeStart,
                            onStart,
                            onKeyDown,
                            onExit
                        };
                    },
                    items: ({ query }) => {
                        if (suggestionsComponent) {
                            suggestionsComponent.instance.filterItems(query);
                        }

                        // suggestions plugin need to return something,
                        // but we are using the angular suggestionsComponent
                        // https://tiptap.dev/api/utilities/suggestion
                        return [];
                    }
                }
            };
        },

        addCommands() {
            return {
                addHeading:
                    ({ range, type }) =>
                    ({ chain }) => {
                        return chain()
                            .focus()
                            .deleteRange(range)
                            .toggleHeading({ level: type.level as Level })
                            .focus()
                            .run();
                    },
                addContentletBlock:
                    ({ range, payload }) =>
                    ({ chain }) => {
                        return chain()
                            .deleteRange(range)
                            .command((props) => {
                                const node = props.editor.schema.nodes['dotContent'].create({
                                    data: payload
                                });
                                props.tr.replaceSelectionWith(node);

                                return true;
                            })
                            .run();
                    },
                addNextLine:
                    () =>
                    ({ chain }) => {
                        return chain()
                            .command((props) => {
                                const { selection } = props.state;
                                props.commands.insertContentAt(selection.head, {
                                    type: 'paragraph'
                                });

                                return true;
                            })
                            .focus()
                            .run();
                    }
            };
        },

        addProseMirrorPlugins() {
            return [
                FloatingActionsPlugin({
                    // The plugin's `command` contract has no `customBlocks`; supply it from the
                    // closure rather than widening the shared type.
                    command: (props) => execCommand({ ...props, customBlocks }),
                    editor: this.editor,
                    render: () => {
                        return {
                            onStart,
                            onKeyDown,
                            onExit
                        };
                    }
                }),
                Suggestion({
                    editor: this.editor,
                    ...this.options.suggestion
                })
            ];
        }
    });
};
