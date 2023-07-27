import { Node } from '@tiptap/core';
declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        AIContent: {
            addHelloWorld: () => ReturnType;
        };
    }
}
export const AIContent = Node.create({
    name: 'aiContent',
    apiKey: '',
    description: 'AI Content',
    addAttributes() {
        return {};
    },
    parseHTML() {
        return [
            {
                tag: 'div'
            }
        ];
    },
    renderHTML() {
        return ['div'];
    },
    addOptions() {
        return {
            inline: false
        };
    },
    inline() {
        return this.options.inline;
    },
    group() {
        return 'block';
    },
    addCommands() {
        return {
            ...this.parent?.(),
            addHelloWorld:
                () =>
                ({ commands }) => {
                    return commands.insertContent({ type: this.name });
                }
        };
    },
    addNodeView() {
        return () => {
            const dom = document.createElement('div');

            const input = document.createElement('input');
            input.type = 'text';
            input.placeholder = 'Ask AI to write something';
            input.style.width = '100%';
            input.style.outline = 'none';
            input.style.color = '#6C7389';
            input.style.fontFamily = 'Assistant';
            input.style.fontSize = '16px';
            input.style.fontWeight = '400';
            input.style.lineHeight = '22.4px';
            input.style.padding = '10px 8px';
            input.style.alignSelf = 'stretch';
            input.style.borderRadius = '6px';
            input.style.border = '1px solid #D1D4DB';

            const icon = document.createElement('i');
            icon.classList.add('your-icon-class');
            input.appendChild(icon);

            dom.append(input);

            return { dom };
        };
    }
});
