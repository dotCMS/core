import useImageSrc from "@react/hooks/useImageSrc";
import type { DotCMSContentlet } from "@dotcms/types";
import type { FC } from "react";

export type ActivityProps = DotCMSContentlet;

export const Activity: FC<ActivityProps> = ({
  title,
  description,
  image,
  urlTitle,
}) => {
  const src = useImageSrc({ src: image?.idPath ?? image, width: 100 });

  return (
    <article className="p-4 overflow-hidden bg-white rounded shadow-lg">
      {image && (
        <img
          className="w-full"
          src={src}
          width={100}
          height={100}
          alt="Activity Image"
        />
      )}
      <div className="px-6 py-4">
        <p className="mb-2 text-xl font-bold">{title}</p>
        <p className="text-base line-clamp-3">{description}</p>
      </div>
      <div className="px-6 pt-4 pb-2">
        <a
          href={`/activities/${urlTitle || "#"}`}
          className="inline-block px-4 py-2 font-bold text-white bg-blue-500 rounded-full hover:bg-blue-700"
        >
          Link to detail →
        </a>
      </div>
    </article>
  );
};
