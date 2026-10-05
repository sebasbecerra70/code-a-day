"""Arithmetic expression evaluator: tokenizer -> shunting-yard (infix to RPN) -> stack evaluation.

Supports + - * / % ^ (right-associative), unary minus/plus, parentheses,
decimal numbers, and variables.
"""

from __future__ import annotations

import math
import re

# precedence, right-associative?
OPS = {
    "+": (1, False),
    "-": (1, False),
    "*": (2, False),
    "/": (2, False),
    "%": (2, False),
    "neg": (3, True),  # unary minus binds tighter than * but looser than ^
    "^": (4, True),
}

_TOKEN = re.compile(r"\s*(?:(\d+\.?\d*|\.\d+)|([A-Za-z_]\w*)|(.))")


def tokenize(expr: str) -> list[str | float]:
    tokens: list[str | float] = []
    pos = 0
    expr = expr.rstrip()
    while pos < len(expr):
        m = _TOKEN.match(expr, pos)
        num, name, sym = m.groups()
        if num is not None:
            tokens.append(float(num))
        elif name is not None:
            tokens.append(name)
        elif sym in "+-*/%^()":
            tokens.append(sym)
        else:
            raise SyntaxError(f"unexpected character {sym!r} at {m.start(3)}")
        pos = m.end()
    return tokens


def to_rpn(tokens: list[str | float]) -> list[str | float]:
    out: list[str | float] = []
    stack: list[str] = []
    expect_operand = True  # distinguishes unary from binary +/-
    for tok in tokens:
        if isinstance(tok, float) or (tok not in OPS and tok not in "()"):
            if not expect_operand:
                raise SyntaxError(f"missing operator before {tok!r}")
            out.append(tok)
            expect_operand = False
        elif tok == "(":
            if not expect_operand:
                raise SyntaxError("missing operator before '('")
            stack.append(tok)
        elif tok == ")":
            if expect_operand:
                raise SyntaxError("unexpected ')'")
            while stack and stack[-1] != "(":
                out.append(stack.pop())
            if not stack:
                raise SyntaxError("unbalanced ')'")
            stack.pop()
        else:
            if expect_operand:
                if tok == "+":
                    continue  # unary plus is a no-op
                if tok != "-":
                    raise SyntaxError(f"operator {tok!r} missing left operand")
                # A prefix operator has no left operand, so it never pops anything.
                stack.append("neg")
                continue
            prec, right = OPS[tok]
            # Pop operators that bind at least as tightly (strictly tighter for right-assoc).
            while stack and stack[-1] != "(":
                top_prec = OPS[stack[-1]][0]
                if top_prec > prec or (top_prec == prec and not right):
                    out.append(stack.pop())
                else:
                    break
            stack.append(tok)
            expect_operand = True
    if expect_operand:
        raise SyntaxError("unexpected end of expression")
    while stack:
        op = stack.pop()
        if op == "(":
            raise SyntaxError("unbalanced '('")
        out.append(op)
    return out


BINARY = {
    "+": lambda a, b: a + b,
    "-": lambda a, b: a - b,
    "*": lambda a, b: a * b,
    "/": lambda a, b: a / b,
    "%": math.fmod,
    "^": lambda a, b: a**b,
}


def eval_rpn(rpn: list[str | float], variables: dict[str, float] | None = None) -> float:
    variables = variables or {}
    st: list[float] = []
    for tok in rpn:
        if isinstance(tok, float):
            st.append(tok)
        elif tok == "neg":
            st.append(-st.pop())
        elif tok in BINARY:
            b, a = st.pop(), st.pop()
            if tok in "/%" and b == 0:
                raise ZeroDivisionError("division by zero")
            st.append(BINARY[tok](a, b))
        elif tok in variables:
            st.append(float(variables[tok]))
        else:
            raise NameError(f"unknown variable {tok!r}")
    return st[0]


def evaluate(expr: str, variables: dict[str, float] | None = None) -> float:
    return eval_rpn(to_rpn(tokenize(expr)), variables)
