import Link from "next/link";

interface ErrorLayoutProps {
    status: number;
    heading: string;
    body: string;
}

export function ErrorLayout({ status, heading, body }: ErrorLayoutProps) {
    return (
        <div className="flex min-h-dvh w-full items-center justify-center bg-bg px-6">
            <section className="mx-auto max-w-xl text-center">
                <p className="font-display text-[clamp(5rem,10vw,9rem)] font-semibold leading-none text-primary">
                    {status}
                </p>
                <h1 className="mt-2 font-display text-3xl font-semibold tracking-tight text-ink md:text-4xl">
                    {heading}
                </h1>
                <p className="mx-auto mt-4 max-w-md text-lg leading-relaxed text-muted">
                    {body}
                </p>
                <Link
                    href="/"
                    className="mt-8 inline-flex items-center gap-2 rounded-full bg-primary px-6 py-3 font-semibold text-bg transition-colors hover:bg-primary-deep"
                >
                    Back to home
                    <span aria-hidden="true">→</span>
                </Link>
            </section>
        </div>
    );
}
