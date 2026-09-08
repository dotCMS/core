import { Plugin } from 'prosemirror-state';
import { Decoration, DecorationSet } from 'prosemirror-view';

export const PlaceholderPlugin = new Plugin({
    state: {
        init() {
            return DecorationSet.empty;
        },
        // `this` is annotated because ProseMirror binds the state field's `init`/`apply` to the
        // Plugin instance, not to this object literal — which is what makes `tr.getMeta(this)`
        // match the `tr.setMeta(PlaceholderPlugin, ...)` calls in `asset-uploader.extension`.
        apply(this: Plugin, tr, set: DecorationSet) {
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
                set = set.remove(
                    set.find(undefined, undefined, (spec) => spec['key'] == action.remove.id)
                );
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
