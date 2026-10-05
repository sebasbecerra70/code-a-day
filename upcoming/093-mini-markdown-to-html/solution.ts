// A small Markdown-to-HTML converter covering the everyday subset:
// headings, paragraphs, emphasis, inline code, links, fenced code blocks,
// blockquotes, ordered/unordered lists, and horizontal rules.
// Two passes: a line-based block parser, then an inline tokenizer.

export function escapeHtml(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

/** Converts inline markup. Code spans are cut out first so their contents stay literal. */
export function renderInline(text: string): string {
  const parts = text.split(/(`[^`]+`)/);
  return parts
    .map((part, i) => {
      if (i % 2 === 1) return `<code>${escapeHtml(part.slice(1, -1))}</code>`;
      let s = escapeHtml(part);
      s = s.replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (_, label: string, href: string) =>
        /^\s*javascript:/i.test(href) ? label : `<a href="${href}">${label}</a>`,
      );
      s = s.replace(/(\*\*|__)(?=\S)(.+?)(?<=\S)\1/g, "<strong>$2</strong>");
      s = s.replace(/(\*|_)(?=\S)(.+?)(?<=\S)\1/g, "<em>$2</em>");
      return s;
    })
    .join("");
}

const HR = /^ {0,3}([-*_])( *\1){2,} *$/;
const HEADING = /^(#{1,6})\s+(.*?)\s*#*\s*$/;
const FENCE = /^```(\w*)\s*$/;
const LIST_ITEM = /^(\s*)([-*+]|\d+\.)\s+(.*)$/;

export function markdownToHtml(md: string): string {
  const lines = md.replace(/\r\n?/g, "\n").split("\n");
  const out: string[] = [];
  let i = 0;

  const isBlockStart = (line: string) =>
    HEADING.test(line) || FENCE.test(line) || HR.test(line) || line.startsWith(">") || LIST_ITEM.test(line);

  while (i < lines.length) {
    const line = lines[i];
    if (line.trim() === "") {
      i++;
      continue;
    }

    let m: RegExpMatchArray | null;
    if ((m = line.match(FENCE))) {
      const body: string[] = [];
      i++;
      while (i < lines.length && !/^```\s*$/.test(lines[i])) body.push(lines[i++]);
      i++; // closing fence (or EOF)
      const cls = m[1] ? ` class="language-${m[1]}"` : "";
      out.push(`<pre><code${cls}>${escapeHtml(body.join("\n"))}</code></pre>`);
    } else if (HR.test(line)) {
      out.push("<hr>");
      i++;
    } else if ((m = line.match(HEADING))) {
      const level = m[1].length;
      out.push(`<h${level}>${renderInline(m[2])}</h${level}>`);
      i++;
    } else if (line.startsWith(">")) {
      const inner: string[] = [];
      while (i < lines.length && lines[i].startsWith(">")) inner.push(lines[i++].replace(/^> ?/, ""));
      out.push(`<blockquote>\n${markdownToHtml(inner.join("\n"))}\n</blockquote>`); // recursive: quotes can hold any block
    } else if (LIST_ITEM.test(line)) {
      const ordered = /^\s*\d+\./.test(line);
      const items: string[] = [];
      while (i < lines.length && (m = lines[i].match(LIST_ITEM)) && /^\d+\./.test(m[2]) === ordered) {
        items.push(`<li>${renderInline(m[3])}</li>`);
        i++;
      }
      const tag = ordered ? "ol" : "ul";
      out.push(`<${tag}>\n${items.join("\n")}\n</${tag}>`);
    } else {
      const para: string[] = [];
      while (i < lines.length && lines[i].trim() !== "" && !isBlockStart(lines[i])) para.push(lines[i++].trim());
      out.push(`<p>${renderInline(para.join(" "))}</p>`);
    }
  }
  return out.join("\n");
}
