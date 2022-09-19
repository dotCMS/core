import { Plugin } from 'prosemirror-state';
import { Decoration, DecorationSet } from 'prosemirror-view';

export const PlaceholderPlugin = new Plugin({
    state: {
        init() {
            return DecorationSet.empty;
        },
        apply(tr, set) {
            // Adjust decoration positions to changes made by the transaction
            set = set.map(tr.mapping, tr.doc);
            // See if the transaction adds or removes any placeholders
            const action = tr.getMeta(this);
            if (action && action.add) {
                const id = action.add.id;
                const deco = Decoration.widget(action.add.pos, action.add.element, {
                    key: id
                });
                set = set.add(tr.doc, [deco]);
            } else if (action && action.remove) {
                set = set.remove(set.find(null, null, (spec) => spec.key == action.remove.id));
            }

            return set;
        }
    },
    props: {
        decorations(state) {
            return this.getState(state);
        }
    }
});
