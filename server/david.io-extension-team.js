/* david.io Team - Team ID + Auth Key generator that plugs into the game's OWN team system.
   Inside Open Settings (double-tap ".") there is a "david.io Team" block:
     - Team name, number of players, number of groups (parts), random key
     - "Quick team" = one click: random key for 5 players
     - Join = paste an invite (Team ID + Auth Key [+ server])
   Team chat = normal Enter chat, teammate dots = normal minimap dots, members = normal Players list.
   Players list label = YOUR team name (+ group letter when you use groups). Rename updates every group of the team.
   Keys from the original system keep working. Only Team IDs starting with "DVD-" (or old "LWK-") use the david.io server. */
(function () {
  "use strict";
  if (window.__dvTeamLoaded) return;
  window.__dvTeamLoaded = true;

  var PFX = ["DVD-", "LWK-"], DEFAULT_SRV = "http://127.0.0.1:8787", GL = "ABCDEF";
  function ls(k, v) {
    try { if (v === undefined) return localStorage.getItem(k); if (v === null) localStorage.removeItem(k); else localStorage.setItem(k, v); } catch (e) {}
    return null;
  }
  function isOurs(t) { t = String(t || "").toUpperCase(); for (var i = 0; i < PFX.length; i++) if (t.indexOf(PFX[i]) === 0) return true; return false; }
  function base() {
    var u = String(ls("dv_team_srv") || ls("lwk_team_srv") || DEFAULT_SRV).trim().replace(/\/+$/, "");
    if (/^wss?:/i.test(u)) u = u.replace(/^ws/i, "http");
    return u;
  }
  function cleanSrv(u) {
    u = String(u || "").trim().replace(/\/+$/, "");
    if (/^wss?:/i.test(u)) u = u.replace(/^ws/i, "http");
    return /^https?:\/\/[^\s/]+/i.test(u) ? u : "";
  }

  /* ===== 1) route our own teams to the relay server (same request format the game already uses) ===== */
  var _open = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function (m, url) {
    try {
      if (typeof url === "string") {
        var i = url.indexOf("ntl-slither.com/slither/ntlplay-mt.php?");
        if (i > 0) {
          var q = url.slice(url.indexOf("?", i) + 1), t = /(?:^|&)tid=([^&]*)/.exec(q);
          if (t && isOurs(decodeURIComponent(t[1]))) {
            arguments[1] = base() + "/poll?" + q + "&owner=" + encodeURIComponent(ls("NTLCOM") || "");
          }
        }
      }
    } catch (e) {}
    return _open.apply(this, arguments);
  };

  /* ===== 2) saved info about the team I am in (name / group / size) ===== */
  function getMeta() { try { var m = JSON.parse(ls("dv_team_meta") || "null"); return m && typeof m === "object" ? m : null; } catch (e) { return null; } }
  function setMeta(tid, n, g, m) { ls("dv_team_meta", JSON.stringify({ tid: tid, n: String(n || "").slice(0, 24), g: String(g || "").slice(0, 2), m: +m || 0 })); }

  var CSS =
    "#dv-tm{padding:2px 0 4px}#dv-tm .r{display:flex;gap:6px;align-items:center;margin:4px 0;flex-wrap:wrap}" +
    "#dv-tm .r input[type=text]{flex:1;min-width:0}#dv-tm label{font-size:11px;opacity:.8;display:flex;align-items:center;gap:4px}" +
    "#dv-tm select{background:#1d1d1d;color:#fff;border:1px solid #555;border-radius:4px;padding:2px 4px}" +
    "#dv-tm .st{font-size:11px;opacity:.8;min-height:14px;word-break:break-all}" +
    "#dv-tm .st.bad{color:#ffb347;opacity:1}#dv-tm .st.ok{color:#7be0a4;opacity:1}" +
    "#dv-tm .res{margin:4px 0}#dv-tm .res .row{display:flex;gap:6px;align-items:center;margin:3px 0;font-size:11px}" +
    "#dv-tm .res .row b{min-width:62px}#dv-tm .res .row span{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;opacity:.85}" +
    "#dv-tm details{margin-top:4px}#dv-tm summary{cursor:pointer;font-size:11px;opacity:.65}";

  function $(id) { return document.getElementById(id); }
  function req(method, url, cb) {
    var x = new XMLHttpRequest();
    x.open(method, url, true); x.timeout = 6000;
    x.onload = function () { var d = null; try { d = JSON.parse(x.responseText); } catch (e) {} cb(x.status === 200 && d ? null : "bad", d); };
    x.onerror = x.ontimeout = function () { cb("net"); };
    x.send(null);
  }
  function st(msg, cls) { var e = $("dv-tm-st"); if (e) { e.textContent = msg; e.className = "st " + (cls || ""); } }
  function setVal(id, v) {
    var e = $(id); if (!e) return;
    e.value = v;
    try { e.dispatchEvent(new Event("input", { bubbles: true })); } catch (x) {}
  }
  function current() {
    var t = ls("NTLTIDKEY") || "", a = ls("NTLAUTHKEY") || "";
    return isOurs(t) ? { tid: t, auth: a } : null;
  }
  function inviteFor(tid, auth, n, g, m) {
    return tid + " " + auth + " " + base() + " meta:" + encodeURIComponent(JSON.stringify({ n: n || "", g: g || "", m: m || 0 }));
  }
  // what the game shows in the players list / "Save as": exactly the team name (+ group letter only when there are groups)
  function labelOf(n, g) { n = String(n || "").replace(/\s+/g, " ").trim().slice(0, 24) || "Team"; return g ? n + " · " + g : n; }
  function describe(meta) {
    if (!meta) return "";
    var s = meta.n || "Team";
    if (meta.g) s += " · Group " + meta.g;
    if (meta.m) s += " · " + meta.m + " players";
    return s;
  }
  // saves through the game's own "Save" button, so everything behaves exactly like typing the keys yourself
  function apply(tid, auth, srv, label) {
    if (srv) ls("dv_team_srv", srv);
    if (!$("in_tidkey") || !$("savekeys")) return false;
    var nick = ""; try { nick = (ls("nick") || "").slice(0, 24); } catch (e) {}
    setVal("in_tidkey", tid); setVal("in_authkey", auth);
    setVal("in_comkey", label || labelOf(nick, ""));
    $("savekeys").disabled = false;
    $("savekeys").click();
    return true;
  }
  function copy(text, done) {
    function fb() {
      var ta = document.createElement("textarea"); ta.value = text; ta.style.cssText = "position:fixed;left:-999px";
      document.body.appendChild(ta); ta.select(); var ok = false; try { ok = document.execCommand("copy"); } catch (e) {} ta.remove(); done(ok);
    }
    try { navigator.clipboard.writeText(text).then(function () { done(true); }, fb); } catch (e) { fb(); }
  }

  var infoBusy = 0, syncedLabel = "";
  function refreshState() {
    var stEl = $("dv-tm-st");
    if (!stEl || stEl.getAttribute("data-busy")) return;
    var c = current();
    if (!c) { if (!/\bok\b|bad/.test(stEl.className)) st("Not in a david.io team"); return; }
    var meta = getMeta(); if (meta && meta.tid !== c.tid) meta = null;
    st("In " + (describe(meta) || ("team " + c.tid)), "ok");
    if (infoBusy) return; infoBusy = 1;
    var q = "?tid=" + encodeURIComponent(c.tid) + "&auth=" + encodeURIComponent(c.auth);
    req("GET", base() + "/info" + q, function (err, d) {
      infoBusy = 0;
      var e2 = $("dv-tm-st"); if (!e2 || e2.getAttribute("data-busy") || err || !d) return;
      setMeta(c.tid, d.name, d.group, d.max);
      var mm = { n: d.name, g: d.group, m: d.max };
      var line = "In " + describe(mm) + " · " + d.online + "/" + d.max + " online" + (d.online >= d.max ? " (full)" : "");
      st(line, "ok");
      // keep the game's players-list label equal to the team name (also picks up renames done by teammates)
      var want = labelOf(d.name, d.group), cm = $("in_comkey"), key = c.tid + "|" + want;
      if (cm && cm.value !== want && syncedLabel !== key) { syncedLabel = key; apply(c.tid, c.auth, "", want); }
      var nm = $("dv-tm-ren"); if (nm && document.activeElement !== nm && !nm.value) nm.placeholder = "Rename team (now: " + (d.name || "Team") + ")";
      // all groups of this team: online / limit
      req("GET", base() + "/groups" + q, function (e3, g) {
        var ge = $("dv-tm-groups"); if (!ge) return;
        if (e3 || !g || !g.groups || g.groups.length < 2) { ge.textContent = ""; return; }
        ge.textContent = "Groups: " + g.groups.map(function (x) { return x.group + " " + x.online + "/" + x.max; }).join("  ·  ");
      });
    });
  }

  function showResults(list) {
    var box = $("dv-tm-res"); if (!box) return;
    box.textContent = "";
    list.forEach(function (it) {
      var row = document.createElement("div"); row.className = "row";
      var b = document.createElement("b"); b.textContent = it.g ? "Group " + it.g : "Team";
      var sp = document.createElement("span"); sp.textContent = it.tid + "  ·  " + it.m + " players"; sp.title = it.tid;
      var use = document.createElement("button"); use.className = "mygb"; use.textContent = "Use";
      var cp = document.createElement("button"); cp.className = "mygb"; cp.textContent = "Copy invite";
      use.onclick = function () {
        if (!apply(it.tid, it.auth, "", labelOf(it.n, it.g))) { st("Open this from the settings panel", "bad"); return; }
        setMeta(it.tid, it.n, it.g, it.m); st("Using " + describe(it), "ok");
      };
      cp.onclick = function () { copy(inviteFor(it.tid, it.auth, it.n, it.g, it.m), function (ok) { st(ok ? "Invite copied · " + describe(it) : "Copy failed", ok ? "ok" : "bad"); }); };
      row.appendChild(b); row.appendChild(sp); row.appendChild(use); row.appendChild(cp); box.appendChild(row);
    });
  }

  // creates `groups` keys (one per group), each with its own random Team ID + Auth Key and player limit
  function generate(name, players, groups, mine) {
    name = String(name || "").replace(/\s+/g, " ").trim().slice(0, 24) || "Team";
    var out = [], i = 0, failed = false, fam = Math.random().toString(36).slice(2, 10);
    st("Creating " + (groups > 1 ? groups + " groups…" : "team…")); $("dv-tm-st").setAttribute("data-busy", "1");
    (function next() {
      if (i >= groups) { done(); return; }
      var g = groups > 1 ? GL[i] : "";
      req("POST", base() + "/create?name=" + encodeURIComponent(name) + "&group=" + g + "&max=" + players + "&fam=" + fam, function (err, d) {
        if (err || !d || !d.tid || !d.auth) { failed = true; done(); return; }
        out.push({ tid: d.tid, auth: d.auth, n: d.name || name, g: d.group || g, m: d.max || players });
        i++; next();
      });
    })();
    function done() {
      $("dv-tm-st").removeAttribute("data-busy");
      if (failed || !out.length) { st("Can't reach team server (" + base() + "). Check 'Team server' below.", "bad"); return; }
      showResults(out);
      var pick = out[0];
      if (groups > 1) for (var k = 0; k < out.length; k++) if (out[k].g === mine) pick = out[k];
      if (!apply(pick.tid, pick.auth, "", labelOf(pick.n, groups > 1 ? pick.g : ""))) { st("Open this from the settings panel", "bad"); return; }
      setMeta(pick.tid, pick.n, pick.g, pick.m);
      copy(inviteFor(pick.tid, pick.auth, pick.n, pick.g, pick.m), function (ok) {
        st("Created " + describe(pick) + " · saved" + (ok ? " · invite copied" : "") + (groups > 1 ? " · send each group its own invite" : "") +
          (/127\.0\.0\.1|localhost/.test(base()) ? " · friends need a public https server URL" : ""), "ok");
      });
    }
  }

  function build(sp) {
    var tbl = sp.querySelector(".opensett-content table"); if (!tbl) return;
    var tb = tbl.tBodies[0] || tbl, first = tb.rows[0];
    var h = document.createElement("tr"); h.className = "tsep"; h.id = "dv-tm-sec";
    h.innerHTML = '<td colspan="2" class="tsep">david.io Team · generate / join</td>';
    var b = document.createElement("tr");
    var pOpts = [2, 3, 4, 5, 6, 8, 10, 15, 20, 30].map(function (n) { return '<option value="' + n + '"' + (n === 5 ? " selected" : "") + ">" + n + "</option>"; }).join("");
    var gOpts = [1, 2, 3, 4, 5, 6].map(function (n) { return '<option value="' + n + '">' + (n === 1 ? "1 (no groups)" : n + " groups") + "</option>"; }).join("");
    b.innerHTML = '<td colspan="2"><div id="dv-tm">' +
      '<div class="r"><input type="text" id="dv-tm-name" class="clients" maxlength="24" autocomplete="off" placeholder="Team name"></div>' +
      '<div class="r"><label>Players <select id="dv-tm-pl">' + pOpts + '</select></label>' +
      '<label>Groups <select id="dv-tm-gr">' + gOpts + '</select></label>' +
      '<label id="dv-tm-minewrap" style="display:none">My group <select id="dv-tm-mine"></select></label></div>' +
      '<div class="r"><button class="mybutton" id="dv-tm-gen">Generate team</button>' +
      '<button class="mybutton" id="dv-tm-quick" title="New team, random key, 5 players">Quick team (random · 5)</button></div>' +
      '<div class="res" id="dv-tm-res"></div>' +
      '<div class="r"><button class="mygb" id="dv-tm-cp">Copy my invite</button></div>' +
      '<div class="r"><input type="text" id="dv-tm-inv" class="clients" autocomplete="off" placeholder="Join: paste invite (Team ID + Auth Key)">' +
      '<button class="mybutton" id="dv-tm-join">Join</button></div>' +
      '<div class="r"><input type="text" id="dv-tm-ren" class="clients" maxlength="24" autocomplete="off" placeholder="Rename team"><button class="mygb" id="dv-tm-ren-go">Rename</button></div>' +
      '<div class="st" id="dv-tm-st"></div><div class="st" id="dv-tm-groups"></div>' +
      '<details><summary>Team server</summary><div class="r"><input type="text" id="dv-tm-srv" class="clients" autocomplete="off" placeholder="https://your-server">' +
      '<button class="mygb" id="dv-tm-srvsave">Save</button></div></details></div></td>';
    tb.insertBefore(h, first ? first.nextSibling : null);
    tb.insertBefore(b, h.nextSibling);
    var style = document.createElement("style"); style.textContent = CSS; h.appendChild(style);
    $("dv-tm-srv").value = base();
    var nk = ""; try { nk = (ls("nick") || "").slice(0, 24); } catch (e) {}
    var cm = getMeta(); $("dv-tm-name").value = (cm && cm.n) || nk;

    function groupsChanged() {
      var n = +$("dv-tm-gr").value, sel = $("dv-tm-mine"), keep = sel.value;
      sel.innerHTML = ""; for (var i = 0; i < n; i++) { var o = document.createElement("option"); o.value = o.textContent = GL[i]; sel.appendChild(o); }
      if (keep && keep.charCodeAt(0) - 65 < n) sel.value = keep;
      $("dv-tm-minewrap").style.display = n > 1 ? "" : "none";
    }
    $("dv-tm-gr").onchange = groupsChanged; groupsChanged();

    $("dv-tm-gen").onclick = function () { generate($("dv-tm-name").value, +$("dv-tm-pl").value, +$("dv-tm-gr").value, $("dv-tm-mine").value); };
    $("dv-tm-quick").onclick = function () { generate($("dv-tm-name").value || nk || "Team", 5, 1, ""); };
    $("dv-tm-cp").onclick = function () {
      var c = current(); if (!c) { st("No david.io team yet — press Generate", "bad"); return; }
      var m = getMeta(); if (m && m.tid !== c.tid) m = null;
      copy(inviteFor(c.tid, c.auth, m && m.n, m && m.g, m && m.m), function (ok) { st(ok ? "Invite copied" : "Copy failed", ok ? "ok" : "bad"); });
    };
    $("dv-tm-join").onclick = function () {
      var tk = String($("dv-tm-inv").value || "").split(/[\s,|;]+/).filter(Boolean), tid = "", auth = "", srv = "", meta = null;
      tk.forEach(function (w) {
        if (/^(https?|wss?):\/\//i.test(w)) srv = cleanSrv(w);
        else if (/^meta:/i.test(w)) { try { meta = JSON.parse(decodeURIComponent(w.slice(5))); } catch (e) {} }
        else if (!tid && isOurs(w)) tid = w.toUpperCase();
        else if (!auth && w.length >= 16) auth = w;
      });
      if (!tid || !auth) { st("Invite needs Team ID (DVD-…) and Auth Key", "bad"); return; }
      var useSrv = srv || base(); if (srv) ls("dv_team_srv", srv);
      $("dv-tm-st").setAttribute("data-busy", "1"); st("Joining…");
      req("GET", useSrv + "/info?tid=" + encodeURIComponent(tid) + "&auth=" + encodeURIComponent(auth), function (err, d) {
        $("dv-tm-st").removeAttribute("data-busy");
        if (!err && d) meta = { n: d.name, g: d.group, m: d.max };   // server is the source of truth for name / group
        if (!apply(tid, auth, srv, labelOf(meta && meta.n, meta && meta.g))) { st("Open this from the settings panel", "bad"); return; }
        if (meta) setMeta(tid, meta.n, meta.g, meta.m); else setMeta(tid, "", "", 0);
        $("dv-tm-inv").value = ""; $("dv-tm-srv").value = base();
        st("Joined " + (meta ? describe(meta) : tid), "ok");
      });
    };
    $("dv-tm-ren-go").onclick = function () {
      var c = current(); if (!c) { st("No david.io team yet — press Generate", "bad"); return; }
      var nn = String($("dv-tm-ren").value || "").replace(/\s+/g, " ").trim().slice(0, 24);
      if (!nn) { st("Type the new team name first", "bad"); return; }
      req("POST", base() + "/rename?tid=" + encodeURIComponent(c.tid) + "&auth=" + encodeURIComponent(c.auth) + "&name=" + encodeURIComponent(nn), function (err, d) {
        if (err || !d || !d.ok) { st("Rename failed (update server.js to the new version?)", "bad"); return; }
        var m = getMeta(); var g = m && m.tid === c.tid ? m.g : (d.group || "");
        setMeta(c.tid, d.name, g, m && m.m); $("dv-tm-ren").value = ""; $("dv-tm-name").value = d.name;
        apply(c.tid, c.auth, "", labelOf(d.name, g)); st("Renamed to " + labelOf(d.name, g) + (d.groups > 1 ? " · all " + d.groups + " groups updated" : ""), "ok");
      });
    };
    $("dv-tm-srvsave").onclick = function () {
      var u = cleanSrv($("dv-tm-srv").value);
      if (!u) { st("Server URL must start with http:// or https://", "bad"); return; }
      ls("dv_team_srv", u); st("Server saved", "ok");
    };
    refreshState();
  }

  var tick = null;
  function watch() {
    var sp = $("settpage");
    if (sp && !$("dv-tm-sec")) build(sp);
    if (sp) { if (!tick) tick = setInterval(function () { if (!$("settpage")) { clearInterval(tick); tick = null; return; } refreshState(); }, 3000); }
  }
  function start() {
    if (!document.body) { document.addEventListener("DOMContentLoaded", start, { once: true }); return; }
    // settings panel is prepended to <body> each time it opens -> watch direct children only (cheap)
    new MutationObserver(watch).observe(document.body, { childList: true });
    watch();
  }
  start();
})();
