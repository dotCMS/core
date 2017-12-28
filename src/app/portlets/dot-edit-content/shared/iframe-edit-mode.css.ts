// tslint:disable:max-line-length

const animation = '100ms ease-in';
const mdShadow1 = '0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24)';
const mdShadow3 = '0 10px 20px rgba(0, 0, 0, 0.19), 0 6px 6px rgba(0, 0, 0, 0.23)';

export const EDIT_PAGE_CSS = `
    [data-dot-object="container"] {
        border: solid 1px #53c2f9;
        min-height: 120px;
        margin: 10px 0;
    }

    [data-dot-object="container"].no {
        border-color: red;
        box-shadow: 0 0 20px red;
        border-radious: 2px;
        background-color: #ff00000f;
    }

    [data-dot-object="contentlet"] {
        margin: 36px 20px 20px 20px;
        position: relative;
        padding-top: 25px;
        min-height: 60px;
        transition: background ${animation};
        background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAQElEQVQoU2NkIAIEH/6VxkhIHUjRWlu2WXgVwhSBDMOpEFkRToXoirAqxKYIQyEuRSgK8SmCKySkCKyQGEUghQCguSaB0AmkRwAAAABJRU5ErkJggg==");
    }

    [data-dot-object="container"]:hover [data-dot-object="contentlet"] {
        background: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAQElEQVQoU2NkIAIEH/r5n5GQOpCitXbsjHgVwhSBDMOpEFkRToXoirAqxKYIQyEuRSgK8SmCKySkCKyQGEUghQCQPycYlScX0wAAAABJRU5ErkJggg==");
    }

    .dotedit-contentlet__content {
        min-height: 100px;
        position: relative;
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

    .dotedit-container__toolbar {
        position:relative;
        margin:0 0 -26px 8px;
        display: table;
    }

    .dotedit-container__toolbar button,
    .dotedit-contentlet__toolbar button {
        width: 32px;
        height: 32px;
        border: none;
        border-radius: 16px;
        box-shadow: ${mdShadow1};
        font-size: 0;
        outline: none;
    }

    .dotedit-container__toolbar button:hover,
    .dotedit-contentlet__toolbar button:hover {
        box-shadow: ${mdShadow3};
    }

    .dotedit-container__toolbar button:active,
    .dotedit-contentlet__toolbar button:active {
        box-shadow: ${mdShadow1};
    }

    .dotedit-contentlet__toolbar {
        position: absolute;
        right: 0;
        top: -16px;
        font-size: 0;
        opacity: 0;
        transition: opacity ${animation};
    }

    [data-dot-object="contentlet"]:hover .dotedit-contentlet__toolbar {
        opacity: 1;
    }

     .dotedit-contentlet__toolbar button {
        margin-right: 8px;
     }

    .dotedit-container__add,
    .dotedit-contentlet__drag,
    .dotedit-contentlet__edit,
    .dotedit-contentlet__remove {
        background-position: center;
        background-repeat: no-repeat;
        transition: background-color ${animation},
                    box-shadow ${animation},
                    color ${animation};
    }

    .dotedit-container__add {
        background-image: url(data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjRkZGRkZGIiBoZWlnaHQ9IjI0IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIyNCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4gICAgPHBhdGggZD0iTTE5IDEzaC02djZoLTJ2LTZINXYtMmg2VjVoMnY2aDZ2MnoiLz4gICAgPHBhdGggZD0iTTAgMGgyNHYyNEgweiIgZmlsbD0ibm9uZSIvPjwvc3ZnPg==);
        background-color: #0E80CB;
    }

     .dotedit-container__add:hover {
        background-color: #0b629b;
     }

     .dotedit-container__add:focus {
        background-color: #0b629b
     }

     .dotedit-container__add:active {
        background-color: #07446c;
     }

     .dotedit-contentlet__drag {
        touch-action: none;
        cursor: move;
        background-image: url(data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjNDQ0NDQ0IiBoZWlnaHQ9IjE4IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIxOCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczp4bGluaz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94bGluayI+ICAgIDxkZWZzPiAgICAgICAgPHBhdGggZD0iTTAgMGgyNHYyNEgwVjB6IiBpZD0iYSIvPiAgICA8L2RlZnM+ICAgIDxjbGlwUGF0aCBpZD0iYiI+ICAgICAgICA8dXNlIG92ZXJmbG93PSJ2aXNpYmxlIiB4bGluazpocmVmPSIjYSIvPiAgICA8L2NsaXBQYXRoPiAgICA8cGF0aCBjbGlwLXBhdGg9InVybCgjYikiIGQ9Ik0yMCA5SDR2MmgxNlY5ek00IDE1aDE2di0ySDR2MnoiLz48L3N2Zz4=);
    }

     .dotedit-contentlet__edit {
        background-image: url(data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjNDQ0NDQ0IiBoZWlnaHQ9IjE4IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIxOCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4gICAgPHBhdGggZD0iTTMgMTcuMjVWMjFoMy43NUwxNy44MSA5Ljk0bC0zLjc1LTMuNzVMMyAxNy4yNXpNMjAuNzEgNy4wNGMuMzktLjM5LjM5LTEuMDIgMC0xLjQxbC0yLjM0LTIuMzRjLS4zOS0uMzktMS4wMi0uMzktMS40MSAwbC0xLjgzIDEuODMgMy43NSAzLjc1IDEuODMtMS44M3oiLz4gICAgPHBhdGggZD0iTTAgMGgyNHYyNEgweiIgZmlsbD0ibm9uZSIvPjwvc3ZnPg==);
     }

     .dotedit-contentlet__remove {
        background-image: url(data:image/svg+xml;base64,PHN2ZyBmaWxsPSIjNDQ0NDQ0IiBoZWlnaHQ9IjE4IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHdpZHRoPSIxOCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4gICAgPHBhdGggZD0iTTE5IDYuNDFMMTcuNTkgNSAxMiAxMC41OSA2LjQxIDUgNSA2LjQxIDEwLjU5IDEyIDUgMTcuNTkgNi40MSAxOSAxMiAxMy40MSAxNy41OSAxOSAxOSAxNy41OSAxMy40MSAxMnoiLz4gICAgPHBhdGggZD0iTTAgMGgyNHYyNEgweiIgZmlsbD0ibm9uZSIvPjwvc3ZnPg==);
     }

     .dotedit-container__menu {
          width: 100px;
          background-color: #ffffff;
          box-shadow: ${mdShadow1};
          font-family: Roboto, "Helvetica Neue", Helvetica, Arial, "Lucida Grande", sans-serif;
          font-size: 13px;
          position: absolute;
          z-index:1;
          visibility: hidden;
          opacity: 0;
          transition: opacity ${animation};
     }

     .dotedit-container__toolbar.active .dotedit-container__menu {
        visibility: visible;
        opacity: 1;
     }

     .dotedit-container__menu ul {
        list-style: none;
        margin: 0;
        padding: 0;
        min-width: 100px;
        padding: 8px 0;
     }

     .dotedit-container__menu-item a {
        padding: 8px;
        line-height: 16px;
        display: block;
        cursor: pointer;
     }

     .dotedit-container__menu-item a:hover {
        background-color: #e7e7e7;
     }

     .dotedit-container__menu-item a, .dotedit-container__menu-item a:visited {
        color: inherit;
        text-decoration: none;
     }
`;

