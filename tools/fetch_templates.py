"""Точечная выкачка android-шаблонов из export_templates.tpz без полного скачивания.

usage:
  python tools/fetch_templates.py probe   # показать размеры нужных entries
  python tools/fetch_templates.py fetch   # скачать только android_source.zip + apks
  python tools/fetch_templates.py extract # распаковать в android/build и templates
"""
import os, re, struct, sys, time, urllib.request, threading, zipfile, zlib, shutil

URL = "https://github.com/godotengine/godot/releases/download/4.7-stable/Godot_v4.7-stable_export_templates.tpz"
NEEDED = ["android_source.zip", "android_debug.apk", "android_release.apk", "version.txt"]
OUT = "tpl_parts"
WORKERS = 24
CHUNK = 1 << 20

def get_range(url, start, end, retries=5):
    last = None
    for _ in range(retries):
        try:
            req = urllib.request.Request(url, headers={"Range": "bytes=%d-%d" % (start, end),
                                                       "User-Agent": "curl/8"})
            with urllib.request.urlopen(req, timeout=90) as r:
                return r.read()
        except Exception as e:  # noqa: BLE001
            last = e
            time.sleep(2)
    raise last

def head(url):
    req = urllib.request.Request(url, method="HEAD", headers={"User-Agent": "curl/8"})
    with urllib.request.urlopen(req, timeout=90) as r:
        return int(r.headers["Content-Length"]), r.geturl()

def central_dir(url, size):
    tail = get_range(url, max(0, size - 70000), size - 1)
    i = tail.rfind(b"PK\x05\x06")
    if i < 0:
        raise SystemExit("EOCD not found")
    _sig, _d, _c, n, _sz, cd_size, cd_off, _cl = struct.unpack_from("<IHHHHIIH", tail, i)
    cd = get_range(url, cd_off, cd_off + cd_size - 1)
    entries, p = {}, 0
    for _ in range(n):
        if cd[p:p+4] != b"PK\x01\x02":
            break
        (method, _t, _f, crc, csize, usize, nlen, elen, clen,
         _disk, _iattr, _eattr, off) = struct.unpack_from("<HHHIIIHHHHHII", cd, p + 10)
        name = cd[p + 46:p + 46 + nlen].decode("utf-8", "replace")
        entries[name] = dict(method=method, crc=crc, csize=csize, usize=usize, off=off)
        p += 46 + nlen + elen + clen
    return entries

def entry_data_span(url, e):
    head_bytes = get_range(url, e["off"], e["off"] + 29)
    nlen, elen = struct.unpack_from("<HH", head_bytes, 26)
    start = e["off"] + 30 + nlen + elen
    return start, start + e["csize"] - 1

def parallel_get(url, start, end, dest):
    total = end - start + 1
    with open(dest, "wb") as f:
        f.truncate(total)
    done = [0]
    lock = threading.Lock()
    ranges = [(s, min(s + CHUNK - 1, end)) for s in range(start, end + 1, CHUNK)]
    t0 = time.time()
    stop = threading.Event()

    def worker():
        fh = open(dest, "r+b")
        try:
            while True:
                with lock:
                    if not ranges:
                        return
                    s, e = ranges.pop(0)
                data = get_range(url, s, e)
                fh.seek(s - start)
                fh.write(data)
                with lock:
                    done[0] += len(data)
                    el = time.time() - t0
                    if el > 3 and int(el) % 5 == 0:
                        print("  %.1f/%.1f MB  %.0f KB/s" % (done[0] / 1e6, total / 1e6, done[0] / el / 1024),
                              flush=True)
        finally:
            fh.close()

    threads = [threading.Thread(target=worker, daemon=True) for _ in range(min(WORKERS, len(ranges)))]
    [t.start() for t in threads]
    [t.join() for t in threads]
    print("  done %.1f MB in %.0fs" % (total / 1e6, time.time() - t0), flush=True)

def probe():
    size, final = head(URL)
    print("remote size: %.1f MB" % (size / 1e6))
    ents = central_dir(URL, size)
    for n in NEEDED:
        e = ents.get(n)
        if e:
            print("%-22s %8.1f MB  method=%d  crc=%08x" % (n, e["csize"] / 1e6, e["method"], e["crc"]))
        else:
            print("%-22s MISSING" % n)
    return size, final, ents

def fetch():
    os.makedirs(OUT, exist_ok=True)
    size, _final, ents = probe()
    for n in NEEDED:
        e = ents.get(n)
        if not e:
            continue
        dst = os.path.join(OUT, n)
        if os.path.exists(dst) and os.path.getsize(dst) == e["csize"]:
            print("skip (done):", n)
            continue
        s, en = entry_data_span(URL, e)
        print("downloading", n, "%.1f MB" % (e["csize"] / 1e6))
        parallel_get(URL, s, en, dst)
        blob = open(dst, "rb").read()
        if e["method"] == 8:
            blob = zlib.decompress(blob, -15)
        ok = (zlib.crc32(blob) & 0xFFFFFFFF) == e["crc"]
        print("  crc:", "OK" if ok else "FAIL")
        if not ok:
            os.remove(dst)
            raise SystemExit("crc mismatch for " + n)
        if e["method"] == 8:
            open(dst, "wb").write(blob)

def extract():
    tdir = os.path.join(os.environ["APPDATA"], "Godot", "export_templates", "4.7.stable")
    os.makedirs(tdir, exist_ok=True)
    src = os.path.join(OUT, "android_source.zip")
    if os.path.exists(src):
        shutil.copy(src, os.path.join(tdir, "android_source.zip"))
        if os.path.exists("android/build"):
            shutil.rmtree("android/build")
        with zipfile.ZipFile(src) as z:
            z.extractall("android/build")
        print("installed android/build from source template")
    for apk in ("android_debug.apk", "android_release.apk"):
        p = os.path.join(OUT, apk)
        if os.path.exists(p):
            shutil.copy(p, os.path.join(tdir, apk))
            print("installed", apk)
    open(os.path.join(tdir, "version.txt"), "w").write("4.7.stable")

if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "probe"
    {"probe": probe, "fetch": fetch, "extract": extract}[mode]()
