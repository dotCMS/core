import tippy, { Instance as TippyInstance } from 'tippy.js';

import { Extension, isNodeEmpty } from '@tiptap/core';
import { Plugin, PluginKey } from '@tiptap/pm/state';

export const DotCMSPlusButton = Extension.create({
    name: 'dotCMSPlusButton',

    addProseMirrorPlugins() {
        const editor = this.editor;

        return [
            new Plugin({
                key: new PluginKey('dotCMSPlusButton'),
                view(view) {
                    let tippyInstance: TippyInstance | null = null;
                    let buttonEl: HTMLButtonElement | null = null;

                    const create = () => {
                        if (tippyInstance) return;

                        buttonEl = document.createElement('button');
                        buttonEl.textContent = '+';
                        buttonEl.classList.add('add-button');

                        // Use mousedown to avoid focus loss and keep selection stable
                        buttonEl.addEventListener(
                            'mousedown',
                            (e: MouseEvent) => {
                                e.preventDefault();
                                editor.chain().focus().insertContent('/').run();
                            },
                            { passive: false }
                        );

                        tippyInstance = tippy(document.body, {
                            content: buttonEl,
                            trigger: 'manual',
                            interactive: true,
                            placement: 'left',
                            hideOnClick: false,
                            appendTo: () => document.body,
                            getReferenceClientRect: () => new DOMRect(0, 0, 0, 0)
                        });
                    };

                    const destroy = () => {
                        if (tippyInstance) {
                            tippyInstance.destroy();
                            tippyInstance = null;
                        }
                        buttonEl = null;
                    };

                    const isCollapsedSelection = (): boolean => {
                        return view.state.selection.empty;
                    };

                    const isAllowedEmptyBlock = (): { pos: number; dom: HTMLElement } | null => {
                        if (!editor.isEditable) return null;
                        if (!isCollapsedSelection()) return null;

                        const { state } = view;
                        const $from = state.selection.$from;

                        // Walk up to find the nearest paragraph or heading
                        for (let depth = $from.depth; depth >= 0; depth--) {
                            const node = $from.node(depth);
                            if (!node) continue;

                            const isParagraph = node.type && node.type.name === 'paragraph';
                            const isHeading = node.type && node.type.name === 'heading';

                            if (isParagraph || isHeading) {
                                if (!isNodeEmpty(node)) return null;

                                // Position of the node start
                                const pos = depth === 0 ? 0 : $from.before(depth);
                                const dom = view.nodeDOM(pos) as HTMLElement | null;
                                if (dom) {
                                    return { pos, dom };
                                }
                                break;
                            }
                        }

                        return null;
                    };

                    const getTargetRect = (): DOMRect | null => {
                        const target = isAllowedEmptyBlock();
                        if (!target) return null;
                        const rect = target.dom.getBoundingClientRect();
                        return new DOMRect(rect.left, rect.top, rect.width, rect.height);
                    };

                    const hide = () => {
                        if (tippyInstance) {
                            tippyInstance.hide();
                        }
                    };

                    const showAtCurrent = () => {
                        const rect = getTargetRect();
                        if (!rect) {
                            hide();
                            return;
                        }
                        if (!tippyInstance) create();
                        if (!tippyInstance) return;
                        tippyInstance.setProps({ getReferenceClientRect: () => rect });
                        tippyInstance.show();
                    };

                    create();

                    return {
                        update() {
                            const allowed = isAllowedEmptyBlock();
                            if (!allowed) {
                                hide();
                                return;
                            }
                            showAtCurrent();
                        },
                        destroy() {
                            destroy();
                        }
                    };
                }
            })
        ];
    }
});
