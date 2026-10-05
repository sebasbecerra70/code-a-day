import io
import json
import random
import subprocess
import sys
from collections import Counter
from pathlib import Path

import pytest

from solution import SpaceSaving, main, parse, parse_line, report, status_filter, top_k

LOG = """\
127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326
10.0.0.2 - - [10/Oct/2000:13:55:37 -0700] "GET /index.html HTTP/1.1" 200 512 "-" "curl/8.0"
10.0.0.2 - - [10/Oct/2000:13:55:38 -0700] "POST /login?next=/home HTTP/1.1" 302 - "https://x/" "Mozilla/5.0 (X11)"
10.0.0.3 - - [10/Oct/2000:13:55:39 -0700] "GET /index.html HTTP/1.1" 404 100 "-" "curl/8.0"
this line is garbage
10.0.0.3 - - [10/Oct/2000:13:55:40 -0700] "GET /login?next=/x HTTP/1.1" 500 0 "-" "curl/8.0"

10.0.0.2 - - [10/Oct/2000:13:55:41 -0700] "GET /index.html HTTP/1.1" 503 0
"""


def test_parse_common_and_combined_formats():
    e = parse_line('127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] "GET /a.gif HTTP/1.0" 200 2326')
    assert (e.ip, e.method, e.path, e.status, e.size, e.agent) == ("127.0.0.1", "GET", "/a.gif", 200, 2326, None)
    e = parse_line('::1 - - [x] "POST /login?a=1 HTTP/2.0" 302 - "ref" "Mozilla/5.0 (X11)"')
    assert (e.ip, e.path, e.size, e.agent) == ("::1", "/login", 0, "Mozilla/5.0 (X11)")


def test_malformed_lines_counted_blank_lines_ignored():
    entries, bad = parse(LOG.splitlines())
    assert len(entries) == 6 and bad == 1
    assert parse_line('1.2.3.4 - - [t] "GET / HTTP/1.1" 2000 1') is None


def test_report_by_path_and_ip():
    entries, _ = parse(LOG.splitlines())
    rep = report(entries, by="path", k=2)
    assert rep["top"] == [("/index.html", 3), ("/login", 2)]
    assert rep["total"] == 6 and rep["unique"] == 3 and rep["bytes"] == 2326 + 512 + 100
    assert report(entries, by="ip", k=1)["top"] == [("10.0.0.2", 3)]


def test_status_filters():
    entries, _ = parse(LOG.splitlines())
    assert report(entries, by="status", status="5xx")["top"] == [("500", 1), ("503", 1)]
    assert report(entries, status="404,302")["total"] == 2
    f = status_filter("4xx")
    assert f(404) and f(499) and not f(500)
    with pytest.raises(ValueError):
        status_filter("6xx")


def test_top_k_ties_and_k_larger_than_n():
    c = Counter({"b": 2, "a": 2, "c": 5, "d": 1})
    assert top_k(c, 3) == [("c", 5), ("a", 2), ("b", 2)]
    assert top_k(c, 100) == [("c", 5), ("a", 2), ("b", 2), ("d", 1)]
    assert top_k(Counter(), 3) == []


def test_space_saving_finds_heavy_hitters():
    rng = random.Random(9)
    stream = ["hot"] * 3000 + ["warm"] * 1500 + [f"cold{rng.randrange(5000)}" for _ in range(5500)]
    rng.shuffle(stream)
    ss = SpaceSaving(50)
    for x in stream:
        ss.add(x)
    top = [k for k, _ in ss.top(2)]
    assert top == ["hot", "warm"]
    # Guarantee: count - error <= true count <= count
    true = Counter(stream)
    for k, c in ss.counts.items():
        assert c - ss.errors[k] <= true[k] <= c


def test_cli_text_and_json(tmp_path: Path):
    log = tmp_path / "access.log"
    log.write_text(LOG)
    out = io.StringIO()
    assert main([str(log), "-k", "2"], stdout=out) == 0
    text = out.getvalue()
    assert text.splitlines()[0] == "6 requests, 3 unique path values, 2938 bytes"
    assert "3   50.0%  /index.html" in text and "(1 malformed lines skipped)" in text
    out = io.StringIO()
    assert main(["-", "--by", "ip", "--json", "-k", "1"], stdin=io.StringIO(LOG), stdout=out) == 0
    data = json.loads(out.getvalue())
    assert data["top"] == [{"key": "10.0.0.2", "count": 3}] and data["malformed"] == 1


def test_cli_errors(capsys):
    assert main(["/nope.log"]) == 1
    assert main(["-", "-k", "0"], stdin=io.StringIO("")) == 2
    assert main(["-", "--status", "abc"], stdin=io.StringIO(LOG), stdout=io.StringIO()) == 2
    assert "bad status filter" in capsys.readouterr().err


def test_cli_subprocess():
    proc = subprocess.run(
        [sys.executable, str(Path(__file__).with_name("solution.py")), "-", "--by", "method", "-k", "1"],
        input=LOG, capture_output=True, text=True, check=True,
    )
    assert "5   83.3%  GET" in proc.stdout
