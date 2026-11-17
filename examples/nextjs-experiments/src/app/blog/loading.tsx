/**
 * An example of an experiment on a route that streams. This file makes Next.js wrap the blog's
 * page in `<Suspense fallback={<Loading />}>`: it is the same as writing that boundary yourself
 * around the part that waits for dotCMS, and the SDK works the same either way:
 *
 *   <Suspense fallback={<p>Loading the blog…</p>}>
 *       <Blog />  // the part that waits for dotCMS, with its DotCMSExperiment
 *   </Suspense>
 *
 * The page answers at once with this fallback, and the blog, with its experiment's markup,
 * arrives when dotCMS answers: a returning visitor is sent to their variant when that part
 * arrives, and the SDK decides the marks as they stream in. The catch-all route renders in one
 * piece, for comparison.
 */
export default function Loading() {
    return <p className="p-8 text-center text-gray-500">Loading the blog…</p>;
}
