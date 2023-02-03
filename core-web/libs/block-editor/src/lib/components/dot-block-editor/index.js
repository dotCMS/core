'use strict';
Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
function Cr(n, e, t) {
    for (let r = 0; ; r++) {
        if (r == n.childCount || r == e.childCount) return n.childCount == e.childCount ? null : t;
        let i = n.child(r),
            o = e.child(r);
        if (i == o) {
            t += i.nodeSize;
            continue;
        }
        if (!i.sameMarkup(o)) return t;
        if (i.isText && i.text != o.text) {
            for (let s = 0; i.text[s] == o.text[s]; s++) t++;
            return t;
        }
        if (i.content.size || o.content.size) {
            let s = Cr(i.content, o.content, t + 1);
            if (s != null) return s;
        }
        t += i.nodeSize;
    }
}
function Ir(n, e, t, r) {
    for (let i = n.childCount, o = e.childCount; ; ) {
        if (i == 0 || o == 0) return i == o ? null : { a: t, b: r };
        let s = n.child(--i),
            a = e.child(--o),
            l = s.nodeSize;
        if (s == a) {
            (t -= l), (r -= l);
            continue;
        }
        if (!s.sameMarkup(a)) return { a: t, b: r };
        if (s.isText && s.text != a.text) {
            let u = 0,
                c = Math.min(s.text.length, a.text.length);
            for (; u < c && s.text[s.text.length - u - 1] == a.text[a.text.length - u - 1]; )
                u++, t--, r--;
            return { a: t, b: r };
        }
        if (s.content.size || a.content.size) {
            let u = Ir(s.content, a.content, t - 1, r - 1);
            if (u) return u;
        }
        (t -= l), (r -= l);
    }
}
class k {
    constructor(e, t) {
        if (((this.content = e), (this.size = t || 0), t == null))
            for (let r = 0; r < e.length; r++) this.size += e[r].nodeSize;
    }
    nodesBetween(e, t, r, i = 0, o) {
        for (let s = 0, a = 0; a < t; s++) {
            let l = this.content[s],
                u = a + l.nodeSize;
            if (u > e && r(l, i + a, o || null, s) !== !1 && l.content.size) {
                let c = a + 1;
                l.nodesBetween(Math.max(0, e - c), Math.min(l.content.size, t - c), r, i + c);
            }
            a = u;
        }
    }
    descendants(e) {
        this.nodesBetween(0, this.size, e);
    }
    textBetween(e, t, r, i) {
        let o = '',
            s = !0;
        return (
            this.nodesBetween(
                e,
                t,
                (a, l) => {
                    a.isText
                        ? ((o += a.text.slice(Math.max(e, l) - l, t - l)), (s = !r))
                        : a.isLeaf
                        ? (i
                              ? (o += typeof i == 'function' ? i(a) : i)
                              : a.type.spec.leafText && (o += a.type.spec.leafText(a)),
                          (s = !r))
                        : !s && a.isBlock && ((o += r), (s = !0));
                },
                0
            ),
            o
        );
    }
    append(e) {
        if (!e.size) return this;
        if (!this.size) return e;
        let t = this.lastChild,
            r = e.firstChild,
            i = this.content.slice(),
            o = 0;
        for (
            t.isText &&
            t.sameMarkup(r) &&
            ((i[i.length - 1] = t.withText(t.text + r.text)), (o = 1));
            o < e.content.length;
            o++
        )
            i.push(e.content[o]);
        return new k(i, this.size + e.size);
    }
    cut(e, t = this.size) {
        if (e == 0 && t == this.size) return this;
        let r = [],
            i = 0;
        if (t > e)
            for (let o = 0, s = 0; s < t; o++) {
                let a = this.content[o],
                    l = s + a.nodeSize;
                l > e &&
                    ((s < e || l > t) &&
                        (a.isText
                            ? (a = a.cut(Math.max(0, e - s), Math.min(a.text.length, t - s)))
                            : (a = a.cut(
                                  Math.max(0, e - s - 1),
                                  Math.min(a.content.size, t - s - 1)
                              ))),
                    r.push(a),
                    (i += a.nodeSize)),
                    (s = l);
            }
        return new k(r, i);
    }
    cutByIndex(e, t) {
        return e == t
            ? k.empty
            : e == 0 && t == this.content.length
            ? this
            : new k(this.content.slice(e, t));
    }
    replaceChild(e, t) {
        let r = this.content[e];
        if (r == t) return this;
        let i = this.content.slice(),
            o = this.size + t.nodeSize - r.nodeSize;
        return (i[e] = t), new k(i, o);
    }
    addToStart(e) {
        return new k([e].concat(this.content), this.size + e.nodeSize);
    }
    addToEnd(e) {
        return new k(this.content.concat(e), this.size + e.nodeSize);
    }
    eq(e) {
        if (this.content.length != e.content.length) return !1;
        for (let t = 0; t < this.content.length; t++)
            if (!this.content[t].eq(e.content[t])) return !1;
        return !0;
    }
    get firstChild() {
        return this.content.length ? this.content[0] : null;
    }
    get lastChild() {
        return this.content.length ? this.content[this.content.length - 1] : null;
    }
    get childCount() {
        return this.content.length;
    }
    child(e) {
        let t = this.content[e];
        if (!t) throw new RangeError('Index ' + e + ' out of range for ' + this);
        return t;
    }
    maybeChild(e) {
        return this.content[e] || null;
    }
    forEach(e) {
        for (let t = 0, r = 0; t < this.content.length; t++) {
            let i = this.content[t];
            e(i, r, t), (r += i.nodeSize);
        }
    }
    findDiffStart(e, t = 0) {
        return Cr(this, e, t);
    }
    findDiffEnd(e, t = this.size, r = e.size) {
        return Ir(this, e, t, r);
    }
    findIndex(e, t = -1) {
        if (e == 0) return Lt(0, e);
        if (e == this.size) return Lt(this.content.length, e);
        if (e > this.size || e < 0)
            throw new RangeError(`Position ${e} outside of fragment (${this})`);
        for (let r = 0, i = 0; ; r++) {
            let o = this.child(r),
                s = i + o.nodeSize;
            if (s >= e) return s == e || t > 0 ? Lt(r + 1, s) : Lt(r, i);
            i = s;
        }
    }
    toString() {
        return '<' + this.toStringInner() + '>';
    }
    toStringInner() {
        return this.content.join(', ');
    }
    toJSON() {
        return this.content.length ? this.content.map((e) => e.toJSON()) : null;
    }
    static fromJSON(e, t) {
        if (!t) return k.empty;
        if (!Array.isArray(t)) throw new RangeError('Invalid input for Fragment.fromJSON');
        return new k(t.map(e.nodeFromJSON));
    }
    static fromArray(e) {
        if (!e.length) return k.empty;
        let t,
            r = 0;
        for (let i = 0; i < e.length; i++) {
            let o = e[i];
            (r += o.nodeSize),
                i && o.isText && e[i - 1].sameMarkup(o)
                    ? (t || (t = e.slice(0, i)),
                      (t[t.length - 1] = o.withText(t[t.length - 1].text + o.text)))
                    : t && t.push(o);
        }
        return new k(t || e, r);
    }
    static from(e) {
        if (!e) return k.empty;
        if (e instanceof k) return e;
        if (Array.isArray(e)) return this.fromArray(e);
        if (e.attrs) return new k([e], e.nodeSize);
        throw new RangeError(
            'Can not convert ' +
                e +
                ' to a Fragment' +
                (e.nodesBetween
                    ? ' (looks like multiple versions of prosemirror-model were loaded)'
                    : '')
        );
    }
}
k.empty = new k([], 0);
const ln = { index: 0, offset: 0 };
function Lt(n, e) {
    return (ln.index = n), (ln.offset = e), ln;
}
function wn(n, e) {
    if (n === e) return !0;
    if (!(n && typeof n == 'object') || !(e && typeof e == 'object')) return !1;
    let t = Array.isArray(n);
    if (Array.isArray(e) != t) return !1;
    if (t) {
        if (n.length != e.length) return !1;
        for (let r = 0; r < n.length; r++) if (!wn(n[r], e[r])) return !1;
    } else {
        for (let r in n) if (!(r in e) || !wn(n[r], e[r])) return !1;
        for (let r in e) if (!(r in n)) return !1;
    }
    return !0;
}
let re = class {
    constructor(e, t) {
        (this.type = e), (this.attrs = t);
    }
    addToSet(e) {
        let t,
            r = !1;
        for (let i = 0; i < e.length; i++) {
            let o = e[i];
            if (this.eq(o)) return e;
            if (this.type.excludes(o.type)) t || (t = e.slice(0, i));
            else {
                if (o.type.excludes(this.type)) return e;
                !r &&
                    o.type.rank > this.type.rank &&
                    (t || (t = e.slice(0, i)), t.push(this), (r = !0)),
                    t && t.push(o);
            }
        }
        return t || (t = e.slice()), r || t.push(this), t;
    }
    removeFromSet(e) {
        for (let t = 0; t < e.length; t++)
            if (this.eq(e[t])) return e.slice(0, t).concat(e.slice(t + 1));
        return e;
    }
    isInSet(e) {
        for (let t = 0; t < e.length; t++) if (this.eq(e[t])) return !0;
        return !1;
    }
    eq(e) {
        return this == e || (this.type == e.type && wn(this.attrs, e.attrs));
    }
    toJSON() {
        let e = { type: this.type.name };
        for (let t in this.attrs) {
            e.attrs = this.attrs;
            break;
        }
        return e;
    }
    static fromJSON(e, t) {
        if (!t) throw new RangeError('Invalid input for Mark.fromJSON');
        let r = e.marks[t.type];
        if (!r) throw new RangeError(`There is no mark type ${t.type} in this schema`);
        return r.create(t.attrs);
    }
    static sameSet(e, t) {
        if (e == t) return !0;
        if (e.length != t.length) return !1;
        for (let r = 0; r < e.length; r++) if (!e[r].eq(t[r])) return !1;
        return !0;
    }
    static setFrom(e) {
        if (!e || (Array.isArray(e) && e.length == 0)) return re.none;
        if (e instanceof re) return [e];
        let t = e.slice();
        return t.sort((r, i) => r.type.rank - i.type.rank), t;
    }
};
re.none = [];
class Bi extends Error {}
class E {
    constructor(e, t, r) {
        (this.content = e), (this.openStart = t), (this.openEnd = r);
    }
    get size() {
        return this.content.size - this.openStart - this.openEnd;
    }
    insertAt(e, t) {
        let r = Rr(this.content, e + this.openStart, t);
        return r && new E(r, this.openStart, this.openEnd);
    }
    removeBetween(e, t) {
        return new E(
            Nr(this.content, e + this.openStart, t + this.openStart),
            this.openStart,
            this.openEnd
        );
    }
    eq(e) {
        return (
            this.content.eq(e.content) && this.openStart == e.openStart && this.openEnd == e.openEnd
        );
    }
    toString() {
        return this.content + '(' + this.openStart + ',' + this.openEnd + ')';
    }
    toJSON() {
        if (!this.content.size) return null;
        let e = { content: this.content.toJSON() };
        return (
            this.openStart > 0 && (e.openStart = this.openStart),
            this.openEnd > 0 && (e.openEnd = this.openEnd),
            e
        );
    }
    static fromJSON(e, t) {
        if (!t) return E.empty;
        let r = t.openStart || 0,
            i = t.openEnd || 0;
        if (typeof r != 'number' || typeof i != 'number')
            throw new RangeError('Invalid input for Slice.fromJSON');
        return new E(k.fromJSON(e, t.content), r, i);
    }
    static maxOpen(e, t = !0) {
        let r = 0,
            i = 0;
        for (
            let o = e.firstChild;
            o && !o.isLeaf && (t || !o.type.spec.isolating);
            o = o.firstChild
        )
            r++;
        for (let o = e.lastChild; o && !o.isLeaf && (t || !o.type.spec.isolating); o = o.lastChild)
            i++;
        return new E(e, r, i);
    }
}
E.empty = new E(k.empty, 0, 0);
function Nr(n, e, t) {
    let { index: r, offset: i } = n.findIndex(e),
        o = n.maybeChild(r),
        { index: s, offset: a } = n.findIndex(t);
    if (i == e || o.isText) {
        if (a != t && !n.child(s).isText) throw new RangeError('Removing non-flat range');
        return n.cut(0, e).append(n.cut(t));
    }
    if (r != s) throw new RangeError('Removing non-flat range');
    return n.replaceChild(r, o.copy(Nr(o.content, e - i - 1, t - i - 1)));
}
function Rr(n, e, t, r) {
    let { index: i, offset: o } = n.findIndex(e),
        s = n.maybeChild(i);
    if (o == e || s.isText)
        return r && !r.canReplace(i, i, t) ? null : n.cut(0, e).append(t).append(n.cut(e));
    let a = Rr(s.content, e - o - 1, t);
    return a && n.replaceChild(i, s.copy(a));
}
class bn {
    constructor(e, t, r) {
        (this.$from = e), (this.$to = t), (this.depth = r);
    }
    get start() {
        return this.$from.before(this.depth + 1);
    }
    get end() {
        return this.$to.after(this.depth + 1);
    }
    get parent() {
        return this.$from.node(this.depth);
    }
    get startIndex() {
        return this.$from.index(this.depth);
    }
    get endIndex() {
        return this.$to.indexAfter(this.depth);
    }
}
class Ut {
    constructor(e, t) {
        (this.schema = e),
            (this.rules = t),
            (this.tags = []),
            (this.styles = []),
            t.forEach((r) => {
                r.tag ? this.tags.push(r) : r.style && this.styles.push(r);
            }),
            (this.normalizeLists = !this.tags.some((r) => {
                if (!/^(ul|ol)\b/.test(r.tag) || !r.node) return !1;
                let i = e.nodes[r.node];
                return i.contentMatch.matchType(i);
            }));
    }
    parse(e, t = {}) {
        let r = new Hn(this, t, !1);
        return r.addAll(e, t.from, t.to), r.finish();
    }
    parseSlice(e, t = {}) {
        let r = new Hn(this, t, !0);
        return r.addAll(e, t.from, t.to), E.maxOpen(r.finish());
    }
    matchTag(e, t, r) {
        for (let i = r ? this.tags.indexOf(r) + 1 : 0; i < this.tags.length; i++) {
            let o = this.tags[i];
            if (
                Li(e, o.tag) &&
                (o.namespace === void 0 || e.namespaceURI == o.namespace) &&
                (!o.context || t.matchesContext(o.context))
            ) {
                if (o.getAttrs) {
                    let s = o.getAttrs(e);
                    if (s === !1) continue;
                    o.attrs = s || void 0;
                }
                return o;
            }
        }
    }
    matchStyle(e, t, r, i) {
        for (let o = i ? this.styles.indexOf(i) + 1 : 0; o < this.styles.length; o++) {
            let s = this.styles[o],
                a = s.style;
            if (
                !(
                    a.indexOf(e) != 0 ||
                    (s.context && !r.matchesContext(s.context)) ||
                    (a.length > e.length &&
                        (a.charCodeAt(e.length) != 61 || a.slice(e.length + 1) != t))
                )
            ) {
                if (s.getAttrs) {
                    let l = s.getAttrs(t);
                    if (l === !1) continue;
                    s.attrs = l || void 0;
                }
                return s;
            }
        }
    }
    static schemaRules(e) {
        let t = [];
        function r(i) {
            let o = i.priority == null ? 50 : i.priority,
                s = 0;
            for (; s < t.length; s++) {
                let a = t[s];
                if ((a.priority == null ? 50 : a.priority) < o) break;
            }
            t.splice(s, 0, i);
        }
        for (let i in e.marks) {
            let o = e.marks[i].spec.parseDOM;
            o &&
                o.forEach((s) => {
                    r((s = qn(s))), s.mark || s.ignore || s.clearMark || (s.mark = i);
                });
        }
        for (let i in e.nodes) {
            let o = e.nodes[i].spec.parseDOM;
            o &&
                o.forEach((s) => {
                    r((s = qn(s))), s.node || s.ignore || s.mark || (s.node = i);
                });
        }
        return t;
    }
    static fromSchema(e) {
        return e.cached.domParser || (e.cached.domParser = new Ut(e, Ut.schemaRules(e)));
    }
}
const Dr = {
        address: !0,
        article: !0,
        aside: !0,
        blockquote: !0,
        canvas: !0,
        dd: !0,
        div: !0,
        dl: !0,
        fieldset: !0,
        figcaption: !0,
        figure: !0,
        footer: !0,
        form: !0,
        h1: !0,
        h2: !0,
        h3: !0,
        h4: !0,
        h5: !0,
        h6: !0,
        header: !0,
        hgroup: !0,
        hr: !0,
        li: !0,
        noscript: !0,
        ol: !0,
        output: !0,
        p: !0,
        pre: !0,
        section: !0,
        table: !0,
        tfoot: !0,
        ul: !0
    },
    zi = { head: !0, noscript: !0, object: !0, script: !0, style: !0, title: !0 },
    Pr = { ol: !0, ul: !0 },
    Kt = 1,
    Gt = 2,
    pt = 4;
function Jn(n, e, t) {
    return e != null
        ? (e ? Kt : 0) | (e === 'full' ? Gt : 0)
        : n && n.whitespace == 'pre'
        ? Kt | Gt
        : t & ~pt;
}
class $t {
    constructor(e, t, r, i, o, s, a) {
        (this.type = e),
            (this.attrs = t),
            (this.marks = r),
            (this.pendingMarks = i),
            (this.solid = o),
            (this.options = a),
            (this.content = []),
            (this.activeMarks = re.none),
            (this.stashMarks = []),
            (this.match = s || (a & pt ? null : e.contentMatch));
    }
    findWrapping(e) {
        if (!this.match) {
            if (!this.type) return [];
            let t = this.type.contentMatch.fillBefore(k.from(e));
            if (t) this.match = this.type.contentMatch.matchFragment(t);
            else {
                let r = this.type.contentMatch,
                    i;
                return (i = r.findWrapping(e.type)) ? ((this.match = r), i) : null;
            }
        }
        return this.match.findWrapping(e.type);
    }
    finish(e) {
        if (!(this.options & Kt)) {
            let r = this.content[this.content.length - 1],
                i;
            if (r && r.isText && (i = /[ \t\r\n\u000c]+$/.exec(r.text))) {
                let o = r;
                r.text.length == i[0].length
                    ? this.content.pop()
                    : (this.content[this.content.length - 1] = o.withText(
                          o.text.slice(0, o.text.length - i[0].length)
                      ));
            }
        }
        let t = k.from(this.content);
        return (
            !e && this.match && (t = t.append(this.match.fillBefore(k.empty, !0))),
            this.type ? this.type.create(this.attrs, t, this.marks) : t
        );
    }
    popFromStashMark(e) {
        for (let t = this.stashMarks.length - 1; t >= 0; t--)
            if (e.eq(this.stashMarks[t])) return this.stashMarks.splice(t, 1)[0];
    }
    applyPending(e) {
        for (let t = 0, r = this.pendingMarks; t < r.length; t++) {
            let i = r[t];
            (this.type ? this.type.allowsMarkType(i.type) : ji(i.type, e)) &&
                !i.isInSet(this.activeMarks) &&
                ((this.activeMarks = i.addToSet(this.activeMarks)),
                (this.pendingMarks = i.removeFromSet(this.pendingMarks)));
        }
    }
    inlineContext(e) {
        return this.type
            ? this.type.inlineContent
            : this.content.length
            ? this.content[0].isInline
            : e.parentNode && !Dr.hasOwnProperty(e.parentNode.nodeName.toLowerCase());
    }
}
class Hn {
    constructor(e, t, r) {
        (this.parser = e), (this.options = t), (this.isOpen = r), (this.open = 0);
        let i = t.topNode,
            o,
            s = Jn(null, t.preserveWhitespace, 0) | (r ? pt : 0);
        i
            ? (o = new $t(
                  i.type,
                  i.attrs,
                  re.none,
                  re.none,
                  !0,
                  t.topMatch || i.type.contentMatch,
                  s
              ))
            : r
            ? (o = new $t(null, null, re.none, re.none, !0, null, s))
            : (o = new $t(e.schema.topNodeType, null, re.none, re.none, !0, null, s)),
            (this.nodes = [o]),
            (this.find = t.findPositions),
            (this.needsBlock = !1);
    }
    get top() {
        return this.nodes[this.open];
    }
    addDOM(e) {
        if (e.nodeType == 3) this.addTextNode(e);
        else if (e.nodeType == 1) {
            let t = e.getAttribute('style');
            if (!t) this.addElement(e);
            else {
                let r = this.readStyles($i(t));
                if (!r) return;
                let [i, o] = r,
                    s = this.top;
                for (let a = 0; a < o.length; a++) this.removePendingMark(o[a], s);
                for (let a = 0; a < i.length; a++) this.addPendingMark(i[a]);
                this.addElement(e);
                for (let a = 0; a < i.length; a++) this.removePendingMark(i[a], s);
                for (let a = 0; a < o.length; a++) this.addPendingMark(o[a]);
            }
        }
    }
    addTextNode(e) {
        let t = e.nodeValue,
            r = this.top;
        if (r.options & Gt || r.inlineContext(e) || /[^ \t\r\n\u000c]/.test(t)) {
            if (r.options & Kt)
                r.options & Gt
                    ? (t = t.replace(
                          /\r\n?/g,
                          `
`
                      ))
                    : (t = t.replace(/\r?\n|\r/g, ' '));
            else if (
                ((t = t.replace(/[ \t\r\n\u000c]+/g, ' ')),
                /^[ \t\r\n\u000c]/.test(t) && this.open == this.nodes.length - 1)
            ) {
                let i = r.content[r.content.length - 1],
                    o = e.previousSibling;
                (!i ||
                    (o && o.nodeName == 'BR') ||
                    (i.isText && /[ \t\r\n\u000c]$/.test(i.text))) &&
                    (t = t.slice(1));
            }
            t && this.insertNode(this.parser.schema.text(t)), this.findInText(e);
        } else this.findInside(e);
    }
    addElement(e, t) {
        let r = e.nodeName.toLowerCase(),
            i;
        Pr.hasOwnProperty(r) && this.parser.normalizeLists && Fi(e);
        let o =
            (this.options.ruleFromNode && this.options.ruleFromNode(e)) ||
            (i = this.parser.matchTag(e, this, t));
        if (o ? o.ignore : zi.hasOwnProperty(r)) this.findInside(e), this.ignoreFallback(e);
        else if (!o || o.skip || o.closeParent) {
            o && o.closeParent
                ? (this.open = Math.max(0, this.open - 1))
                : o && o.skip.nodeType && (e = o.skip);
            let s,
                a = this.top,
                l = this.needsBlock;
            if (Dr.hasOwnProperty(r))
                a.content.length &&
                    a.content[0].isInline &&
                    this.open &&
                    (this.open--, (a = this.top)),
                    (s = !0),
                    a.type || (this.needsBlock = !0);
            else if (!e.firstChild) {
                this.leafFallback(e);
                return;
            }
            this.addAll(e), s && this.sync(a), (this.needsBlock = l);
        } else this.addElementByRule(e, o, o.consuming === !1 ? i : void 0);
    }
    leafFallback(e) {
        e.nodeName == 'BR' &&
            this.top.type &&
            this.top.type.inlineContent &&
            this.addTextNode(
                e.ownerDocument.createTextNode(`
`)
            );
    }
    ignoreFallback(e) {
        e.nodeName == 'BR' &&
            (!this.top.type || !this.top.type.inlineContent) &&
            this.findPlace(this.parser.schema.text('-'));
    }
    readStyles(e) {
        let t = re.none,
            r = re.none;
        e: for (let i = 0; i < e.length; i += 2)
            for (let o = void 0; ; ) {
                let s = this.parser.matchStyle(e[i], e[i + 1], this, o);
                if (!s) continue e;
                if (s.ignore) return null;
                if (
                    (s.clearMark
                        ? this.top.pendingMarks.forEach((a) => {
                              s.clearMark(a) && (r = a.addToSet(r));
                          })
                        : (t = this.parser.schema.marks[s.mark].create(s.attrs).addToSet(t)),
                    s.consuming === !1)
                )
                    o = s;
                else break;
            }
        return [t, r];
    }
    addElementByRule(e, t, r) {
        let i, o, s;
        t.node
            ? ((o = this.parser.schema.nodes[t.node]),
              o.isLeaf
                  ? this.insertNode(o.create(t.attrs)) || this.leafFallback(e)
                  : (i = this.enter(o, t.attrs || null, t.preserveWhitespace)))
            : ((s = this.parser.schema.marks[t.mark].create(t.attrs)), this.addPendingMark(s));
        let a = this.top;
        if (o && o.isLeaf) this.findInside(e);
        else if (r) this.addElement(e, r);
        else if (t.getContent)
            this.findInside(e),
                t.getContent(e, this.parser.schema).forEach((l) => this.insertNode(l));
        else {
            let l = e;
            typeof t.contentElement == 'string'
                ? (l = e.querySelector(t.contentElement))
                : typeof t.contentElement == 'function'
                ? (l = t.contentElement(e))
                : t.contentElement && (l = t.contentElement),
                this.findAround(e, l, !0),
                this.addAll(l);
        }
        i && this.sync(a) && this.open--, s && this.removePendingMark(s, a);
    }
    addAll(e, t, r) {
        let i = t || 0;
        for (
            let o = t ? e.childNodes[t] : e.firstChild, s = r == null ? null : e.childNodes[r];
            o != s;
            o = o.nextSibling, ++i
        )
            this.findAtPoint(e, i), this.addDOM(o);
        this.findAtPoint(e, i);
    }
    findPlace(e) {
        let t, r;
        for (let i = this.open; i >= 0; i--) {
            let o = this.nodes[i],
                s = o.findWrapping(e);
            if ((s && (!t || t.length > s.length) && ((t = s), (r = o), !s.length)) || o.solid)
                break;
        }
        if (!t) return !1;
        this.sync(r);
        for (let i = 0; i < t.length; i++) this.enterInner(t[i], null, !1);
        return !0;
    }
    insertNode(e) {
        if (e.isInline && this.needsBlock && !this.top.type) {
            let t = this.textblockFromContext();
            t && this.enterInner(t);
        }
        if (this.findPlace(e)) {
            this.closeExtra();
            let t = this.top;
            t.applyPending(e.type), t.match && (t.match = t.match.matchType(e.type));
            let r = t.activeMarks;
            for (let i = 0; i < e.marks.length; i++)
                (!t.type || t.type.allowsMarkType(e.marks[i].type)) && (r = e.marks[i].addToSet(r));
            return t.content.push(e.mark(r)), !0;
        }
        return !1;
    }
    enter(e, t, r) {
        let i = this.findPlace(e.create(t));
        return i && this.enterInner(e, t, !0, r), i;
    }
    enterInner(e, t = null, r = !1, i) {
        this.closeExtra();
        let o = this.top;
        o.applyPending(e), (o.match = o.match && o.match.matchType(e));
        let s = Jn(e, i, o.options);
        o.options & pt && o.content.length == 0 && (s |= pt),
            this.nodes.push(new $t(e, t, o.activeMarks, o.pendingMarks, r, null, s)),
            this.open++;
    }
    closeExtra(e = !1) {
        let t = this.nodes.length - 1;
        if (t > this.open) {
            for (; t > this.open; t--) this.nodes[t - 1].content.push(this.nodes[t].finish(e));
            this.nodes.length = this.open + 1;
        }
    }
    finish() {
        return (
            (this.open = 0),
            this.closeExtra(this.isOpen),
            this.nodes[0].finish(this.isOpen || this.options.topOpen)
        );
    }
    sync(e) {
        for (let t = this.open; t >= 0; t--) if (this.nodes[t] == e) return (this.open = t), !0;
        return !1;
    }
    get currentPos() {
        this.closeExtra();
        let e = 0;
        for (let t = this.open; t >= 0; t--) {
            let r = this.nodes[t].content;
            for (let i = r.length - 1; i >= 0; i--) e += r[i].nodeSize;
            t && e++;
        }
        return e;
    }
    findAtPoint(e, t) {
        if (this.find)
            for (let r = 0; r < this.find.length; r++)
                this.find[r].node == e &&
                    this.find[r].offset == t &&
                    (this.find[r].pos = this.currentPos);
    }
    findInside(e) {
        if (this.find)
            for (let t = 0; t < this.find.length; t++)
                this.find[t].pos == null &&
                    e.nodeType == 1 &&
                    e.contains(this.find[t].node) &&
                    (this.find[t].pos = this.currentPos);
    }
    findAround(e, t, r) {
        if (e != t && this.find)
            for (let i = 0; i < this.find.length; i++)
                this.find[i].pos == null &&
                    e.nodeType == 1 &&
                    e.contains(this.find[i].node) &&
                    t.compareDocumentPosition(this.find[i].node) & (r ? 2 : 4) &&
                    (this.find[i].pos = this.currentPos);
    }
    findInText(e) {
        if (this.find)
            for (let t = 0; t < this.find.length; t++)
                this.find[t].node == e &&
                    (this.find[t].pos =
                        this.currentPos - (e.nodeValue.length - this.find[t].offset));
    }
    matchesContext(e) {
        if (e.indexOf('|') > -1) return e.split(/\s*\|\s*/).some(this.matchesContext, this);
        let t = e.split('/'),
            r = this.options.context,
            i = !this.isOpen && (!r || r.parent.type == this.nodes[0].type),
            o = -(r ? r.depth + 1 : 0) + (i ? 0 : 1),
            s = (a, l) => {
                for (; a >= 0; a--) {
                    let u = t[a];
                    if (u == '') {
                        if (a == t.length - 1 || a == 0) continue;
                        for (; l >= o; l--) if (s(a - 1, l)) return !0;
                        return !1;
                    } else {
                        let c =
                            l > 0 || (l == 0 && i)
                                ? this.nodes[l].type
                                : r && l >= o
                                ? r.node(l - o).type
                                : null;
                        if (!c || (c.name != u && c.groups.indexOf(u) == -1)) return !1;
                        l--;
                    }
                }
                return !0;
            };
        return s(t.length - 1, this.open);
    }
    textblockFromContext() {
        let e = this.options.context;
        if (e)
            for (let t = e.depth; t >= 0; t--) {
                let r = e.node(t).contentMatchAt(e.indexAfter(t)).defaultType;
                if (r && r.isTextblock && r.defaultAttrs) return r;
            }
        for (let t in this.parser.schema.nodes) {
            let r = this.parser.schema.nodes[t];
            if (r.isTextblock && r.defaultAttrs) return r;
        }
    }
    addPendingMark(e) {
        let t = Vi(e, this.top.pendingMarks);
        t && this.top.stashMarks.push(t),
            (this.top.pendingMarks = e.addToSet(this.top.pendingMarks));
    }
    removePendingMark(e, t) {
        for (let r = this.open; r >= 0; r--) {
            let i = this.nodes[r];
            if (i.pendingMarks.lastIndexOf(e) > -1)
                i.pendingMarks = e.removeFromSet(i.pendingMarks);
            else {
                i.activeMarks = e.removeFromSet(i.activeMarks);
                let s = i.popFromStashMark(e);
                s &&
                    i.type &&
                    i.type.allowsMarkType(s.type) &&
                    (i.activeMarks = s.addToSet(i.activeMarks));
            }
            if (i == t) break;
        }
    }
}
function Fi(n) {
    for (let e = n.firstChild, t = null; e; e = e.nextSibling) {
        let r = e.nodeType == 1 ? e.nodeName.toLowerCase() : null;
        r && Pr.hasOwnProperty(r) && t
            ? (t.appendChild(e), (e = t))
            : r == 'li'
            ? (t = e)
            : r && (t = null);
    }
}
function Li(n, e) {
    return (
        n.matches ||
        n.msMatchesSelector ||
        n.webkitMatchesSelector ||
        n.mozMatchesSelector
    ).call(n, e);
}
function $i(n) {
    let e = /\s*([\w-]+)\s*:\s*([^;]+)/g,
        t,
        r = [];
    for (; (t = e.exec(n)); ) r.push(t[1], t[2].trim());
    return r;
}
function qn(n) {
    let e = {};
    for (let t in n) e[t] = n[t];
    return e;
}
function ji(n, e) {
    let t = e.schema.nodes;
    for (let r in t) {
        let i = t[r];
        if (!i.allowsMarkType(n)) continue;
        let o = [],
            s = (a) => {
                o.push(a);
                for (let l = 0; l < a.edgeCount; l++) {
                    let { type: u, next: c } = a.edge(l);
                    if (u == e || (o.indexOf(c) < 0 && s(c))) return !0;
                }
            };
        if (s(i.contentMatch)) return !0;
    }
}
function Vi(n, e) {
    for (let t = 0; t < e.length; t++) if (n.eq(e[t])) return e[t];
}
const Br = 65535,
    zr = Math.pow(2, 16);
