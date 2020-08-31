import Fragment from 'stencil-fragment';
import { fieldCustomProcess, getFieldsFromLayout, getErrorMessage } from './utils';
import { getClassNames, getOriginalStatus, updateStatus } from '../../utils';
import { DotUploadService } from './services/dot-upload.service';
const SUBMIT_FORM_API_URL = '/api/v1/workflow/actions/default/fire/NEW';
const fallbackErrorMessages = {
    500: '500 Internal Server Error',
    400: '400 Bad Request',
    401: '401 Unauthorized Error'
};
export class DotFormComponent {
    constructor() {
        this.resetLabel = 'Reset';
        this.submitLabel = 'Submit';
        this.layout = [];
        this.variable = '';
        this.status = getOriginalStatus();
        this.errorMessage = '';
        this.uploadFileInProgress = false;
        this.fieldsStatus = {};
        this.value = {};
    }
    onValueChange(event) {
        const { tagName } = event.target;
        const { name, value } = event.detail;
        const process = fieldCustomProcess[tagName];
        if (tagName === 'DOT-BINARY-FILE' && value) {
            this.uploadFile(event).then((tempFile) => {
                this.value[name] = tempFile && tempFile.id;
            });
        }
        else {
            this.value[name] = process ? process(value) : value;
        }
    }
    onStatusChange({ detail }) {
        this.fieldsStatus[detail.name] = detail.status;
        this.status = updateStatus(this.status, {
            dotTouched: this.getTouched(),
            dotPristine: this.getStatusValueByName('dotPristine'),
            dotValid: this.getStatusValueByName('dotValid')
        });
    }
    layoutWatch() {
        this.value = this.getUpdateValue();
    }
    fieldsToShowWatch() {
        this.value = this.getUpdateValue();
    }
    hostData() {
        return {
            class: getClassNames(this.status, this.status.dotValid)
        };
    }
    componentWillLoad() {
        this.value = this.getUpdateValue();
    }
    render() {
        return (h(Fragment, null,
            h("form", { onSubmit: this.handleSubmit.bind(this) },
                this.layout.map((row) => (h("dot-form-row", { row: row, "fields-to-show": this.fieldsToShow }))),
                h("div", { class: "dot-form__buttons" },
                    h("button", { type: "reset", onClick: () => this.resetForm() }, this.resetLabel),
                    h("button", { type: "submit", disabled: !this.status.dotValid || this.uploadFileInProgress }, this.submitLabel))),
            h("dot-error-message", null, this.errorMessage)));
    }
    getStatusValueByName(name) {
        return Object.values(this.fieldsStatus)
            .map((field) => field[name])
            .every((item) => item === true);
    }
    getTouched() {
        return Object.values(this.fieldsStatus)
            .map((field) => field.dotTouched)
            .includes(true);
    }
    handleSubmit(event) {
        event.preventDefault();
        fetch(SUBMIT_FORM_API_URL, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                contentlet: Object.assign({ contentType: this.variable }, this.value)
            })
        })
            .then(async (response) => {
            if (response.status !== 200) {
                const error = {
                    message: await response.text(),
                    status: response.status
                };
                throw error;
            }
            return response.json();
        })
            .then((jsonResponse) => {
            const contentlet = jsonResponse.entity;
            this.runSuccessCallback(contentlet);
        })
            .catch(({ message, status }) => {
            this.errorMessage = getErrorMessage(message) || fallbackErrorMessages[status];
        });
    }
    runSuccessCallback(contentlet) {
        const successCallback = this.getSuccessCallback();
        if (successCallback) {
            return function () {
                return eval(successCallback);
            }.call({ contentlet });
        }
    }
    getSuccessCallback() {
        const successCallback = getFieldsFromLayout(this.layout).filter((field) => field.variable === 'formSuccessCallback')[0];
        return successCallback.values;
    }
    resetForm() {
        const elements = Array.from(this.el.querySelectorAll('form dot-form-column > *'));
        elements.forEach((element) => {
            try {
                element.reset();
            }
            catch (error) {
                console.warn(`${element.tagName}`, error);
            }
        });
    }
    getUpdateValue() {
        return getFieldsFromLayout(this.layout)
            .filter((field) => field.fixed === false)
            .reduce((acc, { variable, defaultValue, dataType, values }) => {
            return Object.assign({}, acc, { [variable]: defaultValue || (dataType !== 'TEXT' ? values : null) });
        }, {});
    }
    getMaxSize(event) {
        const attributes = [...event.target.attributes];
        const maxSize = attributes.filter((item) => {
            return item.name === 'max-file-length';
        })[0];
        return maxSize && maxSize.value;
    }
    uploadFile(event) {
        const uploadService = new DotUploadService();
        const file = event.detail.value;
        const maxSize = this.getMaxSize(event);
        const binary = event.target;
        if (!maxSize || file.size <= maxSize) {
            this.uploadFileInProgress = true;
            binary.errorMessage = '';
            return uploadService
                .uploadFile(file, maxSize)
                .then((tempFile) => {
                this.errorMessage = '';
                binary.previewImageUrl = tempFile.thumbnailUrl;
                binary.previewImageName = tempFile.fileName;
                this.uploadFileInProgress = false;
                return tempFile;
            })
                .catch(({ message, status }) => {
                binary.clearValue();
                this.uploadFileInProgress = false;
                this.errorMessage = getErrorMessage(message) || fallbackErrorMessages[status];
                return null;
            });
        }
        else {
            binary.reset();
            binary.errorMessage = `File size larger than allowed ${maxSize} bytes`;
            return Promise.resolve(null);
        }
    }
    static get is() { return "dot-form"; }
    static get properties() { return {
        "el": {
            "elementRef": true
        },
        "errorMessage": {
            "state": true
        },
        "fieldsToShow": {
            "type": String,
            "attr": "fields-to-show",
            "watchCallbacks": ["fieldsToShowWatch"]
        },
        "layout": {
            "type": "Any",
            "attr": "layout",
            "reflectToAttr": true,
            "watchCallbacks": ["layoutWatch"]
        },
        "resetLabel": {
            "type": String,
            "attr": "reset-label",
            "reflectToAttr": true
        },
        "status": {
            "state": true
        },
        "submitLabel": {
            "type": String,
            "attr": "submit-label",
            "reflectToAttr": true
        },
        "uploadFileInProgress": {
            "state": true
        },
        "variable": {
            "type": String,
            "attr": "variable",
            "reflectToAttr": true
        }
    }; }
    static get listeners() { return [{
            "name": "valueChange",
            "method": "onValueChange"
        }, {
            "name": "statusChange",
            "method": "onStatusChange"
        }]; }
    static get style() { return "/**style-placeholder:dot-form:**/"; }
}
