<img src="https://www2.dotcms.com/dA/99fe3769-d649/256w/dotcms.png" title="dotcms - Universal content management system ">


[![Merge Queue](https://github.com/dotCMS/core/actions/workflows/cicd_2-merge-queue.yml/badge.svg)](https://github.com/dotCMS/core/actions/workflows/cicd_2-merge-queue.yml)

[![Trunk](https://github.com/dotCMS/core/actions/workflows/cicd_3-trunk.yml/badge.svg)](https://github.com/dotCMS/core/actions/workflows/cicd_3-trunk.yml)

[![Nightly](https://github.com/dotCMS/core/actions/workflows/cicd_4-nightly.yml/badge.svg)](https://github.com/dotCMS/core/actions/workflows/cicd_4-nightly.yml)


dotCMS is the leading Universal content management system powering thousands of digital experiences for over 150 customers worldwide.

With powerful visual editing tools, multi-tenancy and a tech-agnostic architecture dotCMS empowers technical and content teams to collaborate efficiently and deliver content globally, on any stack, and at any scale.

- **BSL Licensed** - Generous BSL 1.1 licensing terms makes dotCMS free to use in many cases.
- **Universal Visual Editing** - Full featured page editing that works both headlessly and for SSR content, in your SPA or for web page rendering and delivery.
- **Multi-tenancy** - Manage multiple sites in multiple languages, from microsites to brand sites to intranet/extranets or knowledge centers all from a single instance.
- **REST & GraphQL APIs** - instant endpoints for your all your content
- **Search & RAG Built in** - dotCMS indexes all content and assets in Elasticsearch for real time search-abiliity and facet based searches.  dotCMS can also vectorize all content and assets, offering semantic content searching to power AI applications such as chatbots or knowledge centers.
- **Personalization, Rules & A/B Testing** - Empower your marketing teams with targeting and content optimizations.
- **Cloud, Cloud Anywhere or Self-Hosted** - Flexible deployment works with your IT/cloud strategies. 
- **Feature Rich** - Custom content workflows, scriptable APIs, push and static publishing, custom roles and permissions, osgi based plugin architecture.  Do more with more.


dotCMS is available as a docker image or as a cloud based product.

-  [Docker Images](https://www.dotcms.com/download)
-  [dotCMS Cloud](https://www.dotcms.com/product/dotcms-cloud)


---

## Licensing

dotCMS is licensed under the terms of the BSL 1.1 license and all features are free to use by companies, individuals and organizations whose total finances are below $5,000,000 USD.  For more information about the BSL license terms, see the [dotCMS BSL FAQ](http://www.dotcms.com/bsl-faq). 

## Contributing

[CONTRIBUTING](/CONTRIBUTING.md)

## Requirements

For a complete list of requirements, see [this page](http://www.dotcms.com/docs/latest/dotcms-technology-requirements).

## Getting Help

| Source          | Location                                                            |
| --------------- | ------------------------------------------------------------------- |
| Installation    | [Installation](https://www.dotcms.com/docs/latest/installation)         |
| Documentation   | [Documentation](https://www.dotcms.com/docs/latest/table-of-contents)   |
| Videos          | [Helpful Videos](http://www.dotcms.com/videos/)                         |
| Merge Queue   | [Merge Queue](https://github.com/dotCMS/core/queue/main)                          |
| Forums/Listserv | [via Google Groups](https://groups.google.com/forum/#!forum/dotCMS) |
| Twitter         | @dotCMS                                                             |
| Main Site       | [dotCMS.com](https://www.dotcms.com/)                                   |

## Release Tracks

dotCMS GA releases follow CalVer (`YY.0M.0D-NN`, e.g. `26.06.11-01`). On top of that linear
release stream we publish three **floating Docker tags** — *release tracks* — so you can pick
how fresh a release each environment receives:

| Tag                       | Age of release    | Use for                                   |
| ------------------------- | ----------------- | ----------------------------------------- |
| `dotcms/dotcms:latest`    | newest GA (~days) | tracking the latest release               |
| `dotcms/dotcms:standard`  | ~2 weeks old      | a short soak before adopting a release    |
| `dotcms/dotcms:trailing`  | ~4 weeks old      | the most conservative posture             |

Pin the track you want in your deployment manifest, e.g. `image: dotcms/dotcms:standard`, and
you will roll forward automatically as releases age into that track. Pin an exact version
(`dotcms/dotcms:26.06.11-01`) instead if you never want automatic movement.

One engine (under [`cicd/evergreen-tracks/`](cicd/evergreen-tracks/)) re-points every track tag
by image digest, on two triggers: the release pipeline moves `latest` on-demand the moment a GA
ships, and a daily scheduled job ages `standard`/`trailing` forward. Two design choices are worth
understanding:

- **Age is measured from the CalVer date in the version string, not from when the image was
  built or published.** This protects emergency backports: if we cut a patch of an *older*
  release on short notice, it is built today but logically belongs to the older release line.
  Using the build/publish date would make that patch look brand new and let it jump onto the
  `standard`/`trailing` tracks ahead of releases that are genuinely older. Anchoring to the
  embedded CalVer date keeps every release in its true place on the timeline.
- **A track never moves backward automatically, and a release can be "tainted".** If a bad
  release is found, it is tainted so it will not advance onto tracks it has not yet reached —
  a known-bad build can never propagate from `latest` down to `standard`/`trailing`. A track
  can also be **held** (frozen) at a specific version for incident response. Both controls live
  as marker tags in the registry; there is no separate datastore.

> **GitOps / Argo note:** because these tags float (the same tag is re-pointed to newer
> digests over time), a tag reference alone will not trigger a redeploy. Use Argo CD Image
> Updater or a periodic rollout refresh to pick up track movements.

The promotion engine and its scheduled/admin workflows live under
[`cicd/evergreen-tracks/`](cicd/evergreen-tracks/).