function Wi(n, e) {
    return n + e * zr;
}
function Un(n) {
    return n & Br;
}
function Ji(n) {
    return (n - (n & Br)) / zr;
}
const Fr = 1,
    Lr = 2,
    Jt = 4,
    $r = 8;
class Kn {
    constructor(e, t, r) {
        (this.pos = e), (this.delInfo = t), (this.recover = r);
    }
    get deleted() {
        return (this.delInfo & $r) > 0;
    }
    get deletedBefore() {
        return (this.delInfo & (Fr | Jt)) > 0;
    }
    get deletedAfter() {
        return (this.delInfo & (Lr | Jt)) > 0;
    }
    get deletedAcross() {
        return (this.delInfo & Jt) > 0;
    }
}
class oe {
    constructor(e, t = !1) {
        if (((this.ranges = e), (this.inverted = t), !e.length && oe.empty)) return oe.empty;
    }
    recover(e) {
        let t = 0,
            r = Un(e);
        if (!this.inverted)
            for (let i = 0; i < r; i++) t += this.ranges[i * 3 + 2] - this.ranges[i * 3 + 1];
        return this.ranges[r * 3] + t + Ji(e);
    }
    mapResult(e, t = 1) {
        return this._map(e, t, !1);
    }
    map(e, t = 1) {
        return this._map(e, t, !0);
    }
    _map(e, t, r) {
        let i = 0,
            o = this.inverted ? 2 : 1,
            s = this.inverted ? 1 : 2;
        for (let a = 0; a < this.ranges.length; a += 3) {
            let l = this.ranges[a] - (this.inverted ? i : 0);
            if (l > e) break;
            let u = this.ranges[a + o],
                c = this.ranges[a + s],
                d = l + u;
            if (e <= d) {
                let p = u ? (e == l ? -1 : e == d ? 1 : t) : t,
                    h = l + i + (p < 0 ? 0 : c);
                if (r) return h;
                let v = e == (t < 0 ? l : d) ? null : Wi(a / 3, e - l),
                    g = e == l ? Lr : e == d ? Fr : Jt;
                return (t < 0 ? e != l : e != d) && (g |= $r), new Kn(h, g, v);
            }
            i += c - u;
        }
        return r ? e + i : new Kn(e + i, 0, null);
    }
    touches(e, t) {
        let r = 0,
            i = Un(t),
            o = this.inverted ? 2 : 1,
            s = this.inverted ? 1 : 2;
        for (let a = 0; a < this.ranges.length; a += 3) {
            let l = this.ranges[a] - (this.inverted ? r : 0);
            if (l > e) break;
            let u = this.ranges[a + o],
                c = l + u;
            if (e <= c && a == i * 3) return !0;
            r += this.ranges[a + s] - u;
        }
        return !1;
    }
    forEach(e) {
        let t = this.inverted ? 2 : 1,
            r = this.inverted ? 1 : 2;
        for (let i = 0, o = 0; i < this.ranges.length; i += 3) {
            let s = this.ranges[i],
                a = s - (this.inverted ? o : 0),
                l = s + (this.inverted ? 0 : o),
                u = this.ranges[i + t],
                c = this.ranges[i + r];
            e(a, a + u, l, l + c), (o += c - u);
        }
    }
    invert() {
        return new oe(this.ranges, !this.inverted);
    }
    toString() {
        return (this.inverted ? '-' : '') + JSON.stringify(this.ranges);
    }
    static offset(e) {
        return e == 0 ? oe.empty : new oe(e < 0 ? [0, -e, 0] : [0, 0, e]);
    }
}
oe.empty = new oe([]);
const cn = Object.create(null);
class _ {
    getMap() {
        return oe.empty;
    }
    merge(e) {
        return null;
    }
    static fromJSON(e, t) {
        if (!t || !t.stepType) throw new RangeError('Invalid input for Step.fromJSON');
        let r = cn[t.stepType];
        if (!r) throw new RangeError(`No step type ${t.stepType} defined`);
        return r.fromJSON(e, t);
    }
    static jsonID(e, t) {
        if (e in cn) throw new RangeError('Duplicate use of step JSON ID ' + e);
        return (cn[e] = t), (t.prototype.jsonID = e), t;
    }
}
class j {
    constructor(e, t) {
        (this.doc = e), (this.failed = t);
    }
    static ok(e) {
        return new j(e, null);
    }
    static fail(e) {
        return new j(null, e);
    }
    static fromReplace(e, t, r, i) {
        try {
            return j.ok(e.replace(t, r, i));
        } catch (o) {
            if (o instanceof Bi) return j.fail(o.message);
            throw o;
        }
    }
}
function Cn(n, e, t) {
    let r = [];
    for (let i = 0; i < n.childCount; i++) {
        let o = n.child(i);
        o.content.size && (o = o.copy(Cn(o.content, e, o))),
            o.isInline && (o = e(o, t, i)),
            r.push(o);
    }
    return k.fromArray(r);
}
class ze extends _ {
    constructor(e, t, r) {
        super(), (this.from = e), (this.to = t), (this.mark = r);
    }
    apply(e) {
        let t = e.slice(this.from, this.to),
            r = e.resolve(this.from),
            i = r.node(r.sharedDepth(this.to)),
            o = new E(
                Cn(
                    t.content,
                    (s, a) =>
                        !s.isAtom || !a.type.allowsMarkType(this.mark.type)
                            ? s
                            : s.mark(this.mark.addToSet(s.marks)),
                    i
                ),
                t.openStart,
                t.openEnd
            );
        return j.fromReplace(e, this.from, this.to, o);
    }
    invert() {
        return new Fe(this.from, this.to, this.mark);
    }
    map(e) {
        let t = e.mapResult(this.from, 1),
            r = e.mapResult(this.to, -1);
        return (t.deleted && r.deleted) || t.pos >= r.pos ? null : new ze(t.pos, r.pos, this.mark);
    }
    merge(e) {
        return e instanceof ze && e.mark.eq(this.mark) && this.from <= e.to && this.to >= e.from
            ? new ze(Math.min(this.from, e.from), Math.max(this.to, e.to), this.mark)
            : null;
    }
    toJSON() {
        return { stepType: 'addMark', mark: this.mark.toJSON(), from: this.from, to: this.to };
    }
    static fromJSON(e, t) {
        if (typeof t.from != 'number' || typeof t.to != 'number')
            throw new RangeError('Invalid input for AddMarkStep.fromJSON');
        return new ze(t.from, t.to, e.markFromJSON(t.mark));
    }
}
_.jsonID('addMark', ze);
class Fe extends _ {
    constructor(e, t, r) {
        super(), (this.from = e), (this.to = t), (this.mark = r);
    }
    apply(e) {
        let t = e.slice(this.from, this.to),
            r = new E(
                Cn(t.content, (i) => i.mark(this.mark.removeFromSet(i.marks)), e),
                t.openStart,
                t.openEnd
            );
        return j.fromReplace(e, this.from, this.to, r);
    }
    invert() {
        return new ze(this.from, this.to, this.mark);
    }
    map(e) {
        let t = e.mapResult(this.from, 1),
            r = e.mapResult(this.to, -1);
        return (t.deleted && r.deleted) || t.pos >= r.pos ? null : new Fe(t.pos, r.pos, this.mark);
    }
    merge(e) {
        return e instanceof Fe && e.mark.eq(this.mark) && this.from <= e.to && this.to >= e.from
            ? new Fe(Math.min(this.from, e.from), Math.max(this.to, e.to), this.mark)
            : null;
    }
    toJSON() {
        return { stepType: 'removeMark', mark: this.mark.toJSON(), from: this.from, to: this.to };
    }
    static fromJSON(e, t) {
        if (typeof t.from != 'number' || typeof t.to != 'number')
            throw new RangeError('Invalid input for RemoveMarkStep.fromJSON');
        return new Fe(t.from, t.to, e.markFromJSON(t.mark));
    }
}
_.jsonID('removeMark', Fe);
class Le extends _ {
    constructor(e, t) {
        super(), (this.pos = e), (this.mark = t);
    }
    apply(e) {
        let t = e.nodeAt(this.pos);
        if (!t) return j.fail("No node at mark step's position");
        let r = t.type.create(t.attrs, null, this.mark.addToSet(t.marks));
        return j.fromReplace(e, this.pos, this.pos + 1, new E(k.from(r), 0, t.isLeaf ? 0 : 1));
    }
    invert(e) {
        let t = e.nodeAt(this.pos);
        if (t) {
            let r = this.mark.addToSet(t.marks);
            if (r.length == t.marks.length) {
                for (let i = 0; i < t.marks.length; i++)
                    if (!t.marks[i].isInSet(r)) return new Le(this.pos, t.marks[i]);
                return new Le(this.pos, this.mark);
            }
        }
        return new yt(this.pos, this.mark);
    }
    map(e) {
        let t = e.mapResult(this.pos, 1);
        return t.deletedAfter ? null : new Le(t.pos, this.mark);
    }
    toJSON() {
        return { stepType: 'addNodeMark', pos: this.pos, mark: this.mark.toJSON() };
    }
    static fromJSON(e, t) {
        if (typeof t.pos != 'number')
            throw new RangeError('Invalid input for AddNodeMarkStep.fromJSON');
        return new Le(t.pos, e.markFromJSON(t.mark));
    }
}
_.jsonID('addNodeMark', Le);
class yt extends _ {
    constructor(e, t) {
        super(), (this.pos = e), (this.mark = t);
    }
    apply(e) {
        let t = e.nodeAt(this.pos);
        if (!t) return j.fail("No node at mark step's position");
        let r = t.type.create(t.attrs, null, this.mark.removeFromSet(t.marks));
        return j.fromReplace(e, this.pos, this.pos + 1, new E(k.from(r), 0, t.isLeaf ? 0 : 1));
    }
    invert(e) {
        let t = e.nodeAt(this.pos);
        return !t || !this.mark.isInSet(t.marks) ? this : new Le(this.pos, this.mark);
    }
    map(e) {
        let t = e.mapResult(this.pos, 1);
        return t.deletedAfter ? null : new yt(t.pos, this.mark);
    }
    toJSON() {
        return { stepType: 'removeNodeMark', pos: this.pos, mark: this.mark.toJSON() };
    }
    static fromJSON(e, t) {
        if (typeof t.pos != 'number')
            throw new RangeError('Invalid input for RemoveNodeMarkStep.fromJSON');
        return new yt(t.pos, e.markFromJSON(t.mark));
    }
}
_.jsonID('removeNodeMark', yt);
class ie extends _ {
    constructor(e, t, r, i = !1) {
        super(), (this.from = e), (this.to = t), (this.slice = r), (this.structure = i);
    }
    apply(e) {
        return this.structure && xn(e, this.from, this.to)
            ? j.fail('Structure replace would overwrite content')
            : j.fromReplace(e, this.from, this.to, this.slice);
    }
    getMap() {
        return new oe([this.from, this.to - this.from, this.slice.size]);
    }
    invert(e) {
        return new ie(this.from, this.from + this.slice.size, e.slice(this.from, this.to));
    }
    map(e) {
        let t = e.mapResult(this.from, 1),
            r = e.mapResult(this.to, -1);
        return t.deletedAcross && r.deletedAcross
            ? null
            : new ie(t.pos, Math.max(t.pos, r.pos), this.slice);
    }
    merge(e) {
        if (!(e instanceof ie) || e.structure || this.structure) return null;
        if (this.from + this.slice.size == e.from && !this.slice.openEnd && !e.slice.openStart) {
            let t =
                this.slice.size + e.slice.size == 0
                    ? E.empty
                    : new E(
                          this.slice.content.append(e.slice.content),
                          this.slice.openStart,
                          e.slice.openEnd
                      );
            return new ie(this.from, this.to + (e.to - e.from), t, this.structure);
        } else if (e.to == this.from && !this.slice.openStart && !e.slice.openEnd) {
            let t =
                this.slice.size + e.slice.size == 0
                    ? E.empty
                    : new E(
                          e.slice.content.append(this.slice.content),
                          e.slice.openStart,
                          this.slice.openEnd
                      );
            return new ie(e.from, this.to, t, this.structure);
        } else return null;
    }
    toJSON() {
        let e = { stepType: 'replace', from: this.from, to: this.to };
        return (
            this.slice.size && (e.slice = this.slice.toJSON()),
            this.structure && (e.structure = !0),
            e
        );
    }
    static fromJSON(e, t) {
        if (typeof t.from != 'number' || typeof t.to != 'number')
            throw new RangeError('Invalid input for ReplaceStep.fromJSON');
        return new ie(t.from, t.to, E.fromJSON(e, t.slice), !!t.structure);
    }
}
_.jsonID('replace', ie);
class G extends _ {
    constructor(e, t, r, i, o, s, a = !1) {
        super(),
            (this.from = e),
            (this.to = t),
            (this.gapFrom = r),
            (this.gapTo = i),
            (this.slice = o),
            (this.insert = s),
            (this.structure = a);
    }
    apply(e) {
        if (this.structure && (xn(e, this.from, this.gapFrom) || xn(e, this.gapTo, this.to)))
            return j.fail('Structure gap-replace would overwrite content');
        let t = e.slice(this.gapFrom, this.gapTo);
        if (t.openStart || t.openEnd) return j.fail('Gap is not a flat range');
        let r = this.slice.insertAt(this.insert, t.content);
        return r ? j.fromReplace(e, this.from, this.to, r) : j.fail('Content does not fit in gap');
    }
    getMap() {
        return new oe([
            this.from,
            this.gapFrom - this.from,
            this.insert,
            this.gapTo,
            this.to - this.gapTo,
            this.slice.size - this.insert
        ]);
    }
    invert(e) {
        let t = this.gapTo - this.gapFrom;
        return new G(
            this.from,
            this.from + this.slice.size + t,
            this.from + this.insert,
            this.from + this.insert + t,
            e
                .slice(this.from, this.to)
                .removeBetween(this.gapFrom - this.from, this.gapTo - this.from),
            this.gapFrom - this.from,
            this.structure
        );
    }
    map(e) {
        let t = e.mapResult(this.from, 1),
            r = e.mapResult(this.to, -1),
            i = e.map(this.gapFrom, -1),
            o = e.map(this.gapTo, 1);
        return (t.deletedAcross && r.deletedAcross) || i < t.pos || o > r.pos
            ? null
            : new G(t.pos, r.pos, i, o, this.slice, this.insert, this.structure);
    }
    toJSON() {
        let e = {
            stepType: 'replaceAround',
            from: this.from,
            to: this.to,
            gapFrom: this.gapFrom,
            gapTo: this.gapTo,
            insert: this.insert
        };
        return (
            this.slice.size && (e.slice = this.slice.toJSON()),
            this.structure && (e.structure = !0),
            e
        );
    }
    static fromJSON(e, t) {
        if (
            typeof t.from != 'number' ||
            typeof t.to != 'number' ||
            typeof t.gapFrom != 'number' ||
            typeof t.gapTo != 'number' ||
            typeof t.insert != 'number'
        )
            throw new RangeError('Invalid input for ReplaceAroundStep.fromJSON');
        return new G(
            t.from,
            t.to,
            t.gapFrom,
            t.gapTo,
            E.fromJSON(e, t.slice),
            t.insert,
            !!t.structure
        );
    }
}
_.jsonID('replaceAround', G);
function xn(n, e, t) {
    let r = n.resolve(e),
        i = t - e,
        o = r.depth;
    for (; i > 0 && o > 0 && r.indexAfter(o) == r.node(o).childCount; ) o--, i--;
    if (i > 0) {
        let s = r.node(o).maybeChild(r.indexAfter(o));
        for (; i > 0; ) {
            if (!s || s.isLeaf) return !0;
            (s = s.firstChild), i--;
        }
    }
    return !1;
}
function Hi(n, e, t) {
    return (e == 0 || n.canReplace(e, n.childCount)) && (t == n.childCount || n.canReplace(0, t));
}
function nt(n) {
    let t = n.parent.content.cutByIndex(n.startIndex, n.endIndex);
    for (let r = n.depth; ; --r) {
        let i = n.$from.node(r),
            o = n.$from.index(r),
            s = n.$to.indexAfter(r);
        if (r < n.depth && i.canReplace(o, s, t)) return r;
        if (r == 0 || i.type.spec.isolating || !Hi(i, o, s)) break;
    }
    return null;
}
function jr(n, e, t = null, r = n) {
    let i = qi(n, e),
        o = i && Ui(r, e);
    return o ? i.map(Gn).concat({ type: e, attrs: t }).concat(o.map(Gn)) : null;
}
function Gn(n) {
    return { type: n, attrs: null };
}
function qi(n, e) {
    let { parent: t, startIndex: r, endIndex: i } = n,
        o = t.contentMatchAt(r).findWrapping(e);
    if (!o) return null;
    let s = o.length ? o[0] : e;
    return t.canReplaceWith(r, i, s) ? o : null;
}
function Ui(n, e) {
    let { parent: t, startIndex: r, endIndex: i } = n,
        o = t.child(r),
        s = e.contentMatch.findWrapping(o.type);
    if (!s) return null;
    let l = (s.length ? s[s.length - 1] : e).contentMatch;
    for (let u = r; l && u < i; u++) l = l.matchType(t.child(u).type);
    return !l || !l.validEnd ? null : s;
}
function Xe(n, e, t = 1, r) {
    let i = n.resolve(e),
        o = i.depth - t,
        s = (r && r[r.length - 1]) || i.parent;
    if (
        o < 0 ||
        i.parent.type.spec.isolating ||
        !i.parent.canReplace(i.index(), i.parent.childCount) ||
        !s.type.validContent(i.parent.content.cutByIndex(i.index(), i.parent.childCount))
    )
        return !1;
    for (let u = i.depth - 1, c = t - 2; u > o; u--, c--) {
        let d = i.node(u),
            p = i.index(u);
        if (d.type.spec.isolating) return !1;
        let h = d.content.cutByIndex(p, d.childCount),
            v = (r && r[c]) || d;
        if (
            (v != d && (h = h.replaceChild(0, v.type.create(v.attrs))),
            !d.canReplace(p + 1, d.childCount) || !v.type.validContent(h))
        )
            return !1;
    }
    let a = i.indexAfter(o),
        l = r && r[0];
    return i.node(o).canReplaceWith(a, a, l ? l.type : i.node(o + 1).type);
}
function Ve(n, e) {
    let t = n.resolve(e),
        r = t.index();
    return Vr(t.nodeBefore, t.nodeAfter) && t.parent.canReplace(r, r + 1);
}
function Vr(n, e) {
    return !!(n && e && !n.isLeaf && n.canAppend(e));
}
function Wr(n, e, t = -1) {
    let r = n.resolve(e);
    for (let i = r.depth; ; i--) {
        let o,
            s,
            a = r.index(i);
        if (
            (i == r.depth
                ? ((o = r.nodeBefore), (s = r.nodeAfter))
                : t > 0
                ? ((o = r.node(i + 1)), a++, (s = r.node(i).maybeChild(a)))
                : ((o = r.node(i).maybeChild(a - 1)), (s = r.node(i + 1))),
            o && !o.isTextblock && Vr(o, s) && r.node(i).canReplace(a, a + 1))
        )
            return e;
        if (i == 0) break;
        e = t < 0 ? r.before(i) : r.after(i);
    }
}
function Jr(n, e, t = e, r = E.empty) {
    if (e == t && !r.size) return null;
    let i = n.resolve(e),
        o = n.resolve(t);
    return Ki(i, o, r) ? new ie(e, t, r) : new Gi(i, o, r).fit();
}
function Ki(n, e, t) {
    return (
        !t.openStart &&
        !t.openEnd &&
        n.start() == e.start() &&
        n.parent.canReplace(n.index(), e.index(), t.content)
    );
}
class Gi {
    constructor(e, t, r) {
        (this.$from = e),
            (this.$to = t),
            (this.unplaced = r),
            (this.frontier = []),
            (this.placed = k.empty);
        for (let i = 0; i <= e.depth; i++) {
            let o = e.node(i);
            this.frontier.push({ type: o.type, match: o.contentMatchAt(e.indexAfter(i)) });
        }
        for (let i = e.depth; i > 0; i--) this.placed = k.from(e.node(i).copy(this.placed));
    }
    get depth() {
        return this.frontier.length - 1;
    }
    fit() {
        for (; this.unplaced.size; ) {
            let u = this.findFittable();
            u ? this.placeNodes(u) : this.openMore() || this.dropNode();
        }
        let e = this.mustMoveInline(),
            t = this.placed.size - this.depth - this.$from.depth,
            r = this.$from,
            i = this.close(e < 0 ? this.$to : r.doc.resolve(e));
        if (!i) return null;
        let o = this.placed,
            s = r.depth,
            a = i.depth;
        for (; s && a && o.childCount == 1; ) (o = o.firstChild.content), s--, a--;
        let l = new E(o, s, a);
        return e > -1
            ? new G(r.pos, e, this.$to.pos, this.$to.end(), l, t)
            : l.size || r.pos != this.$to.pos
            ? new ie(r.pos, i.pos, l)
            : null;
    }
    findFittable() {
        let e = this.unplaced.openStart;
        for (let t = this.unplaced.content, r = 0, i = this.unplaced.openEnd; r < e; r++) {
            let o = t.firstChild;
            if ((t.childCount > 1 && (i = 0), o.type.spec.isolating && i <= r)) {
                e = r;
                break;
            }
            t = o.content;
        }
        for (let t = 1; t <= 2; t++)
            for (let r = t == 1 ? e : this.unplaced.openStart; r >= 0; r--) {
                let i,
                    o = null;
                r
                    ? ((o = un(this.unplaced.content, r - 1).firstChild), (i = o.content))
                    : (i = this.unplaced.content);
                let s = i.firstChild;
                for (let a = this.depth; a >= 0; a--) {
                    let { type: l, match: u } = this.frontier[a],
                        c,
                        d = null;
                    if (
                        t == 1 &&
                        (s
                            ? u.matchType(s.type) || (d = u.fillBefore(k.from(s), !1))
                            : o && l.compatibleContent(o.type))
                    )
                        return { sliceDepth: r, frontierDepth: a, parent: o, inject: d };
                    if (t == 2 && s && (c = u.findWrapping(s.type)))
                        return { sliceDepth: r, frontierDepth: a, parent: o, wrap: c };
                    if (o && u.matchType(o.type)) break;
                }
            }
    }
    openMore() {
        let { content: e, openStart: t, openEnd: r } = this.unplaced,
            i = un(e, t);
        return !i.childCount || i.firstChild.isLeaf
            ? !1
            : ((this.unplaced = new E(e, t + 1, Math.max(r, i.size + t >= e.size - r ? t + 1 : 0))),
              !0);
    }
    dropNode() {
        let { content: e, openStart: t, openEnd: r } = this.unplaced,
            i = un(e, t);
        if (i.childCount <= 1 && t > 0) {
            let o = e.size - t <= t + i.size;
            this.unplaced = new E(ft(e, t - 1, 1), t - 1, o ? t - 1 : r);
        } else this.unplaced = new E(ft(e, t, 1), t, r);
    }
    placeNodes({ sliceDepth: e, frontierDepth: t, parent: r, inject: i, wrap: o }) {
        for (; this.depth > t; ) this.closeFrontierNode();
        if (o) for (let g = 0; g < o.length; g++) this.openFrontierNode(o[g]);
        let s = this.unplaced,
            a = r ? r.content : s.content,
            l = s.openStart - e,
            u = 0,
            c = [],
            { match: d, type: p } = this.frontier[t];
        if (i) {
            for (let g = 0; g < i.childCount; g++) c.push(i.child(g));
            d = d.matchFragment(i);
        }
        let h = a.size + e - (s.content.size - s.openEnd);
        for (; u < a.childCount; ) {
            let g = a.child(u),
                w = d.matchType(g.type);
            if (!w) break;
            u++,
                (u > 1 || l == 0 || g.content.size) &&
                    ((d = w),
                    c.push(
                        Hr(
                            g.mark(p.allowedMarks(g.marks)),
                            u == 1 ? l : 0,
                            u == a.childCount ? h : -1
                        )
                    ));
        }
        let v = u == a.childCount;
        v || (h = -1),
            (this.placed = dt(this.placed, t, k.from(c))),
            (this.frontier[t].match = d),
            v &&
                h < 0 &&
                r &&
                r.type == this.frontier[this.depth].type &&
                this.frontier.length > 1 &&
                this.closeFrontierNode();
        for (let g = 0, w = a; g < h; g++) {
            let x = w.lastChild;
            this.frontier.push({ type: x.type, match: x.contentMatchAt(x.childCount) }),
                (w = x.content);
        }
        this.unplaced = v
            ? e == 0
                ? E.empty
                : new E(ft(s.content, e - 1, 1), e - 1, h < 0 ? s.openEnd : e - 1)
            : new E(ft(s.content, e, u), s.openStart, s.openEnd);
    }
    mustMoveInline() {
        if (!this.$to.parent.isTextblock) return -1;
        let e = this.frontier[this.depth],
            t;
        if (
            !e.type.isTextblock ||
            !fn(this.$to, this.$to.depth, e.type, e.match, !1) ||
            (this.$to.depth == this.depth &&
                (t = this.findCloseLevel(this.$to)) &&
                t.depth == this.depth)
        )
            return -1;
        let { depth: r } = this.$to,
            i = this.$to.after(r);
        for (; r > 1 && i == this.$to.end(--r); ) ++i;
        return i;
    }
    findCloseLevel(e) {
        e: for (let t = Math.min(this.depth, e.depth); t >= 0; t--) {
            let { match: r, type: i } = this.frontier[t],
                o = t < e.depth && e.end(t + 1) == e.pos + (e.depth - (t + 1)),
                s = fn(e, t, i, r, o);
            if (s) {
                for (let a = t - 1; a >= 0; a--) {
                    let { match: l, type: u } = this.frontier[a],
                        c = fn(e, a, u, l, !0);
                    if (!c || c.childCount) continue e;
                }
                return { depth: t, fit: s, move: o ? e.doc.resolve(e.after(t + 1)) : e };
            }
        }
    }
    close(e) {
        let t = this.findCloseLevel(e);
        if (!t) return null;
        for (; this.depth > t.depth; ) this.closeFrontierNode();
        t.fit.childCount && (this.placed = dt(this.placed, t.depth, t.fit)), (e = t.move);
        for (let r = t.depth + 1; r <= e.depth; r++) {
            let i = e.node(r),
                o = i.type.contentMatch.fillBefore(i.content, !0, e.index(r));
            this.openFrontierNode(i.type, i.attrs, o);
        }
        return e;
    }
    openFrontierNode(e, t = null, r) {
        let i = this.frontier[this.depth];
        (i.match = i.match.matchType(e)),
            (this.placed = dt(this.placed, this.depth, k.from(e.create(t, r)))),
            this.frontier.push({ type: e, match: e.contentMatch });
    }
    closeFrontierNode() {
        let t = this.frontier.pop().match.fillBefore(k.empty, !0);
        t.childCount && (this.placed = dt(this.placed, this.frontier.length, t));
    }
}
function ft(n, e, t) {
    return e == 0
        ? n.cutByIndex(t, n.childCount)
        : n.replaceChild(0, n.firstChild.copy(ft(n.firstChild.content, e - 1, t)));
}
function dt(n, e, t) {
    return e == 0
        ? n.append(t)
        : n.replaceChild(n.childCount - 1, n.lastChild.copy(dt(n.lastChild.content, e - 1, t)));
}
function un(n, e) {
    for (let t = 0; t < e; t++) n = n.firstChild.content;
    return n;
}
function Hr(n, e, t) {
    if (e <= 0) return n;
    let r = n.content;
    return (
        e > 1 && (r = r.replaceChild(0, Hr(r.firstChild, e - 1, r.childCount == 1 ? t - 1 : 0))),
        e > 0 &&
            ((r = n.type.contentMatch.fillBefore(r).append(r)),
            t <= 0 && (r = r.append(n.type.contentMatch.matchFragment(r).fillBefore(k.empty, !0)))),
        n.copy(r)
    );
}
function fn(n, e, t, r, i) {
    let o = n.node(e),
        s = i ? n.indexAfter(e) : n.index(e);
    if (s == o.childCount && !t.compatibleContent(o.type)) return null;
    let a = r.fillBefore(o.content, !0, s);
    return a && !Yi(t, o.content, s) ? a : null;
}
function Yi(n, e, t) {
    for (let r = t; r < e.childCount; r++) if (!n.allowsMarks(e.child(r).marks)) return !0;
    return !1;
}
class ht extends _ {
    constructor(e, t, r) {
        super(), (this.pos = e), (this.attr = t), (this.value = r);
    }
    apply(e) {
        let t = e.nodeAt(this.pos);
        if (!t) return j.fail("No node at attribute step's position");
        let r = Object.create(null);
        for (let o in t.attrs) r[o] = t.attrs[o];
        r[this.attr] = this.value;
        let i = t.type.create(r, null, t.marks);
        return j.fromReplace(e, this.pos, this.pos + 1, new E(k.from(i), 0, t.isLeaf ? 0 : 1));
    }
    getMap() {
        return oe.empty;
    }
    invert(e) {
        return new ht(this.pos, this.attr, e.nodeAt(this.pos).attrs[this.attr]);
    }
    map(e) {
        let t = e.mapResult(this.pos, 1);
        return t.deletedAfter ? null : new ht(t.pos, this.attr, this.value);
    }
    toJSON() {
        return { stepType: 'attr', pos: this.pos, attr: this.attr, value: this.value };
    }
    static fromJSON(e, t) {
        if (typeof t.pos != 'number' || typeof t.attr != 'string')
            throw new RangeError('Invalid input for AttrStep.fromJSON');
        return new ht(t.pos, t.attr, t.value);
    }
}
_.jsonID('attr', ht);
let wt = class extends Error {};
wt = function n(e) {
    let t = Error.call(this, e);
    return (t.__proto__ = n.prototype), t;
};
wt.prototype = Object.create(Error.prototype);
wt.prototype.constructor = wt;
wt.prototype.name = 'TransformError';
const dn = Object.create(null);
class R {
    constructor(e, t, r) {
        (this.$anchor = e), (this.$head = t), (this.ranges = r || [new Xi(e.min(t), e.max(t))]);
    }
    get anchor() {
        return this.$anchor.pos;
    }
    get head() {
        return this.$head.pos;
    }
    get from() {
        return this.$from.pos;
    }
    get to() {
        return this.$to.pos;
    }
    get $from() {
        return this.ranges[0].$from;
    }
    get $to() {
        return this.ranges[0].$to;
    }
    get empty() {
        let e = this.ranges;
        for (let t = 0; t < e.length; t++) if (e[t].$from.pos != e[t].$to.pos) return !1;
        return !0;
    }
    content() {
        return this.$from.doc.slice(this.from, this.to, !0);
    }
    replace(e, t = E.empty) {
        let r = t.content.lastChild,
            i = null;
        for (let a = 0; a < t.openEnd; a++) (i = r), (r = r.lastChild);
        let o = e.steps.length,
            s = this.ranges;
        for (let a = 0; a < s.length; a++) {
            let { $from: l, $to: u } = s[a],
                c = e.mapping.slice(o);
            e.replaceRange(c.map(l.pos), c.map(u.pos), a ? E.empty : t),
                a == 0 && _n(e, o, (r ? r.isInline : i && i.isTextblock) ? -1 : 1);
        }
    }
    replaceWith(e, t) {
        let r = e.steps.length,
            i = this.ranges;
        for (let o = 0; o < i.length; o++) {
            let { $from: s, $to: a } = i[o],
                l = e.mapping.slice(r),
                u = l.map(s.pos),
                c = l.map(a.pos);
            o ? e.deleteRange(u, c) : (e.replaceRangeWith(u, c, t), _n(e, r, t.isInline ? -1 : 1));
        }
    }
    static findFrom(e, t, r = !1) {
        let i = e.parent.inlineContent ? new $(e) : Ge(e.node(0), e.parent, e.pos, e.index(), t, r);
        if (i) return i;
        for (let o = e.depth - 1; o >= 0; o--) {
            let s =
                t < 0
                    ? Ge(e.node(0), e.node(o), e.before(o + 1), e.index(o), t, r)
                    : Ge(e.node(0), e.node(o), e.after(o + 1), e.index(o) + 1, t, r);
            if (s) return s;
        }
        return null;
    }
    static near(e, t = 1) {
        return this.findFrom(e, t) || this.findFrom(e, -t) || new ge(e.node(0));
    }
    static atStart(e) {
        return Ge(e, e, 0, 0, 1) || new ge(e);
    }
    static atEnd(e) {
        return Ge(e, e, e.content.size, e.childCount, -1) || new ge(e);
    }
    static fromJSON(e, t) {
        if (!t || !t.type) throw new RangeError('Invalid input for Selection.fromJSON');
        let r = dn[t.type];
        if (!r) throw new RangeError(`No selection type ${t.type} defined`);
        return r.fromJSON(e, t);
    }
    static jsonID(e, t) {
        if (e in dn) throw new RangeError('Duplicate use of selection JSON ID ' + e);
        return (dn[e] = t), (t.prototype.jsonID = e), t;
    }
    getBookmark() {
        return $.between(this.$anchor, this.$head).getBookmark();
    }
}
R.prototype.visible = !0;
class Xi {
    constructor(e, t) {
        (this.$from = e), (this.$to = t);
    }
}
let Yn = !1;
function Xn(n) {
    !Yn &&
        !n.parent.inlineContent &&
        ((Yn = !0),
        console.warn(
            'TextSelection endpoint not pointing into a node with inline content (' +
                n.parent.type.name +
                ')'
        ));
}
class $ extends R {
    constructor(e, t = e) {
        Xn(e), Xn(t), super(e, t);
    }
    get $cursor() {
        return this.$anchor.pos == this.$head.pos ? this.$head : null;
    }
    map(e, t) {
        let r = e.resolve(t.map(this.head));
        if (!r.parent.inlineContent) return R.near(r);
        let i = e.resolve(t.map(this.anchor));
        return new $(i.parent.inlineContent ? i : r, r);
    }
    replace(e, t = E.empty) {
        if ((super.replace(e, t), t == E.empty)) {
            let r = this.$from.marksAcross(this.$to);
            r && e.ensureMarks(r);
        }
    }
    eq(e) {
        return e instanceof $ && e.anchor == this.anchor && e.head == this.head;
    }
    getBookmark() {
        return new tn(this.anchor, this.head);
    }
    toJSON() {
        return { type: 'text', anchor: this.anchor, head: this.head };
    }
    static fromJSON(e, t) {
        if (typeof t.anchor != 'number' || typeof t.head != 'number')
            throw new RangeError('Invalid input for TextSelection.fromJSON');
        return new $(e.resolve(t.anchor), e.resolve(t.head));
    }
    static create(e, t, r = t) {
        let i = e.resolve(t);
        return new this(i, r == t ? i : e.resolve(r));
    }
    static between(e, t, r) {
        let i = e.pos - t.pos;
        if (((!r || i) && (r = i >= 0 ? 1 : -1), !t.parent.inlineContent)) {
            let o = R.findFrom(t, r, !0) || R.findFrom(t, -r, !0);
            if (o) t = o.$head;
            else return R.near(t, r);
        }
        return (
            e.parent.inlineContent ||
                (i == 0
                    ? (e = t)
                    : ((e = (R.findFrom(e, -r, !0) || R.findFrom(e, r, !0)).$anchor),
                      e.pos < t.pos != i < 0 && (e = t))),
            new $(e, t)
        );
    }
}
R.jsonID('text', $);
class tn {
    constructor(e, t) {
        (this.anchor = e), (this.head = t);
    }
    map(e) {
        return new tn(e.map(this.anchor), e.map(this.head));
    }
    resolve(e) {
        return $.between(e.resolve(this.anchor), e.resolve(this.head));
    }
}
class P extends R {
    constructor(e) {
        let t = e.nodeAfter,
            r = e.node(0).resolve(e.pos + t.nodeSize);
        super(e, r), (this.node = t);
    }
    map(e, t) {
        let { deleted: r, pos: i } = t.mapResult(this.anchor),
            o = e.resolve(i);
        return r ? R.near(o) : new P(o);
    }
    content() {
        return new E(k.from(this.node), 0, 0);
    }
    eq(e) {
        return e instanceof P && e.anchor == this.anchor;
    }
    toJSON() {
        return { type: 'node', anchor: this.anchor };
    }
    getBookmark() {
        return new In(this.anchor);
    }
    static fromJSON(e, t) {
        if (typeof t.anchor != 'number')
            throw new RangeError('Invalid input for NodeSelection.fromJSON');
        return new P(e.resolve(t.anchor));
    }
    static create(e, t) {
        return new P(e.resolve(t));
    }
    static isSelectable(e) {
        return !e.isText && e.type.spec.selectable !== !1;
    }
}
P.prototype.visible = !1;
R.jsonID('node', P);
class In {
    constructor(e) {
        this.anchor = e;
    }
    map(e) {
        let { deleted: t, pos: r } = e.mapResult(this.anchor);
        return t ? new tn(r, r) : new In(r);
    }
    resolve(e) {
        let t = e.resolve(this.anchor),
            r = t.nodeAfter;
        return r && P.isSelectable(r) ? new P(t) : R.near(t);
    }
}
class ge extends R {
    constructor(e) {
        super(e.resolve(0), e.resolve(e.content.size));
    }
    replace(e, t = E.empty) {
        if (t == E.empty) {
            e.delete(0, e.doc.content.size);
            let r = R.atStart(e.doc);
            r.eq(e.selection) || e.setSelection(r);
        } else super.replace(e, t);
    }
    toJSON() {
        return { type: 'all' };
    }
    static fromJSON(e) {
        return new ge(e);
    }
    map(e) {
        return new ge(e);
    }
    eq(e) {
        return e instanceof ge;
    }
    getBookmark() {
        return _i;
    }
}
R.jsonID('all', ge);
const _i = {
    map() {
        return this;
    },
    resolve(n) {
        return new ge(n);
    }
};
function Ge(n, e, t, r, i, o = !1) {
    if (e.inlineContent) return $.create(n, t);
    for (let s = r - (i > 0 ? 0 : 1); i > 0 ? s < e.childCount : s >= 0; s += i) {
        let a = e.child(s);
        if (a.isAtom) {
            if (!o && P.isSelectable(a)) return P.create(n, t - (i < 0 ? a.nodeSize : 0));
        } else {
            let l = Ge(n, a, t + i, i < 0 ? a.childCount : 0, i, o);
            if (l) return l;
        }
        t += a.nodeSize * i;
    }
    return null;
}
function _n(n, e, t) {
    let r = n.steps.length - 1;
    if (r < e) return;
    let i = n.steps[r];
    if (!(i instanceof ie || i instanceof G)) return;
    let o = n.mapping.maps[r],
        s;
    o.forEach((a, l, u, c) => {
        s == null && (s = c);
    }),
        n.setSelection(R.near(n.doc.resolve(s), t));
}
function Qn(n, e) {
    return !e || !n ? n : n.bind(e);
}
class jt {
    constructor(e, t, r) {
        (this.name = e), (this.init = Qn(t.init, r)), (this.apply = Qn(t.apply, r));
    }
}
new jt('doc', {
    init(n) {
        return n.doc || n.schema.topNodeType.createAndFill();
    },
    apply(n) {
        return n.doc;
    }
}),
    new jt('selection', {
        init(n, e) {
            return n.selection || R.atStart(e.doc);
        },
        apply(n) {
            return n.selection;
        }
    }),
    new jt('storedMarks', {
        init(n) {
            return n.storedMarks || null;
        },
        apply(n, e, t, r) {
            return r.selection.$cursor ? n.storedMarks : null;
        }
    }),
    new jt('scrollToSelection', {
        init() {
            return 0;
        },
        apply(n, e) {
            return n.scrolledIntoView ? e + 1 : e;
        }
    });
