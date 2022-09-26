import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { take } from 'rxjs/operators';
import { StructureTypeView } from '@models/contentlet';
import { DotContentTypeService } from '@services/dot-content-type';
import { zip } from 'rxjs';

export interface DotContainerPropertiesState {
    activeTabIndex: number;
    contentTypes: StructureTypeView[];
    showPrePostLoopInput: boolean;
}

@Injectable()
export class DotContainerPropertiesStore extends ComponentStore<DotContainerPropertiesState> {
    constructor(private dotContentTypeService: DotContentTypeService) {
        super();

        const contentTypes$ = this.dotContentTypeService.getAllContentTypes();

        zip(contentTypes$)
            .pipe(take(1))
            .subscribe(([contentTypes]) => {
                this.setState({
                    activeTabIndex: 1,
                    contentTypes: contentTypes,
                    showPrePostLoopInput: false
                });
            });
    }

    readonly vm$ = this.select(
        ({ activeTabIndex, contentTypes, showPrePostLoopInput }: DotContainerPropertiesState) => {
            return {
                activeTabIndex,
                contentTypes,
                showPrePostLoopInput
            };
        }
    );

    readonly updatePrePostLoopInputVisibility = this.updater<boolean>(
        (state: DotContainerPropertiesState, showPrePostLoopInput: boolean) => {
            return {
                ...state,
                showPrePostLoopInput
            };
        }
    );
}
