import { Component, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { DotContainerPropertiesStore } from '@portlets/dot-containers/dot-container-create/dot-container-properties/store/dot-container-properties.store';
import { MonacoEditor } from '@models/monaco-editor';
import { DotAlertConfirmService } from '@dotcms/app/api/services/dot-alert-confirm';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { ActivatedRoute } from '@angular/router';
import { pluck, take } from 'rxjs/operators';
import { DotContainer } from '@dotcms/app/shared/models/container/dot-container.model';

@Component({
    selector: 'dot-container-properties',
    templateUrl: './dot-container-properties.component.html',
    styleUrls: ['./dot-container-properties.component.scss'],
    providers: [DotContainerPropertiesStore]
})
export class DotContainerPropertiesComponent implements OnInit {
    vm$ = this.store.vm$;
    editor: MonacoEditor;
    form: UntypedFormGroup;

    constructor(
        private store: DotContainerPropertiesStore,
        private dotMessageService: DotMessageService,
        private fb: UntypedFormBuilder,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotRouterService: DotRouterService,
        private activatedRoute: ActivatedRoute
    ) {
        //
    }

    ngOnInit(): void {
        this.activatedRoute.data
            .pipe(pluck('container'), take(1))
            .subscribe((container: DotContainer) => {
                if (container) {
                    if (container.preLoop) {
                        this.store.updatePrePostLoopAndContentTypeVisibilty({
                            showPrePostLoopInput: true,
                            isContentTypeVisible: true
                        });
                    }

                    this.form = this.fb.group({
                        title: container.title,
                        friendlyName: container.friendlyName,
                        maxContentlets: container.maxContentlets,
                        code: container.code,
                        preLoop: container.preLoop,
                        postLoop: container.postLoop,
                        containerStructures: this.fb.array([])
                    });
                } else {
                    this.form = this.fb.group({
                        title: '',
                        friendlyName: '',
                        maxContentlets: 1,
                        code: '',
                        preLoop: '',
                        postLoop: '',
                        containerStructures: this.fb.array([])
                    });
                }
            });
    }

    /**
     * This method shows the Pre- and Post-Loop Inputs
     *
     * @return void
     * @memberof DotContainerPropertiesComponent
     */
    showLoopInput(): void {
        this.store.updatePrePostLoopInputVisibility(true);
    }

    /**
     * This method shows the Content type inputs
     */
    showContentTypeAndCode(): void {
        this.store.updateContentTypeVisibilty(true);
    }

    /**
     * This method saves the container
     */
    saveContainer(): void {
        this.store.saveContainer(this.form.value);
    }

    /**
     * This method navigates the user back to previous page
     */
    cancel(): void {
        this.dotRouterService.goToPreviousUrl();
    }

    clearContent() {
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.store.updateContentTypeVisibilty(false);
                this.form.reset();
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get(
                'message.container.properties.confirm.clear.content.title'
            ),
            message: this.dotMessageService.get(
                'message.container.properties..confirm.clear.content.message'
            )
        });
    }
}