function qr(n, e, t) {
    for (let r in n) {
        let i = n[r];
        i instanceof Function ? (i = i.bind(e)) : r == 'handleDOMEvents' && (i = qr(i, e, {})),
            (t[r] = i);
    }
    return t;
}
class rt {
    constructor(e) {
        (this.spec = e),
            (this.props = {}),
            e.props && qr(e.props, this, this.props),
            (this.key = e.key ? e.key.key : Ur('plugin'));
    }
    getState(e) {
        return e[this.key];
    }
}
const pn = Object.create(null);
function Ur(n) {
    return n in pn ? n + '$' + ++pn[n] : ((pn[n] = 0), n + '$');
}
class it {
    constructor(e = 'key') {
        this.key = Ur(e);
    }
    get(e) {
        return e.config.pluginsByKey[this.key];
    }
    getState(e) {
        return e[this.key];
    }
}
const Qi = (n, e) =>
    n.selection.empty ? !1 : (e && e(n.tr.deleteSelection().scrollIntoView()), !0);
function Zi(n, e) {
    let { $cursor: t } = n.selection;
    return !t || (e ? !e.endOfTextblock('backward', n) : t.parentOffset > 0) ? null : t;
}
const eo = (n, e, t) => {
    let r = Zi(n, t);
    if (!r) return !1;
    let i = Kr(r);
    if (!i) {
        let s = r.blockRange(),
            a = s && nt(s);
        return a == null ? !1 : (e && e(n.tr.lift(s, a).scrollIntoView()), !0);
    }
    let o = i.nodeBefore;
    if (!o.type.spec.isolating && Xr(n, i, e)) return !0;
    if (r.parent.content.size == 0 && (_e(o, 'end') || P.isSelectable(o))) {
        let s = Jr(n.doc, r.before(), r.after(), E.empty);
        if (s && s.slice.size < s.to - s.from) {
            if (e) {
                let a = n.tr.step(s);
                a.setSelection(
                    _e(o, 'end')
                        ? R.findFrom(a.doc.resolve(a.mapping.map(i.pos, -1)), -1)
                        : P.create(a.doc, i.pos - o.nodeSize)
                ),
                    e(a.scrollIntoView());
            }
            return !0;
        }
    }
    return o.isAtom && i.depth == r.depth - 1
        ? (e && e(n.tr.delete(i.pos - o.nodeSize, i.pos).scrollIntoView()), !0)
        : !1;
};
function _e(n, e, t = !1) {
    for (let r = n; r; r = e == 'start' ? r.firstChild : r.lastChild) {
        if (r.isTextblock) return !0;
        if (t && r.childCount != 1) return !1;
    }
    return !1;
}
const to = (n, e, t) => {
    let { $head: r, empty: i } = n.selection,
        o = r;
    if (!i) return !1;
    if (r.parent.isTextblock) {
        if (t ? !t.endOfTextblock('backward', n) : r.parentOffset > 0) return !1;
        o = Kr(r);
    }
    let s = o && o.nodeBefore;
    return !s || !P.isSelectable(s)
        ? !1
        : (e && e(n.tr.setSelection(P.create(n.doc, o.pos - s.nodeSize)).scrollIntoView()), !0);
};
function Kr(n) {
    if (!n.parent.type.spec.isolating)
        for (let e = n.depth - 1; e >= 0; e--) {
            if (n.index(e) > 0) return n.doc.resolve(n.before(e + 1));
            if (n.node(e).type.spec.isolating) break;
        }
    return null;
}
function no(n, e) {
    let { $cursor: t } = n.selection;
    return !t || (e ? !e.endOfTextblock('forward', n) : t.parentOffset < t.parent.content.size)
        ? null
        : t;
}
const ro = (n, e, t) => {
        let r = no(n, t);
        if (!r) return !1;
        let i = Gr(r);
        if (!i) return !1;
        let o = i.nodeAfter;
        if (Xr(n, i, e)) return !0;
        if (r.parent.content.size == 0 && (_e(o, 'start') || P.isSelectable(o))) {
            let s = Jr(n.doc, r.before(), r.after(), E.empty);
            if (s && s.slice.size < s.to - s.from) {
                if (e) {
                    let a = n.tr.step(s);
                    a.setSelection(
                        _e(o, 'start')
                            ? R.findFrom(a.doc.resolve(a.mapping.map(i.pos)), 1)
                            : P.create(a.doc, a.mapping.map(i.pos))
                    ),
                        e(a.scrollIntoView());
                }
                return !0;
            }
        }
        return o.isAtom && i.depth == r.depth - 1
            ? (e && e(n.tr.delete(i.pos, i.pos + o.nodeSize).scrollIntoView()), !0)
            : !1;
    },
    io = (n, e, t) => {
        let { $head: r, empty: i } = n.selection,
            o = r;
        if (!i) return !1;
        if (r.parent.isTextblock) {
            if (t ? !t.endOfTextblock('forward', n) : r.parentOffset < r.parent.content.size)
                return !1;
            o = Gr(r);
        }
        let s = o && o.nodeAfter;
        return !s || !P.isSelectable(s)
            ? !1
            : (e && e(n.tr.setSelection(P.create(n.doc, o.pos)).scrollIntoView()), !0);
    };
function Gr(n) {
    if (!n.parent.type.spec.isolating)
        for (let e = n.depth - 1; e >= 0; e--) {
            let t = n.node(e);
            if (n.index(e) + 1 < t.childCount) return n.doc.resolve(n.after(e + 1));
            if (t.type.spec.isolating) break;
        }
    return null;
}
const oo = (n, e) => {
        let t = n.selection,
            r = t instanceof P,
            i;
        if (r) {
            if (t.node.isTextblock || !Ve(n.doc, t.from)) return !1;
            i = t.from;
        } else if (((i = Wr(n.doc, t.from, -1)), i == null)) return !1;
        if (e) {
            let o = n.tr.join(i);
            r && o.setSelection(P.create(o.doc, i - n.doc.resolve(i).nodeBefore.nodeSize)),
                e(o.scrollIntoView());
        }
        return !0;
    },
    so = (n, e) => {
        let t = n.selection,
            r;
        if (t instanceof P) {
            if (t.node.isTextblock || !Ve(n.doc, t.to)) return !1;
            r = t.to;
        } else if (((r = Wr(n.doc, t.to, 1)), r == null)) return !1;
        return e && e(n.tr.join(r).scrollIntoView()), !0;
    },
    ao = (n, e) => {
        let { $from: t, $to: r } = n.selection,
            i = t.blockRange(r),
            o = i && nt(i);
        return o == null ? !1 : (e && e(n.tr.lift(i, o).scrollIntoView()), !0);
    },
    lo = (n, e) => {
        let { $head: t, $anchor: r } = n.selection;
        return !t.parent.type.spec.code || !t.sameParent(r)
            ? !1
            : (e &&
                  e(
                      n.tr
                          .insertText(
                              `
`
                          )
                          .scrollIntoView()
                  ),
              !0);
    };
function Yr(n) {
    for (let e = 0; e < n.edgeCount; e++) {
        let { type: t } = n.edge(e);
        if (t.isTextblock && !t.hasRequiredAttrs()) return t;
    }
    return null;
}
const co = (n, e) => {
        let { $head: t, $anchor: r } = n.selection;
        if (!t.parent.type.spec.code || !t.sameParent(r)) return !1;
        let i = t.node(-1),
            o = t.indexAfter(-1),
            s = Yr(i.contentMatchAt(o));
        if (!s || !i.canReplaceWith(o, o, s)) return !1;
        if (e) {
            let a = t.after(),
                l = n.tr.replaceWith(a, a, s.createAndFill());
            l.setSelection(R.near(l.doc.resolve(a), 1)), e(l.scrollIntoView());
        }
        return !0;
    },
    uo = (n, e) => {
        let t = n.selection,
            { $from: r, $to: i } = t;
        if (t instanceof ge || r.parent.inlineContent || i.parent.inlineContent) return !1;
        let o = Yr(i.parent.contentMatchAt(i.indexAfter()));
        if (!o || !o.isTextblock) return !1;
        if (e) {
            let s = (!r.parentOffset && i.index() < i.parent.childCount ? r : i).pos,
                a = n.tr.insert(s, o.createAndFill());
            a.setSelection($.create(a.doc, s + 1)), e(a.scrollIntoView());
        }
        return !0;
    },
    fo = (n, e) => {
        let { $cursor: t } = n.selection;
        if (!t || t.parent.content.size) return !1;
        if (t.depth > 1 && t.after() != t.end(-1)) {
            let o = t.before();
            if (Xe(n.doc, o)) return e && e(n.tr.split(o).scrollIntoView()), !0;
        }
        let r = t.blockRange(),
            i = r && nt(r);
        return i == null ? !1 : (e && e(n.tr.lift(r, i).scrollIntoView()), !0);
    },
    po = (n, e) => {
        let { $from: t, to: r } = n.selection,
            i,
            o = t.sharedDepth(r);
        return o == 0 ? !1 : ((i = t.before(o)), e && e(n.tr.setSelection(P.create(n.doc, i))), !0);
    };
function ho(n, e, t) {
    let r = e.nodeBefore,
        i = e.nodeAfter,
        o = e.index();
    return !r || !i || !r.type.compatibleContent(i.type)
        ? !1
        : !r.content.size && e.parent.canReplace(o - 1, o)
        ? (t && t(n.tr.delete(e.pos - r.nodeSize, e.pos).scrollIntoView()), !0)
        : !e.parent.canReplace(o, o + 1) || !(i.isTextblock || Ve(n.doc, e.pos))
        ? !1
        : (t &&
              t(
                  n.tr
                      .clearIncompatible(e.pos, r.type, r.contentMatchAt(r.childCount))
                      .join(e.pos)
                      .scrollIntoView()
              ),
          !0);
}
function Xr(n, e, t) {
    let r = e.nodeBefore,
        i = e.nodeAfter,
        o,
        s;
    if (r.type.spec.isolating || i.type.spec.isolating) return !1;
    if (ho(n, e, t)) return !0;
    let a = e.parent.canReplace(e.index(), e.index() + 1);
    if (
        a &&
        (o = (s = r.contentMatchAt(r.childCount)).findWrapping(i.type)) &&
        s.matchType(o[0] || i.type).validEnd
    ) {
        if (t) {
            let d = e.pos + i.nodeSize,
                p = k.empty;
            for (let g = o.length - 1; g >= 0; g--) p = k.from(o[g].create(null, p));
            p = k.from(r.copy(p));
            let h = n.tr.step(new G(e.pos - 1, d, e.pos, d, new E(p, 1, 0), o.length, !0)),
                v = d + 2 * o.length;
            Ve(h.doc, v) && h.join(v), t(h.scrollIntoView());
        }
        return !0;
    }
    let l = R.findFrom(e, 1),
        u = l && l.$from.blockRange(l.$to),
        c = u && nt(u);
    if (c != null && c >= e.depth) return t && t(n.tr.lift(u, c).scrollIntoView()), !0;
    if (a && _e(i, 'start', !0) && _e(r, 'end')) {
        let d = r,
            p = [];
        for (; p.push(d), !d.isTextblock; ) d = d.lastChild;
        let h = i,
            v = 1;
        for (; !h.isTextblock; h = h.firstChild) v++;
        if (d.canReplace(d.childCount, d.childCount, h.content)) {
            if (t) {
                let g = k.empty;
                for (let x = p.length - 1; x >= 0; x--) g = k.from(p[x].copy(g));
                let w = n.tr.step(
                    new G(
                        e.pos - p.length,
                        e.pos + i.nodeSize,
                        e.pos + v,
                        e.pos + i.nodeSize - v,
                        new E(g, p.length, 0),
                        0,
                        !0
                    )
                );
                t(w.scrollIntoView());
            }
            return !0;
        }
    }
    return !1;
}
function _r(n) {
    return function (e, t) {
        let r = e.selection,
            i = n < 0 ? r.$from : r.$to,
            o = i.depth;
        for (; i.node(o).isInline; ) {
            if (!o) return !1;
            o--;
        }
        return i.node(o).isTextblock
            ? (t && t(e.tr.setSelection($.create(e.doc, n < 0 ? i.start(o) : i.end(o)))), !0)
            : !1;
    };
}
const mo = _r(-1),
    go = _r(1);
