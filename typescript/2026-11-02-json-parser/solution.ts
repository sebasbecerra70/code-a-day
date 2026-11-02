// A spec-compliant JSON parser (RFC 8259) written as a recursive-descent
// parser over the input string, with position-aware error messages.

export type JSONValue = null | boolean | number | string | JSONValue[] | { [k: string]: JSONValue };

export class JSONParseError extends SyntaxError {
  constructor(message: string, readonly position: number) {
    super(`${message} at position ${position}`);
  }
}

export function parseJSON(text: string): JSONValue {
  let i = 0;

  const fail = (msg: string): never => {
    throw new JSONParseError(msg, i);
  };

  const skipWs = () => {
    while (i < text.length && " \t\n\r".includes(text[i])) i++;
  };

  const expect = (ch: string) => {
    if (text[i] !== ch) fail(`Expected '${ch}'`);
    i++;
  };

  const literal = <T>(word: string, value: T): T => {
    if (text.startsWith(word, i)) {
      i += word.length;
      return value;
    }
    return fail("Unexpected token");
  };

  const parseNumber = (): number => {
    // -? (0 | [1-9]\d*) (\.\d+)? ([eE][+-]?\d+)?
    const m = /-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?/y;
    m.lastIndex = i;
    const match = m.exec(text);
    if (!match) fail("Invalid number");
    i += match![0].length;
    return Number(match![0]);
  };

  const ESCAPES: Record<string, string> = { '"': '"', "\\": "\\", "/": "/", b: "\b", f: "\f", n: "\n", r: "\r", t: "\t" };

  const parseString = (): string => {
    expect('"');
    let out = "";
    for (;;) {
      if (i >= text.length) fail("Unterminated string");
      const ch = text[i];
      if (ch === '"') {
        i++;
        return out;
      }
      if (ch === "\\") {
        const esc = text[i + 1];
        if (esc === "u") {
          const hex = text.slice(i + 2, i + 6);
          if (!/^[0-9a-fA-F]{4}$/.test(hex)) fail("Invalid unicode escape");
          // Surrogate pairs arrive as two \u escapes; concatenating code units joins them.
          out += String.fromCharCode(parseInt(hex, 16));
          i += 6;
        } else if (esc in ESCAPES) {
          out += ESCAPES[esc];
          i += 2;
        } else {
          fail("Invalid escape");
        }
      } else if (ch < " ") {
        fail("Control character in string");
      } else {
        out += ch;
        i++;
      }
    }
  };

  const parseArray = (): JSONValue[] => {
    expect("[");
    const arr: JSONValue[] = [];
    skipWs();
    if (text[i] === "]") {
      i++;
      return arr;
    }
    for (;;) {
      arr.push(parseValue());
      skipWs();
      if (text[i] === ",") i++;
      else if (text[i] === "]") {
        i++;
        return arr;
      } else fail("Expected ',' or ']'");
    }
  };

  const parseObject = (): { [k: string]: JSONValue } => {
    expect("{");
    // Null prototype so keys like "__proto__" are plain data.
    const obj: { [k: string]: JSONValue } = Object.create(null);
    skipWs();
    if (text[i] === "}") {
      i++;
      return obj;
    }
    for (;;) {
      skipWs();
      if (text[i] !== '"') fail("Expected string key");
      const key = parseString();
      skipWs();
      expect(":");
      obj[key] = parseValue();
      skipWs();
      if (text[i] === ",") i++;
      else if (text[i] === "}") {
        i++;
        return obj;
      } else fail("Expected ',' or '}'");
    }
  };

  function parseValue(): JSONValue {
    skipWs();
    const ch = text[i];
    if (ch === "{") return parseObject();
    if (ch === "[") return parseArray();
    if (ch === '"') return parseString();
    if (ch === "t") return literal("true", true);
    if (ch === "f") return literal("false", false);
    if (ch === "n") return literal("null", null);
    if (ch === "-" || (ch >= "0" && ch <= "9")) return parseNumber();
    return fail(i >= text.length ? "Unexpected end of input" : "Unexpected token");
  }

  const value = parseValue();
  skipWs();
  if (i < text.length) fail("Unexpected trailing characters");
  return value;
}
