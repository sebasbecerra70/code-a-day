import random

import pytest

from solution import evaluate, to_rpn, tokenize


def test_precedence_and_associativity():
    assert evaluate("1 + 2 * 3") == 7
    assert evaluate("(1 + 2) * 3") == 9
    assert evaluate("10 - 4 - 3") == 3  # left-assoc
    assert evaluate("2 ^ 3 ^ 2") == 512  # right-assoc
    assert evaluate("100 / 10 / 5") == 2
    assert evaluate("7 % 4 * 2") == 6


def test_rpn_conversion():
    assert to_rpn(tokenize("3 + 4 * 2 / (1 - 5) ^ 2 ^ 3")) == [
        3.0, 4.0, 2.0, "*", 1.0, 5.0, "-", 2.0, 3.0, "^", "^", "/", "+",
    ]


def test_unary_minus_and_plus():
    assert evaluate("-3") == -3
    assert evaluate("--3") == 3
    assert evaluate("2 * -3") == -6
    assert evaluate("-2 ^ 2") == -4  # exponent binds tighter than negation
    assert evaluate("2 ^ -1") == 0.5
    assert evaluate("+4 - -(1 + 1)") == 6


def test_decimals_and_variables():
    assert evaluate("1.5 * .5 + 2.") == 2.75
    assert evaluate("x * (y + 1)", {"x": 3, "y": 4}) == 15
    with pytest.raises(NameError):
        evaluate("z + 1")


def test_syntax_errors():
    for bad in ["", "1 +", "* 2", "(1 + 2", "1 + 2)", "1 2", "()", "2 (3)", "1 $ 2", "x y"]:
        with pytest.raises(SyntaxError):
            evaluate(bad, {"x": 1, "y": 2})


def test_division_by_zero():
    with pytest.raises(ZeroDivisionError):
        evaluate("1 / (2 - 2)")
    with pytest.raises(ZeroDivisionError):
        evaluate("5 % 0")


def test_randomized_against_python_eval():
    rng = random.Random(0)

    def gen(depth):
        if depth > 3 or rng.random() < 0.3:
            return str(rng.randint(0, 9))
        kind = rng.random()
        if kind < 0.15:
            return f"-{gen(depth + 1)}"
        if kind < 0.3:
            return f"({gen(depth + 1)})"
        op = rng.choice("+-*")
        return f"{gen(depth + 1)} {op} {gen(depth + 1)}"

    for _ in range(500):
        e = gen(0)
        assert evaluate(e) == eval(e), e  # Python has the same precedence for + - * and unary -
