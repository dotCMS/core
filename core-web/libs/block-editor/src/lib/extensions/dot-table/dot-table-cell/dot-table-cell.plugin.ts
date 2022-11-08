import { EditorState, Plugin, PluginKey, NodeSelection } from 'prosemirror-state';

export const DotTableCellPlugin = (options: any) => {
    const component = options.component.instance;

    return new Plugin<any>({
        key: options.pluginKey as PluginKey,
        state: {
            init() {},
            apply(tr, state) {
                const action = tr.getMeta(this);
                console.log(action);
            }
        }
    });
};
