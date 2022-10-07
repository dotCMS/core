import { ComponentStore } from '@ngrx/component-store';
import { StructureTypeView } from '@models/contentlet';
import { take } from 'rxjs/operators';
import { DotContentTypeService } from '@dotcms/app/api/services/dot-content-type';
import { Injectable } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { DotCMSContentType } from '@dotcms/dotcms-models';

export interface DotContentEditorState {
    activeTabIndex: number;
    contentTypes: MenuItem[];
    selectedContentTypes: MenuItem[];
}

@Injectable()
export class DotContentEditorStore extends ComponentStore<DotContentEditorState> {
    constructor(private dotContentTypeService: DotContentTypeService) {
        super({
            activeTabIndex: 1,
            contentTypes: [],
            selectedContentTypes: []
        });

        this.dotContentTypeService
            .getContentTypes({ page: 999 }) //TODO: Add call to get all contentTypes
            .pipe(take(1))
            .subscribe((contentTypes) => {
                const mappedContentTypes = this.mapActions(contentTypes);
                this.setState({
                    activeTabIndex: 1,
                    contentTypes: mappedContentTypes,
                    selectedContentTypes: [mappedContentTypes[0]]
                });
            });
    }

    readonly vm$ = this.select((state: DotContentEditorState) => state);

    updateActiveTabIndex = this.updater<number>((state, activeTabIndex) => {
        return {
            ...state,
            activeTabIndex
        };
    });
    updateClosedTab = this.updater<number>((state, closedTabIndex) => {
        const { selectedContentTypes } = this.get();

        const updatedSelectedContentTypes = selectedContentTypes.filter(
            (val, index) => index !== closedTabIndex
        );

        return {
            ...state,
            selectedContentTypes: updatedSelectedContentTypes
        };
    });

    updateSelectedContentType = this.updater<MenuItem>((state, selectedContentType) => {
        const selected = state.selectedContentTypes;
        selected.push(selectedContentType);

        return {
            ...state,
            selectedContentTypes: selected
        };
    });

    private mapActions(contentTypes: DotCMSContentType[]): MenuItem[] {
        return contentTypes.map((contentType) => {
            const menuItem = {
                label: contentType.name,
                command: () => {
                    if (!this.checkIfAlreadyExists(menuItem.label)) {
                        this.updateSelectedContentType(menuItem);
                    }
                }
            };

            return menuItem;
        });
    }

    private checkIfAlreadyExists(label: string): boolean {
        const { selectedContentTypes } = this.get();

        return selectedContentTypes.some((contentType) => label === contentType.label);
    }
}
