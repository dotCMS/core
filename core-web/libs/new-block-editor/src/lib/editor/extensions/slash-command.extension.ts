import { Editor, Extension } from '@tiptap/core';
import Suggestion, {
    SuggestionKeyDownProps,
    SuggestionPluginKey,
    SuggestionProps
} from '@tiptap/suggestion';

import { BlockItem, SlashMenuService } from '../components/slash-menu/slash-menu.service';

function hideDragGutterForSlashMenu(editor: Editor): void {
    // TipTap's drag-handle plugin apply() runs both metas in one transaction but the
    // hideDragHandle branch always sets locked = false after lockDragHandle ran, so the
    // lock never sticks. Two transactions: hide first, then lock (mousemove stays no-op).
    editor.chain().setMeta('hideDragHandle', true).run();
    editor.chain().lockDragHandle().run();
}

function showDragGutterAfterSlashMenu(editor: Editor): void {
    editor.commands.unlockDragHandle();
}

export function createSlashCommandExtension(menuService: SlashMenuService) {
    return Extension.create({
        name: 'slashCommand',

        onDestroy() {
            menuService.detachEditor();
        },

        addProseMirrorPlugins() {
            menuService.attachEditor(this.editor);
            return [
                Suggestion<BlockItem>({
                    editor: this.editor,
                    char: '/',
                    startOfLine: true,
                    allowSpaces: true,

                    items: ({ query }) => menuService.filterItems(query),

                    command: ({ editor, range, props }) => {
                        if (props.onSelect) {
                            // Always pass range so keepRange items can clean it up themselves later.
                            props.onSelect(editor, range);
                            // keepRange items (e.g. sub-menus) skip deleteRange so the suggestion
                            // session stays alive and keyboard navigation keeps working.
                            if (!props.keepRange) {
                                editor.chain().focus().deleteRange(range).run();
                            }
                        } else if (props.apply) {
                            props.apply(editor.chain().focus().deleteRange(range)).run();
                        }
                    },

                    render: () => ({
                        onStart: (props: SuggestionProps<BlockItem>) => {
                            menuService.open(props.items, props.clientRect ?? null, props.command);
                            hideDragGutterForSlashMenu(props.editor);
                        },
                        onUpdate: (props: SuggestionProps<BlockItem>) => {
                            menuService.update(
                                props.items,
                                props.clientRect ?? null,
                                props.command
                            );
                            hideDragGutterForSlashMenu(props.editor);
                        },
                        onExit: (props: SuggestionProps<BlockItem>) => {
                            // Suggestion calls onExit for both real exits and for (moved && changed)
                            // while the slash match is still active — e.g. after we delete the query
                            // text and the decoration range/query updates in one transaction. Only
                            // tear down the Angular menu when the plugin has actually deactivated.
                            const slashState = SuggestionPluginKey.getState(props.editor.state);
                            if (slashState?.active) {
                                return;
                            }
                            menuService.close();
                            showDragGutterAfterSlashMenu(props.editor);
                        },
                        onKeyDown: ({ event }: SuggestionKeyDownProps) =>
                            menuService.handleKeyDown(event)
                    })
                })
            ];
        }
    });
}
