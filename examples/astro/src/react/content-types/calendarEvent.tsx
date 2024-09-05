import type { DotCMSContentlet } from "@dotcms/types";
import useImageSrc from "@react/hooks/useImageSrc";
import type { FC } from "react";

export type CalendarEventProps = DotCMSContentlet;

export type LocationsAndActivities = {
  locations: any[];
  activities: any[];
};

const extractLocationsAndActivities = (
  contentlet: DotCMSContentlet,
): LocationsAndActivities => {
  const initialValue = {
    locations: [],
    activities: [],
  };

  return (
    contentlet?.reduce((acc: any, { activities, ...location }: any) => {
      // This is a relationship between Contentlets
      // Event -> Location -> Activities
      // Depth 3
      acc.activities = acc.activities.concat(activities);
      acc.locations.push(location);

      return acc;
    }, initialValue) ?? initialValue
  );
};

export const CalendarEvent: FC<CalendarEventProps> = ({
  image,
  title,
  urlMap,
  description,
  location,
}) => {
  const { locations, activities } = extractLocationsAndActivities(location);

  const src = useImageSrc({ src: image?.idPath ?? image });

  return (
    <div className="relative flex bg-clip-border rounded-xl shadow-md w-full flex-row bg-slate-100">
      <div className="relative w-2/5 m-0 overflow-hidden bg-slate-100 rounded-r-none bg-clip-border rounded-xl shrink-0">
        {image && (
          <img
            src={src}
            alt={title}
            className="absolute w-full h-full top-0 left-0"
          />
        )}
      </div>
      <div className="p-6">
        <h4 className="block mb-2 text-2xl antialiased font-semibold leading-snug tracking-normal text-blue-gray-900">
          {title}
        </h4>
        {!!locations.length && !!locations[0]?.title && (
          <div className="block mb-2 text-base antialiased leading-snug tracking-normal text-blue-gray-900 break-all">
            <span className="cursor-auto select-none font-semibold underline">
              Locations:
            </span>
            &nbsp;
            {locations?.map(({ title, url }, index) => {
              return (
                <a key={index} href={url}>
                  <span className="bg-yellow-100 text-yellow-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-yellow-900 dark:text-yellow-300">
                    {title}
                  </span>
                </a>
              );
            })}
          </div>
        )}
        {!!activities.length && !!activities[0]?.title && (
          <div className="block mb-2 text-base antialiased leading-snug tracking-normal text-blue-gray-900 break-all">
            <span className="cursor-auto select-none font-semibold underline">
              Activities:
            </span>
            &nbsp;
            {activities.slice(0, 3).map(({ title, urlMap }, index) => {
              return (
                <a key={index} href={urlMap}>
                  <span className="bg-indigo-100 text-indigo-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded dark:bg-indigo-900 dark:text-indigo-300">
                    {title}
                  </span>
                </a>
              );
            })}
          </div>
        )}
        <div
          className="block mb-8 text-base antialiased font-normal leading-relaxed line-clamp-3"
          dangerouslySetInnerHTML={{ __html: description }}
        />
        <a href={urlMap}>
          <button className="flex items-center gap-2 px-6 py-3 text-xs font-bold text-center uppercase align-middle transition-all rounded-lg select-none disabled:opacity-50 disabled:shadow-none disabled:pointer-events-none hover:bg-gray-900/10 active:bg-gray-900/20">
            Learn More
          </button>
        </a>
      </div>
    </div>
  );
};
