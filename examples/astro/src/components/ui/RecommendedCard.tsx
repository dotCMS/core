import type { DotCMSBasicContentlet } from "@dotcms/types";
import { EditButton } from "./EditButton";

interface RecommendedCardProps extends DotCMSBasicContentlet {
  urlMap: string;
}

const dateFormatOptions: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "long",
  day: "numeric",
};

export const RecommendedCard = ({
  contentlet,
}: {
  contentlet: RecommendedCardProps;
}) => {
  const { url, title, inode, image, urlMap, modDate } = contentlet;

  return (
    <div className="flex gap-7 relative">
      <EditButton contentlet={contentlet} />
      <a className="relative min-w-32" href={urlMap || url}>
        {image && (
          <img
            src={`/dA/${inode}/250w`}
            alt={title}
            className="object-cover w-[128px] h-[88px]"
          />
        )}
      </a>
      <div className="flex flex-col gap-1">
        <a className="text-sm text-white font-bold" href={urlMap || url}>
          {title}
        </a>
        <time className="text-gray-400">
          {new Date(modDate).toLocaleDateString("en-US", dateFormatOptions)}
        </time>
      </div>
    </div>
  );
};
