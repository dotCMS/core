import BubbleMenu, { BubbleMenuOptions } from '@tiptap/extension-bubble-menu';
import { DotBubbleMenuPlugin } from '../plugins/dot-bubble-menu.plugin';

export const DotBubbleMenuExtension = BubbleMenu.extend<BubbleMenuOptions>({
    addProseMirrorPlugins() {
        if (!this.options.element) {
            return [];
        }

        return [
            DotBubbleMenuPlugin({
                pluginKey: this.options.pluginKey,
                editor: this.editor,
                element: this.options.element,
                tippyOptions: this.options.tippyOptions,
                shouldShow: this.options.shouldShow
            })
        ];
    }
});
