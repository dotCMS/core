import Link from "next/link";

interface CallToActionProps {
    title?: string;
    subHeading?: string;
    buttonText1?: string;
    buttonUrl1?: string;
    buttonText2?: string;
    buttonUrl2?: string;
}

function CallToAction({
    title,
    subHeading,
    buttonText1,
    buttonUrl1,
    buttonText2,
    buttonUrl2,
}: CallToActionProps) {
    return (
        <div className="relative isolate flex w-full flex-col justify-center overflow-hidden rounded-3xl bg-primary-deep p-8 text-bg sm:p-12 md:p-16">
            {/* Soft radial highlight for depth, kept subtle. */}
            <div
                aria-hidden="true"
                className="absolute -right-24 -top-24 size-72 rounded-full bg-accent/20 blur-3xl"
            />
            <div className="relative max-w-2xl">
                {title && (
                    <h2 className="font-display text-[clamp(1.85rem,1.2rem+2.4vw,3rem)] font-semibold leading-tight">
                        {title}
                    </h2>
                )}
                {subHeading && (
                    <div
                        className="mt-4 text-lg leading-relaxed text-bg/85 [&_a]:underline"
                        dangerouslySetInnerHTML={{ __html: subHeading }}
                    />
                )}

                {(buttonUrl1 || buttonUrl2) && (
                    <div className="mt-8 flex flex-wrap gap-4">
                        {buttonText1 && buttonUrl1 && (
                            <Link
                                href={buttonUrl1}
                                className="inline-flex items-center gap-2 rounded-full bg-accent px-6 py-3 font-semibold text-bg shadow-lg transition-transform duration-300 ease-(--ease-out-quart) hover:-translate-y-0.5"
                            >
                                {buttonText1}
                                <span aria-hidden="true">→</span>
                            </Link>
                        )}
                        {buttonText2 && buttonUrl2 && (
                            <Link
                                href={buttonUrl2}
                                className="inline-flex items-center rounded-full border border-bg/30 px-6 py-3 font-semibold text-bg transition-colors hover:border-bg/60 hover:bg-bg/10"
                            >
                                {buttonText2}
                            </Link>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

export default CallToAction;
