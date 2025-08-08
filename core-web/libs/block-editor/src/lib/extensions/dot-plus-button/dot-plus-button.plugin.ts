import tippy, { Instance as TippyInstance } from 'tippy.js';

import { Extension, isNodeEmpty } from '@tiptap/core';
import { Plugin, PluginKey } from '@tiptap/pm/state';

export const DotCMSPlusButton = Extension.create({
    name: 'dotCMSPlusButton',

    addProseMirrorPlugins() {
        const editor = this.editor;

        const PLUS_BUTTON_CLASS = 'add-button';
        const pluginKey = new PluginKey('dotCMSPlusButton');

        type AllowedTarget = { pos: number; dom: HTMLElement };

        return [
            new Plugin({
                key: pluginKey,
                view(view) {
                    let tippyInstance: TippyInstance | null = null;
                    let buttonEl: HTMLButtonElement | null = null;

                    const createPlusButton = (
                        onMouseDown: (e: MouseEvent) => void
                    ): HTMLButtonElement => {
                        const el = document.createElement('button');
                        el.textContent = '+';
                        el.classList.add(PLUS_BUTTON_CLASS);
                        el.addEventListener('mousedown', onMouseDown, { passive: false });
                        return el;
                    };

                    const createTippy = (content: HTMLElement): TippyInstance =>
                        tippy(document.body, {
                            content,
                            trigger: 'manual',
                            interactive: true,
                            placement: 'left',
                            hideOnClick: false,
                            appendTo: () => document.body,
                            getReferenceClientRect: () => new DOMRect(0, 0, 0, 0)
                        });

                    const ensureTippy = () => {
                        if (!buttonEl) {
                            buttonEl = createPlusButton((e: MouseEvent) => {
                                e.preventDefault();
                                editor.chain().focus().insertContent('/').run();
                            });
                        }
                        if (!tippyInstance) {
                            tippyInstance = createTippy(buttonEl);
                        }
                    };

                    const destroyAll = () => {
                        if (tippyInstance) {
                            tippyInstance.destroy();
                            tippyInstance = null;
                        }
                        buttonEl = null;
                    };

                    const selectionIsCollapsed = (): boolean => view.state.selection.empty;

                    const findAllowedEmptyBlock = (): AllowedTarget | null => {
                        if (!editor.isEditable) return null;
                        if (!selectionIsCollapsed()) return null;

                        const { state } = view;
                        const $from = state.selection.$from;

                        for (let depth = $from.depth; depth >= 0; depth--) {
                            const node = $from.node(depth);
                            if (!node) continue;

                            const typeName = node.type?.name;
                            const isParagraph = typeName === 'paragraph';
                            const isHeading = typeName === 'heading';

                            if (isParagraph || isHeading) {
                                if (!isNodeEmpty(node)) return null;

                                const pos = depth === 0 ? 0 : $from.before(depth);
                                const dom = view.nodeDOM(pos) as HTMLElement | null;
                                if (dom) return { pos, dom };
                                break;
                            }
                        }
                        return null;
                    };

                    const computeTargetRect = (target: AllowedTarget): DOMRect => {
                        const rect = target.dom.getBoundingClientRect();
                        return new DOMRect(rect.left, rect.top, rect.width, rect.height);
                    };

                    const hide = () => {
                        if (tippyInstance) tippyInstance.hide();
                    };

                    const showAtTarget = (target: AllowedTarget) => {
                        ensureTippy();
                        if (!tippyInstance) return;
                        const rect = computeTargetRect(target);
                        tippyInstance.setProps({ getReferenceClientRect: () => rect });
                        tippyInstance.show();
                    };

                    return {
                        update() {
                            const target = findAllowedEmptyBlock();
                            if (!target) return hide();
                            showAtTarget(target);
                        },
                        destroy() {
                            destroyAll();
                        }
                    };
                }
            })
        ];
    }
});
