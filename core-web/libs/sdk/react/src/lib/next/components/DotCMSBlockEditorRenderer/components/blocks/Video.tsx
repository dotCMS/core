import { BlockEditorNode } from '@dotcms/types';

interface DotCMSVideoProps {
    data: {
        identifier: string;
        thumbnail: string;
    };
    src: string;
    mimeType: string;
    width: number;
    height: number;
}

/**
 * Renders a video component for displaying videos.
 *
 * @param props - The properties for the video component.
 * @returns The rendered video component.
 */
export const DotCMSVideo = (node: BlockEditorNode) => {
    const { data, src, mimeType, width, height } = node.attrs as DotCMSVideoProps;

    // Fix this path
    const srcUrl = data?.identifier ? `${''}${src}` : src;
    const poster = data?.thumbnail ? `${''}${data?.thumbnail}` : 'poster-image.jpg';

    return (
        <video controls preload="metadata" poster={poster} width={width} height={height}>
            <track default kind="captions" srcLang="en" />
            <source src={srcUrl} type={mimeType} />
            Your browser does not support the <code>video</code> element.
        </video>
    );
};
