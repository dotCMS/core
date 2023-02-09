import { Component, Host, h, Prop, State } from '@stencil/core';
import { DotContentletItem } from '../../models/dot-contentlet-item.model';

@Component({
    tag: 'dot-video-thumbnail',
    styleUrl: 'dot-video-thumbnail.scss'
})
export class DotVideoThumbnail {
    @State() src: string = null;

    /**
     *
     *
     * @type {DotContentletItem}
     * @memberof DotVideoThumbnail
     */
    @Prop() contentlet: DotContentletItem;

    /**
     *
     *
     * @type {string}
     * @memberof DotVideoThumbnail
     */
    @Prop({ reflect: true }) width: number = null;

    render() {
        // If there is a static `width`, there's not need for the image to cover the wrapper
        return <Host>{this.src && <img class={!this.width && 'cover'} src={this.src} />}</Host>;
    }

    componentDidLoad() {
        this.setVideoThumbnail();
    }

    /**
     *
     * @private
     * @memberof DotVideoThumbnail
     */
    setVideoThumbnail() {
        const video = document.createElement('video') as HTMLVideoElement;
        video.preload = 'metadata';
        // The `#t=0.1` is to only download the first frame of the video.
        // https://stackoverflow.com/questions/7323053/dynamically-using-the-first-frame-as-poster-in-html5-video
        video.src = `/dA/${this.contentlet.inode}#t=0.1`;
        video.addEventListener('canplaythrough', () => this.createVideoImage(video));
    }

    private createVideoImage(video: HTMLVideoElement) {
        const { videoWidth, videoHeight } = video;
        // The image thumbnail component is limited to a 500 with
        const width = this.width ?? (videoWidth < 500 ? videoWidth : 500);
        const height = this.determineNewHeight(videoHeight, videoWidth, width);

        console.log('this.width', this.width);
        console.log('width', width);
        console.log('height', height);

        const canvas = document.createElement('canvas') as HTMLCanvasElement;
        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        ctx.drawImage(video, 0, 0, width, height);

        this.src = canvas.toDataURL('image/jpeg');
    }

    /**
     * Calculates the proper height of an image with a custom width, preserving the original aspect ratio.
     *
     * @param originalHeight
     * @param originalWidth
     * @param newWidth
     */
    determineNewHeight(originalHeight, originalWidth, newWidth) {
        return (originalHeight / originalWidth) * newWidth;
    }
}
