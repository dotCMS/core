// tslint:disable:max-line-length

const animation = '100ms ease-in';
const mdShadow1 = '0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24)';
const mdShadow3 = '0 10px 20px rgba(0, 0, 0, 0.19), 0 6px 6px rgba(0, 0, 0, 0.23)';
const white = '#fff';
const grayLight = '#c5c5c5';

export const getEditPageCss = (timestampId: string): string => {
    return `
    ${timestampId}:root {
        --color-background: #3A3847;
        --color-main: #C336E5;
        --color-main_mod: #D369EC;
        --color-main_rgb: 195, 54, 229;
        --color-sec: #54428E;
        --color-sec_rgb: 84, 66, 142;
    }

    ${timestampId} [data-dot-object="container"] {
        border: solid 1px #53c2f9 !important;
        margin-bottom: 40px !important;
        min-height: 120px !important;
        display: flex !important;
        flex-direction: column !important;
        width: 100% !important;
    }

    ${timestampId} [data-dot-object="container"].no {
        background-color: #ff00000f !important;
        border-color: red !important;
        border-radious: 2px !important;
        box-shadow: 0 0 20px red !important;
    }

    ${timestampId} [data-dot-object="container"].disabled {
        border-color: ${grayLight} !important;
    }

    ${timestampId} [data-dot-object="contentlet"] {
        background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAQElEQVQoU2NkIAIEH/6VxkhIHUjRWlu2WXgVwhSBDMOpEFkRToXoirAqxKYIQyEuRSgK8SmCKySkCKyQGEUghQCguSaB0AmkRwAAAABJRU5ErkJggg==") !important;
        margin: 40px 16px 16px !important;
        min-height: 60px !important;
        position: relative;
        padding-top: 25px !important;
        transition: background ${animation} !important;
    }

    ${timestampId} [data-dot-object="contentlet"][data-dot-has-page-lang-version="false"] {
        display: none !important;
    }

    ${timestampId} [data-dot-object="container"]:hover [data-dot-object="contentlet"] {
        background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAQElEQVQoU2NkIAIEH/r5n5GQOpCitXbsjHgVwhSBDMOpEFkRToXoirAqxKYIQyEuRSgK8SmCKySkCKyQGEUghQCQPycYlScX0wAAAABJRU5ErkJggg==") !important;
    }

    ${timestampId} [data-dot-object="edit-content"] {
        background-image: url(data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjNDQ0NDQ0IiBoZWlnaHQ9IjE4IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIxOCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4gICAgPHBhdGggZD0iTTMgMTcuMjVWMjFoMy43NUwxNy44MSA5Ljk0bC0zLjc1LTMuNzVMMyAxNy4yNXpNMjAuNzEgNy4wNGMuMzktLjM5LjM5LTEuMDIgMC0xLjQxbC0yLjM0LTIuMzRjLS4zOS0uMzktMS4wMi0uMzktMS40MSAwbC0xLjgzIDEuODMgMy43NSAzLjc1IDEuODMtMS44M3oiLz4gICAgPHBhdGggZD0iTTAgMGgyNHYyNEgweiIgZmlsbD0ibm9uZSIvPjwvc3ZnPg==) !important;
        background-position: center !important;
        background-repeat: no-repeat !important;
        border-radius: 16px !important;
        cursor: pointer !important;
        float: right !important;
        height: 32px !important;
        opacity: 0.7 !important;
        position: relative! important;
        transition: all ${animation} !important;
        width: 32px !important;
        z-index: 2147483647 !important;
    }

    ${timestampId} [data-dot-object="edit-content"]:hover {
        opacity: 1 !important;
    }

    ${timestampId} .dotedit-container__toolbar {
        float: right !important;
        font-size: 0 !important;
        transform: translate(-8px, 17px) !important;
        z-index: 9999999 !important;
        position: relative !important;
    }

    ${timestampId} .dotedit-container__toolbar button,
    ${timestampId} .dotedit-contentlet__toolbar button {
        box-shadow: ${mdShadow1} !important;
        border: none !important;
        border-radius: 16px !important;
        cursor: pointer !important;
        font-size: 0 !important;
        height: 32px !important;
        outline: none !important;
        position: relative !important;
        width: 32px !important;
        z-index: 2147483646 !important;
    }

    ${timestampId} .dotedit-container__toolbar button:not([disabled]):hover,
    ${timestampId} .dotedit-contentlet__toolbar button:not([disabled]):hover {
        box-shadow: ${mdShadow3} !important;
    }

    ${timestampId} .dotedit-container__toolbar button:active,
    ${timestampId} .dotedit-contentlet__toolbar button:active {
        box-shadow: ${mdShadow1} !important;
    }

    ${timestampId} .dotedit-container__toolbar button:disabled {
        background-color: ${grayLight} !important;
    }

    ${timestampId} .dotedit-contentlet__toolbar {
        display: flex !important;
        font-size: 0 !important;
        opacity: 0 !important;
        position: absolute !important;
        right: 0 !important;
        top: -16px !important;
        transition: opacity ${animation} !important;
    }

    ${timestampId} [data-dot-object="contentlet"]:hover .dotedit-contentlet__toolbar {
        opacity: 1 !important;
    }

    ${timestampId} .dotedit-contentlet__toolbar button {
        background-color: ${white} !important;
    }

    ${timestampId} .dotedit-contentlet__toolbar > * {
        margin-right: 8px !important;
    }

    ${timestampId} .dotedit-contentlet__toolbar .dotedit-contentlet__disabled {
        opacity: 0.25 !important;
        pointer-events: none !important;
    }

    ${timestampId} .dotedit-contentlet__toolbar button:last-child {
        margin-right: 0 !important;
    }

    ${timestampId} .dotedit-container__add,
    ${timestampId} .dotedit-contentlet__drag,
    ${timestampId} .dotedit-contentlet__edit,
    ${timestampId} .dotedit-contentlet__remove,
    ${timestampId} .dotedit-contentlet__code {
        background-position: center !important;
        background-repeat: no-repeat !important;
        transition: background-color ${animation},
                    box-shadow ${animation},
                    color ${animation} !important;
    }

    ${timestampId} .dotedit-container__add:focus,
    ${timestampId} .dotedit-contentlet__drag:focus,
    ${timestampId} .dotedit-contentlet__edit:focus,
    ${timestampId} .dotedit-contentlet__remove:focus,
    ${timestampId} .dotedit-contentlet__code:focus {
        outline: none !important;
    }

    ${timestampId} .dotedit-container__add {
        background-image: url(data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjRkZGRkZGIiBoZWlnaHQ9IjI0IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIyNCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4gICAgPHBhdGggZD0iTTE5IDEzaC02djZoLTJ2LTZINXYtMmg2VjVoMnY2aDZ2MnoiLz4gICAgPHBhdGggZD0iTTAgMGgyNHYyNEgweiIgZmlsbD0ibm9uZSIvPjwvc3ZnPg==) !important;
        background-color: var(--color-main) !important;
    }

    ${timestampId} .dotedit-container__add:hover {
        background-color: var(--color-main_mod) !important;
    }

    ${timestampId} .dotedit-container__add:focus {
        background-color: var(--color-main_mod) !important;
    }

    ${timestampId} .dotedit-container__add:active {
        background-color: var(--color-main_mod) !important;
    }

    ${timestampId} button.dotedit-contentlet__drag {
        background-image: url(data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjNDQ0NDQ0IiBoZWlnaHQ9IjE4IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIxOCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczp4bGluaz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94bGluayI+ICAgIDxkZWZzPiAgICAgICAgPHBhdGggZD0iTTAgMGgyNHYyNEgwVjB6IiBpZD0iYSIvPiAgICA8L2RlZnM+ICAgIDxjbGlwUGF0aCBpZD0iYiI+ICAgICAgICA8dXNlIG92ZXJmbG93PSJ2aXNpYmxlIiB4bGluazpocmVmPSIjYSIvPiAgICA8L2NsaXBQYXRoPiAgICA8cGF0aCBjbGlwLXBhdGg9InVybCgjYikiIGQ9Ik0yMCA5SDR2MmgxNlY5ek00IDE1aDE2di0ySDR2MnoiLz48L3N2Zz4=) !important;
        cursor: move !important;
        touch-action: none !important;
    }

    ${timestampId} .dotedit-contentlet__edit {
        background-image: url(data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjNDQ0NDQ0IiBoZWlnaHQ9IjE4IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIxOCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4gICAgPHBhdGggZD0iTTMgMTcuMjVWMjFoMy43NUwxNy44MSA5Ljk0bC0zLjc1LTMuNzVMMyAxNy4yNXpNMjAuNzEgNy4wNGMuMzktLjM5LjM5LTEuMDIgMC0xLjQxbC0yLjM0LTIuMzRjLS4zOS0uMzktMS4wMi0uMzktMS40MSAwbC0xLjgzIDEuODMgMy43NSAzLjc1IDEuODMtMS44M3oiLz4gICAgPHBhdGggZD0iTTAgMGgyNHYyNEgweiIgZmlsbD0ibm9uZSIvPjwvc3ZnPg==) !important;
    }

    ${timestampId} .dotedit-contentlet__remove {
        background-image: url(data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjNDQ0NDQ0IiBoZWlnaHQ9IjE4IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIxOCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4gICAgPHBhdGggZD0iTTE5IDYuNDFMMTcuNTkgNSAxMiAxMC41OSA2LjQxIDUgNSA2LjQxIDEwLjU5IDEyIDUgMTcuNTkgNi40MSAxOSAxMiAxMy40MSAxNy41OSAxOSAxOSAxNy41OSAxMy40MSAxMnoiLz4gICAgPHBhdGggZD0iTTAgMGgyNHYyNEgweiIgZmlsbD0ibm9uZSIvPjwvc3ZnPg==) !important;
    }

    ${timestampId} .dotedit-contentlet__code {
        background-image: url(data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjMDAwMDAwIiBoZWlnaHQ9IjI0IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIyNCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4gICAgPHBhdGggZD0iTTAgMGgyNHYyNEgwVjB6IiBmaWxsPSJub25lIi8+ICAgIDxwYXRoIGQ9Ik05LjQgMTYuNkw0LjggMTJsNC42LTQuNkw4IDZsLTYgNiA2IDYgMS40LTEuNHptNS4yIDBsNC42LTQuNi00LjYtNC42TDE2IDZsNiA2LTYgNi0xLjQtMS40eiIvPjwvc3ZnPg==) !important;
    }

    ${timestampId} .dotedit-menu {
        position: relative !important;
    }

    ${timestampId} .dotedit-menu__list {
        color: #000 !important;
        background-color: #ffffff !important;
        box-shadow: ${mdShadow1} !important;
        font-family: Roboto, "Helvetica Neue", Helvetica, Arial, "Lucida Grande", sans-serif !important;
        font-size: 13px !important;
        list-style: none !important;
        margin: 0 !important;
        min-width: 100px !important;
        opacity: 0 !important;
        padding: 8px 0 !important;
        position: absolute !important;
        right: 0 !important;
        transition: all ${animation} !important;
        visibility: hidden !important;
        z-index:1 !important;
    }

    ${timestampId} .dotedit-menu__list.active {
        opacity: 1 !important;
        visibility: visible !important;
        z-index: 2147483647 !important;
    }

    ${timestampId} .dotedit-menu__item {
        position: relative;
    }

    ${timestampId} .dotedit-menu__item a {
        cursor: pointer !important;
        display: block !important;
        line-height: 16px !important;
        padding: 8px !important;
        white-space: nowrap !important;
    }

    ${timestampId} .dotedit-menu__item a:hover {
        background-color: #e7e7e7 !important;
        text-decoration: none !important;
    }

    ${timestampId} .dotedit-menu__item[dot-title]:hover:after {
        content: attr(dot-title) !important;
        right: 100% !important;
        position: absolute !important;
        top: 4px !important;
        background: rgba(0,0,0,.7);
        font-size: 12px;
        color: #fff;
        padding: 4px;
        border-radius: 3px;
        margin-right: 4px;
        line-height: 14px;
        white-space: nowrap;
    }

    ${timestampId} .dotedit-menu__item a,
    ${timestampId} .dotedit-menu__item a:visited {
        color: inherit !important;
        text-decoration: none !important;
    }

    ${timestampId} .dotedit-menu__item--disabled a,
    ${timestampId} .dotedit-menu__item--disabled a:hover,
    ${timestampId} .dotedit-menu__item--disabled a:active,
    ${timestampId} .dotedit-menu__item--disabled a:focus,
    ${timestampId} .dotedit-menu__item--disabled a:visited {
        color: ${grayLight} !important;
        cursor: not-allowed !important;
        pointer-events: none !important;
    }

    ${timestampId} .loader,
    ${timestampId} .loader:after {
        border-radius: 50% !important;
        height: 32px !important;
        width: 32px !important;
    }

    ${timestampId} .loader {
        animation: load8 1.1s infinite linear !important;
        border-bottom: solid 5px rgba(var(--color-sec_rgb), 0.2) !important;
        border-left: solid 5px var(--color-sec) !important;
        border-right: solid 5px rgba(var(--color-sec_rgb), 0.2) !important;
        border-top: solid 5px rgba(var(--color-sec_rgb), 0.2) !important;
        display: inline-block !important;
        font-size: 10px !important;
        overflow: hidden !important;
        position: relative !important;
        text-indent: -9999em !important;
        vertical-align: middle !important;
    }

    ${timestampId} .loader__overlay {
        align-items: center !important;
        background-color: rgba(255, 255, 255, 0.8) !important;
        bottom: 0 !important;
        display: flex !important;
        justify-content: center !important;
        left: 0 !important;
        overflow: hidden !important;
        position: absolute !important;
        right: 0 !important;
        top: 0 !important;
        z-index: 1 !important;
    }

    @keyframes load8 {
        0% {
            transform: rotate(0deg);
        }
        100% {
            transform: rotate(360deg);
        }
    }
`;
};
