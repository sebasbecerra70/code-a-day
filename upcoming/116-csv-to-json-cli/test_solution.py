import io
import json
import subprocess
import sys
from pathlib import Path

import pytest

from solution import convert, infer, main, rows_to_records

SAMPLE = 'name,age,city,zip\nAda,36,"London, UK",02134\nAlan,,"Wilmslow",\n'


def run(text, **kw):
    out = io.StringIO()
    n = convert(io.StringIO(text), out, **kw)
    return n, out.getvalue()


def test_basic_array_output_is_valid_json():
    n, text = run(SAMPLE)
    assert n == 2
    data = json.loads(text)
    assert data[0] == {"name": "Ada", "age": "36", "city": "London, UK", "zip": "02134"}
    assert data[1]["age"] == ""


def test_infer_types():
    assert [infer(v) for v in ["", "true", "FALSE", "42", "-7", "3.5", ".5", "1e3", "007", "0", "0.25", "nan", "abc", "+1"]] == [
        None, True, False, 42, -7, 3.5, 0.5, 1000.0, "007", 0, 0.25, "nan", "abc", 1
    ]
    _, text = run(SAMPLE, infer_types=True)
    data = json.loads(text)
    assert data[0]["age"] == 36 and data[0]["zip"] == "02134"
    assert data[1]["age"] is None


def test_jsonl_and_compact():
    _, text = run(SAMPLE, jsonl=True)
    lines = text.splitlines()
    assert len(lines) == 2 and json.loads(lines[1])["name"] == "Alan"
    _, compact = run(SAMPLE, indent=None)
    assert "\n" not in compact.strip() and len(json.loads(compact)) == 2


def test_empty_input_and_header_only():
    assert run("") == (0, "[]\n")
    assert run("a,b\n") == (0, "[]\n")
    assert run("a,b\n", jsonl=True) == (0, "")


def test_select_columns_and_unknown_column():
    _, text = run(SAMPLE, select=["city", "name"])
    data = json.loads(text)
    assert list(data[0]) == ["city", "name"]
    with pytest.raises(ValueError, match="unknown column"):
        run(SAMPLE, select=["nope"])


def test_ragged_rows_blank_lines_and_strict():
    recs = list(rows_to_records([["a", "b", "c"], ["1"], [], ["1", "2", "3", "4"]]))
    assert recs == [{"a": "1", "b": None, "c": None}, {"a": "1", "b": "2", "c": "3"}]
    with pytest.raises(ValueError, match="line 4: expected 3 fields, got 4"):
        list(rows_to_records([["a", "b", "c"], ["1"], [], ["1", "2", "3", "4"]], strict=True))


def test_no_header_and_duplicate_headers():
    assert list(rows_to_records([["x", "y"]], header=False)) == [{"column_1": "x", "column_2": "y"}]
    assert list(rows_to_records([["a", "a", ""], ["1", "2", "3"]])) == [{"a": "1", "a_2": "2", "column_3": "3"}]


def test_quoted_fields_with_newlines_and_unicode():
    _, text = run('k,v\n"multi\nline","naïve ""quoted"""\n')
    assert json.loads(text) == [{"k": "multi\nline", "v": 'naïve "quoted"'}]
    assert "naïve" in text  # ensure_ascii=False keeps it readable


def test_main_with_files_and_tsv(tmp_path: Path):
    src = tmp_path / "in.tsv"
    src.write_text("﻿id\tok\n1\ttrue\n", encoding="utf-8")  # BOM is stripped
    out = tmp_path / "out.json"
    assert main([str(src), "-d", "\\t", "--infer-types", "-o", str(out)]) == 0
    assert json.loads(out.read_text()) == [{"id": 1, "ok": True}]


def test_main_errors(capsys):
    assert main(["/no/such/file.csv"]) == 1
    assert "No such file" in capsys.readouterr().err
    assert main(["-", "--strict"], stdin=io.StringIO("a\n1,2\n"), stdout=io.StringIO()) == 1
    assert main(["-", "-d", ";;"], stdin=io.StringIO(""), stdout=io.StringIO()) == 2


def test_cli_subprocess_stdin():
    proc = subprocess.run(
        [sys.executable, str(Path(__file__).with_name("solution.py")), "-", "--jsonl", "--select", "name"],
        input=SAMPLE, capture_output=True, text=True, check=True,
    )
    assert proc.stdout == '{"name": "Ada"}\n{"name": "Alan"}\n'
