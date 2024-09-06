import {
    Component,
    Element,
    Listen,
    Prop,
    State,
    Watch,
    h,
    Host,
    Event,
    EventEmitter
} from '@stencil/core';

import { DotFieldStatus } from '../../../models';
import { fieldCustomProcess, getFieldsFromLayout, getErrorMessage } from './utils';
import { getClassNames, getOriginalStatus, updateStatus } from '../../../utils';
import {
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeField,
    DotCMSTempFile,
    DotCMSContentlet,
    DotHttpErrorResponse
} from '@dotcms/dotcms-models';
import { DotUploadService, fallbackErrorMessages } from './services/dot-upload.service';
import { DotBinaryFileComponent } from '../dot-binary-file/dot-binary-file';

const SUBMIT_FORM_API_URL = '/api/v1/workflow/actions/default/fire/NEW';

@Component({
    tag: 'dot-form',
    styleUrl: 'dot-form.scss'
})
export class DotFormComponent {
    @Element()
    el: HTMLElement;

    /** (optional) List of fields (variableName) separated by comma, to be shown */
    @Prop()
    fieldsToShow: string;

    /** (optional) Text to be rendered on Reset button */
    @Prop({ reflect: true })
    resetLabel = 'Reset';

    /** (optional) Text to be rendered on Submit button */
    @Prop({ reflect: true })
    submitLabel = 'Submit';

    /** Layout metada to be rendered */
    @Prop({ reflect: true })
    layout: DotCMSContentTypeLayoutRow[] = [];

    /** Content type variable name */
    @Prop({ reflect: true })
    variable = '';

    @State()
    status: DotFieldStatus = getOriginalStatus();
    @State()
    errorMessage = '';
    @State()
    uploadFileInProgress = false;
    @State()
    processingSubmit = false;

    /**Emit when submit the form */
    @Event()
    submit: EventEmitter<DotCMSContentlet>;

    private fieldsStatus: { [key: string]: { [key: string]: boolean } } = {};
    private value = {};

    /**
     * Update the form value when valueChange in any of the child fields.
     *
     * @param CustomEvent event
     * @memberof DotFormComponent
     */
    @Listen('dotValueChange')
    onValueChange(event: CustomEvent): void {
        const { tagName } = event.target as HTMLElement;
        const { name, value } = event.detail;
        const process = fieldCustomProcess[tagName];
        if (tagName === 'DOT-BINARY-FILE' && value) {
            this.uploadFile(event).then((tempFile: DotCMSTempFile) => {
                this.value[name] = tempFile && tempFile.id;
            });
        } else {
            this.value[name] = process ? process(value) : value;
        }
    }

    /**
     * Update the form status when dotStatusChange in any of the child fields
     *
     * @param CustomEvent event
     * @memberof DotFormComponent
     */
    @Listen('dotStatusChange')
    onStatusChange({ detail }: CustomEvent): void {
        this.fieldsStatus[detail.name] = detail.status;

        this.status = updateStatus(this.status, {
            dotTouched: this.getTouched(),
            dotPristine: this.getStatusValueByName('dotPristine'),
            dotValid: this.getStatusValueByName('dotValid')
        });
    }

    @Watch('layout')
    layoutWatch() {
        this.value = this.getUpdateValue();
    }

    @Watch('fieldsToShow')
    fieldsToShowWatch() {
        this.value = this.getUpdateValue();
    }

    componentWillLoad() {
        this.value = this.getUpdateValue();
    }

    render() {
        const classes = getClassNames(this.status, this.status.dotValid);

        return (
            <Host class={{ ...classes }}>
                <form onSubmit={this.handleSubmit.bind(this)}>
                    {this.layout.map((row: DotCMSContentTypeLayoutRow) => (
                        <dot-form-row row={row} fields-to-show={this.fieldsToShow} />
                    ))}
                    <div class="dot-form__buttons">
                        <button type="reset" onClick={() => this.resetForm()}>
                            {this.resetLabel}
                        </button>
                        <button
                            type="submit"
                            disabled={
                                !this.status.dotValid ||
                                this.uploadFileInProgress ||
                                this.processingSubmit
                            }>
                            {this.submitLabel}
                        </button>
                    </div>
                </form>
                <dot-error-message>{this.errorMessage}</dot-error-message>
            </Host>
        );
    }

