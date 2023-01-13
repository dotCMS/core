import { ComponentStore } from '@ngrx/component-store';

import { Injectable } from '@angular/core';

import { MenuItem } from 'primeng/api';

import { take } from 'rxjs/operators';

import { DotContentTypeService } from '@dotcms/app/api/services/dot-content-type';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotContainerStructure } from '@models/container/dot-container.model';

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
    readonly contentTypes$ = this.select(({ contentTypes }: DotContentEditorState) => contentTypes);

    updateActiveTabIndex = this.updater<number>((state, activeTabIndex) => {
        return {
            ...state,
            activeTabIndex
        };
    });

    updateClosedTab = this.updater<number>((state, closedTabIndex) => {
        const { selectedContentTypes, contentTypesData } = this.get();

        const updatedSelectedContentTypes = selectedContentTypes.filter(
            (val, index) => index !== closedTabIndex
        );

        const updatedContentTypesData = contentTypesData.filter(
            (val, index) => index !== closedTabIndex
        );

        return {
            ...state,
            selectedContentTypes: updatedSelectedContentTypes,
            contentTypesData: updatedContentTypesData
        };
    });

    updateSelectedContentType = this.updater<MenuItem>((state, selectedContentType) => {
        return {
            ...state,
            contentTypesData: [...state.contentTypesData, selectedContentType],
            selectedContentTypes: [...state.selectedContentTypes, selectedContentType]
        };
    });

    updateSelectedContentTypeContent = this.updater<string>((state, code) => {
        const { contentTypesData, activeTabIndex } = this.get();
        const contentTypes = [...contentTypesData];
        const currentContent = contentTypes[activeTabIndex - 1];

        contentTypes[activeTabIndex - 1] = {
            ...currentContent,
            state: { ...currentContent.state, code: code || currentContent?.state?.code || '' }
        };

        return {
            ...state,
            contentTypesData: contentTypes
        };
    });

    updateRetrievedContentTypes = this.updater<DotContainerStructure[]>(
        (state, containerStructures) => {
            const { contentTypes, selectedContentTypes } = this.get();
            const availableselectedContentTypes = [];
            // if containerStructures strcuture available we don't need to add default contentType
            if (containerStructures.length === 0) {
                availableselectedContentTypes.push(...selectedContentTypes);
            }

            contentTypes.forEach((contentType) => {
                const foundSelectedContentType = containerStructures.find(
                    (content) => content.contentTypeVar === contentType.state.contentType.variable
                );
                if (foundSelectedContentType) {
                    availableselectedContentTypes.push({
                        ...contentType,
                        state: { ...contentType.state, ...foundSelectedContentType }
                    });
                }
            });

            return {
                ...state,
                selectedContentTypes: availableselectedContentTypes,
                contentTypesData: availableselectedContentTypes
            };
        }
    );

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
