import { ComponentStore } from '@ngrx/component-store';
import { take } from 'rxjs/operators';
import { DotContentTypeService } from '@dotcms/app/api/services/dot-content-type';
import { Injectable } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { DotCMSContentType } from '@dotcms/dotcms-models';

export interface DotContentEditorState {
    activeTabIndex: number;
    contentTypes: MenuItem[];
    selectedContentTypes: MenuItem[];
    contentTypesData: MenuItem[];
}

@Injectable()
export class DotContentEditorStore extends ComponentStore<DotContentEditorState> {
    constructor(private dotContentTypeService: DotContentTypeService) {
        super({
            activeTabIndex: 1,
            contentTypes: [],
            selectedContentTypes: [],
            contentTypesData: []
        });

        this.dotContentTypeService
            .getContentTypes({ page: 999 }) //TODO: Add call to get all contentTypes
            .pipe(take(1))
            .subscribe((contentTypes) => {
                const mappedContentTypes = this.mapActions(contentTypes);
                this.setState({
                    activeTabIndex: 1,
                    contentTypes: mappedContentTypes,
                    selectedContentTypes: [mappedContentTypes[0]],
                    contentTypesData: [mappedContentTypes[0]]
                });
            });
    }

    readonly vm$ = this.select((state: DotContentEditorState) => state);
    readonly contentTypeData$ = this.select(
        ({ contentTypesData }: DotContentEditorState) => contentTypesData
    );

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
        const contentTypesData = state.contentTypesData;
        selected.push(selectedContentType);
        contentTypesData.push(selectedContentType);

        return {
            ...state,
            contentTypesData,
            selectedContentTypes: selected
        };
    });

    updateSelectedContentTypeContent = this.updater<string>((state, code) => {
        const { contentTypesData, activeTabIndex } = this.get();
        const contentTypes = [...contentTypesData];
        const currentContent = contentTypes[activeTabIndex - 1];
        const contentType = {
            ...currentContent,
            state: { ...currentContent.state, code: code || currentContent?.state?.code || '' }
        };
        contentTypes[activeTabIndex - 1] = contentType;

        return {
            ...state,
            contentTypesData: contentTypes
        };
    });

    private mapActions(contentTypes: DotCMSContentType[]): MenuItem[] {
        return contentTypes.map((contentType) => {
            const menuItem = {
                label: contentType.name,
                state: {
                    code: '',
                    contentType
                },
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
