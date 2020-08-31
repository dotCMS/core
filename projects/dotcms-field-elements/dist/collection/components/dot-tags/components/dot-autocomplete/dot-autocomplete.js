import autoComplete from '@tarekraafat/autocomplete.js/dist/js/autoComplete';
export class DotAutocompleteComponent {
    constructor() {
        this.disabled = false;
        this.placeholder = '';
        this.threshold = 0;
        this.maxResults = 0;
        this.debounce = 300;
        this.data = null;
        this.id = `autoComplete${new Date().getTime()}`;
        this.keyEvent = {
            Enter: this.emitEnter.bind(this),
            Escape: this.clean.bind(this)
        };
    }
    componentDidLoad() {
        if (this.data) {
            this.initAutocomplete();
        }
    }
    render() {
        return (h("input", { autoComplete: "off", disabled: this.disabled || null, id: this.id, onBlur: (event) => this.handleBlur(event), onKeyDown: (event) => this.handleKeyDown(event), placeholder: this.placeholder || null }));
    }
    watchThreshold() {
        this.initAutocomplete();
    }
    watchData() {
        this.initAutocomplete();
    }
    watchMaxResults() {
        this.initAutocomplete();
    }
    handleKeyDown(event) {
        const { value } = this.getInputElement();
        if (value && this.keyEvent[event.key]) {
            event.preventDefault();
            this.keyEvent[event.key](value);
        }
    }
    handleBlur(event) {
        event.preventDefault();
        setTimeout(() => {
            if (document.activeElement.parentElement !== this.getResultList()) {
                this.clean();
                this.lostFocus.emit(event);
            }
        }, 0);
    }
    clean() {
        this.getInputElement().value = '';
        this.cleanOptions();
    }
    cleanOptions() {
        this.getResultList().innerHTML = '';
    }
    emitselect(select) {
        this.clean();
        this.selection.emit(select);
    }
    emitEnter(select) {
        if (select) {
            this.clean();
            this.enter.emit(select);
        }
    }
    getInputElement() {
        return this.el.querySelector(`#${this.id}`);
    }
    initAutocomplete() {
        this.clearList();
        new autoComplete({
            data: {
                src: async () => this.getData()
            },
            sort: (a, b) => {
                if (a.match < b.match) {
                    return -1;
                }
                if (a.match > b.match) {
                    return 1;
                }
                return 0;
            },
            placeHolder: this.placeholder,
            selector: `#${this.id}`,
            threshold: this.threshold,
            searchEngine: 'strict',
            highlight: true,
            maxResults: this.maxResults,
            debounce: this.debounce,
            resultsList: {
                container: () => this.getResultListId(),
                destination: this.getInputElement(),
                position: 'afterend'
            },
            resultItem: ({ match }) => match,
            onSelection: ({ event, selection }) => {
                event.preventDefault();
                this.focusOnInput();
                this.emitselect(selection.value);
            }
        });
    }
    clearList() {
        const list = this.getResultList();
        if (list) {
            list.remove();
        }
    }
    focusOnInput() {
        this.getInputElement().focus();
    }
    getResultList() {
        return this.el.querySelector(`#${this.getResultListId()}`);
    }
    getResultListId() {
        return `${this.id}_results_list`;
    }
    async getData() {
        const autocomplete = this.getInputElement();
        autocomplete.setAttribute('placeholder', 'Loading...');
        const data = typeof this.data === 'function' ? await this.data() : [];
        autocomplete.setAttribute('placeholder', this.placeholder || '');
        return data;
    }
    static get is() { return "dot-autocomplete"; }
    static get properties() { return {
        "data": {
            "type": "Any",
            "attr": "data",
            "watchCallbacks": ["watchData"]
        },
        "debounce": {
            "type": Number,
            "attr": "debounce",
            "reflectToAttr": true
        },
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "maxResults": {
            "type": Number,
            "attr": "max-results",
            "reflectToAttr": true,
            "watchCallbacks": ["watchMaxResults"]
        },
        "placeholder": {
            "type": String,
            "attr": "placeholder",
            "reflectToAttr": true
        },
        "threshold": {
            "type": Number,
            "attr": "threshold",
            "reflectToAttr": true,
            "watchCallbacks": ["watchThreshold"]
        }
    }; }
    static get events() { return [{
            "name": "selection",
            "method": "selection",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "enter",
            "method": "enter",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "lostFocus",
            "method": "lostFocus",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return "/**style-placeholder:dot-autocomplete:**/"; }
}