function vo(n, e = null) {
    return function (t, r) {
        let { $from: i, $to: o } = t.selection,
            s = i.blockRange(o),
            a = s && jr(s, n, e);
        return a ? (r && r(t.tr.wrap(s, a).scrollIntoView()), !0) : !1;
    };
}
function Zn(n, e = null) {
    return function (t, r) {
        let i = !1;
        for (let o = 0; o < t.selection.ranges.length && !i; o++) {
            let {
                $from: { pos: s },
                $to: { pos: a }
            } = t.selection.ranges[o];
            t.doc.nodesBetween(s, a, (l, u) => {
                if (i) return !1;
                if (!(!l.isTextblock || l.hasMarkup(n, e)))
                    if (l.type == n) i = !0;
                    else {
                        let c = t.doc.resolve(u),
                            d = c.index();
                        i = c.parent.canReplaceWith(d, d + 1, n);
                    }
            });
        }
        if (!i) return !1;
        if (r) {
            let o = t.tr;
            for (let s = 0; s < t.selection.ranges.length; s++) {
                let {
                    $from: { pos: a },
                    $to: { pos: l }
                } = t.selection.ranges[s];
                o.setBlockType(a, l, n, e);
            }
            r(o.scrollIntoView());
        }
        return !0;
    };
}
typeof navigator < 'u'
    ? /Mac|iP(hone|[oa]d)/.test(navigator.platform)
    : typeof os < 'u' && os.platform && os.platform() == 'darwin';
function yo(n, e = null) {
    return function (t, r) {
        let { $from: i, $to: o } = t.selection,
            s = i.blockRange(o),
            a = !1,
            l = s;
        if (!s) return !1;
        if (s.depth >= 2 && i.node(s.depth - 1).type.compatibleContent(n) && s.startIndex == 0) {
            if (i.index(s.depth - 1) == 0) return !1;
            let c = t.doc.resolve(s.start - 2);
            (l = new bn(c, c, s.depth)),
                s.endIndex < s.parent.childCount &&
                    (s = new bn(i, t.doc.resolve(o.end(s.depth)), s.depth)),
                (a = !0);
        }
        let u = jr(l, n, e, s);
        return u ? (r && r(wo(t.tr, s, u, a, n).scrollIntoView()), !0) : !1;
    };
}
function wo(n, e, t, r, i) {
    let o = k.empty;
    for (let c = t.length - 1; c >= 0; c--) o = k.from(t[c].type.create(t[c].attrs, o));
    n.step(new G(e.start - (r ? 2 : 0), e.end, e.start, e.end, new E(o, 0, 0), t.length, !0));
    let s = 0;
    for (let c = 0; c < t.length; c++) t[c].type == i && (s = c + 1);
    let a = t.length - s,
        l = e.start + t.length - (r ? 2 : 0),
        u = e.parent;
    for (let c = e.startIndex, d = e.endIndex, p = !0; c < d; c++, p = !1)
        !p && Xe(n.doc, l, a) && (n.split(l, a), (l += 2 * a)), (l += u.child(c).nodeSize);
    return n;
}
function bo(n) {
    return function (e, t) {
        let { $from: r, $to: i } = e.selection,
            o = r.blockRange(i, (s) => s.childCount > 0 && s.firstChild.type == n);
        return o ? (t ? (r.node(o.depth - 1).type == n ? xo(e, t, n, o) : ko(e, t, o)) : !0) : !1;
    };
}
function xo(n, e, t, r) {
    let i = n.tr,
        o = r.end,
        s = r.$to.end(r.depth);
    o < s &&
        (i.step(new G(o - 1, s, o, s, new E(k.from(t.create(null, r.parent.copy())), 1, 0), 1, !0)),
        (r = new bn(i.doc.resolve(r.$from.pos), i.doc.resolve(s), r.depth)));
    const a = nt(r);
    if (a == null) return !1;
    i.lift(r, a);
    let l = i.mapping.map(o, -1) - 1;
    return Ve(i.doc, l) && i.join(l), e(i.scrollIntoView()), !0;
}
function ko(n, e, t) {
    let r = n.tr,
        i = t.parent;
    for (let h = t.end, v = t.endIndex - 1, g = t.startIndex; v > g; v--)
        (h -= i.child(v).nodeSize), r.delete(h - 1, h + 1);
    let o = r.doc.resolve(t.start),
        s = o.nodeAfter;
    if (r.mapping.map(t.end) != t.start + o.nodeAfter.nodeSize) return !1;
    let a = t.startIndex == 0,
        l = t.endIndex == i.childCount,
        u = o.node(-1),
        c = o.index(-1);
    if (!u.canReplace(c + (a ? 0 : 1), c + 1, s.content.append(l ? k.empty : k.from(i)))) return !1;
    let d = o.pos,
        p = d + s.nodeSize;
    return (
        r.step(
            new G(
                d - (a ? 1 : 0),
                p + (l ? 1 : 0),
                d + 1,
                p - 1,
                new E(
                    (a ? k.empty : k.from(i.copy(k.empty))).append(
                        l ? k.empty : k.from(i.copy(k.empty))
                    ),
                    a ? 0 : 1,
                    l ? 0 : 1
                ),
                a ? 0 : 1
            )
        ),
        e(r.scrollIntoView()),
        !0
    );
}
function So(n) {
    return function (e, t) {
        let { $from: r, $to: i } = e.selection,
            o = r.blockRange(i, (u) => u.childCount > 0 && u.firstChild.type == n);
        if (!o) return !1;
        let s = o.startIndex;
        if (s == 0) return !1;
        let a = o.parent,
            l = a.child(s - 1);
        if (l.type != n) return !1;
        if (t) {
            let u = l.lastChild && l.lastChild.type == a.type,
                c = k.from(u ? n.create() : null),
                d = new E(k.from(n.create(null, k.from(a.type.create(null, c)))), u ? 3 : 1, 0),
                p = o.start,
                h = o.end;
            t(e.tr.step(new G(p - (u ? 3 : 1), h, p, h, d, 1, !0)).scrollIntoView());
        }
        return !0;
    };
}
function Qr(n) {
    const { state: e, transaction: t } = n;
    let { selection: r } = t,
        { doc: i } = t,
        { storedMarks: o } = t;
    return {
        ...e,
        apply: e.apply.bind(e),
        applyTransaction: e.applyTransaction.bind(e),
        filterTransaction: e.filterTransaction,
        plugins: e.plugins,
        schema: e.schema,
        reconfigure: e.reconfigure.bind(e),
        toJSON: e.toJSON.bind(e),
        get storedMarks() {
            return o;
        },
        get selection() {
            return r;
        },
        get doc() {
            return i;
        },
        get tr() {
            return (r = t.selection), (i = t.doc), (o = t.storedMarks), t;
        }
    };
}
class Eo {
    constructor(e) {
        (this.editor = e.editor),
            (this.rawCommands = this.editor.extensionManager.commands),
            (this.customState = e.state);
    }
    get hasCustomState() {
        return !!this.customState;
    }
    get state() {
        return this.customState || this.editor.state;
    }
    get commands() {
        const { rawCommands: e, editor: t, state: r } = this,
            { view: i } = t,
            { tr: o } = r,
            s = this.buildProps(o);
        return Object.fromEntries(
            Object.entries(e).map(([a, l]) => [
                a,
                (...c) => {
                    const d = l(...c)(s);
                    return (
                        !o.getMeta('preventDispatch') && !this.hasCustomState && i.dispatch(o), d
                    );
                }
            ])
        );
    }
    get chain() {
        return () => this.createChain();
    }
    get can() {
        return () => this.createCan();
    }
    createChain(e, t = !0) {
        const { rawCommands: r, editor: i, state: o } = this,
            { view: s } = i,
            a = [],
            l = !!e,
            u = e || o.tr,
            c = () => (
                !l && t && !u.getMeta('preventDispatch') && !this.hasCustomState && s.dispatch(u),
                a.every((p) => p === !0)
            ),
            d = {
                ...Object.fromEntries(
                    Object.entries(r).map(([p, h]) => [
                        p,
                        (...g) => {
                            const w = this.buildProps(u, t),
                                x = h(...g)(w);
                            return a.push(x), d;
                        }
                    ])
                ),
                run: c
            };
        return d;
    }
    createCan(e) {
        const { rawCommands: t, state: r } = this,
            i = !1,
            o = e || r.tr,
            s = this.buildProps(o, i);
        return {
            ...Object.fromEntries(
                Object.entries(t).map(([l, u]) => [
                    l,
                    (...c) => u(...c)({ ...s, dispatch: void 0 })
                ])
            ),
            chain: () => this.createChain(o, i)
        };
    }
    buildProps(e, t = !0) {
        const { rawCommands: r, editor: i, state: o } = this,
            { view: s } = i;
        o.storedMarks && e.setStoredMarks(o.storedMarks);
        const a = {
            tr: e,
            editor: i,
            view: s,
            state: Qr({ state: o, transaction: e }),
            dispatch: t ? () => {} : void 0,
            chain: () => this.createChain(e),
            can: () => this.createCan(e),
            get commands() {
                return Object.fromEntries(
                    Object.entries(r).map(([l, u]) => [l, (...c) => u(...c)(a)])
                );
            }
        };
        return a;
    }
}
function V(n, e, t) {
    return n.config[e] === void 0 && n.parent
        ? V(n.parent, e, t)
        : typeof n.config[e] == 'function'
        ? n.config[e].bind({ ...t, parent: n.parent ? V(n.parent, e, t) : null })
        : n.config[e];
}
function Oo(n) {
    const e = n.filter((i) => i.type === 'extension'),
        t = n.filter((i) => i.type === 'node'),
        r = n.filter((i) => i.type === 'mark');
    return { baseExtensions: e, nodeExtensions: t, markExtensions: r };
}
function U(n, e) {
    if (typeof n == 'string') {
        if (!e.nodes[n])
            throw Error(
                `There is no node type named '${n}'. Maybe you forgot to add the extension?`
            );
        return e.nodes[n];
    }
    return n;
}
function Mo(...n) {
    return n
        .filter((e) => !!e)
        .reduce((e, t) => {
            const r = { ...e };
            return (
                Object.entries(t).forEach(([i, o]) => {
                    if (!r[i]) {
                        r[i] = o;
                        return;
                    }
                    i === 'class'
                        ? (r[i] = [r[i], o].join(' '))
                        : i === 'style'
                        ? (r[i] = [r[i], o].join('; '))
                        : (r[i] = o);
                }),
                r
            );
        }, {});
}
function To(n) {
    return typeof n == 'function';
}
function W(n, e = void 0, ...t) {
    return To(n) ? (e ? n.bind(e)(...t) : n(...t)) : n;
}
function Ao(n) {
    return Object.prototype.toString.call(n) === '[object RegExp]';
}
class Co {
    constructor(e) {
        (this.find = e.find), (this.handler = e.handler);
    }
}
class Io {
    constructor(e) {
        (this.find = e.find), (this.handler = e.handler);
    }
}
function No(n) {
    return Object.prototype.toString.call(n).slice(8, -1);
}
function hn(n) {
    return No(n) !== 'Object'
        ? !1
        : n.constructor === Object && Object.getPrototypeOf(n) === Object.prototype;
}
function nn(n, e) {
    const t = { ...n };
    return (
        hn(n) &&
            hn(e) &&
            Object.keys(e).forEach((r) => {
                hn(e[r])
                    ? r in n
                        ? (t[r] = nn(n[r], e[r]))
                        : Object.assign(t, { [r]: e[r] })
                    : Object.assign(t, { [r]: e[r] });
            }),
        t
    );
}
class ve {
    constructor(e = {}) {
        (this.type = 'extension'),
            (this.name = 'extension'),
            (this.parent = null),
            (this.child = null),
            (this.config = { name: this.name, defaultOptions: {} }),
            (this.config = { ...this.config, ...e }),
            (this.name = this.config.name),
            e.defaultOptions &&
                console.warn(
                    `[tiptap warn]: BREAKING CHANGE: "defaultOptions" is deprecated. Please use "addOptions" instead. Found in extension: "${this.name}".`
                ),
            (this.options = this.config.defaultOptions),
            this.config.addOptions &&
                (this.options = W(V(this, 'addOptions', { name: this.name }))),
            (this.storage =
                W(V(this, 'addStorage', { name: this.name, options: this.options })) || {});
    }
    static create(e = {}) {
        return new ve(e);
    }
    configure(e = {}) {
        const t = this.extend();
        return (
            (t.options = nn(this.options, e)),
            (t.storage = W(V(t, 'addStorage', { name: t.name, options: t.options }))),
            t
        );
    }
    extend(e = {}) {
        const t = new ve(e);
        return (
            (t.parent = this),
            (this.child = t),
            (t.name = e.name ? e.name : t.parent.name),
            e.defaultOptions &&
                console.warn(
                    `[tiptap warn]: BREAKING CHANGE: "defaultOptions" is deprecated. Please use "addOptions" instead. Found in extension: "${t.name}".`
                ),
            (t.options = W(V(t, 'addOptions', { name: t.name }))),
            (t.storage = W(V(t, 'addStorage', { name: t.name, options: t.options }))),
            t
        );
    }
}
function Ro(n, e, t) {
    const { from: r, to: i } = e,
        {
            blockSeparator: o = `

`,
            textSerializers: s = {}
        } = t || {};
    let a = '',
        l = !0;
    return (
        n.nodesBetween(r, i, (u, c, d, p) => {
            var h;
            const v = s?.[u.type.name];
            v
                ? (u.isBlock && !l && ((a += o), (l = !0)),
                  d && (a += v({ node: u, pos: c, parent: d, index: p, range: e })))
                : u.isText
                ? ((a +=
                      (h = u?.text) === null || h === void 0
                          ? void 0
                          : h.slice(Math.max(r, c) - c, i - c)),
                  (l = !1))
                : u.isBlock && !l && ((a += o), (l = !0));
        }),
        a
    );
}
function Do(n) {
    return Object.fromEntries(
        Object.entries(n.nodes)
            .filter(([, e]) => e.spec.toText)
            .map(([e, t]) => [e, t.spec.toText])
    );
}
ve.create({
    name: 'clipboardTextSerializer',
    addProseMirrorPlugins() {
        return [
            new rt({
                key: new it('clipboardTextSerializer'),
                props: {
                    clipboardTextSerializer: () => {
                        const { editor: n } = this,
                            { state: e, schema: t } = n,
                            { doc: r, selection: i } = e,
                            { ranges: o } = i,
                            s = Math.min(...o.map((c) => c.$from.pos)),
                            a = Math.max(...o.map((c) => c.$to.pos)),
                            l = Do(t);
                        return Ro(r, { from: s, to: a }, { textSerializers: l });
                    }
                }
            })
        ];
    }
});
const Po =
        () =>
        ({ editor: n, view: e }) => (
            requestAnimationFrame(() => {
                var t;
                n.isDestroyed ||
                    (e.dom.blur(),
                    (t = window?.getSelection()) === null || t === void 0 || t.removeAllRanges());
            }),
            !0
        ),
    Bo =
        (n = !1) =>
        ({ commands: e }) =>
            e.setContent('', n),
    zo =
        () =>
        ({ state: n, tr: e, dispatch: t }) => {
            const { selection: r } = e,
                { ranges: i } = r;
            return (
                t &&
                    i.forEach(({ $from: o, $to: s }) => {
                        n.doc.nodesBetween(o.pos, s.pos, (a, l) => {
                            if (a.type.isText) return;
                            const { doc: u, mapping: c } = e,
                                d = u.resolve(c.map(l)),
                                p = u.resolve(c.map(l + a.nodeSize)),
                                h = d.blockRange(p);
                            if (!h) return;
                            const v = nt(h);
                            if (a.type.isTextblock) {
                                const { defaultType: g } = d.parent.contentMatchAt(d.index());
                                e.setNodeMarkup(h.start, g);
                            }
                            (v || v === 0) && e.lift(h, v);
                        });
                    }),
                !0
            );
        },
    Fo = (n) => (e) => n(e),
    Lo =
        () =>
        ({ state: n, dispatch: e }) =>
            uo(n, e),
    $o =
        () =>
        ({ tr: n, dispatch: e }) => {
            const { selection: t } = n,
                r = t.$anchor.node();
            if (r.content.size > 0) return !1;
            const i = n.selection.$anchor;
            for (let o = i.depth; o > 0; o -= 1)
                if (i.node(o).type === r.type) {
                    if (e) {
                        const a = i.before(o),
                            l = i.after(o);
                        n.delete(a, l).scrollIntoView();
                    }
                    return !0;
                }
            return !1;
        },
    jo =
        (n) =>
        ({ tr: e, state: t, dispatch: r }) => {
            const i = U(n, t.schema),
                o = e.selection.$anchor;
            for (let s = o.depth; s > 0; s -= 1)
                if (o.node(s).type === i) {
                    if (r) {
                        const l = o.before(s),
                            u = o.after(s);
                        e.delete(l, u).scrollIntoView();
                    }
                    return !0;
                }
            return !1;
        },
    Vo =
        (n) =>
        ({ tr: e, dispatch: t }) => {
            const { from: r, to: i } = n;
            return t && e.delete(r, i), !0;
        },
    Wo =
        () =>
        ({ state: n, dispatch: e }) =>
            Qi(n, e),
    Jo =
        () =>
        ({ commands: n }) =>
            n.keyboardShortcut('Enter'),
    Ho =
        () =>
        ({ state: n, dispatch: e }) =>
            co(n, e);
function Yt(n, e, t = { strict: !0 }) {
    const r = Object.keys(e);
    return r.length
        ? r.every((i) => (t.strict ? e[i] === n[i] : Ao(e[i]) ? e[i].test(n[i]) : e[i] === n[i]))
        : !0;
}
function kn(n, e, t = {}) {
    return n.find((r) => r.type === e && Yt(r.attrs, t));
}
function qo(n, e, t = {}) {
    return !!kn(n, e, t);
}
function Nn(n, e, t = {}) {
    if (!n || !e) return;
    let r = n.parent.childAfter(n.parentOffset);
    if (
        (n.parentOffset === r.offset &&
            r.offset !== 0 &&
            (r = n.parent.childBefore(n.parentOffset)),
        !r.node)
    )
        return;
    const i = kn([...r.node.marks], e, t);
    if (!i) return;
    let o = r.index,
        s = n.start() + r.offset,
        a = o + 1,
        l = s + r.node.nodeSize;
    for (kn([...r.node.marks], e, t); o > 0 && i.isInSet(n.parent.child(o - 1).marks); )
        (o -= 1), (s -= n.parent.child(o).nodeSize);
    for (; a < n.parent.childCount && qo([...n.parent.child(a).marks], e, t); )
        (l += n.parent.child(a).nodeSize), (a += 1);
    return { from: s, to: l };
}
function Ae(n, e) {
    if (typeof n == 'string') {
        if (!e.marks[n])
            throw Error(
                `There is no mark type named '${n}'. Maybe you forgot to add the extension?`
            );
        return e.marks[n];
    }
    return n;
}
const Uo =
        (n, e = {}) =>
        ({ tr: t, state: r, dispatch: i }) => {
            const o = Ae(n, r.schema),
                { doc: s, selection: a } = t,
                { $from: l, from: u, to: c } = a;
            if (i) {
                const d = Nn(l, o, e);
                if (d && d.from <= u && d.to >= c) {
                    const p = $.create(s, d.from, d.to);
                    t.setSelection(p);
                }
            }
            return !0;
        },
    Ko = (n) => (e) => {
        const t = typeof n == 'function' ? n(e) : n;
        for (let r = 0; r < t.length; r += 1) if (t[r](e)) return !0;
        return !1;
    };
function Zr(n) {
    return n instanceof $;
}
function Ee(n = 0, e = 0, t = 0) {
    return Math.min(Math.max(n, e), t);
}
function Go(n, e = null) {
    if (!e) return null;
    const t = R.atStart(n),
        r = R.atEnd(n);
    if (e === 'start' || e === !0) return t;
    if (e === 'end') return r;
    const i = t.from,
        o = r.to;
    return e === 'all'
        ? $.create(n, Ee(0, i, o), Ee(n.content.size, i, o))
        : $.create(n, Ee(e, i, o), Ee(e, i, o));
}
function Rn() {
    return (
        ['iPad Simulator', 'iPhone Simulator', 'iPod Simulator', 'iPad', 'iPhone', 'iPod'].includes(
            navigator.platform
        ) ||
        (navigator.userAgent.includes('Mac') && 'ontouchend' in document)
    );
}
const Yo =
        (n = null, e = {}) =>
        ({ editor: t, view: r, tr: i, dispatch: o }) => {
            e = { scrollIntoView: !0, ...e };
            const s = () => {
                Rn() && r.dom.focus(),
                    requestAnimationFrame(() => {
                        t.isDestroyed ||
                            (r.focus(), e?.scrollIntoView && t.commands.scrollIntoView());
                    });
            };
            if ((r.hasFocus() && n === null) || n === !1) return !0;
            if (o && n === null && !Zr(t.state.selection)) return s(), !0;
            const a = Go(i.doc, n) || t.state.selection,
                l = t.state.selection.eq(a);
            return (
                o &&
                    (l || i.setSelection(a),
                    l && i.storedMarks && i.setStoredMarks(i.storedMarks),
                    s()),
                !0
            );
        },
    Xo = (n, e) => (t) => n.every((r, i) => e(r, { ...t, index: i })),
    _o =
        (n, e) =>
        ({ tr: t, commands: r }) =>
            r.insertContentAt({ from: t.selection.from, to: t.selection.to }, n, e);
function er(n) {
    const e = `<body>${n}</body>`;
    return new window.DOMParser().parseFromString(e, 'text/html').body;
}
function Xt(n, e, t) {
    if (((t = { slice: !0, parseOptions: {}, ...t }), typeof n == 'object' && n !== null))
        try {
            return Array.isArray(n)
                ? k.fromArray(n.map((r) => e.nodeFromJSON(r)))
                : e.nodeFromJSON(n);
        } catch (r) {
            return (
                console.warn('[tiptap warn]: Invalid content.', 'Passed value:', n, 'Error:', r),
                Xt('', e, t)
            );
        }
    if (typeof n == 'string') {
        const r = Ut.fromSchema(e);
        return t.slice
            ? r.parseSlice(er(n), t.parseOptions).content
            : r.parse(er(n), t.parseOptions);
    }
    return Xt('', e, t);
}
function Qo(n, e, t) {
    const r = n.steps.length - 1;
    if (r < e) return;
    const i = n.steps[r];
    if (!(i instanceof ie || i instanceof G)) return;
    const o = n.mapping.maps[r];
    let s = 0;
    o.forEach((a, l, u, c) => {
        s === 0 && (s = c);
    }),
        n.setSelection(R.near(n.doc.resolve(s), t));
}
const Zo = (n) => n.toString().startsWith('<'),
    es =
        (n, e, t) =>
        ({ tr: r, dispatch: i, editor: o }) => {
            if (i) {
                t = { parseOptions: {}, updateSelection: !0, ...t };
                const s = Xt(e, o.schema, {
                    parseOptions: { preserveWhitespace: 'full', ...t.parseOptions }
                });
                if (s.toString() === '<>') return !0;
                let { from: a, to: l } = typeof n == 'number' ? { from: n, to: n } : n,
                    u = !0,
                    c = !0;
                if (
                    ((Zo(s) ? s : [s]).forEach((p) => {
                        p.check(),
                            (u = u ? p.isText && p.marks.length === 0 : !1),
                            (c = c ? p.isBlock : !1);
                    }),
                    a === l && c)
                ) {
                    const { parent: p } = r.doc.resolve(a);
                    p.isTextblock && !p.type.spec.code && !p.childCount && ((a -= 1), (l += 1));
                }
                u ? r.insertText(e, a, l) : r.replaceWith(a, l, s),
                    t.updateSelection && Qo(r, r.steps.length - 1, -1);
            }
            return !0;
        },
    ts =
        () =>
        ({ state: n, dispatch: e }) =>
            oo(n, e),
    ns =
        () =>
        ({ state: n, dispatch: e }) =>
            so(n, e),
    rs =
        () =>
        ({ state: n, dispatch: e }) =>
            eo(n, e),
    is =
        () =>
        ({ state: n, dispatch: e }) =>
            ro(n, e);
function ei() {
    return typeof navigator < 'u' ? /Mac/.test(navigator.platform) : !1;
}
function ss(n) {
    const e = n.split(/-(?!$)/);
    let t = e[e.length - 1];
    t === 'Space' && (t = ' ');
    let r, i, o, s;
    for (let a = 0; a < e.length - 1; a += 1) {
        const l = e[a];
        if (/^(cmd|meta|m)$/i.test(l)) s = !0;
        else if (/^a(lt)?$/i.test(l)) r = !0;
        else if (/^(c|ctrl|control)$/i.test(l)) i = !0;
        else if (/^s(hift)?$/i.test(l)) o = !0;
        else if (/^mod$/i.test(l)) Rn() || ei() ? (s = !0) : (i = !0);
        else throw new Error(`Unrecognized modifier name: ${l}`);
    }
    return (
        r && (t = `Alt-${t}`),
        i && (t = `Ctrl-${t}`),
        s && (t = `Meta-${t}`),
        o && (t = `Shift-${t}`),
        t
    );
}
const as =
    (n) =>
    ({ editor: e, view: t, tr: r, dispatch: i }) => {
        const o = ss(n).split(/-(?!$)/),
            s = o.find((u) => !['Alt', 'Ctrl', 'Meta', 'Shift'].includes(u)),
            a = new KeyboardEvent('keydown', {
                key: s === 'Space' ? ' ' : s,
                altKey: o.includes('Alt'),
                ctrlKey: o.includes('Ctrl'),
                metaKey: o.includes('Meta'),
                shiftKey: o.includes('Shift'),
                bubbles: !0,
                cancelable: !0
            }),
            l = e.captureTransaction(() => {
                t.someProp('handleKeyDown', (u) => u(t, a));
            });
        return (
            l?.steps.forEach((u) => {
                const c = u.map(r.mapping);
                c && i && r.maybeStep(c);
            }),
            !0
        );
    };
