import { ComponentRef, ViewContainerRef } from '@angular/core';
import { Subject } from 'rxjs';
import { filter, take, takeUntil } from 'rxjs/operators';

import { PluginKey } from 'prosemirror-state';
import { Editor, Extension, Range } from '@tiptap/core';
import { FloatingMenuPluginProps } from '@tiptap/extension-floating-menu';
import { Level } from '@tiptap/extension-heading';
import Suggestion, { SuggestionOptions, SuggestionProps } from '@tiptap/suggestion';
import tippy, { GetReferenceClientRect } from 'tippy.js';

import {
    // Floating Menu
    FLOATING_ACTIONS_MENU_KEYBOARD,
    FloatingActionsKeydownProps,
    FloatingActionsPlugin,
    FloatingActionsProps,
    // Suggestions
    ItemsType,
    SuggestionsCommandProps,
    SuggestionsComponent,
    CONTENT_SUGGESTION_ID,
    SuggestionPopperModifiers,
    findParentNode,
    NodeTypes
} from '@dotcms/block-editor';

import { ActionButtonComponent } from './action-button.component';

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
            addContentletBlock: ({ range: Range, payload: unknown }) => ReturnType;
            addNextLine: () => ReturnType;
        };
    }
}

export type FloatingMenuOptions = Omit<FloatingMenuPluginProps, 'editor' | 'element'> & {
    element: HTMLElement | null;
    suggestion: Omit<SuggestionOptions, 'editor'>;
};

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
    props
}: {
    editor: Editor;
    range: Range;
    props: SuggestionsCommandProps;
}) {
    const whatToDo = {
        dotContent: () => {
            editor
                .chain()
                .addContentletBlock({ range, payload: props.payload })
                .addNextLine()
                .run();
        },
        heading: () => {
            editor.chain().addHeading({ range, type: props.type }).run();
        },
        table: () => {
            editor.commands
                .openForm(
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
                )
                .pipe(
                    take(1),
                    filter((value) => !!value)
                )
                .subscribe((value) => {
                    requestAnimationFrame(() => {
                        editor
                            .chain()
                            .insertTable({
                                rows: value.rows,
                                cols: value.columns,
                                withHeaderRow: !!value.header
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
        image: () => {
            editor.commands.toggleImageForm(true);
        }
    };

    whatToDo[props.type.name]
        ? whatToDo[props.type.name]()
        : editor.chain().setTextSelection(range).focus().run();
}

export const ActionsMenu = (viewContainerRef: ViewContainerRef) => {
    let myTippy;
    let suggestionsComponent: ComponentRef<SuggestionsComponent>;
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
            myTippy = getTippyInstance({
                element: editor.options.element.parentElement,
                content: suggestionsComponent.location.nativeElement,
                rect: clientRect,
                onHide: () => {
                    const transaction = editor.state.tr.setMeta(FLOATING_ACTIONS_MENU_KEYBOARD, {
                        open: false
                    });
                    editor.view.dispatch(transaction);
                }
            });
        }
    }

    function onBeforeStart({ editor }): void {
        const isTableCell =
            findParentNode(editor.view.state.selection.$from, [NodeTypes.TABLE_CELL])?.type.name ===
            NodeTypes.TABLE_CELL;

        shouldShow = !isTableCell;
    }

    function setUpSuggestionComponent(editor: Editor, range: Range) {
        const { allowedBlocks, allowedContentTypes, lang } = editor.storage.dotConfig;
        suggestionsComponent = viewContainerRef.createComponent(SuggestionsComponent);

        // Setting Inputs
        suggestionsComponent.instance.currentLanguage = lang;
        suggestionsComponent.instance.allowedContentTypes = allowedContentTypes;
        suggestionsComponent.instance.allowedBlocks = allowedBlocks.length > 1 ? allowedBlocks : [];
        suggestionsComponent.instance.onSelection = (item) => {
            const suggestionQuery = suggestionKey.getState(editor.view.state).query?.length || 0;
            range.to = range.to + suggestionQuery;
            execCommand({ editor: editor, range: range, props: item });
        };

        // Needs to be called after settings the component Inputs
        // To avoid calling the `onInit` hook before the Inputs are initialized
        suggestionsComponent.changeDetectorRef.detectChanges();

        if (allowedBlocks.length <= 1 || allowedBlocks.includes(CONTENT_SUGGESTION_ID)) {
            suggestionsComponent.instance.addContentletItem();
        }

        // Subscribe to @Output
        suggestionsComponent.instance.clearFilter.pipe(takeUntil(destroy$)).subscribe((type) => {
            const queryRange = {
                to: range.to + suggestionKey.getState(editor.view.state).query?.length,
                from: type === ItemsType.BLOCK ? range.from : range.from + 1
            };
            editor.chain().deleteRange(queryRange).run();
        });
    }

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
            myTippy.hide();

            return true;
        }

        if (key === 'Enter') {
            suggestionsComponent.instance.execCommand();

            return true;
        }

        if (key === 'ArrowDown' || key === 'ArrowUp') {
            suggestionsComponent.instance.updateSelection(event);

            return true;
        }

        return false;
    }

    function onExit() {
        myTippy?.destroy();
        suggestionsComponent?.destroy();
        suggestionsComponent = null;
        destroy$.next(true);
        destroy$.complete();
    }

    return Extension.create<FloatingMenuOptions>({
        name: 'actionsMenu',

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
                                const node = props.editor.schema.nodes.dotContent.create({
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
            const button = viewContainerRef.createComponent(ActionButtonComponent);

            return [
                FloatingActionsPlugin({
                    command: execCommand,
                    editor: this.editor,
                    element: button.location.nativeElement,
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
