# -*- coding: utf-8 -*-
"""weapi/eapi 协议验证：与 Android NeteaseDirect.kt 同参同法，实测网易云官方接口"""
import base64
import json
import os
import random
import string
import sys
import urllib.parse
import urllib.request
from Crypto.Cipher import AES

NONCE = "0CoJUm6Qyw8W8jud"
EAPI_KEY = "e82ckenh8dichen8"
IV = b"0102030405060708"
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
MODULUS = int(
    "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b7251"
    "52b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312e"
    "cbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d"
    "813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7", 16)
EXP = int("010001", 16)


def aes_cbc(data: bytes, key: bytes) -> bytes:
    return AES.new(key, AES.MODE_CBC, IV).encrypt(data)


def pkcs7(data: bytes) -> bytes:
    pad = 16 - len(data) % 16
    return data + bytes([pad]) * pad


def rsa_encrypt(data: bytes) -> str:
    # RSA PKCS1 v1.5 加密（与 Android 端 RSA/ECB/PKCS1Padding 一致）
    from Crypto.PublicKey import RSA
    from Crypto.Cipher import PKCS1_v1_5
    key = RSA.construct((MODULUS, EXP))
    cipher = PKCS1_v1_5.new(key)
    return cipher.encrypt(data).hex()


def weapi(body: dict):
    text = json.dumps(body, separators=(",", ":"))
    secret = os.urandom(16)
    p1 = aes_cbc(pkcs7(text.encode()), secret)
    p2 = aes_cbc(p1, NONCE.encode())
    params = base64.b64encode(p2).decode()
    enc_sec_key = rsa_encrypt(secret)
    return {"params": params, "encSecKey": enc_sec_key}


def eapi(path: str, body) -> str:
    text = body if isinstance(body, str) else json.dumps(body, separators=(",", ":"))
    msg = f"nobody{path}use{text}music"
    enc = aes_cbc(pkcs7(msg.encode()), EAPI_KEY.encode())
    return base64.b64encode(enc).decode()


def post_form(path: str, form: dict):
    if path.startswith("/api/"):
        path = "/weapi" + path[4:]
    data = urllib.parse.urlencode(form).encode()
    req = urllib.request.Request("https://music.163.com" + path, data=data, method="POST")
    req.add_header("User-Agent", UA)
    req.add_header("Referer", "https://music.163.com/")
    req.add_header("Origin", "https://music.163.com")
    req.add_header("Content-Type", "application/x-www-form-urlencoded")
    with urllib.request.urlopen(req, timeout=15) as r:
        return json.loads(r.read().decode())


def post_eapi(path: str, body):
    if path.startswith("/api/"):
        path = "/eapi" + path[4:]
    p = eapi(path, body)
    url = f"https://music.163.com{path}?params={urllib.parse.quote(p)}"
    req = urllib.request.Request(url, data=b"", method="POST")
    req.add_header("User-Agent", UA)
    req.add_header("Referer", "https://music.163.com/")
    req.add_header("Origin", "https://music.163.com")
    with urllib.request.urlopen(req, timeout=15) as r:
        return json.loads(r.read().decode())


def check(name, ok, extra=""):
    print(f"[{'PASS' if ok else 'FAIL'}] {name} {extra}")
    return ok


def main():
    print("== weapi/eapi 协议真实验证 ==")
    ok = True

    # 1. 二维码 key（weapi）
    r = post_form("/api/login/qrcode/unikey", weapi({"type": 1}))
    unikey = (r.get("data") or {}).get("unikey", "")
    ok &= check("qr key", r.get("code") == 200 and len(unikey) == 32, f"unikey={unikey[:8]}...")

    # 2. 搜索（weapi cloudsearch）
    r = post_form("/api/cloudsearch/pc", weapi({"s": "周杰伦", "type": 1, "limit": 3, "offset": 0}))
    songs = (r.get("result") or {}).get("songs") or []
    ok &= check("search", r.get("code") == 200 and len(songs) > 0,
                f"首曲={songs[0].get('name') if songs else 'N/A'}")
    song_id = songs[0]["id"] if songs else 347230

    # 3. 歌曲详情（weapi）
    r = post_form("/api/v3/song/detail", weapi({"c": f'[{{"id":{song_id}}}]'}))
    det = (r.get("songs") or [{}])[0]
    ok &= check("song detail", r.get("code") == 200 and det.get("id") == song_id,
                f"名称={det.get('name')}")

    # 4. 歌词（weapi）
    r = post_form("/api/song/lyric", weapi({"id": song_id, "lv": -1, "kv": -1, "tv": -1, "rv": -1}))
    lrc = (r.get("lrc") or {}).get("lyric") or ""
    ok &= check("lyric", r.get("code") == 200 and len(lrc) > 10, f"歌词前20字={lrc[:20]!r}")

    # 5. 播放地址（eapi，最关键）
    r = post_eapi("/api/song/enhance/player/url/v1",
                  {"ids": f"[{song_id}]", "level": "exhigh", "encodeType": "aac"})
    data = (r.get("data") or [{}])[0]
    url = data.get("url") or ""
    ok &= check("song url (eapi)", r.get("code") == 200 and url.startswith("http"),
                f"url={url[:80]}...  level={data.get('level')} br={data.get('br')}")

    # 6. 推荐歌单（weapi）
    r = post_form("/api/v3/discovery/recommend/resource", weapi({}))
    rec = r.get("recommend") or []
    ok &= check("recommend/resource", r.get("code") == 200 and len(rec) > 0,
                f"推荐数={len(rec)}")

    # 7. 排行榜（weapi）
    r = post_form("/api/toplist", weapi({}))
    tl = r.get("list") or []
    ok &= check("toplist", r.get("code") == 200 and len(tl) > 0, f"榜单数={len(tl)}")

    # 8. 热搜（weapi）
    r = post_form("/api/search/hot", weapi({}))
    hots = ((r.get("result") or {}).get("hots")) or []
    ok &= check("hot search", r.get("code") == 200 and len(hots) > 0,
                f"热搜1={(hots[0] or {}).get('first')}")

    print("=" * 46)
    print("全部通过 ✅" if ok else "存在失败 ❌")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
