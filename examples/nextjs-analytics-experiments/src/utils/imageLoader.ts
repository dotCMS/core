const ImageLoader = ({ src, width = 250 }: { src: string; width?: number }) => {
  const dotcmsURL = new URL(
    process.env.NEXT_PUBLIC_DOTCMS_HOST ?? "http://localhost:8080",
  ).origin;
  const imageSRC = src.includes("/dA/") ? src : `/dA/${src}`;

  return `${dotcmsURL}${imageSRC}/${width}w`;
};

export default ImageLoader;