function Dn(n, e, t = {}) {
    const { from: r, to: i, empty: o } = n.selection,
        s = e ? U(e, n.schema) : null,
        a = [];
    n.doc.nodesBetween(r, i, (d, p) => {
        if (d.isText) return;
        const h = Math.max(r, p),
            v = Math.min(i, p + d.nodeSize);
        a.push({ node: d, from: h, to: v });
    });
    const l = i - r,
        u = a
            .filter((d) => (s ? s.name === d.node.type.name : !0))
            .filter((d) => Yt(d.node.attrs, t, { strict: !1 }));
    return o ? !!u.length : u.reduce((d, p) => d + p.to - p.from, 0) >= l;
}
const ls =
        (n, e = {}) =>
        ({ state: t, dispatch: r }) => {
            const i = U(n, t.schema);
            return Dn(t, i, e) ? ao(t, r) : !1;
        },
    cs =
        () =>
        ({ state: n, dispatch: e }) =>
            fo(n, e),
    us =
        (n) =>
        ({ state: e, dispatch: t }) => {
            const r = U(n, e.schema);
            return bo(r)(e, t);
        },
    fs =
        () =>
        ({ state: n, dispatch: e }) =>
            lo(n, e);
function ti(n, e) {
    return e.nodes[n] ? 'node' : e.marks[n] ? 'mark' : null;
}
function tr(n, e) {
    const t = typeof e == 'string' ? [e] : e;
    return Object.keys(n).reduce((r, i) => (t.includes(i) || (r[i] = n[i]), r), {});
}
const ds =
        (n, e) =>
        ({ tr: t, state: r, dispatch: i }) => {
            let o = null,
                s = null;
            const a = ti(typeof n == 'string' ? n : n.name, r.schema);
            return a
                ? (a === 'node' && (o = U(n, r.schema)),
                  a === 'mark' && (s = Ae(n, r.schema)),
                  i &&
                      t.selection.ranges.forEach((l) => {
                          r.doc.nodesBetween(l.$from.pos, l.$to.pos, (u, c) => {
                              o && o === u.type && t.setNodeMarkup(c, void 0, tr(u.attrs, e)),
                                  s &&
                                      u.marks.length &&
                                      u.marks.forEach((d) => {
                                          s === d.type &&
                                              t.addMark(
                                                  c,
                                                  c + u.nodeSize,
                                                  s.create(tr(d.attrs, e))
                                              );
                                      });
                          });
                      }),
                  !0)
                : !1;
        },
    ps =
        () =>
        ({ tr: n, dispatch: e }) => (e && n.scrollIntoView(), !0),
    hs =
        () =>
        ({ tr: n, commands: e }) =>
            e.setTextSelection({ from: 0, to: n.doc.content.size }),
    ms =
        () =>
        ({ state: n, dispatch: e }) =>
            to(n, e),
    gs =
        () =>
        ({ state: n, dispatch: e }) =>
            io(n, e),
    vs =
        () =>
        ({ state: n, dispatch: e }) =>
            po(n, e),
    ys =
        () =>
        ({ state: n, dispatch: e }) =>
            go(n, e),
    ws =
        () =>
        ({ state: n, dispatch: e }) =>
            mo(n, e);
function bs(n, e, t = {}) {
    return Xt(n, e, { slice: !1, parseOptions: t });
}
const xs =
    (n, e = !1, t = {}) =>
    ({ tr: r, editor: i, dispatch: o }) => {
        const { doc: s } = r,
            a = bs(n, i.schema, t);
        return o && r.replaceWith(0, s.content.size, a).setMeta('preventUpdate', !e), !0;
    };
function ks(n) {
    for (let e = 0; e < n.edgeCount; e += 1) {
        const { type: t } = n.edge(e);
        if (t.isTextblock && !t.hasRequiredAttrs()) return t;
    }
    return null;
}
function Ss(n, e) {
    for (let t = n.depth; t > 0; t -= 1) {
        const r = n.node(t);
        if (e(r)) return { pos: t > 0 ? n.before(t) : 0, start: n.start(t), depth: t, node: r };
    }
}
function Pn(n) {
    return (e) => Ss(e.$from, n);
}
function Es(n, e) {
    const t = Ae(e, n.schema),
        { from: r, to: i, empty: o } = n.selection,
        s = [];
    o
        ? (n.storedMarks && s.push(...n.storedMarks), s.push(...n.selection.$head.marks()))
        : n.doc.nodesBetween(r, i, (l) => {
              s.push(...l.marks);
          });
    const a = s.find((l) => l.type.name === t.name);
    return a ? { ...a.attrs } : {};
}
function ni(n, e, t) {
    const r = [];
    return (
        n === e
            ? t
                  .resolve(n)
                  .marks()
                  .forEach((i) => {
                      const o = t.resolve(n - 1),
                          s = Nn(o, i.type);
                      s && r.push({ mark: i, ...s });
                  })
            : t.nodesBetween(n, e, (i, o) => {
                  r.push(...i.marks.map((s) => ({ from: o, to: o + i.nodeSize, mark: s })));
              }),
        r
    );
}
function Os(n, e, t = {}) {
    const { empty: r, ranges: i } = n.selection,
        o = e ? Ae(e, n.schema) : null;
    if (r)
        return !!(n.storedMarks || n.selection.$from.marks())
            .filter((d) => (o ? o.name === d.type.name : !0))
            .find((d) => Yt(d.attrs, t, { strict: !1 }));
    let s = 0;
    const a = [];
    if (
        (i.forEach(({ $from: d, $to: p }) => {
            const h = d.pos,
                v = p.pos;
            n.doc.nodesBetween(h, v, (g, w) => {
                if (!g.isText && !g.marks.length) return;
                const x = Math.max(h, w),
                    A = Math.min(v, w + g.nodeSize),
                    N = A - x;
                (s += N), a.push(...g.marks.map((f) => ({ mark: f, from: x, to: A })));
            });
        }),
        s === 0)
    )
        return !1;
    const l = a
            .filter((d) => (o ? o.name === d.mark.type.name : !0))
            .filter((d) => Yt(d.mark.attrs, t, { strict: !1 }))
            .reduce((d, p) => d + p.to - p.from, 0),
        u = a
            .filter((d) => (o ? d.mark.type !== o && d.mark.type.excludes(o) : !0))
            .reduce((d, p) => d + p.to - p.from, 0);
    return (l > 0 ? l + u : l) >= s;
}
function nr(n, e) {
    const { nodeExtensions: t } = Oo(e),
        r = t.find((s) => s.name === n);
    if (!r) return !1;
    const i = { name: r.name, options: r.options, storage: r.storage },
        o = W(V(r, 'group', i));
    return typeof o != 'string' ? !1 : o.split(' ').includes('list');
}
function Ms(n, e, t) {
    const i = n.state.doc.content.size,
        o = Ee(e, 0, i),
        s = Ee(t, 0, i),
        a = n.coordsAtPos(o),
        l = n.coordsAtPos(s, -1),
        u = Math.min(a.top, l.top),
        c = Math.max(a.bottom, l.bottom),
        d = Math.min(a.left, l.left),
        p = Math.max(a.right, l.right),
        h = p - d,
        v = c - u,
        x = { top: u, bottom: c, left: d, right: p, width: h, height: v, x: d, y: u };
    return { ...x, toJSON: () => x };
}
function Ts(n, e, t) {
    var r;
    const { selection: i } = e;
    let o = null;
    if ((Zr(i) && (o = i.$cursor), o)) {
        const a = (r = n.storedMarks) !== null && r !== void 0 ? r : o.marks();
        return !!t.isInSet(a) || !a.some((l) => l.type.excludes(t));
    }
    const { ranges: s } = i;
    return s.some(({ $from: a, $to: l }) => {
        let u = a.depth === 0 ? n.doc.inlineContent && n.doc.type.allowsMarkType(t) : !1;
        return (
            n.doc.nodesBetween(a.pos, l.pos, (c, d, p) => {
                if (u) return !1;
                if (c.isInline) {
                    const h = !p || p.type.allowsMarkType(t),
                        v = !!t.isInSet(c.marks) || !c.marks.some((g) => g.type.excludes(t));
                    u = h && v;
                }
                return !u;
            }),
            u
        );
    });
}
const As =
        (n, e = {}) =>
        ({ tr: t, state: r, dispatch: i }) => {
            const { selection: o } = t,
                { empty: s, ranges: a } = o,
                l = Ae(n, r.schema);
            if (i)
                if (s) {
                    const u = Es(r, l);
                    t.addStoredMark(l.create({ ...u, ...e }));
                } else
                    a.forEach((u) => {
                        const c = u.$from.pos,
                            d = u.$to.pos;
                        r.doc.nodesBetween(c, d, (p, h) => {
                            const v = Math.max(h, c),
                                g = Math.min(h + p.nodeSize, d);
                            p.marks.find((x) => x.type === l)
                                ? p.marks.forEach((x) => {
                                      l === x.type &&
                                          t.addMark(v, g, l.create({ ...x.attrs, ...e }));
                                  })
                                : t.addMark(v, g, l.create(e));
                        });
                    });
            return Ts(r, t, l);
        },
    Cs =
        (n, e) =>
        ({ tr: t }) => (t.setMeta(n, e), !0),
    Is =
        (n, e = {}) =>
        ({ state: t, dispatch: r, chain: i }) => {
            const o = U(n, t.schema);
            return o.isTextblock
                ? i()
                      .command(({ commands: s }) => (Zn(o, e)(t) ? !0 : s.clearNodes()))
                      .command(({ state: s }) => Zn(o, e)(s, r))
                      .run()
                : (console.warn(
                      '[tiptap warn]: Currently "setNode()" only supports text block nodes.'
                  ),
                  !1);
        },
    Ns =
        (n) =>
        ({ tr: e, dispatch: t }) => {
            if (t) {
                const { doc: r } = e,
                    i = Ee(n, 0, r.content.size),
                    o = P.create(r, i);
                e.setSelection(o);
            }
            return !0;
        },
    Rs =
        (n) =>
        ({ tr: e, dispatch: t }) => {
            if (t) {
                const { doc: r } = e,
                    { from: i, to: o } = typeof n == 'number' ? { from: n, to: n } : n,
                    s = $.atStart(r).from,
                    a = $.atEnd(r).to,
                    l = Ee(i, s, a),
                    u = Ee(o, s, a),
                    c = $.create(r, l, u);
                e.setSelection(c);
            }
            return !0;
        },
    Ds =
        (n) =>
        ({ state: e, dispatch: t }) => {
            const r = U(n, e.schema);
            return So(r)(e, t);
        };
function Ht(n, e, t) {
    return Object.fromEntries(
        Object.entries(t).filter(([r]) => {
            const i = n.find((o) => o.type === e && o.name === r);
            return i ? i.attribute.keepOnSplit : !1;
        })
    );
}
function rr(n, e) {
    const t = n.storedMarks || (n.selection.$to.parentOffset && n.selection.$from.marks());
    if (t) {
        const r = t.filter((i) => e?.includes(i.type.name));
        n.tr.ensureMarks(r);
    }
}
const Ps =
        ({ keepMarks: n = !0 } = {}) =>
        ({ tr: e, state: t, dispatch: r, editor: i }) => {
            const { selection: o, doc: s } = e,
                { $from: a, $to: l } = o,
                u = i.extensionManager.attributes,
                c = Ht(u, a.node().type.name, a.node().attrs);
            if (o instanceof P && o.node.isBlock)
                return !a.parentOffset || !Xe(s, a.pos)
                    ? !1
                    : (r &&
                          (n && rr(t, i.extensionManager.splittableMarks),
                          e.split(a.pos).scrollIntoView()),
                      !0);
            if (!a.parent.isBlock) return !1;
            if (r) {
                const d = l.parentOffset === l.parent.content.size;
                o instanceof $ && e.deleteSelection();
                const p = a.depth === 0 ? void 0 : ks(a.node(-1).contentMatchAt(a.indexAfter(-1)));
                let h = d && p ? [{ type: p, attrs: c }] : void 0,
                    v = Xe(e.doc, e.mapping.map(a.pos), 1, h);
                if (
                    (!h &&
                        !v &&
                        Xe(e.doc, e.mapping.map(a.pos), 1, p ? [{ type: p }] : void 0) &&
                        ((v = !0), (h = p ? [{ type: p, attrs: c }] : void 0)),
                    v &&
                        (e.split(e.mapping.map(a.pos), 1, h),
                        p && !d && !a.parentOffset && a.parent.type !== p))
                ) {
                    const g = e.mapping.map(a.before()),
                        w = e.doc.resolve(g);
                    a.node(-1).canReplaceWith(w.index(), w.index() + 1, p) &&
                        e.setNodeMarkup(e.mapping.map(a.before()), p);
                }
                n && rr(t, i.extensionManager.splittableMarks), e.scrollIntoView();
            }
            return !0;
        },
    Bs =
        (n) =>
        ({ tr: e, state: t, dispatch: r, editor: i }) => {
            var o;
            const s = U(n, t.schema),
                { $from: a, $to: l } = t.selection,
                u = t.selection.node;
            if ((u && u.isBlock) || a.depth < 2 || !a.sameParent(l)) return !1;
            const c = a.node(-1);
            if (c.type !== s) return !1;
            const d = i.extensionManager.attributes;
            if (a.parent.content.size === 0 && a.node(-1).childCount === a.indexAfter(-1)) {
                if (
                    a.depth === 2 ||
                    a.node(-3).type !== s ||
                    a.index(-2) !== a.node(-2).childCount - 1
                )
                    return !1;
                if (r) {
                    let w = k.empty;
                    const x = a.index(-1) ? 1 : a.index(-2) ? 2 : 3;
                    for (let M = a.depth - x; M >= a.depth - 3; M -= 1)
                        w = k.from(a.node(M).copy(w));
                    const A =
                            a.indexAfter(-1) < a.node(-2).childCount
                                ? 1
                                : a.indexAfter(-2) < a.node(-3).childCount
                                ? 2
                                : 3,
                        N = Ht(d, a.node().type.name, a.node().attrs),
                        f =
                            ((o = s.contentMatch.defaultType) === null || o === void 0
                                ? void 0
                                : o.createAndFill(N)) || void 0;
                    w = w.append(k.from(s.createAndFill(null, f) || void 0));
                    const O = a.before(a.depth - (x - 1));
                    e.replace(O, a.after(-A), new E(w, 4 - x, 0));
                    let y = -1;
                    e.doc.nodesBetween(O, e.doc.content.size, (M, B) => {
                        if (y > -1) return !1;
                        M.isTextblock && M.content.size === 0 && (y = B + 1);
                    }),
                        y > -1 && e.setSelection($.near(e.doc.resolve(y))),
                        e.scrollIntoView();
                }
                return !0;
            }
            const p = l.pos === a.end() ? c.contentMatchAt(0).defaultType : null,
                h = Ht(d, c.type.name, c.attrs),
                v = Ht(d, a.node().type.name, a.node().attrs);
            e.delete(a.pos, l.pos);
            const g = p
                ? [
                      { type: s, attrs: h },
                      { type: p, attrs: v }
                  ]
                : [{ type: s, attrs: h }];
            return Xe(e.doc, a.pos, 2) ? (r && e.split(a.pos, 2, g).scrollIntoView(), !0) : !1;
        },
    ir = (n, e) => {
        const t = Pn((s) => s.type === e)(n.selection);
        if (!t) return !0;
        const r = n.doc.resolve(Math.max(0, t.pos - 1)).before(t.depth);
        if (r === void 0) return !0;
        const i = n.doc.nodeAt(r);
        return t.node.type === i?.type && Ve(n.doc, t.pos) && n.join(t.pos), !0;
    },
    or = (n, e) => {
        const t = Pn((s) => s.type === e)(n.selection);
        if (!t) return !0;
        const r = n.doc.resolve(t.start).after(t.depth);
        if (r === void 0) return !0;
        const i = n.doc.nodeAt(r);
        return t.node.type === i?.type && Ve(n.doc, r) && n.join(r), !0;
    },
    zs =
        (n, e) =>
        ({ editor: t, tr: r, state: i, dispatch: o, chain: s, commands: a, can: l }) => {
            const { extensions: u } = t.extensionManager,
                c = U(n, i.schema),
                d = U(e, i.schema),
                { selection: p } = i,
                { $from: h, $to: v } = p,
                g = h.blockRange(v);
            if (!g) return !1;
            const w = Pn((x) => nr(x.type.name, u))(p);
            if (g.depth >= 1 && w && g.depth - w.depth <= 1) {
                if (w.node.type === c) return a.liftListItem(d);
                if (nr(w.node.type.name, u) && c.validContent(w.node.content) && o)
                    return s()
                        .command(() => (r.setNodeMarkup(w.pos, c), !0))
                        .command(() => ir(r, c))
                        .command(() => or(r, c))
                        .run();
            }
            return s()
                .command(() => (l().wrapInList(c) ? !0 : a.clearNodes()))
                .wrapInList(c)
                .command(() => ir(r, c))
                .command(() => or(r, c))
                .run();
        },
    Fs =
        (n, e = {}, t = {}) =>
        ({ state: r, commands: i }) => {
            const { extendEmptyMarkRange: o = !1 } = t,
                s = Ae(n, r.schema);
            return Os(r, s, e) ? i.unsetMark(s, { extendEmptyMarkRange: o }) : i.setMark(s, e);
        },
    Ls =
        (n, e, t = {}) =>
        ({ state: r, commands: i }) => {
            const o = U(n, r.schema),
                s = U(e, r.schema);
            return Dn(r, o, t) ? i.setNode(s) : i.setNode(o, t);
        },
    $s =
        (n, e = {}) =>
        ({ state: t, commands: r }) => {
            const i = U(n, t.schema);
            return Dn(t, i, e) ? r.lift(i) : r.wrapIn(i, e);
        },
    js =
        () =>
        ({ state: n, dispatch: e }) => {
            const t = n.plugins;
            for (let r = 0; r < t.length; r += 1) {
                const i = t[r];
                let o;
                if (i.spec.isInputRules && (o = i.getState(n))) {
                    if (e) {
                        const s = n.tr,
                            a = o.transform;
                        for (let l = a.steps.length - 1; l >= 0; l -= 1)
                            s.step(a.steps[l].invert(a.docs[l]));
                        if (o.text) {
                            const l = s.doc.resolve(o.from).marks();
                            s.replaceWith(o.from, o.to, n.schema.text(o.text, l));
                        } else s.delete(o.from, o.to);
                    }
                    return !0;
                }
            }
            return !1;
        },
    Vs =
        () =>
        ({ tr: n, dispatch: e }) => {
            const { selection: t } = n,
                { empty: r, ranges: i } = t;
            return (
                r ||
                    (e &&
                        i.forEach((o) => {
                            n.removeMark(o.$from.pos, o.$to.pos);
                        })),
                !0
            );
        },
    Ws =
        (n, e = {}) =>
        ({ tr: t, state: r, dispatch: i }) => {
            var o;
            const { extendEmptyMarkRange: s = !1 } = e,
                { selection: a } = t,
                l = Ae(n, r.schema),
                { $from: u, empty: c, ranges: d } = a;
            if (!i) return !0;
            if (c && s) {
                let { from: p, to: h } = a;
                const v =
                        (o = u.marks().find((w) => w.type === l)) === null || o === void 0
                            ? void 0
                            : o.attrs,
                    g = Nn(u, l, v);
                g && ((p = g.from), (h = g.to)), t.removeMark(p, h, l);
            } else
                d.forEach((p) => {
                    t.removeMark(p.$from.pos, p.$to.pos, l);
                });
            return t.removeStoredMark(l), !0;
        },
    Js =
        (n, e = {}) =>
        ({ tr: t, state: r, dispatch: i }) => {
            let o = null,
                s = null;
            const a = ti(typeof n == 'string' ? n : n.name, r.schema);
            return a
                ? (a === 'node' && (o = U(n, r.schema)),
                  a === 'mark' && (s = Ae(n, r.schema)),
                  i &&
                      t.selection.ranges.forEach((l) => {
                          const u = l.$from.pos,
                              c = l.$to.pos;
                          r.doc.nodesBetween(u, c, (d, p) => {
                              o && o === d.type && t.setNodeMarkup(p, void 0, { ...d.attrs, ...e }),
                                  s &&
                                      d.marks.length &&
                                      d.marks.forEach((h) => {
                                          if (s === h.type) {
                                              const v = Math.max(p, u),
                                                  g = Math.min(p + d.nodeSize, c);
                                              t.addMark(v, g, s.create({ ...h.attrs, ...e }));
                                          }
                                      });
                          });
                      }),
                  !0)
                : !1;
        },
    Hs =
        (n, e = {}) =>
        ({ state: t, dispatch: r }) => {
            const i = U(n, t.schema);
            return vo(i, e)(t, r);
        },
    qs =
        (n, e = {}) =>
        ({ state: t, dispatch: r }) => {
            const i = U(n, t.schema);
            return yo(i, e)(t, r);
        };
var Us = Object.freeze({
    __proto__: null,
    blur: Po,
    clearContent: Bo,
    clearNodes: zo,
    command: Fo,
    createParagraphNear: Lo,
    deleteCurrentNode: $o,
    deleteNode: jo,
    deleteRange: Vo,
    deleteSelection: Wo,
    enter: Jo,
    exitCode: Ho,
    extendMarkRange: Uo,
    first: Ko,
    focus: Yo,
    forEach: Xo,
    insertContent: _o,
    insertContentAt: es,
    joinUp: ts,
    joinDown: ns,
    joinBackward: rs,
    joinForward: is,
    keyboardShortcut: as,
    lift: ls,
    liftEmptyBlock: cs,
    liftListItem: us,
    newlineInCode: fs,
    resetAttributes: ds,
    scrollIntoView: ps,
    selectAll: hs,
    selectNodeBackward: ms,
    selectNodeForward: gs,
    selectParentNode: vs,
    selectTextblockEnd: ys,
    selectTextblockStart: ws,
    setContent: xs,
    setMark: As,
    setMeta: Cs,
    setNode: Is,
    setNodeSelection: Ns,
    setTextSelection: Rs,
    sinkListItem: Ds,
    splitBlock: Ps,
    splitListItem: Bs,
    toggleList: zs,
    toggleMark: Fs,
    toggleNode: Ls,
    toggleWrap: $s,
    undoInputRule: js,
    unsetAllMarks: Vs,
    unsetMark: Ws,
    updateAttributes: Js,
    wrapIn: Hs,
    wrapInList: qs
});
ve.create({
    name: 'commands',
    addCommands() {
        return { ...Us };
    }
});
ve.create({
    name: 'editable',
    addProseMirrorPlugins() {
        return [
            new rt({
                key: new it('editable'),
                props: { editable: () => this.editor.options.editable }
            })
        ];
    }
});
ve.create({
    name: 'focusEvents',
    addProseMirrorPlugins() {
        const { editor: n } = this;
        return [
            new rt({
                key: new it('focusEvents'),
                props: {
                    handleDOMEvents: {
                        focus: (e, t) => {
                            n.isFocused = !0;
                            const r = n.state.tr
                                .setMeta('focus', { event: t })
                                .setMeta('addToHistory', !1);
                            return e.dispatch(r), !1;
                        },
                        blur: (e, t) => {
                            n.isFocused = !1;
                            const r = n.state.tr
                                .setMeta('blur', { event: t })
                                .setMeta('addToHistory', !1);
                            return e.dispatch(r), !1;
                        }
                    }
                }
            })
        ];
    }
});
ve.create({
    name: 'keymap',
    addKeyboardShortcuts() {
        const n = () =>
                this.editor.commands.first(({ commands: s }) => [
                    () => s.undoInputRule(),
                    () =>
                        s.command(({ tr: a }) => {
                            const { selection: l, doc: u } = a,
                                { empty: c, $anchor: d } = l,
                                { pos: p, parent: h } = d,
                                v = R.atStart(u).from === p;
                            return !c || !v || !h.type.isTextblock || h.textContent.length
                                ? !1
                                : s.clearNodes();
                        }),
                    () => s.deleteSelection(),
                    () => s.joinBackward(),
                    () => s.selectNodeBackward()
                ]),
            e = () =>
                this.editor.commands.first(({ commands: s }) => [
                    () => s.deleteSelection(),
                    () => s.deleteCurrentNode(),
                    () => s.joinForward(),
                    () => s.selectNodeForward()
                ]),
            r = {
                Enter: () =>
                    this.editor.commands.first(({ commands: s }) => [
                        () => s.newlineInCode(),
                        () => s.createParagraphNear(),
                        () => s.liftEmptyBlock(),
                        () => s.splitBlock()
                    ]),
                'Mod-Enter': () => this.editor.commands.exitCode(),
                Backspace: n,
                'Mod-Backspace': n,
                'Shift-Backspace': n,
                Delete: e,
                'Mod-Delete': e,
                'Mod-a': () => this.editor.commands.selectAll()
            },
            i = { ...r },
            o = {
                ...r,
                'Ctrl-h': n,
                'Alt-Backspace': n,
                'Ctrl-d': e,
                'Ctrl-Alt-Backspace': e,
                'Alt-Delete': e,
                'Alt-d': e,
                'Ctrl-a': () => this.editor.commands.selectTextblockStart(),
                'Ctrl-e': () => this.editor.commands.selectTextblockEnd()
            };
        return Rn() || ei() ? o : i;
    },
    addProseMirrorPlugins() {
        return [
            new rt({
                key: new it('clearDocument'),
                appendTransaction: (n, e, t) => {
                    if (!(n.some((v) => v.docChanged) && !e.doc.eq(t.doc))) return;
                    const { empty: i, from: o, to: s } = e.selection,
                        a = R.atStart(e.doc).from,
                        l = R.atEnd(e.doc).to,
                        u = o === a && s === l,
                        c = t.doc.textBetween(0, t.doc.content.size, ' ', ' ').length === 0;
                    if (i || !u || !c) return;
                    const d = t.tr,
                        p = Qr({ state: t, transaction: d }),
                        { commands: h } = new Eo({ editor: this.editor, state: p });
                    if ((h.clearNodes(), !!d.steps.length)) return d;
                }
            })
        ];
    }
});
ve.create({
    name: 'tabindex',
    addProseMirrorPlugins() {
        return [
            new rt({
                key: new it('tabindex'),
                props: { attributes: this.editor.isEditable ? { tabindex: '0' } : {} }
            })
        ];
    }
});
function sr(n) {
    return new Co({
        find: n.find,
        handler: ({ state: e, range: t, match: r }) => {
            const i = W(n.getAttributes, void 0, r);
            if (i === !1 || i === null) return null;
            const { tr: o } = e,
                s = r[r.length - 1],
                a = r[0];
            let l = t.to;
            if (s) {
                const u = a.search(/\S/),
                    c = t.from + a.indexOf(s),
                    d = c + s.length;
                if (
                    ni(t.from, t.to, e.doc)
                        .filter((h) =>
                            h.mark.type.excluded.find((g) => g === n.type && g !== h.mark.type)
                        )
                        .filter((h) => h.to > c).length
                )
                    return null;
                d < t.to && o.delete(d, t.to),
                    c > t.from && o.delete(t.from + u, c),
                    (l = t.from + u + s.length),
                    o.addMark(t.from + u, l, n.type.create(i || {})),
                    o.removeStoredMark(n.type);
            }
        }
    });
}
class _t {
    constructor(e = {}) {
        (this.type = 'mark'),
            (this.name = 'mark'),
            (this.parent = null),
            (this.child = null),
            (this.config = { name: this.name, defaultOptions: {} }),
            (this.config = { ...this.config, ...e }),
            (this.name = this.config.name),
            e.defaultOptions &&
                console.warn(
                    `[tiptap warn]: BREAKING CHANGE: "defaultOptions" is deprecated. Please use "addOptions" instead. Found in extension: "${this.name}".`
                ),
            (this.options = this.config.defaultOptions),
            this.config.addOptions &&
                (this.options = W(V(this, 'addOptions', { name: this.name }))),
            (this.storage =
                W(V(this, 'addStorage', { name: this.name, options: this.options })) || {});
    }
    static create(e = {}) {
        return new _t(e);
    }
    configure(e = {}) {
        const t = this.extend();
        return (
            (t.options = nn(this.options, e)),
            (t.storage = W(V(t, 'addStorage', { name: t.name, options: t.options }))),
            t
        );
    }
    extend(e = {}) {
        const t = new _t(e);
        return (
            (t.parent = this),
            (this.child = t),
            (t.name = e.name ? e.name : t.parent.name),
            e.defaultOptions &&
                console.warn(
                    `[tiptap warn]: BREAKING CHANGE: "defaultOptions" is deprecated. Please use "addOptions" instead. Found in extension: "${t.name}".`
                ),
            (t.options = W(V(t, 'addOptions', { name: t.name }))),
            (t.storage = W(V(t, 'addStorage', { name: t.name, options: t.options }))),
            t
        );
    }
    static handleExit({ editor: e, mark: t }) {
        const { tr: r } = e.state,
            i = e.state.selection.$from;
        if (i.pos === i.end()) {
            const s = i.marks();
            if (!!!s.find((u) => u?.type.name === t.name)) return !1;
            const l = s.find((u) => u?.type.name === t.name);
            return l && r.removeStoredMark(l), r.insertText(' ', i.pos), e.view.dispatch(r), !0;
        }
        return !1;
    }
}
class Qt {
    constructor(e = {}) {
        (this.type = 'node'),
            (this.name = 'node'),
            (this.parent = null),
            (this.child = null),
            (this.config = { name: this.name, defaultOptions: {} }),
            (this.config = { ...this.config, ...e }),
            (this.name = this.config.name),
            e.defaultOptions &&
                console.warn(
                    `[tiptap warn]: BREAKING CHANGE: "defaultOptions" is deprecated. Please use "addOptions" instead. Found in extension: "${this.name}".`
                ),
            (this.options = this.config.defaultOptions),
            this.config.addOptions &&
                (this.options = W(V(this, 'addOptions', { name: this.name }))),
            (this.storage =
                W(V(this, 'addStorage', { name: this.name, options: this.options })) || {});
    }
    static create(e = {}) {
        return new Qt(e);
    }
    configure(e = {}) {
        const t = this.extend();
        return (
            (t.options = nn(this.options, e)),
            (t.storage = W(V(t, 'addStorage', { name: t.name, options: t.options }))),
            t
        );
    }
    extend(e = {}) {
        const t = new Qt(e);
        return (
            (t.parent = this),
            (this.child = t),
            (t.name = e.name ? e.name : t.parent.name),
            e.defaultOptions &&
                console.warn(
                    `[tiptap warn]: BREAKING CHANGE: "defaultOptions" is deprecated. Please use "addOptions" instead. Found in extension: "${t.name}".`
                ),
            (t.options = W(V(t, 'addOptions', { name: t.name }))),
            (t.storage = W(V(t, 'addStorage', { name: t.name, options: t.options }))),
            t
        );
    }
}
function ar(n) {
    return new Io({
        find: n.find,
        handler: ({ state: e, range: t, match: r }) => {
            const i = W(n.getAttributes, void 0, r);
            if (i === !1 || i === null) return null;
            const { tr: o } = e,
                s = r[r.length - 1],
                a = r[0];
            let l = t.to;
            if (s) {
                const u = a.search(/\S/),
                    c = t.from + a.indexOf(s),
                    d = c + s.length;
                if (
                    ni(t.from, t.to, e.doc)
                        .filter((h) =>
                            h.mark.type.excluded.find((g) => g === n.type && g !== h.mark.type)
                        )
                        .filter((h) => h.to > c).length
                )
                    return null;
                d < t.to && o.delete(d, t.to),
                    c > t.from && o.delete(t.from + u, c),
                    (l = t.from + u + s.length),
                    o.addMark(t.from + u, l, n.type.create(i || {})),
                    o.removeStoredMark(n.type);
            }
        }
    });
}
const Ks = Qt.create({
    name: 'helloWorld',
    addAttributes() {
        return {};
    },
    parseHTML() {
        return [{ tag: '' }];
    },
    addOptions() {
        return { inline: !1 };
    },
    inline() {
        return this.options.inline;
    },
    group() {
        return 'block';
    },
    addCommands() {
        return {
            addHelloWorld:
                () =>
                ({ commands: n }) =>
                    n.insertContent({ type: this.name })
        };
    },
    addNodeView() {
        return () => {
            const n = document.createElement('div');
            n.contentEditable = 'true';
            const e = document.createElement('label');
            return (
                (e.innerHTML = 'Hello World'),
                (e.contentEditable = 'true'),
                (e.style.fontWeight = 'bold'),
                (e.style.fontSize = '25px'),
                (e.style.paddingBottom = '10px'),
                (n.style.padding = '4px'),
                (n.style.background = '#f9dc5c'),
                (n.style.borderRadius = '5px'),
                (n.style.border = '2px solid #333'),
                (n.style.marginBottom = '10px'),
                n.append(e),
                { dom: n }
            );
        };
    }
});
var Y = 'top',
    se = 'bottom',
    ae = 'right',
    X = 'left',
    rn = 'auto',
    Et = [Y, se, ae, X],
    Qe = 'start',
    bt = 'end',
    Gs = 'clippingParents',
    ri = 'viewport',
    ut = 'popper',
    Ys = 'reference',
    lr = Et.reduce(function (n, e) {
        return n.concat([e + '-' + Qe, e + '-' + bt]);
    }, []),
    ii = [].concat(Et, [rn]).reduce(function (n, e) {
        return n.concat([e, e + '-' + Qe, e + '-' + bt]);
    }, []),
    Xs = 'beforeRead',
    _s = 'read',
    Qs = 'afterRead',
    Zs = 'beforeMain',
    ea = 'main',
    ta = 'afterMain',
    na = 'beforeWrite',
    ra = 'write',
    ia = 'afterWrite',
    Sn = [Xs, _s, Qs, Zs, ea, ta, na, ra, ia];
