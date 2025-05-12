const ImageLoader = ({ src, width = 250 }) => {
    const dotcmsURL = new URL(process.env.NEXT_PUBLIC_DOTCMS_HOST).origin;
    const imageSRC = src.includes('/dA/') ? src : `/dA/${src}`; // Check if the image is a DotCMS asset or a file asset

    return `${dotcmsURL}${imageSRC}/${width}w`;
};

export default ImageLoader;
