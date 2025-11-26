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
export const DotCMSVideo = ({ node }: { node: BlockEditorNode }) => {
    const { data, src, mimeType, width, height } = node.attrs as DotCMSVideoProps;
    const poster = data?.thumbnail;
    const posterAttribute = poster ? { poster } : {};

    return (
        <video controls preload="metadata" width={width} height={height} {...posterAttribute}>
            <track default kind="captions" srcLang="en" />
            <source src={src} type={mimeType} />
            Your browser does not support the <code>video</code> element.
        </video>
    );
};