function ye(n) {
    return n ? (n.nodeName || '').toLowerCase() : null;
}
function le(n) {
    if (n == null) return window;
    if (n.toString() !== '[object Window]') {
        var e = n.ownerDocument;
        return (e && e.defaultView) || window;
    }
    return n;
}
function je(n) {
    var e = le(n).Element;
    return n instanceof e || n instanceof Element;
}
function te(n) {
    var e = le(n).HTMLElement;
    return n instanceof e || n instanceof HTMLElement;
}
function Bn(n) {
    if (typeof ShadowRoot > 'u') return !1;
    var e = le(n).ShadowRoot;
    return n instanceof e || n instanceof ShadowRoot;
}
function oa(n) {
    var e = n.state;
    Object.keys(e.elements).forEach(function (t) {
        var r = e.styles[t] || {},
            i = e.attributes[t] || {},
            o = e.elements[t];
        !te(o) ||
            !ye(o) ||
            (Object.assign(o.style, r),
            Object.keys(i).forEach(function (s) {
                var a = i[s];
                a === !1 ? o.removeAttribute(s) : o.setAttribute(s, a === !0 ? '' : a);
            }));
    });
}
function sa(n) {
    var e = n.state,
        t = {
            popper: { position: e.options.strategy, left: '0', top: '0', margin: '0' },
            arrow: { position: 'absolute' },
            reference: {}
        };
    return (
        Object.assign(e.elements.popper.style, t.popper),
        (e.styles = t),
        e.elements.arrow && Object.assign(e.elements.arrow.style, t.arrow),
        function () {
            Object.keys(e.elements).forEach(function (r) {
                var i = e.elements[r],
                    o = e.attributes[r] || {},
                    s = Object.keys(e.styles.hasOwnProperty(r) ? e.styles[r] : t[r]),
                    a = s.reduce(function (l, u) {
                        return (l[u] = ''), l;
                    }, {});
                !te(i) ||
                    !ye(i) ||
                    (Object.assign(i.style, a),
                    Object.keys(o).forEach(function (l) {
                        i.removeAttribute(l);
                    }));
            });
        }
    );
}
const oi = {
    name: 'applyStyles',
    enabled: !0,
    phase: 'write',
    fn: oa,
    effect: sa,
    requires: ['computeStyles']
};
function ue(n) {
    return n.split('-')[0];
}
var $e = Math.max,
    Zt = Math.min,
    Ze = Math.round;
function En() {
    var n = navigator.userAgentData;
    return n != null && n.brands
        ? n.brands
              .map(function (e) {
                  return e.brand + '/' + e.version;
              })
              .join(' ')
        : navigator.userAgent;
}
function si() {
    return !/^((?!chrome|android).)*safari/i.test(En());
}
function et(n, e, t) {
    e === void 0 && (e = !1), t === void 0 && (t = !1);
    var r = n.getBoundingClientRect(),
        i = 1,
        o = 1;
    e &&
        te(n) &&
        ((i = (n.offsetWidth > 0 && Ze(r.width) / n.offsetWidth) || 1),
        (o = (n.offsetHeight > 0 && Ze(r.height) / n.offsetHeight) || 1));
    var s = je(n) ? le(n) : window,
        a = s.visualViewport,
        l = !si() && t,
        u = (r.left + (l && a ? a.offsetLeft : 0)) / i,
        c = (r.top + (l && a ? a.offsetTop : 0)) / o,
        d = r.width / i,
        p = r.height / o;
    return { width: d, height: p, top: c, right: u + d, bottom: c + p, left: u, x: u, y: c };
}
function zn(n) {
    var e = et(n),
        t = n.offsetWidth,
        r = n.offsetHeight;
    return (
        Math.abs(e.width - t) <= 1 && (t = e.width),
        Math.abs(e.height - r) <= 1 && (r = e.height),
        { x: n.offsetLeft, y: n.offsetTop, width: t, height: r }
    );
}
function ai(n, e) {
    var t = e.getRootNode && e.getRootNode();
    if (n.contains(e)) return !0;
    if (t && Bn(t)) {
        var r = e;
        do {
            if (r && n.isSameNode(r)) return !0;
            r = r.parentNode || r.host;
        } while (r);
    }
    return !1;
}
function fe(n) {
    return le(n).getComputedStyle(n);
}
function aa(n) {
    return ['table', 'td', 'th'].indexOf(ye(n)) >= 0;
}
function Ce(n) {
    return ((je(n) ? n.ownerDocument : n.document) || window.document).documentElement;
}
function on(n) {
    return ye(n) === 'html'
        ? n
        : n.assignedSlot || n.parentNode || (Bn(n) ? n.host : null) || Ce(n);
}
function cr(n) {
    return !te(n) || fe(n).position === 'fixed' ? null : n.offsetParent;
}
function la(n) {
    var e = /firefox/i.test(En()),
        t = /Trident/i.test(En());
    if (t && te(n)) {
        var r = fe(n);
        if (r.position === 'fixed') return null;
    }
    var i = on(n);
    for (Bn(i) && (i = i.host); te(i) && ['html', 'body'].indexOf(ye(i)) < 0; ) {
        var o = fe(i);
        if (
            o.transform !== 'none' ||
            o.perspective !== 'none' ||
            o.contain === 'paint' ||
            ['transform', 'perspective'].indexOf(o.willChange) !== -1 ||
            (e && o.willChange === 'filter') ||
            (e && o.filter && o.filter !== 'none')
        )
            return i;
        i = i.parentNode;
    }
    return null;
}
function Ot(n) {
    for (var e = le(n), t = cr(n); t && aa(t) && fe(t).position === 'static'; ) t = cr(t);
    return t && (ye(t) === 'html' || (ye(t) === 'body' && fe(t).position === 'static'))
        ? e
        : t || la(n) || e;
}
function Fn(n) {
    return ['top', 'bottom'].indexOf(n) >= 0 ? 'x' : 'y';
}
function mt(n, e, t) {
    return $e(n, Zt(e, t));
}
function ca(n, e, t) {
    var r = mt(n, e, t);
    return r > t ? t : r;
}
function li() {
    return { top: 0, right: 0, bottom: 0, left: 0 };
}
function ci(n) {
    return Object.assign({}, li(), n);
}
function ui(n, e) {
    return e.reduce(function (t, r) {
        return (t[r] = n), t;
    }, {});
}
var ua = function (e, t) {
    return (
        (e =
            typeof e == 'function' ? e(Object.assign({}, t.rects, { placement: t.placement })) : e),
        ci(typeof e != 'number' ? e : ui(e, Et))
    );
};
function fa(n) {
    var e,
        t = n.state,
        r = n.name,
        i = n.options,
        o = t.elements.arrow,
        s = t.modifiersData.popperOffsets,
        a = ue(t.placement),
        l = Fn(a),
        u = [X, ae].indexOf(a) >= 0,
        c = u ? 'height' : 'width';
    if (!(!o || !s)) {
        var d = ua(i.padding, t),
            p = zn(o),
            h = l === 'y' ? Y : X,
            v = l === 'y' ? se : ae,
            g = t.rects.reference[c] + t.rects.reference[l] - s[l] - t.rects.popper[c],
            w = s[l] - t.rects.reference[l],
            x = Ot(o),
            A = x ? (l === 'y' ? x.clientHeight || 0 : x.clientWidth || 0) : 0,
            N = g / 2 - w / 2,
            f = d[h],
            O = A - p[c] - d[v],
            y = A / 2 - p[c] / 2 + N,
            M = mt(f, y, O),
            B = l;
        t.modifiersData[r] = ((e = {}), (e[B] = M), (e.centerOffset = M - y), e);
    }
}
function da(n) {
    var e = n.state,
        t = n.options,
        r = t.element,
        i = r === void 0 ? '[data-popper-arrow]' : r;
    if (i != null && !(typeof i == 'string' && ((i = e.elements.popper.querySelector(i)), !i))) {
        if (
            (process.env.NODE_ENV !== 'production' &&
                (te(i) ||
                    console.error(
                        [
                            'Popper: "arrow" element must be an HTMLElement (not an SVGElement).',
                            'To use an SVG arrow, wrap it in an HTMLElement that will be used as',
                            'the arrow.'
                        ].join(' ')
                    )),
            !ai(e.elements.popper, i))
        ) {
            process.env.NODE_ENV !== 'production' &&
                console.error(
                    [
                        'Popper: "arrow" modifier\'s `element` must be a child of the popper',
                        'element.'
                    ].join(' ')
                );
            return;
        }
        e.elements.arrow = i;
    }
}
const pa = {
    name: 'arrow',
    enabled: !0,
    phase: 'main',
    fn: fa,
    effect: da,
    requires: ['popperOffsets'],
    requiresIfExists: ['preventOverflow']
};
function tt(n) {
    return n.split('-')[1];
}
var ha = { top: 'auto', right: 'auto', bottom: 'auto', left: 'auto' };
function ma(n) {
    var e = n.x,
        t = n.y,
        r = window,
        i = r.devicePixelRatio || 1;
    return { x: Ze(e * i) / i || 0, y: Ze(t * i) / i || 0 };
}
function ur(n) {
    var e,
        t = n.popper,
        r = n.popperRect,
        i = n.placement,
        o = n.variation,
        s = n.offsets,
        a = n.position,
        l = n.gpuAcceleration,
        u = n.adaptive,
        c = n.roundOffsets,
        d = n.isFixed,
        p = s.x,
        h = p === void 0 ? 0 : p,
        v = s.y,
        g = v === void 0 ? 0 : v,
        w = typeof c == 'function' ? c({ x: h, y: g }) : { x: h, y: g };
    (h = w.x), (g = w.y);
    var x = s.hasOwnProperty('x'),
        A = s.hasOwnProperty('y'),
        N = X,
        f = Y,
        O = window;
    if (u) {
        var y = Ot(t),
            M = 'clientHeight',
            B = 'clientWidth';
        if (
            (y === le(t) &&
                ((y = Ce(t)),
                fe(y).position !== 'static' &&
                    a === 'absolute' &&
                    ((M = 'scrollHeight'), (B = 'scrollWidth'))),
            (y = y),
            i === Y || ((i === X || i === ae) && o === bt))
        ) {
            f = se;
            var L = d && y === O && O.visualViewport ? O.visualViewport.height : y[M];
            (g -= L - r.height), (g *= l ? 1 : -1);
        }
        if (i === X || ((i === Y || i === se) && o === bt)) {
            N = ae;
            var z = d && y === O && O.visualViewport ? O.visualViewport.width : y[B];
            (h -= z - r.width), (h *= l ? 1 : -1);
        }
    }
    var C = Object.assign({ position: a }, u && ha),
        D = c === !0 ? ma({ x: h, y: g }) : { x: h, y: g };
    if (((h = D.x), (g = D.y), l)) {
        var F;
        return Object.assign(
            {},
            C,
            ((F = {}),
            (F[f] = A ? '0' : ''),
            (F[N] = x ? '0' : ''),
            (F.transform =
                (O.devicePixelRatio || 1) <= 1
                    ? 'translate(' + h + 'px, ' + g + 'px)'
                    : 'translate3d(' + h + 'px, ' + g + 'px, 0)'),
            F)
        );
    }
    return Object.assign(
        {},
        C,
        ((e = {}), (e[f] = A ? g + 'px' : ''), (e[N] = x ? h + 'px' : ''), (e.transform = ''), e)
    );
}
function ga(n) {
    var e = n.state,
        t = n.options,
        r = t.gpuAcceleration,
        i = r === void 0 ? !0 : r,
        o = t.adaptive,
        s = o === void 0 ? !0 : o,
        a = t.roundOffsets,
        l = a === void 0 ? !0 : a;
    if (process.env.NODE_ENV !== 'production') {
        var u = fe(e.elements.popper).transitionProperty || '';
        s &&
            ['transform', 'top', 'right', 'bottom', 'left'].some(function (d) {
                return u.indexOf(d) >= 0;
            }) &&
            console.warn(
                [
                    'Popper: Detected CSS transitions on at least one of the following',
                    'CSS properties: "transform", "top", "right", "bottom", "left".',
                    `

`,
                    'Disable the "computeStyles" modifier\'s `adaptive` option to allow',
                    'for smooth transitions, or remove these properties from the CSS',
                    'transition declaration on the popper element if only transitioning',
                    'opacity or background-color for example.',
                    `

`,
                    'We recommend using the popper element as a wrapper around an inner',
                    'element that can have any CSS property transitioned for animations.'
                ].join(' ')
            );
    }
    var c = {
        placement: ue(e.placement),
        variation: tt(e.placement),
        popper: e.elements.popper,
        popperRect: e.rects.popper,
        gpuAcceleration: i,
        isFixed: e.options.strategy === 'fixed'
    };
    e.modifiersData.popperOffsets != null &&
        (e.styles.popper = Object.assign(
            {},
            e.styles.popper,
            ur(
                Object.assign({}, c, {
                    offsets: e.modifiersData.popperOffsets,
                    position: e.options.strategy,
                    adaptive: s,
                    roundOffsets: l
                })
            )
        )),
        e.modifiersData.arrow != null &&
            (e.styles.arrow = Object.assign(
                {},
                e.styles.arrow,
                ur(
                    Object.assign({}, c, {
                        offsets: e.modifiersData.arrow,
                        position: 'absolute',
                        adaptive: !1,
                        roundOffsets: l
                    })
                )
            )),
        (e.attributes.popper = Object.assign({}, e.attributes.popper, {
            'data-popper-placement': e.placement
        }));
}
const va = { name: 'computeStyles', enabled: !0, phase: 'beforeWrite', fn: ga, data: {} };
var Vt = { passive: !0 };
function ya(n) {
    var e = n.state,
        t = n.instance,
        r = n.options,
        i = r.scroll,
        o = i === void 0 ? !0 : i,
        s = r.resize,
        a = s === void 0 ? !0 : s,
        l = le(e.elements.popper),
        u = [].concat(e.scrollParents.reference, e.scrollParents.popper);
    return (
        o &&
            u.forEach(function (c) {
                c.addEventListener('scroll', t.update, Vt);
            }),
        a && l.addEventListener('resize', t.update, Vt),
        function () {
            o &&
                u.forEach(function (c) {
                    c.removeEventListener('scroll', t.update, Vt);
                }),
                a && l.removeEventListener('resize', t.update, Vt);
        }
    );
}
const wa = {
    name: 'eventListeners',
    enabled: !0,
    phase: 'write',
    fn: function () {},
    effect: ya,
    data: {}
};
var ba = { left: 'right', right: 'left', bottom: 'top', top: 'bottom' };
function qt(n) {
    return n.replace(/left|right|bottom|top/g, function (e) {
        return ba[e];
    });
}
var xa = { start: 'end', end: 'start' };
function fr(n) {
    return n.replace(/start|end/g, function (e) {
        return xa[e];
    });
}
function Ln(n) {
    var e = le(n),
        t = e.pageXOffset,
        r = e.pageYOffset;
    return { scrollLeft: t, scrollTop: r };
}
function $n(n) {
    return et(Ce(n)).left + Ln(n).scrollLeft;
}
function ka(n, e) {
    var t = le(n),
        r = Ce(n),
        i = t.visualViewport,
        o = r.clientWidth,
        s = r.clientHeight,
        a = 0,
        l = 0;
    if (i) {
        (o = i.width), (s = i.height);
        var u = si();
        (u || (!u && e === 'fixed')) && ((a = i.offsetLeft), (l = i.offsetTop));
    }
    return { width: o, height: s, x: a + $n(n), y: l };
}
function Sa(n) {
    var e,
        t = Ce(n),
        r = Ln(n),
        i = (e = n.ownerDocument) == null ? void 0 : e.body,
        o = $e(t.scrollWidth, t.clientWidth, i ? i.scrollWidth : 0, i ? i.clientWidth : 0),
        s = $e(t.scrollHeight, t.clientHeight, i ? i.scrollHeight : 0, i ? i.clientHeight : 0),
        a = -r.scrollLeft + $n(n),
        l = -r.scrollTop;
    return (
        fe(i || t).direction === 'rtl' && (a += $e(t.clientWidth, i ? i.clientWidth : 0) - o),
        { width: o, height: s, x: a, y: l }
    );
}
function jn(n) {
    var e = fe(n),
        t = e.overflow,
        r = e.overflowX,
        i = e.overflowY;
    return /auto|scroll|overlay|hidden/.test(t + i + r);
}
function fi(n) {
    return ['html', 'body', '#document'].indexOf(ye(n)) >= 0
        ? n.ownerDocument.body
        : te(n) && jn(n)
        ? n
        : fi(on(n));
}
function gt(n, e) {
    var t;
    e === void 0 && (e = []);
    var r = fi(n),
        i = r === ((t = n.ownerDocument) == null ? void 0 : t.body),
        o = le(r),
        s = i ? [o].concat(o.visualViewport || [], jn(r) ? r : []) : r,
        a = e.concat(s);
    return i ? a : a.concat(gt(on(s)));
}
function On(n) {
    return Object.assign({}, n, {
        left: n.x,
        top: n.y,
        right: n.x + n.width,
        bottom: n.y + n.height
    });
}
function Ea(n, e) {
    var t = et(n, !1, e === 'fixed');
    return (
        (t.top = t.top + n.clientTop),
        (t.left = t.left + n.clientLeft),
        (t.bottom = t.top + n.clientHeight),
        (t.right = t.left + n.clientWidth),
        (t.width = n.clientWidth),
        (t.height = n.clientHeight),
        (t.x = t.left),
        (t.y = t.top),
        t
    );
}
function dr(n, e, t) {
    return e === ri ? On(ka(n, t)) : je(e) ? Ea(e, t) : On(Sa(Ce(n)));
}
function Oa(n) {
    var e = gt(on(n)),
        t = ['absolute', 'fixed'].indexOf(fe(n).position) >= 0,
        r = t && te(n) ? Ot(n) : n;
    return je(r)
        ? e.filter(function (i) {
              return je(i) && ai(i, r) && ye(i) !== 'body';
          })
        : [];
}
function Ma(n, e, t, r) {
    var i = e === 'clippingParents' ? Oa(n) : [].concat(e),
        o = [].concat(i, [t]),
        s = o[0],
        a = o.reduce(function (l, u) {
            var c = dr(n, u, r);
            return (
                (l.top = $e(c.top, l.top)),
                (l.right = Zt(c.right, l.right)),
                (l.bottom = Zt(c.bottom, l.bottom)),
                (l.left = $e(c.left, l.left)),
                l
            );
        }, dr(n, s, r));
    return (
        (a.width = a.right - a.left),
        (a.height = a.bottom - a.top),
        (a.x = a.left),
        (a.y = a.top),
        a
    );
}
function di(n) {
    var e = n.reference,
        t = n.element,
        r = n.placement,
        i = r ? ue(r) : null,
        o = r ? tt(r) : null,
        s = e.x + e.width / 2 - t.width / 2,
        a = e.y + e.height / 2 - t.height / 2,
        l;
    switch (i) {
        case Y:
            l = { x: s, y: e.y - t.height };
            break;
        case se:
            l = { x: s, y: e.y + e.height };
            break;
        case ae:
            l = { x: e.x + e.width, y: a };
            break;
        case X:
            l = { x: e.x - t.width, y: a };
            break;
        default:
            l = { x: e.x, y: e.y };
    }
    var u = i ? Fn(i) : null;
    if (u != null) {
        var c = u === 'y' ? 'height' : 'width';
        switch (o) {
            case Qe:
                l[u] = l[u] - (e[c] / 2 - t[c] / 2);
                break;
            case bt:
                l[u] = l[u] + (e[c] / 2 - t[c] / 2);
                break;
        }
    }
    return l;
}
function xt(n, e) {
    e === void 0 && (e = {});
    var t = e,
        r = t.placement,
        i = r === void 0 ? n.placement : r,
        o = t.strategy,
        s = o === void 0 ? n.strategy : o,
        a = t.boundary,
        l = a === void 0 ? Gs : a,
        u = t.rootBoundary,
        c = u === void 0 ? ri : u,
        d = t.elementContext,
        p = d === void 0 ? ut : d,
        h = t.altBoundary,
        v = h === void 0 ? !1 : h,
        g = t.padding,
        w = g === void 0 ? 0 : g,
        x = ci(typeof w != 'number' ? w : ui(w, Et)),
        A = p === ut ? Ys : ut,
        N = n.rects.popper,
        f = n.elements[v ? A : p],
        O = Ma(je(f) ? f : f.contextElement || Ce(n.elements.popper), l, c, s),
        y = et(n.elements.reference),
        M = di({ reference: y, element: N, strategy: 'absolute', placement: i }),
        B = On(Object.assign({}, N, M)),
        L = p === ut ? B : y,
        z = {
            top: O.top - L.top + x.top,
            bottom: L.bottom - O.bottom + x.bottom,
            left: O.left - L.left + x.left,
            right: L.right - O.right + x.right
        },
        C = n.modifiersData.offset;
    if (p === ut && C) {
        var D = C[i];
        Object.keys(z).forEach(function (F) {
            var Q = [ae, se].indexOf(F) >= 0 ? 1 : -1,
                Z = [Y, se].indexOf(F) >= 0 ? 'y' : 'x';
            z[F] += D[Z] * Q;
        });
    }
    return z;
}
function Ta(n, e) {
    e === void 0 && (e = {});
    var t = e,
        r = t.placement,
        i = t.boundary,
        o = t.rootBoundary,
        s = t.padding,
        a = t.flipVariations,
        l = t.allowedAutoPlacements,
        u = l === void 0 ? ii : l,
        c = tt(r),
        d = c
            ? a
                ? lr
                : lr.filter(function (v) {
                      return tt(v) === c;
                  })
            : Et,
        p = d.filter(function (v) {
            return u.indexOf(v) >= 0;
        });
    p.length === 0 &&
        ((p = d),
        process.env.NODE_ENV !== 'production' &&
            console.error(
                [
                    'Popper: The `allowedAutoPlacements` option did not allow any',
                    'placements. Ensure the `placement` option matches the variation',
                    'of the allowed placements.',
                    'For example, "auto" cannot be used to allow "bottom-start".',
                    'Use "auto-start" instead.'
                ].join(' ')
            ));
    var h = p.reduce(function (v, g) {
        return (v[g] = xt(n, { placement: g, boundary: i, rootBoundary: o, padding: s })[ue(g)]), v;
    }, {});
    return Object.keys(h).sort(function (v, g) {
        return h[v] - h[g];
    });
}
function Aa(n) {
    if (ue(n) === rn) return [];
    var e = qt(n);
    return [fr(n), e, fr(e)];
}
function Ca(n) {
    var e = n.state,
        t = n.options,
        r = n.name;
    if (!e.modifiersData[r]._skip) {
        for (
            var i = t.mainAxis,
                o = i === void 0 ? !0 : i,
                s = t.altAxis,
                a = s === void 0 ? !0 : s,
                l = t.fallbackPlacements,
                u = t.padding,
                c = t.boundary,
                d = t.rootBoundary,
                p = t.altBoundary,
                h = t.flipVariations,
                v = h === void 0 ? !0 : h,
                g = t.allowedAutoPlacements,
                w = e.options.placement,
                x = ue(w),
                A = x === w,
                N = l || (A || !v ? [qt(w)] : Aa(w)),
                f = [w].concat(N).reduce(function (we, ce) {
                    return we.concat(
                        ue(ce) === rn
                            ? Ta(e, {
                                  placement: ce,
                                  boundary: c,
                                  rootBoundary: d,
                                  padding: u,
                                  flipVariations: v,
                                  allowedAutoPlacements: g
                              })
                            : ce
                    );
                }, []),
                O = e.rects.reference,
                y = e.rects.popper,
                M = new Map(),
                B = !0,
                L = f[0],
                z = 0;
            z < f.length;
            z++
        ) {
            var C = f[z],
                D = ue(C),
                F = tt(C) === Qe,
                Q = [Y, se].indexOf(D) >= 0,
                Z = Q ? 'width' : 'height',
                H = xt(e, {
                    placement: C,
                    boundary: c,
                    rootBoundary: d,
                    altBoundary: p,
                    padding: u
                }),
                q = Q ? (F ? ae : X) : F ? se : Y;
            O[Z] > y[Z] && (q = qt(q));
            var J = qt(q),
                de = [];
            if (
                (o && de.push(H[D] <= 0),
                a && de.push(H[q] <= 0, H[J] <= 0),
                de.every(function (we) {
                    return we;
                }))
            ) {
                (L = C), (B = !1);
                break;
            }
            M.set(C, de);
        }
        if (B)
            for (
                var pe = v ? 3 : 1,
                    Ie = function (ce) {
                        var be = f.find(function (We) {
                            var xe = M.get(We);
                            if (xe)
                                return xe.slice(0, ce).every(function (Je) {
                                    return Je;
                                });
                        });
                        if (be) return (L = be), 'break';
                    },
                    he = pe;
                he > 0;
                he--
            ) {
                var Ne = Ie(he);
                if (Ne === 'break') break;
            }
        e.placement !== L && ((e.modifiersData[r]._skip = !0), (e.placement = L), (e.reset = !0));
    }
}
const Ia = {
    name: 'flip',
    enabled: !0,
    phase: 'main',
    fn: Ca,
    requiresIfExists: ['offset'],
    data: { _skip: !1 }
};
function pr(n, e, t) {
    return (
        t === void 0 && (t = { x: 0, y: 0 }),
        {
            top: n.top - e.height - t.y,
            right: n.right - e.width + t.x,
            bottom: n.bottom - e.height + t.y,
            left: n.left - e.width - t.x
        }
    );
}
function hr(n) {
    return [Y, ae, se, X].some(function (e) {
        return n[e] >= 0;
    });
}
function Na(n) {
    var e = n.state,
        t = n.name,
        r = e.rects.reference,
        i = e.rects.popper,
        o = e.modifiersData.preventOverflow,
        s = xt(e, { elementContext: 'reference' }),
        a = xt(e, { altBoundary: !0 }),
        l = pr(s, r),
        u = pr(a, i, o),
        c = hr(l),
        d = hr(u);
    (e.modifiersData[t] = {
        referenceClippingOffsets: l,
        popperEscapeOffsets: u,
        isReferenceHidden: c,
        hasPopperEscaped: d
    }),
        (e.attributes.popper = Object.assign({}, e.attributes.popper, {
            'data-popper-reference-hidden': c,
            'data-popper-escaped': d
        }));
}
const Ra = {
    name: 'hide',
    enabled: !0,
    phase: 'main',
    requiresIfExists: ['preventOverflow'],
    fn: Na
};
function Da(n, e, t) {
    var r = ue(n),
        i = [X, Y].indexOf(r) >= 0 ? -1 : 1,
        o = typeof t == 'function' ? t(Object.assign({}, e, { placement: n })) : t,
        s = o[0],
        a = o[1];
    return (
        (s = s || 0), (a = (a || 0) * i), [X, ae].indexOf(r) >= 0 ? { x: a, y: s } : { x: s, y: a }
    );
}
function Pa(n) {
    var e = n.state,
        t = n.options,
        r = n.name,
        i = t.offset,
        o = i === void 0 ? [0, 0] : i,
        s = ii.reduce(function (c, d) {
            return (c[d] = Da(d, e.rects, o)), c;
        }, {}),
        a = s[e.placement],
        l = a.x,
        u = a.y;
    e.modifiersData.popperOffsets != null &&
        ((e.modifiersData.popperOffsets.x += l), (e.modifiersData.popperOffsets.y += u)),
        (e.modifiersData[r] = s);
}
const Ba = { name: 'offset', enabled: !0, phase: 'main', requires: ['popperOffsets'], fn: Pa };
function za(n) {
    var e = n.state,
        t = n.name;
    e.modifiersData[t] = di({
        reference: e.rects.reference,
        element: e.rects.popper,
        strategy: 'absolute',
        placement: e.placement
    });
}
const Fa = { name: 'popperOffsets', enabled: !0, phase: 'read', fn: za, data: {} };
function La(n) {
    return n === 'x' ? 'y' : 'x';
}
function $a(n) {
    var e = n.state,
        t = n.options,
        r = n.name,
        i = t.mainAxis,
        o = i === void 0 ? !0 : i,
        s = t.altAxis,
        a = s === void 0 ? !1 : s,
        l = t.boundary,
        u = t.rootBoundary,
        c = t.altBoundary,
        d = t.padding,
        p = t.tether,
        h = p === void 0 ? !0 : p,
        v = t.tetherOffset,
        g = v === void 0 ? 0 : v,
        w = xt(e, { boundary: l, rootBoundary: u, padding: d, altBoundary: c }),
        x = ue(e.placement),
        A = tt(e.placement),
        N = !A,
        f = Fn(x),
        O = La(f),
        y = e.modifiersData.popperOffsets,
        M = e.rects.reference,
        B = e.rects.popper,
        L = typeof g == 'function' ? g(Object.assign({}, e.rects, { placement: e.placement })) : g,
        z =
            typeof L == 'number'
                ? { mainAxis: L, altAxis: L }
                : Object.assign({ mainAxis: 0, altAxis: 0 }, L),
        C = e.modifiersData.offset ? e.modifiersData.offset[e.placement] : null,
        D = { x: 0, y: 0 };
    if (y) {
        if (o) {
            var F,
                Q = f === 'y' ? Y : X,
                Z = f === 'y' ? se : ae,
                H = f === 'y' ? 'height' : 'width',
                q = y[f],
                J = q + w[Q],
                de = q - w[Z],
                pe = h ? -B[H] / 2 : 0,
                Ie = A === Qe ? M[H] : B[H],
                he = A === Qe ? -B[H] : -M[H],
                Ne = e.elements.arrow,
                we = h && Ne ? zn(Ne) : { width: 0, height: 0 },
                ce = e.modifiersData['arrow#persistent']
                    ? e.modifiersData['arrow#persistent'].padding
                    : li(),
                be = ce[Q],
                We = ce[Z],
                xe = mt(0, M[H], we[H]),
                Je = N ? M[H] / 2 - pe - xe - be - z.mainAxis : Ie - xe - be - z.mainAxis,
                Oe = N ? -M[H] / 2 + pe + xe + We + z.mainAxis : he + xe + We + z.mainAxis,
                He = e.elements.arrow && Ot(e.elements.arrow),
                Tt = He ? (f === 'y' ? He.clientTop || 0 : He.clientLeft || 0) : 0,
                ot = (F = C?.[f]) != null ? F : 0,
                At = q + Je - ot - Tt,
                Ct = q + Oe - ot,
                st = mt(h ? Zt(J, At) : J, q, h ? $e(de, Ct) : de);
            (y[f] = st), (D[f] = st - q);
        }
        if (a) {
            var at,
                It = f === 'x' ? Y : X,
                Nt = f === 'x' ? se : ae,
                ke = y[O],
                Me = O === 'y' ? 'height' : 'width',
                lt = ke + w[It],
                Re = ke - w[Nt],
                ct = [Y, X].indexOf(x) !== -1,
                Rt = (at = C?.[O]) != null ? at : 0,
                Dt = ct ? lt : ke - M[Me] - B[Me] - Rt + z.altAxis,
                Pt = ct ? ke + M[Me] + B[Me] - Rt - z.altAxis : Re,
                Bt = h && ct ? ca(Dt, ke, Pt) : mt(h ? Dt : lt, ke, h ? Pt : Re);
            (y[O] = Bt), (D[O] = Bt - ke);
        }
        e.modifiersData[r] = D;
    }
}
const ja = {
    name: 'preventOverflow',
    enabled: !0,
    phase: 'main',
    fn: $a,
    requiresIfExists: ['offset']
};
function Va(n) {
    return { scrollLeft: n.scrollLeft, scrollTop: n.scrollTop };
}
function Wa(n) {
    return n === le(n) || !te(n) ? Ln(n) : Va(n);
}
function Ja(n) {
    var e = n.getBoundingClientRect(),
        t = Ze(e.width) / n.offsetWidth || 1,
        r = Ze(e.height) / n.offsetHeight || 1;
    return t !== 1 || r !== 1;
}
function Ha(n, e, t) {
    t === void 0 && (t = !1);
    var r = te(e),
        i = te(e) && Ja(e),
        o = Ce(e),
        s = et(n, i, t),
        a = { scrollLeft: 0, scrollTop: 0 },
        l = { x: 0, y: 0 };
    return (
        (r || (!r && !t)) &&
            ((ye(e) !== 'body' || jn(o)) && (a = Wa(e)),
            te(e)
                ? ((l = et(e, !0)), (l.x += e.clientLeft), (l.y += e.clientTop))
                : o && (l.x = $n(o))),
        {
            x: s.left + a.scrollLeft - l.x,
            y: s.top + a.scrollTop - l.y,
            width: s.width,
            height: s.height
        }
    );
}
function qa(n) {
    var e = new Map(),
        t = new Set(),
        r = [];
    n.forEach(function (o) {
        e.set(o.name, o);
    });
    function i(o) {
        t.add(o.name);
        var s = [].concat(o.requires || [], o.requiresIfExists || []);
        s.forEach(function (a) {
            if (!t.has(a)) {
                var l = e.get(a);
                l && i(l);
            }
        }),
            r.push(o);
    }
    return (
        n.forEach(function (o) {
            t.has(o.name) || i(o);
        }),
        r
    );
}
function Ua(n) {
    var e = qa(n);
    return Sn.reduce(function (t, r) {
        return t.concat(
            e.filter(function (i) {
                return i.phase === r;
            })
        );
    }, []);
}
function Ka(n) {
    var e;
    return function () {
        return (
            e ||
                (e = new Promise(function (t) {
                    Promise.resolve().then(function () {
                        (e = void 0), t(n());
                    });
                })),
            e
        );
    };
}
function Te(n) {
    for (var e = arguments.length, t = new Array(e > 1 ? e - 1 : 0), r = 1; r < e; r++)
        t[r - 1] = arguments[r];
    return [].concat(t).reduce(function (i, o) {
        return i.replace(/%s/, o);
    }, n);
}
var Pe = 'Popper: modifier "%s" provided an invalid %s property, expected %s but got %s',
    Ga = 'Popper: modifier "%s" requires "%s", but "%s" modifier is not available',
    mr = ['name', 'enabled', 'phase', 'fn', 'effect', 'requires', 'options'];
