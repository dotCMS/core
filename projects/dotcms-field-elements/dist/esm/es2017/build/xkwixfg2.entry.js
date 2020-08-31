import { h } from '../dotcmsfields.core.js';

import { a as Fragment } from './chunk-1d89c98b.js';
import { a as getOriginalStatus, f as updateStatus, c as getClassNames } from './chunk-62cd3eff.js';
import { d as getErrorMessage, e as getFieldsFromLayout, f as fieldCustomProcess } from './chunk-4205a04e.js';

class DotUploadService {
    constructor() { }
    uploadFile(file, maxSize) {
        if (typeof file === 'string') {
            return this.uploadFileByURL(file);
        }
        else {
            return this.uploadBinaryFile(file, maxSize);
        }
    }
    uploadFileByURL(url) {
        const UPLOAD_FILE_FROM_URL = '/api/v1/temp/byUrl';
        return fetch(UPLOAD_FILE_FROM_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Origin: window.location.hostname
            },
            body: JSON.stringify({
                remoteUrl: url
            })
        }).then(async (response) => {
            if (response.status === 200) {
                return (await response.json()).tempFiles[0];
            }
            else {
                const error = {
                    message: (await response.json()).message,
                    status: response.status
                };
                throw error;
            }
        });
    }
    uploadBinaryFile(file, maxSize) {
        let path = `/api/v1/temp`;
        path += maxSize ? `?maxFileLength=${maxSize}` : '';
        const formData = new FormData();
        formData.append('file', file);
        return fetch(path, {
            method: 'POST',
            headers: {
                Origin: window.location.hostname
            },
            body: formData
        }).then(async (response) => {
            if (response.status === 200) {
                return (await response.json()).tempFiles[0];
            }
            else {
                const error = {
                    message: (await response.json()).message,
                    status: response.status
                };
                throw error;
            }
        });
    }
}

const SUBMIT_FORM_API_URL = '/api/v1/workflow/actions/default/fire/NEW';
const fallbackErrorMessages = {
    500: '500 Internal Server Error',
    400: '400 Bad Request',
    401: '401 Unauthorized Error'
};
class DotFormComponent {
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
    static get style() { return "dot-form{display:block}dot-form>form label{margin:0;padding:0}dot-form>form dot-form-column>*{display:block;margin:2rem 0}dot-form>form dot-form-column>:first-child{margin-top:0}dot-form>form dot-form-column>:last-child{margin-bottom:0}dot-form>form .dot-form__buttons{display:-ms-flexbox;display:flex;-ms-flex-direction:row;flex-direction:row;-ms-flex-pack:end;justify-content:flex-end}dot-form>form .dot-form__buttons button:last-child{margin-left:1rem}"; }
}

export { DotFormComponent as DotForm };
