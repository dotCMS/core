import { signalStoreFeature, type, withMethods } from '@ngrx/signals';

import { EditorInsertResult } from './models';

import { CONTAINER_INSERT_ERROR } from '../../../shared/enums';
import { ActionPayload, PageContainer } from '../../../shared/models';
import { insertContentletInContainer, isSelectedContentletGone } from '../../../utils';
import { UVEState } from '../../models';

/**
 * Methods this feature calls on the store: `editorSave` from `withPageApi`, and `resetSelected`
 * and `resetEditorProperties` from `withEditor`. Reached through a type assertion, as
 * `withSelectionAnchor` does, so the feature must be composed after both (see dot-uve.store.ts).
 */
interface EditorInsertMethodDeps {
    editorSave: (pageContainers: PageContainer[]) => void;
    resetSelected: () => void;
    resetEditorProperties: () => void;
}

type StoreWithEditorInsertDeps<T> = T & EditorInsertMethodDeps;

/**
 * Editor insert feature: puts a contentlet into a container position and saves the page.
 *
 * The one place every flow that adds a contentlet to a container goes through: a contentlet or a
 * file dropped on the canvas, and the content, form or widget picked or created in an editor.
 * Telling the user why a container refused the contentlet is left to the caller, since the store
 * holds no UI.
 */
export function withEditorInsert() {
    return signalStoreFeature(
        { state: type<UVEState>() },
        withMethods((store) => {
            const deps = store as StoreWithEditorInsertDeps<typeof store>;

            return {
                /**
                 * Inserts a contentlet at a container position and saves the page's containers,
                 * clearing the selection when the change leaves the selected contentlet out of its
                 * container. When the container refuses the contentlet, nothing is saved and the
                 * drag and hover state is cleared.
                 *
                 * @param into the container position to insert at
                 * @param contentletId the identifier of the contentlet to insert
                 * @returns whether it went in, and why not when it did not
                 */
                editorInsertContentlet(
                    into: ActionPayload,
                    contentletId: string
                ): EditorInsertResult {
                    const { pageContainers, didInsert, errorCode } = insertContentletInContainer({
                        ...into,
                        newContentletId: contentletId
                    });

                    if (!didInsert) {
                        deps.resetEditorProperties();

                        return errorCode === CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED
                            ? {
                                  didInsert: false,
                                  errorCode: CONTAINER_INSERT_ERROR.CONTAINER_LIMIT_REACHED,
                                  maxContentlets: into.container.maxContentlets
                              }
                            : {
                                  didInsert: false,
                                  errorCode: CONTAINER_INSERT_ERROR.DUPLICATE_CONTENT
                              };
                    }

                    if (isSelectedContentletGone(store.editorSelected(), pageContainers)) {
                        deps.resetSelected();
                    }

                    deps.editorSave(pageContainers);

                    return { didInsert: true };
                }
            };
        })
    );
}