function Ya(n) {
    n.forEach(function (e) {
        []
            .concat(Object.keys(e), mr)
            .filter(function (t, r, i) {
                return i.indexOf(t) === r;
            })
            .forEach(function (t) {
                switch (t) {
                    case 'name':
                        typeof e.name != 'string' &&
                            console.error(
                                Te(
                                    Pe,
                                    String(e.name),
                                    '"name"',
                                    '"string"',
                                    '"' + String(e.name) + '"'
                                )
                            );
                        break;
                    case 'enabled':
                        typeof e.enabled != 'boolean' &&
                            console.error(
                                Te(
                                    Pe,
                                    e.name,
                                    '"enabled"',
                                    '"boolean"',
                                    '"' + String(e.enabled) + '"'
                                )
                            );
                        break;
                    case 'phase':
                        Sn.indexOf(e.phase) < 0 &&
                            console.error(
                                Te(
                                    Pe,
                                    e.name,
                                    '"phase"',
                                    'either ' + Sn.join(', '),
                                    '"' + String(e.phase) + '"'
                                )
                            );
                        break;
                    case 'fn':
                        typeof e.fn != 'function' &&
                            console.error(
                                Te(Pe, e.name, '"fn"', '"function"', '"' + String(e.fn) + '"')
                            );
                        break;
                    case 'effect':
                        e.effect != null &&
                            typeof e.effect != 'function' &&
                            console.error(
                                Te(Pe, e.name, '"effect"', '"function"', '"' + String(e.fn) + '"')
                            );
                        break;
                    case 'requires':
                        e.requires != null &&
                            !Array.isArray(e.requires) &&
                            console.error(
                                Te(
                                    Pe,
                                    e.name,
                                    '"requires"',
                                    '"array"',
                                    '"' + String(e.requires) + '"'
                                )
                            );
                        break;
                    case 'requiresIfExists':
                        Array.isArray(e.requiresIfExists) ||
                            console.error(
                                Te(
                                    Pe,
                                    e.name,
                                    '"requiresIfExists"',
                                    '"array"',
                                    '"' + String(e.requiresIfExists) + '"'
                                )
                            );
                        break;
                    case 'options':
                    case 'data':
                        break;
                    default:
                        console.error(
                            'PopperJS: an invalid property has been provided to the "' +
                                e.name +
                                '" modifier, valid properties are ' +
                                mr
                                    .map(function (r) {
                                        return '"' + r + '"';
                                    })
                                    .join(', ') +
                                '; but "' +
                                t +
                                '" was provided.'
                        );
                }
                e.requires &&
                    e.requires.forEach(function (r) {
                        n.find(function (i) {
                            return i.name === r;
                        }) == null && console.error(Te(Ga, String(e.name), r, r));
                    });
            });
    });
}
function Xa(n, e) {
    var t = new Set();
    return n.filter(function (r) {
        var i = e(r);
        if (!t.has(i)) return t.add(i), !0;
    });
}
function _a(n) {
    var e = n.reduce(function (t, r) {
        var i = t[r.name];
        return (
            (t[r.name] = i
                ? Object.assign({}, i, r, {
                      options: Object.assign({}, i.options, r.options),
                      data: Object.assign({}, i.data, r.data)
                  })
                : r),
            t
        );
    }, {});
    return Object.keys(e).map(function (t) {
        return e[t];
    });
}
var gr =
        'Popper: Invalid reference or popper argument provided. They must be either a DOM element or virtual element.',
    Qa =
        'Popper: An infinite loop in the modifiers cycle has been detected! The cycle has been interrupted to prevent a browser crash.',
    vr = { placement: 'bottom', modifiers: [], strategy: 'absolute' };
function yr() {
    for (var n = arguments.length, e = new Array(n), t = 0; t < n; t++) e[t] = arguments[t];
    return !e.some(function (r) {
        return !(r && typeof r.getBoundingClientRect == 'function');
    });
}
function Za(n) {
    n === void 0 && (n = {});
    var e = n,
        t = e.defaultModifiers,
        r = t === void 0 ? [] : t,
        i = e.defaultOptions,
        o = i === void 0 ? vr : i;
    return function (a, l, u) {
        u === void 0 && (u = o);
        var c = {
                placement: 'bottom',
                orderedModifiers: [],
                options: Object.assign({}, vr, o),
                modifiersData: {},
                elements: { reference: a, popper: l },
                attributes: {},
                styles: {}
            },
            d = [],
            p = !1,
            h = {
                state: c,
                setOptions: function (x) {
                    var A = typeof x == 'function' ? x(c.options) : x;
                    g(),
                        (c.options = Object.assign({}, o, c.options, A)),
                        (c.scrollParents = {
                            reference: je(a) ? gt(a) : a.contextElement ? gt(a.contextElement) : [],
                            popper: gt(l)
                        });
                    var N = Ua(_a([].concat(r, c.options.modifiers)));
                    if (
                        ((c.orderedModifiers = N.filter(function (C) {
                            return C.enabled;
                        })),
                        process.env.NODE_ENV !== 'production')
                    ) {
                        var f = Xa([].concat(N, c.options.modifiers), function (C) {
                            var D = C.name;
                            return D;
                        });
                        if ((Ya(f), ue(c.options.placement) === rn)) {
                            var O = c.orderedModifiers.find(function (C) {
                                var D = C.name;
                                return D === 'flip';
                            });
                            O ||
                                console.error(
                                    [
                                        'Popper: "auto" placements require the "flip" modifier be',
                                        'present and enabled to work.'
                                    ].join(' ')
                                );
                        }
                        var y = fe(l),
                            M = y.marginTop,
                            B = y.marginRight,
                            L = y.marginBottom,
                            z = y.marginLeft;
                        [M, B, L, z].some(function (C) {
                            return parseFloat(C);
                        }) &&
                            console.warn(
                                [
                                    'Popper: CSS "margin" styles cannot be used to apply padding',
                                    'between the popper and its reference element or boundary.',
                                    'To replicate margin, use the `offset` modifier, as well as',
                                    'the `padding` option in the `preventOverflow` and `flip`',
                                    'modifiers.'
                                ].join(' ')
                            );
                    }
                    return v(), h.update();
                },
                forceUpdate: function () {
                    if (!p) {
                        var x = c.elements,
                            A = x.reference,
                            N = x.popper;
                        if (!yr(A, N)) {
                            process.env.NODE_ENV !== 'production' && console.error(gr);
                            return;
                        }
                        (c.rects = {
                            reference: Ha(A, Ot(N), c.options.strategy === 'fixed'),
                            popper: zn(N)
                        }),
                            (c.reset = !1),
                            (c.placement = c.options.placement),
                            c.orderedModifiers.forEach(function (C) {
                                return (c.modifiersData[C.name] = Object.assign({}, C.data));
                            });
                        for (var f = 0, O = 0; O < c.orderedModifiers.length; O++) {
                            if (process.env.NODE_ENV !== 'production' && ((f += 1), f > 100)) {
                                console.error(Qa);
                                break;
                            }
                            if (c.reset === !0) {
                                (c.reset = !1), (O = -1);
                                continue;
                            }
                            var y = c.orderedModifiers[O],
                                M = y.fn,
                                B = y.options,
                                L = B === void 0 ? {} : B,
                                z = y.name;
                            typeof M == 'function' &&
                                (c = M({ state: c, options: L, name: z, instance: h }) || c);
                        }
                    }
                },
                update: Ka(function () {
                    return new Promise(function (w) {
                        h.forceUpdate(), w(c);
                    });
                }),
                destroy: function () {
                    g(), (p = !0);
                }
            };
        if (!yr(a, l)) return process.env.NODE_ENV !== 'production' && console.error(gr), h;
        h.setOptions(u).then(function (w) {
            !p && u.onFirstUpdate && u.onFirstUpdate(w);
        });
        function v() {
            c.orderedModifiers.forEach(function (w) {
                var x = w.name,
                    A = w.options,
                    N = A === void 0 ? {} : A,
                    f = w.effect;
                if (typeof f == 'function') {
                    var O = f({ state: c, name: x, instance: h, options: N }),
                        y = function () {};
                    d.push(O || y);
                }
            });
        }
        function g() {
            d.forEach(function (w) {
                return w();
            }),
                (d = []);
        }
        return h;
    };
}
var el = [wa, Fa, va, oi, Ba, Ia, ja, pa, Ra],
    tl = Za({ defaultModifiers: el }),
    nl = 'tippy-box',
    pi = 'tippy-content',
    rl = 'tippy-backdrop',
    hi = 'tippy-arrow',
    mi = 'tippy-svg-arrow',
    Be = { passive: !0, capture: !0 },
    gi = function () {
        return document.body;
    };
function il(n, e) {
    return {}.hasOwnProperty.call(n, e);
}
function mn(n, e, t) {
    if (Array.isArray(n)) {
        var r = n[e];
        return r ?? (Array.isArray(t) ? t[e] : t);
    }
    return n;
}
function Vn(n, e) {
    var t = {}.toString.call(n);
    return t.indexOf('[object') === 0 && t.indexOf(e + ']') > -1;
}
function vi(n, e) {
    return typeof n == 'function' ? n.apply(void 0, e) : n;
}
function wr(n, e) {
    if (e === 0) return n;
    var t;
    return function (r) {
        clearTimeout(t),
            (t = setTimeout(function () {
                n(r);
            }, e));
    };
}
function ol(n, e) {
    var t = Object.assign({}, n);
    return (
        e.forEach(function (r) {
            delete t[r];
        }),
        t
    );
}
function sl(n) {
    return n.split(/\s+/).filter(Boolean);
}
function Ye(n) {
    return [].concat(n);
}
function br(n, e) {
    n.indexOf(e) === -1 && n.push(e);
}
function al(n) {
    return n.filter(function (e, t) {
        return n.indexOf(e) === t;
    });
}
function ll(n) {
    return n.split('-')[0];
}
function en(n) {
    return [].slice.call(n);
}
function xr(n) {
    return Object.keys(n).reduce(function (e, t) {
        return n[t] !== void 0 && (e[t] = n[t]), e;
    }, {});
}
function vt() {
    return document.createElement('div');
}
function kt(n) {
    return ['Element', 'Fragment'].some(function (e) {
        return Vn(n, e);
    });
}
function cl(n) {
    return Vn(n, 'NodeList');
}
function ul(n) {
    return Vn(n, 'MouseEvent');
}
function fl(n) {
    return !!(n && n._tippy && n._tippy.reference === n);
}
function dl(n) {
    return kt(n) ? [n] : cl(n) ? en(n) : Array.isArray(n) ? n : en(document.querySelectorAll(n));
}
function gn(n, e) {
    n.forEach(function (t) {
        t && (t.style.transitionDuration = e + 'ms');
    });
}
function kr(n, e) {
    n.forEach(function (t) {
        t && t.setAttribute('data-state', e);
    });
}
function pl(n) {
    var e,
        t = Ye(n),
        r = t[0];
    return r != null && (e = r.ownerDocument) != null && e.body ? r.ownerDocument : document;
}
function hl(n, e) {
    var t = e.clientX,
        r = e.clientY;
    return n.every(function (i) {
        var o = i.popperRect,
            s = i.popperState,
            a = i.props,
            l = a.interactiveBorder,
            u = ll(s.placement),
            c = s.modifiersData.offset;
        if (!c) return !0;
        var d = u === 'bottom' ? c.top.y : 0,
            p = u === 'top' ? c.bottom.y : 0,
            h = u === 'right' ? c.left.x : 0,
            v = u === 'left' ? c.right.x : 0,
            g = o.top - r + d > l,
            w = r - o.bottom - p > l,
            x = o.left - t + h > l,
            A = t - o.right - v > l;
        return g || w || x || A;
    });
}
function vn(n, e, t) {
    var r = e + 'EventListener';
    ['transitionend', 'webkitTransitionEnd'].forEach(function (i) {
        n[r](i, t);
    });
}
function Sr(n, e) {
    for (var t = e; t; ) {
        var r;
        if (n.contains(t)) return !0;
        t = t.getRootNode == null || (r = t.getRootNode()) == null ? void 0 : r.host;
    }
    return !1;
}
var me = { isTouch: !1 },
    Er = 0;
function ml() {
    me.isTouch ||
        ((me.isTouch = !0), window.performance && document.addEventListener('mousemove', yi));
}
function yi() {
    var n = performance.now();
    n - Er < 20 && ((me.isTouch = !1), document.removeEventListener('mousemove', yi)), (Er = n);
}
function gl() {
    var n = document.activeElement;
    if (fl(n)) {
        var e = n._tippy;
        n.blur && !e.state.isVisible && n.blur();
    }
}
function vl() {
    document.addEventListener('touchstart', ml, Be), window.addEventListener('blur', gl);
}
var yl = typeof window < 'u' && typeof document < 'u',
    wl = yl ? !!window.msCrypto : !1;
function Ke(n) {
    var e = n === 'destroy' ? 'n already-' : ' ';
    return [
        n + '() was called on a' + e + 'destroyed instance. This is a no-op but',
        'indicates a potential memory leak.'
    ].join(' ');
}
function Or(n) {
    var e = /[ \t]{2,}/g,
        t = /^[ \t]*/gm;
    return n.replace(e, ' ').replace(t, '').trim();
}
function bl(n) {
    return Or(
        `
  %ctippy.js

  %c` +
            Or(n) +
            `

  %c👷‍ This is a development-only message. It will be removed in production.
  `
    );
}
function wi(n) {
    return [
        bl(n),
        'color: #00C584; font-size: 1.3em; font-weight: bold;',
        'line-height: 1.5',
        'color: #a6a095;'
    ];
}
var St;
process.env.NODE_ENV !== 'production' && xl();
function xl() {
    St = new Set();
}
function Se(n, e) {
    if (n && !St.has(e)) {
        var t;
        St.add(e), (t = console).warn.apply(t, wi(e));
    }
}
function Mn(n, e) {
    if (n && !St.has(e)) {
        var t;
        St.add(e), (t = console).error.apply(t, wi(e));
    }
}
function kl(n) {
    var e = !n,
        t = Object.prototype.toString.call(n) === '[object Object]' && !n.addEventListener;
    Mn(
        e,
        [
            'tippy() was passed',
            '`' + String(n) + '`',
            'as its targets (first) argument. Valid types are: String, Element,',
            'Element[], or NodeList.'
        ].join(' ')
    ),
        Mn(
            t,
            [
                'tippy() was passed a plain object which is not supported as an argument',
                'for virtual positioning. Use props.getReferenceClientRect instead.'
            ].join(' ')
        );
}
var bi = { animateFill: !1, followCursor: !1, inlinePositioning: !1, sticky: !1 },
    Sl = {
        allowHTML: !1,
        animation: 'fade',
        arrow: !0,
        content: '',
        inertia: !1,
        maxWidth: 350,
        role: 'tooltip',
        theme: '',
        zIndex: 9999
    },
    ee = Object.assign(
        {
            appendTo: gi,
            aria: { content: 'auto', expanded: 'auto' },
            delay: 0,
            duration: [300, 250],
            getReferenceClientRect: null,
            hideOnClick: !0,
            ignoreAttributes: !1,
            interactive: !1,
            interactiveBorder: 2,
            interactiveDebounce: 0,
            moveTransition: '',
            offset: [0, 10],
            onAfterUpdate: function () {},
            onBeforeUpdate: function () {},
            onCreate: function () {},
            onDestroy: function () {},
            onHidden: function () {},
            onHide: function () {},
            onMount: function () {},
            onShow: function () {},
            onShown: function () {},
            onTrigger: function () {},
            onUntrigger: function () {},
            onClickOutside: function () {},
            placement: 'top',
            plugins: [],
            popperOptions: {},
            render: null,
            showOnCreate: !1,
            touch: !0,
            trigger: 'mouseenter focus',
            triggerTarget: null
        },
        bi,
        Sl
    ),
    El = Object.keys(ee),
    Ol = function (e) {
        process.env.NODE_ENV !== 'production' && ki(e, []);
        var t = Object.keys(e);
        t.forEach(function (r) {
            ee[r] = e[r];
        });
    };