    private getStatusValueByName(name: string): boolean {
        return Object.values(this.fieldsStatus)
            .map((field: { [key: string]: boolean }) => field[name])
            .every((item: boolean) => item === true);
    }

    private getTouched(): boolean {
        return Object.values(this.fieldsStatus)
            .map((field: { [key: string]: boolean }) => field.dotTouched)
            .includes(true);
    }

    private handleSubmit(event: Event): void {
        event.preventDefault();
        event.stopPropagation();
        if (!this.processingSubmit) {
            this.processingSubmit = true;
            fetch(SUBMIT_FORM_API_URL, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    contentlet: {
                        contentType: this.variable,
                        ...this.value
                    }
                })
            })
                .then(async (response: Response) => {
                    if (response.status !== 200) {
                        const error: DotHttpErrorResponse = {
                            message: await response.text(),
                            status: response.status
                        };
                        throw error;
                    }
                    return response.json();
                })
                .then((jsonResponse) => {
                    const contentlet = jsonResponse.entity;
                    this.submit.emit(contentlet);
                })
                .catch(({ message, status }: DotHttpErrorResponse) => {
                    this.errorMessage = getErrorMessage(message) || fallbackErrorMessages[status];
                })
                .finally(() => {
                    this.processingSubmit = false;
                });
        }
    }

    private resetForm(): void {
        const elements = Array.from(this.el.querySelectorAll('form dot-form-column > *'));

        elements.forEach(async (element: any) => {
            try {
                await element.reset();
            } catch (error) {
                console.warn(`${element.tagName}`, error);
            }
        });
    }

    private getUpdateValue(): { [key: string]: string } {
        return getFieldsFromLayout(this.layout)
            .filter((field: DotCMSContentTypeField) => field.fixed === false)
            .reduce(
                (
                    acc: { [key: string]: string },
                    { variable, defaultValue, dataType, values }: DotCMSContentTypeField
                ) => {
                    return {
                        ...acc,
                        [variable]: defaultValue || (dataType !== 'TEXT' ? values : null)
                    };
                },
                {}
            );
    }

    private getMaxSize(event: any): string {
        const attributes = [...event.target.attributes];
        const maxSize = attributes.filter((item) => {
            return item.name === 'max-file-length';
        })[0];
        return maxSize && maxSize.value;
    }

    private uploadFile(event: CustomEvent): Promise<DotCMSTempFile> {
        const uploadService = new DotUploadService();
        const file = event.detail.value;
        const maxSize = this.getMaxSize(event);
        const binary: DotBinaryFileComponent = event.target as unknown as DotBinaryFileComponent;

        if (!maxSize || file.size <= maxSize) {
            this.uploadFileInProgress = true;
            binary.errorMessage = '';
            return uploadService
                .uploadFile(file, maxSize)
                .then((tempFile: DotCMSTempFile) => {
                    this.errorMessage = '';
                    binary.previewImageUrl = tempFile.thumbnailUrl;
                    binary.previewImageName = tempFile.fileName;
                    this.uploadFileInProgress = false;
                    return tempFile;
                })
                .catch(({ message, status }: DotHttpErrorResponse) => {
                    binary.clearValue();
                    this.uploadFileInProgress = false;
                    binary.errorMessage = getErrorMessage(message) || fallbackErrorMessages[status];
                    return null;
                });
        } else {
            binary.reset().then(() => {
                binary.errorMessage = `File size larger than allowed ${maxSize} bytes`;
            });
            return Promise.resolve(null);
        }
    }
}
