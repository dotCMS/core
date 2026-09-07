/** Default HTML shown when the editor loads (demo / onboarding). */
export const EDITOR_DEMO_CONTENT = `
      <h1>Block Editor</h1>
      <p>Welcome! Type <strong>/</strong> anywhere to open the block menu and insert content.</p>

      <h2>Text blocks</h2>
      <p>Regular paragraph text. You can write <strong>bold</strong>, <em>italic</em>, and <code>inline code</code>.</p>
      <blockquote><p>A blockquote stands out from the rest of the content — great for callouts or citations.</p></blockquote>
      <pre><code>const greet = (name: string) => \`Hello, \${name}!\`;
console.log(greet('World'));</code></pre>

      <h2>Lists</h2>
      <ul>
        <li>Bullet item one</li>
        <li>Bullet item two</li>
        <li>Bullet item three</li>
      </ul>
      <ol>
        <li>First ordered item</li>
        <li>Second ordered item</li>
        <li>Third ordered item</li>
      </ol>

      <h2>Links</h2>
      <p>Click once to select a link, double-click to edit it. Try it: <a href="https://tiptap.dev">Tiptap docs</a> or <a href="https://angular.dev">Angular docs</a>. You can also paste a URL directly and it will auto-link.</p>

      <h2>Table</h2>
      <table>
        <thead>
          <tr><th>Feature</th><th>Status</th><th>Notes</th></tr>
        </thead>
        <tbody>
          <tr><td>Slash menu</td><td>✅ Done</td><td>Type / to trigger</td></tr>
          <tr><td>Drag &amp; drop</td><td>✅ Done</td><td>Grab the handle on the left</td></tr>
          <tr><td>Tables</td><td>✅ Done</td><td>Resizable columns</td></tr>
          <tr><td>Links</td><td>✅ Done</td><td>Autolink + dialog</td></tr>
          <tr><td>Images</td><td>✅ Done</td><td>URL or file upload</td></tr>
          <tr><td>Video</td><td>✅ Done</td><td>URL or file upload</td></tr>
        </tbody>
      </table>
    `;
