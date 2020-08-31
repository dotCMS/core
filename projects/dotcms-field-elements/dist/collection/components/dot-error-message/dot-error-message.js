export class DotErrorMessageComponent {
    render() {
        return h("slot", null);
    }
    static get is() { return "dot-error-message"; }
    static get style() { return "/**style-placeholder:dot-error-message:**/"; }
}