function xi(n) {
    var e = n.plugins || [],
        t = e.reduce(function (r, i) {
            var o = i.name,
                s = i.defaultValue;
            if (o) {
                var a;
                r[o] = n[o] !== void 0 ? n[o] : (a = ee[o]) != null ? a : s;
            }
            return r;
        }, {});
    return Object.assign({}, n, t);
}
function Ml(n, e) {
    var t = e ? Object.keys(xi(Object.assign({}, ee, { plugins: e }))) : El,
        r = t.reduce(function (i, o) {
            var s = (n.getAttribute('data-tippy-' + o) || '').trim();
            if (!s) return i;
            if (o === 'content') i[o] = s;
            else
                try {
                    i[o] = JSON.parse(s);
                } catch {
                    i[o] = s;
                }
            return i;
        }, {});
    return r;
}
function Mr(n, e) {
    var t = Object.assign(
        {},
        e,
        { content: vi(e.content, [n]) },
        e.ignoreAttributes ? {} : Ml(n, e.plugins)
    );
    return (
        (t.aria = Object.assign({}, ee.aria, t.aria)),
        (t.aria = {
            expanded: t.aria.expanded === 'auto' ? e.interactive : t.aria.expanded,
            content:
                t.aria.content === 'auto' ? (e.interactive ? null : 'describedby') : t.aria.content
        }),
        t
    );
}
function ki(n, e) {
    n === void 0 && (n = {}), e === void 0 && (e = []);
    var t = Object.keys(n);
    t.forEach(function (r) {
        var i = ol(ee, Object.keys(bi)),
            o = !il(i, r);
        o &&
            (o =
                e.filter(function (s) {
                    return s.name === r;
                }).length === 0),
            Se(
                o,
                [
                    '`' + r + '`',
                    "is not a valid prop. You may have spelled it incorrectly, or if it's",
                    'a plugin, forgot to pass it in an array as props.plugins.',
                    `

`,
                    `All props: https://atomiks.github.io/tippyjs/v6/all-props/
`,
                    'Plugins: https://atomiks.github.io/tippyjs/v6/plugins/'
                ].join(' ')
            );
    });
}
var Tl = function () {
    return 'innerHTML';
};
function Tn(n, e) {
    n[Tl()] = e;
}
function Tr(n) {
    var e = vt();
    return (
        n === !0 ? (e.className = hi) : ((e.className = mi), kt(n) ? e.appendChild(n) : Tn(e, n)), e
    );
}
function Ar(n, e) {
    kt(e.content)
        ? (Tn(n, ''), n.appendChild(e.content))
        : typeof e.content != 'function' &&
          (e.allowHTML ? Tn(n, e.content) : (n.textContent = e.content));
}
function An(n) {
    var e = n.firstElementChild,
        t = en(e.children);
    return {
        box: e,
        content: t.find(function (r) {
            return r.classList.contains(pi);
        }),
        arrow: t.find(function (r) {
            return r.classList.contains(hi) || r.classList.contains(mi);
        }),
        backdrop: t.find(function (r) {
            return r.classList.contains(rl);
        })
    };
}
function Si(n) {
    var e = vt(),
        t = vt();
    (t.className = nl), t.setAttribute('data-state', 'hidden'), t.setAttribute('tabindex', '-1');
    var r = vt();
    (r.className = pi),
        r.setAttribute('data-state', 'hidden'),
        Ar(r, n.props),
        e.appendChild(t),
        t.appendChild(r),
        i(n.props, n.props);
    function i(o, s) {
        var a = An(e),
            l = a.box,
            u = a.content,
            c = a.arrow;
        s.theme ? l.setAttribute('data-theme', s.theme) : l.removeAttribute('data-theme'),
            typeof s.animation == 'string'
                ? l.setAttribute('data-animation', s.animation)
                : l.removeAttribute('data-animation'),
            s.inertia ? l.setAttribute('data-inertia', '') : l.removeAttribute('data-inertia'),
            (l.style.maxWidth = typeof s.maxWidth == 'number' ? s.maxWidth + 'px' : s.maxWidth),
            s.role ? l.setAttribute('role', s.role) : l.removeAttribute('role'),
            (o.content !== s.content || o.allowHTML !== s.allowHTML) && Ar(u, n.props),
            s.arrow
                ? c
                    ? o.arrow !== s.arrow && (l.removeChild(c), l.appendChild(Tr(s.arrow)))
                    : l.appendChild(Tr(s.arrow))
                : c && l.removeChild(c);
    }
    return { popper: e, onUpdate: i };
}
Si.$$tippy = !0;
var Al = 1,
    Wt = [],
    yn = [];
function Cl(n, e) {
    var t = Mr(n, Object.assign({}, ee, xi(xr(e)))),
        r,
        i,
        o,
        s = !1,
        a = !1,
        l = !1,
        u = !1,
        c,
        d,
        p,
        h = [],
        v = wr(At, t.interactiveDebounce),
        g,
        w = Al++,
        x = null,
        A = al(t.plugins),
        N = { isEnabled: !0, isVisible: !1, isDestroyed: !1, isMounted: !1, isShown: !1 },
        f = {
            id: w,
            reference: n,
            popper: vt(),
            popperInstance: x,
            props: t,
            state: N,
            plugins: A,
            clearDelayTimeouts: Dt,
            setProps: Pt,
            setContent: Bt,
            show: Ci,
            hide: Ii,
            hideWithInteractivity: Ni,
            enable: ct,
            disable: Rt,
            unmount: Ri,
            destroy: Di
        };
    if (!t.render)
        return (
            process.env.NODE_ENV !== 'production' &&
                Mn(!0, 'render() function has not been supplied.'),
            f
        );
    var O = t.render(f),
        y = O.popper,
        M = O.onUpdate;
    y.setAttribute('data-tippy-root', ''),
        (y.id = 'tippy-' + f.id),
        (f.popper = y),
        (n._tippy = f),
        (y._tippy = f);
    var B = A.map(function (m) {
            return m.fn(f);
        }),
        L = n.hasAttribute('aria-expanded');
    return (
        He(),
        pe(),
        q(),
        J('onCreate', [f]),
        t.showOnCreate && lt(),
        y.addEventListener('mouseenter', function () {
            f.props.interactive && f.state.isVisible && f.clearDelayTimeouts();
        }),
        y.addEventListener('mouseleave', function () {
            f.props.interactive &&
                f.props.trigger.indexOf('mouseenter') >= 0 &&
                Q().addEventListener('mousemove', v);
        }),
        f
    );
    function z() {
        var m = f.props.touch;
        return Array.isArray(m) ? m : [m, 0];
    }
    function C() {
        return z()[0] === 'hold';
    }
    function D() {
        var m;
        return !!((m = f.props.render) != null && m.$$tippy);
    }
    function F() {
        return g || n;
    }
    function Q() {
        var m = F().parentNode;
        return m ? pl(m) : document;
    }
    function Z() {
        return An(y);
    }
    function H(m) {
        return (f.state.isMounted && !f.state.isVisible) || me.isTouch || (c && c.type === 'focus')
            ? 0
            : mn(f.props.delay, m ? 0 : 1, ee.delay);
    }
    function q(m) {
        m === void 0 && (m = !1),
            (y.style.pointerEvents = f.props.interactive && !m ? '' : 'none'),
            (y.style.zIndex = '' + f.props.zIndex);
    }
    function J(m, b, S) {
        if (
            (S === void 0 && (S = !0),
            B.forEach(function (T) {
                T[m] && T[m].apply(T, b);
            }),
            S)
        ) {
            var I;
            (I = f.props)[m].apply(I, b);
        }
    }
    function de() {
        var m = f.props.aria;
        if (m.content) {
            var b = 'aria-' + m.content,
                S = y.id,
                I = Ye(f.props.triggerTarget || n);
            I.forEach(function (T) {
                var K = T.getAttribute(b);
                if (f.state.isVisible) T.setAttribute(b, K ? K + ' ' + S : S);
                else {
                    var ne = K && K.replace(S, '').trim();
                    ne ? T.setAttribute(b, ne) : T.removeAttribute(b);
                }
            });
        }
    }
    function pe() {
        if (!(L || !f.props.aria.expanded)) {
            var m = Ye(f.props.triggerTarget || n);
            m.forEach(function (b) {
                f.props.interactive
                    ? b.setAttribute(
                          'aria-expanded',
                          f.state.isVisible && b === F() ? 'true' : 'false'
                      )
                    : b.removeAttribute('aria-expanded');
            });
        }
    }
    function Ie() {
        Q().removeEventListener('mousemove', v),
            (Wt = Wt.filter(function (m) {
                return m !== v;
            }));
    }
    function he(m) {
        if (!(me.isTouch && (l || m.type === 'mousedown'))) {
            var b = (m.composedPath && m.composedPath()[0]) || m.target;
            if (!(f.props.interactive && Sr(y, b))) {
                if (
                    Ye(f.props.triggerTarget || n).some(function (S) {
                        return Sr(S, b);
                    })
                ) {
                    if (me.isTouch || (f.state.isVisible && f.props.trigger.indexOf('click') >= 0))
                        return;
                } else J('onClickOutside', [f, m]);
                f.props.hideOnClick === !0 &&
                    (f.clearDelayTimeouts(),
                    f.hide(),
                    (a = !0),
                    setTimeout(function () {
                        a = !1;
                    }),
                    f.state.isMounted || be());
            }
        }
    }
    function Ne() {
        l = !0;
    }
    function we() {
        l = !1;
    }
    function ce() {
        var m = Q();
        m.addEventListener('mousedown', he, !0),
            m.addEventListener('touchend', he, Be),
            m.addEventListener('touchstart', we, Be),
            m.addEventListener('touchmove', Ne, Be);
    }
    function be() {
        var m = Q();
        m.removeEventListener('mousedown', he, !0),
            m.removeEventListener('touchend', he, Be),
            m.removeEventListener('touchstart', we, Be),
            m.removeEventListener('touchmove', Ne, Be);
    }
    function We(m, b) {
        Je(m, function () {
            !f.state.isVisible && y.parentNode && y.parentNode.contains(y) && b();
        });
    }
    function xe(m, b) {
        Je(m, b);
    }
    function Je(m, b) {
        var S = Z().box;
        function I(T) {
            T.target === S && (vn(S, 'remove', I), b());
        }
        if (m === 0) return b();
        vn(S, 'remove', d), vn(S, 'add', I), (d = I);
    }
    function Oe(m, b, S) {
        S === void 0 && (S = !1);
        var I = Ye(f.props.triggerTarget || n);
        I.forEach(function (T) {
            T.addEventListener(m, b, S), h.push({ node: T, eventType: m, handler: b, options: S });
        });
    }
    function He() {
        C() && (Oe('touchstart', ot, { passive: !0 }), Oe('touchend', Ct, { passive: !0 })),
            sl(f.props.trigger).forEach(function (m) {
                if (m !== 'manual')
                    switch ((Oe(m, ot), m)) {
                        case 'mouseenter':
                            Oe('mouseleave', Ct);
                            break;
                        case 'focus':
                            Oe(wl ? 'focusout' : 'blur', st);
                            break;
                        case 'focusin':
                            Oe('focusout', st);
                            break;
                    }
            });
    }
    function Tt() {
        h.forEach(function (m) {
            var b = m.node,
                S = m.eventType,
                I = m.handler,
                T = m.options;
            b.removeEventListener(S, I, T);
        }),
            (h = []);
    }
    function ot(m) {
        var b,
            S = !1;
        if (!(!f.state.isEnabled || at(m) || a)) {
            var I = ((b = c) == null ? void 0 : b.type) === 'focus';
            (c = m),
                (g = m.currentTarget),
                pe(),
                !f.state.isVisible &&
                    ul(m) &&
                    Wt.forEach(function (T) {
                        return T(m);
                    }),
                m.type === 'click' &&
                (f.props.trigger.indexOf('mouseenter') < 0 || s) &&
                f.props.hideOnClick !== !1 &&
                f.state.isVisible
                    ? (S = !0)
                    : lt(m),
                m.type === 'click' && (s = !S),
                S && !I && Re(m);
        }
    }
    function At(m) {
        var b = m.target,
            S = F().contains(b) || y.contains(b);
        if (!(m.type === 'mousemove' && S)) {
            var I = Me()
                .concat(y)
                .map(function (T) {
                    var K,
                        ne = T._tippy,
                        qe = (K = ne.popperInstance) == null ? void 0 : K.state;
                    return qe
                        ? { popperRect: T.getBoundingClientRect(), popperState: qe, props: t }
                        : null;
                })
                .filter(Boolean);
            hl(I, m) && (Ie(), Re(m));
        }
    }
    function Ct(m) {
        var b = at(m) || (f.props.trigger.indexOf('click') >= 0 && s);
        if (!b) {
            if (f.props.interactive) {
                f.hideWithInteractivity(m);
                return;
            }
            Re(m);
        }
    }
    function st(m) {
        (f.props.trigger.indexOf('focusin') < 0 && m.target !== F()) ||
            (f.props.interactive && m.relatedTarget && y.contains(m.relatedTarget)) ||
            Re(m);
    }
    function at(m) {
        return me.isTouch ? C() !== m.type.indexOf('touch') >= 0 : !1;
    }
    function It() {
        Nt();
        var m = f.props,
            b = m.popperOptions,
            S = m.placement,
            I = m.offset,
            T = m.getReferenceClientRect,
            K = m.moveTransition,
            ne = D() ? An(y).arrow : null,
            qe = T ? { getBoundingClientRect: T, contextElement: T.contextElement || F() } : n,
            Wn = {
                name: '$$tippy',
                enabled: !0,
                phase: 'beforeWrite',
                requires: ['computeStyles'],
                fn: function (zt) {
                    var Ue = zt.state;
                    if (D()) {
                        var Pi = Z(),
                            an = Pi.box;
                        ['placement', 'reference-hidden', 'escaped'].forEach(function (Ft) {
                            Ft === 'placement'
                                ? an.setAttribute('data-placement', Ue.placement)
                                : Ue.attributes.popper['data-popper-' + Ft]
                                ? an.setAttribute('data-' + Ft, '')
                                : an.removeAttribute('data-' + Ft);
                        }),
                            (Ue.attributes.popper = {});
                    }
                }
            },
            De = [
                { name: 'offset', options: { offset: I } },
                {
                    name: 'preventOverflow',
                    options: { padding: { top: 2, bottom: 2, left: 5, right: 5 } }
                },
                { name: 'flip', options: { padding: 5 } },
                { name: 'computeStyles', options: { adaptive: !K } },
                Wn
            ];
        D() && ne && De.push({ name: 'arrow', options: { element: ne, padding: 3 } }),
            De.push.apply(De, b?.modifiers || []),
            (f.popperInstance = tl(
                qe,
                y,
                Object.assign({}, b, { placement: S, onFirstUpdate: p, modifiers: De })
            ));
    }
    function Nt() {
        f.popperInstance && (f.popperInstance.destroy(), (f.popperInstance = null));
    }
    function ke() {
        var m = f.props.appendTo,
            b,
            S = F();
        (f.props.interactive && m === gi) || m === 'parent' ? (b = S.parentNode) : (b = vi(m, [S])),
            b.contains(y) || b.appendChild(y),
            (f.state.isMounted = !0),
            It(),
            process.env.NODE_ENV !== 'production' &&
                Se(
                    f.props.interactive && m === ee.appendTo && S.nextElementSibling !== y,
                    [
                        'Interactive tippy element may not be accessible via keyboard',
                        'navigation because it is not directly after the reference element',
                        'in the DOM source order.',
                        `

`,
                        'Using a wrapper <div> or <span> tag around the reference element',
                        'solves this by creating a new parentNode context.',
                        `

`,
                        'Specifying `appendTo: document.body` silences this warning, but it',
                        'assumes you are using a focus management solution to handle',
                        'keyboard navigation.',
                        `

`,
                        'See: https://atomiks.github.io/tippyjs/v6/accessibility/#interactivity'
                    ].join(' ')
                );
    }
    function Me() {
        return en(y.querySelectorAll('[data-tippy-root]'));
    }
    function lt(m) {
        f.clearDelayTimeouts(), m && J('onTrigger', [f, m]), ce();
        var b = H(!0),
            S = z(),
            I = S[0],
            T = S[1];
        me.isTouch && I === 'hold' && T && (b = T),
            b
                ? (r = setTimeout(function () {
                      f.show();
                  }, b))
                : f.show();
    }
    function Re(m) {
        if ((f.clearDelayTimeouts(), J('onUntrigger', [f, m]), !f.state.isVisible)) {
            be();
            return;
        }
        if (
            !(
                f.props.trigger.indexOf('mouseenter') >= 0 &&
                f.props.trigger.indexOf('click') >= 0 &&
                ['mouseleave', 'mousemove'].indexOf(m.type) >= 0 &&
                s
            )
        ) {
            var b = H(!1);
            b
                ? (i = setTimeout(function () {
                      f.state.isVisible && f.hide();
                  }, b))
                : (o = requestAnimationFrame(function () {
                      f.hide();
                  }));
        }
    }
    function ct() {
        f.state.isEnabled = !0;
    }
    function Rt() {
        f.hide(), (f.state.isEnabled = !1);
    }
    function Dt() {
        clearTimeout(r), clearTimeout(i), cancelAnimationFrame(o);
    }
    function Pt(m) {
        if (
            (process.env.NODE_ENV !== 'production' && Se(f.state.isDestroyed, Ke('setProps')),
            !f.state.isDestroyed)
        ) {
            J('onBeforeUpdate', [f, m]), Tt();
            var b = f.props,
                S = Mr(n, Object.assign({}, b, xr(m), { ignoreAttributes: !0 }));
            (f.props = S),
                He(),
                b.interactiveDebounce !== S.interactiveDebounce &&
                    (Ie(), (v = wr(At, S.interactiveDebounce))),
                b.triggerTarget && !S.triggerTarget
                    ? Ye(b.triggerTarget).forEach(function (I) {
                          I.removeAttribute('aria-expanded');
                      })
                    : S.triggerTarget && n.removeAttribute('aria-expanded'),
                pe(),
                q(),
                M && M(b, S),
                f.popperInstance &&
                    (It(),
                    Me().forEach(function (I) {
                        requestAnimationFrame(I._tippy.popperInstance.forceUpdate);
                    })),
                J('onAfterUpdate', [f, m]);
        }
    }
    function Bt(m) {
        f.setProps({ content: m });
    }
    function Ci() {
        process.env.NODE_ENV !== 'production' && Se(f.state.isDestroyed, Ke('show'));
        var m = f.state.isVisible,
            b = f.state.isDestroyed,
            S = !f.state.isEnabled,
            I = me.isTouch && !f.props.touch,
            T = mn(f.props.duration, 0, ee.duration);
        if (
            !(m || b || S || I) &&
            !F().hasAttribute('disabled') &&
            (J('onShow', [f], !1), f.props.onShow(f) !== !1)
        ) {
            if (
                ((f.state.isVisible = !0),
                D() && (y.style.visibility = 'visible'),
                q(),
                ce(),
                f.state.isMounted || (y.style.transition = 'none'),
                D())
            ) {
                var K = Z(),
                    ne = K.box,
                    qe = K.content;
                gn([ne, qe], 0);
            }
            (p = function () {
                var De;
                if (!(!f.state.isVisible || u)) {
                    if (
                        ((u = !0),
                        y.offsetHeight,
                        (y.style.transition = f.props.moveTransition),
                        D() && f.props.animation)
                    ) {
                        var sn = Z(),
                            zt = sn.box,
                            Ue = sn.content;
                        gn([zt, Ue], T), kr([zt, Ue], 'visible');
                    }
                    de(),
                        pe(),
                        br(yn, f),
                        (De = f.popperInstance) == null || De.forceUpdate(),
                        J('onMount', [f]),
                        f.props.animation &&
                            D() &&
                            xe(T, function () {
                                (f.state.isShown = !0), J('onShown', [f]);
                            });
                }
            }),
                ke();
        }
    }
    function Ii() {
        process.env.NODE_ENV !== 'production' && Se(f.state.isDestroyed, Ke('hide'));
        var m = !f.state.isVisible,
            b = f.state.isDestroyed,
            S = !f.state.isEnabled,
            I = mn(f.props.duration, 1, ee.duration);
        if (!(m || b || S) && (J('onHide', [f], !1), f.props.onHide(f) !== !1)) {
            if (
                ((f.state.isVisible = !1),
                (f.state.isShown = !1),
                (u = !1),
                (s = !1),
                D() && (y.style.visibility = 'hidden'),
                Ie(),
                be(),
                q(!0),
                D())
            ) {
                var T = Z(),
                    K = T.box,
                    ne = T.content;
                f.props.animation && (gn([K, ne], I), kr([K, ne], 'hidden'));
            }
            de(), pe(), f.props.animation ? D() && We(I, f.unmount) : f.unmount();
        }
    }
    function Ni(m) {
        process.env.NODE_ENV !== 'production' &&
            Se(f.state.isDestroyed, Ke('hideWithInteractivity')),
            Q().addEventListener('mousemove', v),
            br(Wt, v),
            v(m);
    }
    function Ri() {
        process.env.NODE_ENV !== 'production' && Se(f.state.isDestroyed, Ke('unmount')),
            f.state.isVisible && f.hide(),
            f.state.isMounted &&
                (Nt(),
                Me().forEach(function (m) {
                    m._tippy.unmount();
                }),
                y.parentNode && y.parentNode.removeChild(y),
                (yn = yn.filter(function (m) {
                    return m !== f;
                })),
                (f.state.isMounted = !1),
                J('onHidden', [f]));
    }
    function Di() {
        process.env.NODE_ENV !== 'production' && Se(f.state.isDestroyed, Ke('destroy')),
            !f.state.isDestroyed &&
                (f.clearDelayTimeouts(),
                f.unmount(),
                Tt(),
                delete n._tippy,
                (f.state.isDestroyed = !0),
                J('onDestroy', [f]));
    }
}
function Mt(n, e) {
    e === void 0 && (e = {});
    var t = ee.plugins.concat(e.plugins || []);
    process.env.NODE_ENV !== 'production' && (kl(n), ki(e, t)), vl();
    var r = Object.assign({}, e, { plugins: t }),
        i = dl(n);
    if (process.env.NODE_ENV !== 'production') {
        var o = kt(r.content),
            s = i.length > 1;
        Se(
            o && s,
            [
                'tippy() was passed an Element as the `content` prop, but more than',
                'one tippy instance was created by this invocation. This means the',
                'content element will only be appended to the last tippy instance.',
                `

`,
                'Instead, pass the .innerHTML of the element, or use a function that',
                'returns a cloned version of the element instead.',
                `

`,
                `1) content: element.innerHTML
`,
                '2) content: () => element.cloneNode(true)'
            ].join(' ')
        );
    }
    var a = i.reduce(function (l, u) {
        var c = u && Cl(u, r);
        return c && l.push(c), l;
    }, []);
    return kt(n) ? a[0] : a;
}
Mt.defaultProps = ee;
Mt.setDefaultProps = Ol;
Mt.currentInput = me;
Object.assign({}, oi, {
    effect: function (e) {
        var t = e.state,
            r = {
                popper: { position: t.options.strategy, left: '0', top: '0', margin: '0' },
                arrow: { position: 'absolute' },
                reference: {}
            };
        Object.assign(t.elements.popper.style, r.popper),
            (t.styles = r),
            t.elements.arrow && Object.assign(t.elements.arrow.style, r.arrow);
    }
});
Mt.setDefaultProps({ render: Si });
class Il {
    constructor({ editor: e, element: t, view: r, shouldShow: i }) {
        (this.preventHide = !1),
            (this.shouldShow = ({ state: o }) => {
                const { doc: s, selection: a } = o,
                    { head: l } = a;
                return s.resolve(l).parent.content.size == 0;
            }),
            (this.editor = e),
            (this.element = t),
            (this.view = r),
            i && (this.shouldShow = i),
            this.element.remove(),
            (this.element.style.visibility = 'visible');
    }
    createTooltip() {
        const { element: e } = this.editor.options,
            t = !!e.parentElement;
        this.tippy ||
            !t ||
            (this.tippy = Mt(e, {
                duration: 0,
                getReferenceClientRect: null,
                content: this.element,
                interactive: !0,
                trigger: 'manual',
                placement: 'left',
                hideOnClick: 'toggle'
            }));
    }
    update(e, t) {
        this.updateHandler(e, t);
    }
    updateHandler(e, t) {
        const { state: r } = e,
            { selection: i } = r;
        this.createTooltip();
        const { ranges: o } = i,
            s = Math.min(...o.map((u) => u.$from.pos)),
            a = Math.max(...o.map((u) => u.$to.pos));
        if (
            !this.shouldShow?.({
                editor: this.editor,
                view: e,
                state: r,
                oldState: t,
                from: s,
                to: a
            })
        ) {
            this.tippy?.hide();
            return;
        }
        this.tippy?.setProps({ getReferenceClientRect: () => Ms(e, s, a) }), this.tippy?.show();
    }
    destroy() {
        this.tippy?.destroy();
    }
}
const Nl = ve.create({
        name: 'CustomExtension',
        addOptions() {
            return { element: null, pluginKey: '' };
        },
        addProseMirrorPlugins() {
            const n = document.createElement('button');
            return (
                (n.innerText = 'Add'),
                (n.style.position = 'absolute'),
                (n.style.top = '-50px'),
                (n.style.left = '-33px'),
                (n.style.border = '0'),
                (n.style.padding = '5px'),
                (n.onclick = () => this.editor.commands.addHelloWorld()),
                [Ei({ pluginKey: this.options.pluginKey, editor: this.editor, element: n })]
            );
        }
    }),
    Ei = (n) =>
        new rt({
            key: typeof n.pluginKey == 'string' ? new it(n.pluginKey) : n.pluginKey,
            view: (e) => new Il({ view: e, ...n })
        }),
    Oi = /(?:^|\s)((?:\*)((?:[^*]+))(?:\*))$/,
    Mi = /(?:^|\s)((?:\*)((?:[^*]+))(?:\*))/g,
    Ti = /(?:^|\s)((?:_)((?:[^_]+))(?:_))$/,
    Ai = /(?:^|\s)((?:_)((?:[^_]+))(?:_))/g,
    Rl = _t.create({
        name: 'highlightCustom',
        addOptions() {
            return { HTMLAttributes: { style: 'background-color: #FFFF00; padding: 5px;' } };
        },
        renderHTML({ HTMLAttributes: n }) {
            return ['span', Mo(this.options.HTMLAttributes, n), 0];
        },
        addCommands() {
            return {
                setHighlight:
                    () =>
                    ({ commands: n }) =>
                        n.setMark(this.name),
                toggleHighlight:
                    () =>
                    ({ commands: n }) =>
                        n.toggleMark(this.name),
                unsetHighlight:
                    () =>
                    ({ commands: n }) =>
                        n.unsetMark(this.name)
            };
        },
        addKeyboardShortcuts() {
            return {
                'Mod-u': () => this.editor.commands.toggleHighlight(),
                'Mod-U': () => this.editor.commands.toggleHighlight()
            };
        },
        addInputRules() {
            return [sr({ find: Oi, type: this.type }), sr({ find: Ti, type: this.type })];
        },
        addPasteRules() {
            return [ar({ find: Mi, type: this.type }), ar({ find: Ai, type: this.type })];
        }
    });
exports.CustomExtension = Nl;
exports.CustomNode = Ks;
exports.CustomPlugin = Ei;
exports.HighlightCustom = Rl;
exports.starInputRegex = Oi;
exports.starPasteRegex = Mi;
exports.underscoreInputRegex = Ti;
exports.underscorePasteRegex = Ai;
