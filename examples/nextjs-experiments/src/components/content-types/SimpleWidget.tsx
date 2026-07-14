"use client";

import { useId, useState } from "react";

import { useIsEditMode } from "@/hooks/useIsEditMode";

const TRAVEL_BOT_KEY = "908b8a434ad7e539632b8db57f2967c0";

interface SimpleWidgetProps {
    widgetTitle?: string;
    identifier?: string;
    code?: string;
}

export default function SimpleWidget(props: SimpleWidgetProps) {
    const { widgetTitle, identifier } = props;
    const isEditMode = useIsEditMode();

    if (TRAVEL_BOT_KEY == identifier) {
        return (
            <div className="mx-auto my-6 max-w-lg rounded-2xl border border-line bg-bg p-8 text-center">
                <h2 className="font-display text-2xl font-semibold text-ink">
                    Welcome to TravelBot
                </h2>
                <p className="mt-4 text-muted">
                    TravelBot is built with <span className="font-medium text-ink">dotAI</span>,
                    the dotCMS suite of AI features.
                </p>
                <p className="mt-2 text-muted">
                    Configure the dotAI App to enable dotAI and TravelBot.
                </p>
            </div>
        );
    }

    if (widgetTitle === "Booking Date Selector") {
        return <BookingDateSelector />;
    }

    if (isEditMode) {
        return (
            <div
                role="alert"
                className="rounded-xl border border-line bg-surface p-4 text-sm text-muted"
            >
                <h4 className="font-medium text-ink">Simple Widget: {widgetTitle}</h4>
            </div>
        );
    }

    return null;
}

const DESTINATIONS = [
    "Amalfi Coast, Italy",
    "Kyoto, Japan",
    "Patagonia, Chile",
    "Reykjavík, Iceland",
    "Queenstown, New Zealand",
];

function BookingDateSelector() {
    const baseId = useId();
    const [submitted, setSubmitted] = useState(false);

    if (submitted) {
        return (
            <section className="mx-auto max-w-2xl rounded-2xl bg-primary-deep p-8 text-center text-bg sm:p-10">
                <h3 className="font-display text-2xl font-semibold">
                    Your trip is on hold
                </h3>
                <p className="mx-auto mt-3 max-w-md text-bg/80">
                    This is a demo, so nothing was booked. In a real site this would
                    check availability and take you to checkout.
                </p>
                <button
                    type="button"
                    onClick={() => setSubmitted(false)}
                    className="mt-6 inline-flex items-center gap-1.5 rounded-full bg-accent px-6 py-3 font-semibold text-bg transition-transform duration-300 ease-(--ease-out-quart) hover:-translate-y-0.5"
                >
                    Start over
                </button>
            </section>
        );
    }

    return (
        <section className="rounded-2xl bg-surface p-6 sm:p-8">
            <div className="mb-6 flex flex-col gap-2">
                <span className="eyebrow">Plan your trip</span>
                <h3 className="font-display text-h3 font-semibold text-ink">
                    Check availability
                </h3>
            </div>

            <form
                onSubmit={(e) => {
                    e.preventDefault();
                    setSubmitted(true);
                }}
                className="flex flex-wrap items-end gap-4"
            >
                <Field className="min-w-52 flex-[2]" label="Destination" htmlFor={`${baseId}-dest`}>
                    <select
                        id={`${baseId}-dest`}
                        name="destination"
                        required
                        defaultValue=""
                        className="w-full appearance-none rounded-xl border border-line bg-bg px-4 py-3 text-ink focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
                    >
                        <option value="" disabled>
                            Where are you headed?
                        </option>
                        {DESTINATIONS.map((d) => (
                            <option key={d} value={d}>
                                {d}
                            </option>
                        ))}
                    </select>
                </Field>

                <Field className="min-w-36 flex-1" label="Check in" htmlFor={`${baseId}-in`}>
                    <input
                        id={`${baseId}-in`}
                        name="checkIn"
                        type="date"
                        required
                        className="w-full rounded-xl border border-line bg-bg px-4 py-3 text-ink focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
                    />
                </Field>

                <Field className="min-w-36 flex-1" label="Check out" htmlFor={`${baseId}-out`}>
                    <input
                        id={`${baseId}-out`}
                        name="checkOut"
                        type="date"
                        required
                        className="w-full rounded-xl border border-line bg-bg px-4 py-3 text-ink focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
                    />
                </Field>

                <Field className="min-w-24 flex-1" label="Travelers" htmlFor={`${baseId}-guests`}>
                    <input
                        id={`${baseId}-guests`}
                        name="guests"
                        type="number"
                        min={1}
                        max={12}
                        defaultValue={2}
                        className="w-full rounded-xl border border-line bg-bg px-4 py-3 text-ink focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
                    />
                </Field>

                <button
                    type="submit"
                    className="inline-flex h-[50px] shrink-0 items-center justify-center gap-2 rounded-full bg-primary px-6 font-semibold text-bg transition-colors hover:bg-primary-deep"
                >
                    Search
                    <span aria-hidden="true">→</span>
                </button>
            </form>
        </section>
    );
}

function Field({
    label,
    htmlFor,
    className,
    children,
}: {
    label: string;
    htmlFor: string;
    className?: string;
    children: React.ReactNode;
}) {
    return (
        <div className={className}>
            <label
                htmlFor={htmlFor}
                className="mb-1.5 block text-sm font-medium text-ink"
            >
                {label}
            </label>
            {children}
        </div>
    );
}
