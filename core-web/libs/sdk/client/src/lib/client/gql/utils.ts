export const buildQuery = ({
    pageFragment,
    contentQueries
}: {
    pageFragment: string;
    contentQueries: string;
}) => `
  fragment DotCMSPage on DotPage {
    _map
    canEdit
    canLock
    canRead
    template {
      drawed
    }
    containers {
      path
      identifier
      maxContentlets
      containerStructures {
        contentTypeVar
      }
      containerContentlets {
        uuid
        contentlets {
          _map
        }
      }
    }
    layout {
      header
      footer
      body {
        rows {
          columns {
            leftOffset
            styleClass
            width
            left
            containers {
              identifier
              uuid
            }
          }
        }
      }
    }
    viewAs {
      visitor {
        persona {
          name
        }
      }
      language {
        id
        languageCode
        countryCode
        language
        country
      }
    }
  }

  ${
      pageFragment
          ? `
  fragment ClientPage on DotPage {
    ${pageFragment}
  }
  `
          : ''
  }

  query PageContent($url: String!, $languageId: String, $mode: String) {
    page: page(url: $url, languageId: $languageId, pageMode: $mode) {
      ...DotCMSPage
      ${pageFragment ? '...ClientPage' : ''}
    }

    ${contentQueries}
  }
`;
