export const EDIT_PAGE_CSS = `
    [data-dot-object="container"] {
        border: solid 1px #f2f2f2;
        min-height: 100px;
        margin: 10px 0
    }

    [data-dot-object="container"].no {
        border-color: red;
        box-shadow: 0 0 20px red;
        border-radious: 2px;
        background-color: #ff00000f;
    }

    [data-dot-object="container"]:hover {
        border-color: #dddddd;
    }

    [data-dot-object="contentlet"] {
        padding: 10px;
    }

    [data-dot-object="contentlet"]:hover {
        background-color: #f2f2f2;
    }

    .dotedit-contentlet__content {
        min-height: 100px;
        position: relative;
    }

    .dotedit-contentlet__drag {
        touch-action: none;
        cursor: move;
    }

    .loader,
    .loader:after {
        border-radius: 50%;
        width: 32px;
        height: 32px;
    }

    .loader {
        display: inline-block;
        vertical-align: middle;
        font-size: 10px;
        position: relative;
        text-indent: -9999em;
        border-top: solid 5px rgba(0, 0, 0, 0.2);
        border-right: solid 5px rgba(0, 0, 0, 0.2);
        border-bottom: solid 5px rgba(0, 0, 0, 0.2);
        border-left: solid 5px #000;
        -webkit-transform: translateZ(0);
        -ms-transform: translateZ(0);
        transform: translateZ(0);
        -webkit-animation: load8 1.1s infinite linear;
        animation: load8 1.1s infinite linear;
        overflow: hidden;
    }

    .loader__overlay {
        align-items: center;
        background-color: rgba(255, 255, 255, 0.8);
        bottom: 0;
        display: flex;
        justify-content: center;
        left: 0;
        overflow: hidden;
        position: absolute;
        right: 0;
        top: 0;
        z-index: 1;
    }

    @-webkit-keyframes load8 {
        0% {
            -webkit-transform: rotate(0deg);
            transform: rotate(0deg);
        }
        100% {
            -webkit-transform: rotate(360deg);
            transform: rotate(360deg);
        }
    }
    @keyframes load8 {
        0% {
            -webkit-transform: rotate(0deg);
            transform: rotate(0deg);
        }
        100% {
            -webkit-transform: rotate(360deg);
            transform: rotate(360deg);
        }
    }
`;
