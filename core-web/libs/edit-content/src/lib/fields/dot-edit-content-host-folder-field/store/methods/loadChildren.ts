import { patchState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { tap, exhaustMap } from 'rxjs/operators';

import type { TreeNodeSelectItem } from './../../../../models/dot-edit-content-host-folder-field.interface';
import type { DotEditContentService } from './../../../../services/dot-edit-content.service';

export const loadChildren = (store, dotEditContentService: DotEditContentService) => {
    return rxMethod<TreeNodeSelectItem>(
        pipe(
            exhaustMap((event: TreeNodeSelectItem) => {
                const { node } = event;
                const { hostname, path } = node.data;

                return dotEditContentService.getFoldersTreeNode(hostname, path).pipe(
                    tap((children) => {
                        node.leaf = true;
                        node.icon = 'pi pi-folder-open';
                        node.children = [...children];
                        patchState(store, { nodeExpaned: node });
                    })
                );
            })
        )
    );
};
