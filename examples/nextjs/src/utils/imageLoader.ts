import { dotCMSHost } from "@/config/dotcms.config";

/**
 * Custom Next.js image loader that serves images through dotCMS's image API,
 * which handles resizing/optimization. Paths are normalized to the `/dA/`
 * delivery route and a `{width}w` suffix requests the right size.
 */
const ImageLoader = ({
  src,
  width = 250,
}: {
  src: string;
  width?: number;
}): string => {
  const dotcmsURL = new URL(dotCMSHost || "http://localhost:8080").origin;
  const imageSRC = src.includes("/dA/") ? src : `/dA/${src}`;

  return `${dotcmsURL}${imageSRC}/${width}w`;
};

export default ImageLoader;
