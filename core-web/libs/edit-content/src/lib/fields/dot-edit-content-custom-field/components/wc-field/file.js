class DotUrlSlug extends HTMLElement {

  context = {};
  slugValue = '';

  static template = document.createElement('template');

  constructor() {
      super();
      this.appendChild(DotUrlSlug.template.content.cloneNode(true));

      this.inputElement = this.querySelector('#dot-url-slug');
      this.suggestionElement = this.querySelector('#slug-suggestion-link');
      this.suggestionElement.addEventListener('click', this.handleSuggestionClick.bind(this));
  }

  static get observedAttributes() {
      return ['context'];
  }

  attributeChangedCallback(name, _, newValue) {
      if (name === 'context') {
      this.context = JSON.parse(newValue);
      this.handleInputChange();
      }
  }

  slugify(text) {
      if (!text) return '';
      return text.toString().toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[^\w-]+/g, '')
      .replace(/--+/g, '-')
      .replace(/^-+/, '')
      .replace(/-+$/, '');
  }

  handleInputChange() {
      const currentTitle = this.context?.title || '';
      const newSlug = this.slugify(currentTitle);
      this.slugValue = newSlug;
      this.suggestionElement.innerHTML = `Use: ${newSlug}`;
  }

  handleSuggestionClick(event) {
      event.preventDefault();
      this.inputElement.value = this.slugValue;
  }

  dispatchValuesChanged() {
      const event = new CustomEvent('dotChangeValues', {
      bubbles: true,
      composed: true,
      detail: {
          value: { ...this.context },
      },
      });
      this.dispatchEvent(event);
  }
}

DotUrlSlug.template.innerHTML = `
  <input
      type="text"
      placeholder="URL Title"
      id="dot-url-slug"
      class="border-2 border-primary-500"
  />
  <div class="bg-indigo-900">
      <a id="slug-suggestion-link"></a>
  </div>
`;

customElements.define('dot-url-slug', DotUrlSlug);
