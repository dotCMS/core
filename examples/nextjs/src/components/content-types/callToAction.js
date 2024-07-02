import Link from "next/link";

function CallToAction({ title, subHeading, buttonText1, buttonUrl1, buttonText2, buttonUrl2 }) {
    return (
        <div className="w-full h-full bg-slate-100 p-6 rounded-xl flex flex-col">
            <h2 className="block mb-2 text-2xl antialiased font-semibold leading-snug tracking-normal text-blue-gray-900">
                {title}
            </h2>
            <div
                className="block mb-8 text-base antialiased font-normal leading-relaxed line-clamp-3 flex-1"
                dangerouslySetInnerHTML={{ __html: subHeading }}
            />

            <div className="flex w-full gap-5">
                {(buttonText1 && buttonUrl1) && (
                    <Link href={buttonUrl1}>
                        <div className="bg-purple-500 text-white font-semibold text-sm py-2 px-4 rounded-lg">
                            {buttonText1}
                        </div>
                    </Link>
                )}
                {(buttonText2 && buttonUrl2) && (
                    <Link href={buttonUrl2}>
                        <div className="bg-yellow-500 text-white font-semibold text-sm py-2 px-4 rounded-lg">
                            {buttonText2}
                        </div>
                    </Link>
                )}

            </div>
        </div>
    );
}

export default CallToAction;
