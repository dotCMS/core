import Image from "next/image";
import Link from "next/link";

function extractLocationsAndActivities(contentlet) {
    const initialValue = {
        locations: [],
        activities: [],
    };

    return (
        contentlet?.reduce((acc, { activities, ...location }) => {
            acc.activities = acc.activities.concat(activities);
            acc.locations.push(location);

            return acc;
        }, initialValue) ?? initialValue
    );
}

function CalendarEvent({ image, title, urlMap, description, location }) {
    const { locations, activities } = extractLocationsAndActivities(location);

    return (
        <div className="relative flex bg-clip-border rounded-xl shadow-md w-full flex-row bg-slate-100">
            <div className="relative w-2/5 m-0 overflow-hidden bg-slate-100 rounded-r-none bg-clip-border rounded-xl shrink-0">
                {image && (
                    <Image
                        src={image?.idPath ?? image}
                        alt={title}
                        fill={true}
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
                        {locations.map(({ title, url }, index) => {
                            return (
                                <Link key={index} href={url ?? ""}>
                                    <span className="bg-yellow-100 text-yellow-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm dark:bg-yellow-900 dark:text-yellow-300">
                                        {title}
                                    </span>
                                </Link>
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
                        {activities
                            .slice(0, 3)
                            .map(({ title, urlMap }, index) => {
                                return (
                                    <Link key={index} href={urlMap ?? ""}>
                                        <span className="bg-indigo-100 text-indigo-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm dark:bg-indigo-900 dark:text-indigo-300">
                                            {title}
                                        </span>
                                    </Link>
                                );
                            })}
                    </div>
                )}
                <div
                    className="block mb-8 text-base antialiased font-normal leading-relaxed line-clamp-3"
                    dangerouslySetInnerHTML={{ __html: description }}
                />
                <Link href={urlMap}>
                    <div
                        className="flex items-center gap-2 px-6 py-3 text-xs font-bold text-center uppercase align-middle transition-all rounded-lg select-none disabled:opacity-50 disabled:shadow-none disabled:pointer-events-none hover:bg-gray-900/10 active:bg-gray-900/20"
                        type="button"
                    >
                        Learn More
                    </div>
                </Link>
            </div>
        </div>
    );
}

export default CalendarEvent;
