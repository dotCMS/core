import { Component, Prop, h, Host, Event, EventEmitter, State } from '@stencil/core';
import '@material/mwc-circular-progress';

@Component({
    tag: 'dot-html-to-image',
    styleUrl: 'dot-html-to-image.scss',
    shadow: false
})
export class DotHtmlToImage {
    @Prop({ reflect: false })
    value = '';

    @Prop({ reflect: false, mutable: true })
    height = '';

    @Prop({ reflect: false, mutable: true })
    width = '';

    @Event() pageThumbnail: EventEmitter<{
        file: File;
        error?: string;
    }>;
    @State() previewImg: string;
    @State() iframe: HTMLIFrameElement;
    @State() doc: Document;

    boundOnMessageHandler = null;
    iframeId = `iframe_${Math.floor(Date.now() / 1000).toString()}`;
    loadScript = `
        html2canvas(document.body, {
            height: IMG_HEIGHT, // The height of the canvas
            logging: false,
            windowHeight: IMG_HEIGHT, // Window height to use when rendering Element
            width: IMG_WIDTH, // The width of the canvas
            windowWidth: IMG_WIDTH, // Window width to use when rendering Element
        }).then((canvas) => {
            canvas.toBlob((blob) => {
                const fileObj = new File([blob], 'image.png');
                const iframe = parent.document.querySelector('#${this.iframeId}');
                iframe.style.display = 'none';
                window.parent.postMessage({
                    iframeId: '${this.iframeId}',
                    previewImg: canvas.toDataURL(),
                    fileObj
                }, '*')
            });
        }).catch(function (error) {
            window.parent.postMessage({
                iframeId: '${this.iframeId}',
                error
            }, '*')
        });
    ;`;

    componentDidLoad() {
        try {
            this.doc.open();
            this.doc.write(this.value);
            this.doc.close();
        } catch (error) {
            this.pageThumbnail.emit({ file: null, error });
        }
    }

    private onLoad() {
        try {
            this.iframe = document.querySelector(`#${this.iframeId}`) as HTMLIFrameElement;
            this.doc = this.iframe.contentDocument || this.iframe.contentWindow.document;

            const scriptLib = document.createElement('script') as HTMLScriptElement;
            scriptLib.src = '/html/js/html2canvas/html2canvas.min.js';
            scriptLib.type = 'text/javascript';
            this.doc.body.appendChild(scriptLib);

            scriptLib.onload = () => {
                const script: HTMLScriptElement = document.createElement('script');
                script.type = 'text/javascript';
                script.text = this.width
                    ? this.loadScript
                          .replace(/IMG_HEIGHT/g, this.height)
                          .replace(/IMG_WIDTH/g, this.width)
                    : this.loadScript;

                this.doc.body.appendChild(script);

                this.boundOnMessageHandler = this.onMessageHandler.bind(null, this.iframe, this);
                window.addEventListener('message', this.boundOnMessageHandler);
            };
        } catch (error) {
            this.pageThumbnail.emit({ file: null, error });
        }
    }

    render() {
        const iframeStyle = { width: `${this.width}px`, height: `${this.height}px`, opacity: '0' };
        return (
            <Host>
                {!this.previewImg ? (
                    <mwc-circular-progress indeterminate></mwc-circular-progress>
                ) : (
                    ''
                )}

                <iframe style={iframeStyle} onLoad={() => this.onLoad()} id={this.iframeId} />
            </Host>
        );
    }

    disconnectedCallback() {
        window.removeEventListener('message', this.boundOnMessageHandler);
    }

    private onMessageHandler(iframe, component, event) {
        if (event.data.iframeId !== component.iframeId) return;

        if (event.data.error) {
            component.pageThumbnail.emit({ file: null, error: event.data.error });

            return;
        }

        const { previewImg, fileObj } = event.data;
        component.previewImg = previewImg;

        const img = document.createElement('img');
        img.src = previewImg;
        img.style.width = '100%';
        iframe.parentElement.appendChild(img);

        component.pageThumbnail.emit({ file: fileObj });
    }
}
