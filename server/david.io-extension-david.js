/* david.io extras (runs in the page after main-mt.js)
   - Spine mode  : draws every enemy as a thin centre line + head dot (drawing is done by the game renderer, see dvSpineDraw in main-mt.js)
                   toggle with  P  (remappable in the game's key menu, id "dvspine") or the checkbox in Open Settings
   - Brightness / Saturation : screen filter on the game canvas, sliders in Open Settings
   Eyes Back (U) and Center Eyes (I) live in main-mt.js (NTL_EB / NTL_EC). */
(function () {
  "use strict";
  if (window.__dvLoaded) return;
  window.__dvLoaded = 1;

  var KEY = "dv_cfg", cfg = { bright: 100, sat: 100, spine: false };
  try { var s = JSON.parse(localStorage.getItem(KEY) || "{}"); if (s && typeof s === "object") { if (+s.bright) cfg.bright = clamp(+s.bright); if (+s.sat) cfg.sat = clamp(+s.sat); cfg.spine = !!s.spine; } } catch (e) {}
  function clamp(v) { return Math.max(40, Math.min(160, Math.round(v))); }
  function save() { try { localStorage.setItem(KEY, JSON.stringify(cfg)); } catch (e) {} }
  function $(id) { return document.getElementById(id); }

  function alive() {
    try { return !!(typeof playing !== "undefined" && playing && typeof snake !== "undefined" && snake); } catch (e) { return false; }
  }

  /* ---------- tiny toast ---------- */
  var toastEl = null, toastT = 0;
  function toast(msg) {
    if (!document.body) return;
    if (!toastEl) {
      toastEl = document.createElement("div");
      toastEl.style.cssText = "position:fixed;top:14px;left:50%;transform:translateX(-50%);z-index:2147483000;padding:6px 14px;border-radius:14px;" +
        "background:rgba(20,14,50,.9);color:#fff;font:600 13px Arial,sans-serif;border:1px solid rgba(150,110,255,.7);pointer-events:none;transition:opacity .25s;opacity:0";
      document.body.appendChild(toastEl);
    }
    toastEl.textContent = msg; toastEl.style.opacity = "1";
    clearTimeout(toastT); toastT = setTimeout(function () { toastEl.style.opacity = "0"; }, 1400);
  }

  /* ---------- spine flag read by the renderer ---------- */
  function syncSpine() { window.__dvSpine = !!(cfg.spine && alive()); }
  setInterval(syncSpine, 100);

  function setSpine(v, quiet) {
    cfg.spine = !!v; save(); syncSpine();
    var cb = $("dv-spine"); if (cb) cb.checked = cfg.spine;
    if (!quiet) toast("Spine mode " + (cfg.spine ? "ON" : "OFF"));
  }

  /* ---------- brightness / saturation ---------- */
  var lookCs = [], lookN = -1, lookTick = 0, lookF = null, dirty = true;
  window.addEventListener("resize", function () { dirty = true; });
  function applyLook() {
    var f = (cfg.bright !== 100 || cfg.sat !== 100) ? "brightness(" + (cfg.bright / 100) + ") saturate(" + (cfg.sat / 100) + ")" : "";
    var cs = document.getElementsByTagName("canvas");
    if (dirty || cs.length !== lookN || ++lookTick >= 10) {
      lookTick = 0; dirty = false; lookN = cs.length; lookCs = []; lookF = null;
      var iw = window.innerWidth * 0.6, ih = window.innerHeight * 0.6;
      for (var i = 0; i < cs.length; i++) { var c = cs[i]; if (c.clientWidth >= iw && c.clientHeight >= ih) lookCs.push(c); }
    }
    if (f !== lookF) {
      for (var j = 0; j < lookCs.length; j++) if (lookCs[j].style.filter !== f) lookCs[j].style.filter = f;
      lookF = f;
    }
  }
  setInterval(applyLook, 400);

  /* ---------- spine key (remappable) ---------- */
  function curKey() {
    try {
      if (typeof a9 !== "undefined") {
        if (a9 === null && typeof ms === "function") { try { ms(); } catch (e) {} }
        if (a9 && "dvspine" in a9) return String(a9.dvspine || "").toLowerCase();
      }
    } catch (e) {}
    try { var m = JSON.parse(localStorage.getItem("my_keymap") || "null"); if (m && "dvspine" in m) return String(m.dvspine || "").toLowerCase(); } catch (e) {}
    return "p";
  }
  function typing() {
    var el = document.activeElement; if (!el) return false;
    var t = (el.tagName || "").toUpperCase();
    return t === "INPUT" || t === "TEXTAREA" || t === "SELECT" || el.isContentEditable;
  }
  window.addEventListener("keydown", function (e) {
    if (e.repeat || e.ctrlKey || e.altKey || e.metaKey || typing()) return;
    var k = curKey();
    if (!k || (e.key || "").toLowerCase() !== k) return;
    if (!alive()) return;
    setSpine(!cfg.spine);
  }, true);

  /* ---------- controls inside Open Settings ---------- */
  var CSS = "#dv-lk{padding:2px 0 4px}#dv-lk .r{display:flex;gap:8px;align-items:center;margin:5px 0;font-size:12px}" +
    "#dv-lk .r b{min-width:78px;font-weight:600}#dv-lk input[type=range]{flex:1;min-width:0}#dv-lk .v{min-width:34px;text-align:right;opacity:.85}" +
    "#dv-lk .hint{font-size:11px;opacity:.65;margin-top:3px}";
  function build(sp) {
    var tbl = sp.querySelector(".opensett-content table"); if (!tbl) return;
    var tb = tbl.tBodies[0] || tbl, anchor = null;
    var tm = $("dv-tm"); if (tm && tm.closest) anchor = tm.closest("tr");
    if (!anchor) anchor = tb.rows[0];
    var h = document.createElement("tr"); h.className = "tsep"; h.id = "dv-lk-sec";
    h.innerHTML = '<td colspan="2" class="tsep">david.io Look · spine · brightness · saturation</td>';
    var b = document.createElement("tr");
    b.innerHTML = '<td colspan="2"><div id="dv-lk">' +
      '<div class="r"><b>Spine mode</b><input type="checkbox" id="dv-spine"><span class="hint" style="margin:0">key P (change in key menu)</span></div>' +
      '<div class="r"><b>Brightness</b><input type="range" id="dv-bright" min="60" max="140" step="1"><span class="v" id="dv-bright-v"></span></div>' +
      '<div class="r"><b>Saturation</b><input type="range" id="dv-sat" min="60" max="140" step="1"><span class="v" id="dv-sat-v"></span></div>' +
      '<div class="r"><button class="mygb" id="dv-lk-reset">Reset look</button></div>' +
      '<div class="hint">Eyes Back: double-tap U &nbsp;·&nbsp; Center Eyes: I &nbsp;·&nbsp; Spine: P</div></div></td>';
    tb.insertBefore(h, anchor ? anchor.nextSibling : null);
    tb.insertBefore(b, h.nextSibling);
    var style = document.createElement("style"); style.textContent = CSS; h.appendChild(style);

    function paint() { $("dv-bright").value = cfg.bright; $("dv-sat").value = cfg.sat; $("dv-spine").checked = cfg.spine; $("dv-bright-v").textContent = cfg.bright + "%"; $("dv-sat-v").textContent = cfg.sat + "%"; }
    paint();
    $("dv-spine").onchange = function () { setSpine(this.checked, true); };
    $("dv-bright").oninput = function () { cfg.bright = clamp(+this.value); $("dv-bright-v").textContent = cfg.bright + "%"; save(); applyLook(); };
    $("dv-sat").oninput = function () { cfg.sat = clamp(+this.value); $("dv-sat-v").textContent = cfg.sat + "%"; save(); applyLook(); };
    $("dv-lk-reset").onclick = function () { cfg.bright = 100; cfg.sat = 100; save(); paint(); applyLook(); };
  }
  function watch() { var sp = $("settpage"); if (sp && !$("dv-lk-sec")) build(sp); }
  function start() {
    if (!document.body) { document.addEventListener("DOMContentLoaded", start, { once: true }); return; }
    new MutationObserver(watch).observe(document.body, { childList: true });
    watch();
  }
  start();

  window.DAVID = { get spine() { return cfg.spine; }, set spine(v) { setSpine(v); }, get cfg() { return cfg; } };
})();
