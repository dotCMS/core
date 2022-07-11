import { Component, Prop, h, Host, Event, EventEmitter, State } from '@stencil/core';
import '@material/mwc-icon-button';
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

    @Event() pageThumbnail: EventEmitter<File>;
    @State() previewImg: string;

    iframeId = `iframe_${Math.floor(Date.now() / 1000).toString()}`;
    loadScript = `
        html2canvas(document.body, {
            height: IMG_HEIGHT, // up to how many px you want to get
            windowHeight: IMG_HEIGHT, // up to how many px you want to get
            width: IMG_WIDTH, //e.data.width, // up to how many px you want to get
            windowWidth: IMG_WIDTH, //e.data.width, // up to how many px you want to get
        }).then( (canvas) => {
            canvas.toBlob((blob) => {
                const fileObj = new File([blob], 'image.png');
                window.parent.postMessage({
                    iframeId: '${this.iframeId}',
                    previewImg: canvas.toDataURL(),
                    fileObj
                }, '*')
            });
        }).catch(function (error) {
            console.error('oops, something went wrong!', error);
        });
    ;`;

    componentDidLoad() {
        console.log('----scriptTAG', this.loadScript);
        const iframe = document.querySelector(`#${this.iframeId}`) as HTMLIFrameElement;
        const doc = iframe.contentDocument || iframe.contentWindow.document;

        doc.open();
        doc.write(this.value);
        doc.close();

        const scriptLib = document.createElement('script') as HTMLScriptElement;
        scriptLib.src = '/html/js/html2canvas/html2canvas.min.js';
        scriptLib.type = 'text/javascript';

        scriptLib.onload = () => {
            iframe.addEventListener('load', () => {
                const script: HTMLScriptElement = document.createElement('script');
                script.type = 'text/javascript';
                script.text = this.width
                    ? this.loadScript
                          .replace(/IMG_HEIGHT/g, this.height)
                          .replace(/IMG_WIDTH/g, this.width)
                    : this.loadScript;
                doc.body.appendChild(script);
            });
        };
        doc.body.append(scriptLib);

        window.addEventListener('message', (event) => {
            if (event.data.iframeId !== this.iframeId) return;

            const { previewImg, fileObj } = event.data;
            this.previewImg = previewImg;

            const img = document.createElement('img');
            img.src = previewImg;
            img.style.width = '100%';
            iframe.parentElement.appendChild(img);

            console.log('***webcomponent emit file', img, fileObj);
            this.pageThumbnail.emit(fileObj);
        });
    }

    render() {
        return (
            <Host style={{ display: 'flex', 'flex-direction': 'column', 'align-items': 'center' }}>
                {this.previewImg ? (
                    ''
                ) : (
                    <mwc-circular-progress indeterminate></mwc-circular-progress>
                )}
                <iframe id={this.iframeId} style={{ width: '0', height: '0', border: '0' }} />
            </Host>
        );
    }
}
