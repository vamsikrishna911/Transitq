"use strict";
const $ = (s) => document.querySelector(s);
const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const COLORS = ["#1d4ed8", "#16a34a", "#ea580c", "#dc2626", "#9333ea", "#0891b2"];
const ALT_COLORS = ["#1d4ed8", "#f59e0b", "#10b981"];
let stops = [], routes = [], lastRoutes = [], map, netLayer, pathLayer, pending = 0;

async function api(path, opts = {}) {
  pending++; $("#loading").hidden = false;
  try {
    const res = await fetch(path, { headers: { "Content-Type": "application/json" }, ...opts });
    if (res.status === 204) return null;
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || "Request failed (" + res.status + ")");
    return data;
  } finally { if (--pending === 0) $("#loading").hidden = true; }
}
function showError(msg, info) {
  const el = $("#error"); el.textContent = msg; el.className = "alert" + (info ? " info" : ""); el.hidden = false;
  clearTimeout(showError.t); showError.t = setTimeout(() => (el.hidden = true), 7000);
}
const guard = (fn) => async (...a) => { try { $("#error").hidden = true; await fn(...a); } catch (e) { showError(e.message); } };

// ---------- navigation ----------
function go(page) {
  document.querySelectorAll(".page").forEach((p) => p.classList.toggle("active", p.id === page));
  document.querySelectorAll("nav button").forEach((b) => b.classList.toggle("active", b.dataset.page === page));
  $("#sidebar").classList.remove("open");
  if (page === "dashboard") guard(loadDashboard)();
  if (page === "analytics") guard(loadAnalytics)();
  if (page === "stops") guard(loadStops)();
  if (page === "map") guard(showMap)();
}
document.querySelectorAll("nav button").forEach((b) => b.addEventListener("click", () => go(b.dataset.page)));
$("#menuBtn").addEventListener("click", () => $("#sidebar").classList.toggle("open"));

// ---------- helpers ----------
const card = (v, l) => `<div class="card"><div class="v">${esc(v)}</div><div class="l">${esc(l)}</div></div>`;
const bars = (rows, max) => rows.length ? rows.map((r) => `<div class="bar"><span class="n">${esc(r.n)}</span><div class="t"><div class="f" style="width:${Math.max(4, (r.v / (max || 1)) * 100)}%"></div></div><b>${esc(r.v)}</b></div>`).join("") : "<p>No data yet.</p>";
async function refreshRefs() { [stops, routes] = await Promise.all([api("/api/stops"), api("/api/routes")]); fillSelects(); }
function fillSelects() {
  for (const id of ["#src", "#dst"]) {
    const cur = $(id).value;
    $(id).innerHTML = '<option value="">Select a stop…</option>' + stops.map((s) => `<option value="${s.id}">${esc(s.name)}</option>`).join("");
    $(id).value = cur;
  }
}

// ---------- dashboard ----------
async function loadDashboard() {
  const a = await api("/api/analytics");
  $("#dashCards").innerHTML = card(a.totalStops, "Bus stops") + card(a.totalRoutes, "Routes") + card(a.totalConnections, "Connections") + card(a.connectedComponents, "Connected components");
  const max = Math.max(...a.popularStops.map((s) => s.connections), 1);
  $("#dashPopular").innerHTML = bars(a.popularStops.map((s) => ({ n: s.name, v: s.connections })), max);
  $("#dashStats").innerHTML = [
    ["Most connected stop", a.mostConnected ? a.mostConnected.name : "–"],
    ["Avg connection distance", a.avgConnectionDistanceKm + " km"],
    ["Avg journey distance", a.avgJourneyDistanceKm + " km"],
    ["Avg journey time", a.avgJourneyTimeMin + " min"],
    ["Largest connected area", a.largestComponentSize + " stops"],
  ].map(([k, v]) => `<div class="kv"><span>${esc(k)}</span><b>${esc(v)}</b></div>`).join("");
  $("#dashRecent").innerHTML = a.recentSearches.length
    ? "<tr><th>From</th><th>To</th><th>Mode</th><th>Distance</th><th>Time</th><th>Fare</th><th>When</th></tr>" +
      a.recentSearches.map((j) => `<tr><td>${esc(j.source)}</td><td>${esc(j.destination)}</td><td>${esc(j.mode)}</td><td>${j.distanceKm} km</td><td>${j.timeMin} min</td><td>${j.fare}</td><td>${esc(j.createdAt)}</td></tr>`).join("")
    : "<tr><td>No searches yet. Try the Route Planner.</td></tr>";
}

// ---------- planner ----------
function routeCard(r, title, color, idx) {
  const names = r.stops.map((s) => `<span class="chip">${esc(s.name)}</span>`).join('<span class="arrow">→</span>');
  return `<div class="panel route ${idx != null ? "clickable" : ""}" style="border-left-color:${color}" ${idx != null ? `data-alt="${idx}"` : ""}>
    <h2>${esc(title)}</h2>
    <div class="metrics"><div><b>${r.distanceKm} km</b>Distance</div><div><b>${r.timeMin} min</b>Travel time</div><div><b>${r.fare}</b>Estimated fare</div><div><b>${r.stopCount}</b>Stops</div><div><b>${esc(r.lines.join(" → ") || "–")}</b>Bus lines</div></div>
    <div class="chips">${names}</div></div>`;
}
const find = guard(async () => {
  const s = $("#src").value, d = $("#dst").value, m = $("#mode").value;
  if (!s || !d) return showError("Please choose both a source and a destination stop.");
  if (s === d) return showError("Source and destination must be different stops.");
  const [best, alts] = await Promise.all([
    api(`/api/route/find?source=${s}&destination=${d}&mode=${m}`),
    api(`/api/route/alternatives?source=${s}&destination=${d}&mode=${m}`),
  ]);
  if (!best.found) { $("#result").innerHTML = ""; lastRoutes = []; return showError(best.message, true); }
  lastRoutes = alts.found ? alts.routes : [best];
  let html = routeCard(best, "Recommended route", ALT_COLORS[0]) + '<p><button class="primary" id="viewMap">View on map</button></p>';
  const others = lastRoutes.slice(1);
  if (others.length) html += "<h2>Alternative routes</h2>" + others.map((r, i) => routeCard(r, "Alternative " + (i + 1), ALT_COLORS[i + 1], i + 1)).join("");
  else html += "<p>No other reasonable alternative route exists for this journey.</p>";
  $("#result").innerHTML = html;
  $("#viewMap").addEventListener("click", () => go("map"));
  $("#result").querySelectorAll("[data-alt]").forEach((el) => el.addEventListener("click", () => go("map")));
});
$("#findBtn").addEventListener("click", find);
$("#clearBtn").addEventListener("click", () => { $("#src").value = ""; $("#dst").value = ""; $("#mode").value = "TIME"; $("#result").innerHTML = ""; lastRoutes = []; $("#error").hidden = true; });

// ---------- map ----------
const showMap = async () => {
  if (!stops.length) await refreshRefs();
  if (!map) {
    map = L.map("leaflet");
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", { maxZoom: 19, attribution: "&copy; OpenStreetMap contributors" }).addTo(map);
    netLayer = L.layerGroup().addTo(map); pathLayer = L.layerGroup().addTo(map);
  }
  map.invalidateSize();
  netLayer.clearLayers(); pathLayer.clearLayers();
  const byId = Object.fromEntries(stops.map((s) => [s.id, s]));
  const hasPath = lastRoutes.length > 0;
  routes.forEach((r, i) => {
    const pts = r.stopIds.filter((id) => byId[id]).map((id) => [byId[id].latitude, byId[id].longitude]);
    L.polyline(pts, { color: COLORS[i % COLORS.length], weight: 3, opacity: hasPath ? 0.25 : 0.8 }).bindTooltip(r.busCode + " – " + r.name).addTo(netLayer);
  });
  stops.forEach((s) => L.circleMarker([s.latitude, s.longitude], { radius: 5, color: "#1e3a8a", fillColor: "#fff", fillOpacity: 1, weight: 2 })
    .bindPopup(`<b>${esc(s.name)}</b><br>${esc(s.description || "")}`).addTo(netLayer));
  let bounds = L.latLngBounds(stops.map((s) => [s.latitude, s.longitude]));
  if (hasPath) {
    [...lastRoutes].reverse().forEach((r, ri) => {
      const i = lastRoutes.length - 1 - ri;
      L.polyline(r.stops.map((s) => [s.latitude, s.longitude]), { color: ALT_COLORS[i], weight: i === 0 ? 7 : 4, dashArray: i === 0 ? null : "8 6", opacity: 0.95 }).addTo(pathLayer);
    });
    const p = lastRoutes[0].stops, a = p[0], b = p[p.length - 1];
    L.circleMarker([a.latitude, a.longitude], { radius: 10, color: "#166534", fillColor: "#22c55e", fillOpacity: 1 }).bindTooltip("Source: " + a.name, { permanent: true }).addTo(pathLayer);
    L.circleMarker([b.latitude, b.longitude], { radius: 10, color: "#991b1b", fillColor: "#ef4444", fillOpacity: 1 }).bindTooltip("Destination: " + b.name, { permanent: true }).addTo(pathLayer);
    bounds = L.latLngBounds(p.map((s) => [s.latitude, s.longitude]));
  }
  map.fitBounds(bounds.pad(0.2));
  $("#legend").innerHTML = hasPath
    ? lastRoutes.map((r, i) => `<span><i style="background:${ALT_COLORS[i]}"></i>${i === 0 ? "Recommended" : "Alternative " + i} (${r.distanceKm} km, ${r.timeMin} min)</span>`).join("")
    : routes.map((r, i) => `<span><i style="background:${COLORS[i % COLORS.length]}"></i>${esc(r.busCode)} ${esc(r.name)}</span>`).join("") + "<span>Search a route to highlight it here.</span>";
};

// ---------- stops CRUD ----------
function renderStops() {
  const q = $("#stopSearch").value.trim().toLowerCase();
  const rows = stops.filter((s) => s.name.toLowerCase().includes(q) || (s.description || "").toLowerCase().includes(q));
  $("#stopTable").innerHTML = "<tr><th>ID</th><th>Name</th><th>Latitude</th><th>Longitude</th><th>Description</th><th></th></tr>" +
    (rows.map((s) => `<tr><td>${s.id}</td><td>${esc(s.name)}</td><td>${s.latitude}</td><td>${s.longitude}</td><td>${esc(s.description)}</td>
      <td class="actions"><button class="sm" data-act="view" data-id="${s.id}">View</button><button class="sm" data-act="edit" data-id="${s.id}">Edit</button><button class="sm danger" data-act="del" data-id="${s.id}">Delete</button></td></tr>`).join("")
      || '<tr><td colspan="6">No stops match your search.</td></tr>');
}
const loadStops = async () => { await refreshRefs(); renderStops(); };
$("#stopSearch").addEventListener("input", renderStops);
$("#stopTable").addEventListener("click", guard(async (e) => {
  const b = e.target.closest("button[data-act]"); if (!b) return;
  const s = stops.find((x) => x.id === +b.dataset.id);
  if (b.dataset.act === "edit") openForm(s);
  else if (b.dataset.act === "view") {
    const lines = routes.filter((r) => r.stopIds.includes(s.id)).map((r) => r.busCode + " " + r.name);
    $("#detailBody").innerHTML = `<h2>${esc(s.name)}</h2><div class="kv"><span>ID</span><b>${s.id}</b></div><div class="kv"><span>Coordinates</span><b>${s.latitude}, ${s.longitude}</b></div><div class="kv"><span>Description</span><b>${esc(s.description || "–")}</b></div><div class="kv"><span>Served by</span><b>${esc(lines.join(", ") || "No routes")}</b></div>`;
    $("#detailDialog").showModal();
  } else if (confirm(`Delete "${s.name}"? Its connections will be removed too.`)) {
    await api("/api/stops/" + s.id, { method: "DELETE" }); await loadStops();
  }
}));
function openForm(s) {
  $("#dlgTitle").textContent = s ? "Edit Stop" : "Add Stop";
  $("#stopId").value = s ? s.id : ""; $("#sName").value = s ? s.name : ""; $("#sLat").value = s ? s.latitude : "";
  $("#sLng").value = s ? s.longitude : ""; $("#sDesc").value = s ? s.description || "" : "";
  $("#stopDialog").showModal();
}
$("#addStopBtn").addEventListener("click", () => openForm(null));
$("#cancelStop").addEventListener("click", () => $("#stopDialog").close());
$("#closeDetail").addEventListener("click", () => $("#detailDialog").close());
$("#stopForm").addEventListener("submit", (e) => {
  e.preventDefault();
  guard(async () => {
    const id = $("#stopId").value;
    const body = JSON.stringify({ name: $("#sName").value, latitude: +$("#sLat").value, longitude: +$("#sLng").value, description: $("#sDesc").value });
    await api(id ? "/api/stops/" + id : "/api/stops", { method: id ? "PUT" : "POST", body });
    $("#stopDialog").close(); await loadStops();
  })();
});

// ---------- analytics ----------
async function loadAnalytics() {
  const [a, c] = await Promise.all([api("/api/analytics"), api("/api/analytics/connectivity")]);
  $("#anCards").innerHTML = card(a.avgConnectionDistanceKm + " km", "Avg connection distance") + card(a.avgJourneyDistanceKm + " km", "Avg journey distance") +
    card(a.avgJourneyTimeMin + " min", "Avg journey time") + card(c.componentCount, "Connected components") + card(a.largestComponentSize, "Largest reachable area (stops)");
  $("#anDegree").innerHTML = bars(a.popularStops.map((s) => ({ n: s.name, v: s.connections })), Math.max(...a.popularStops.map((s) => s.connections), 1));
  $("#anRoutes").innerHTML = a.popularRoutes.length ? bars(a.popularRoutes.map((r) => ({ n: r.line, v: r.searches })), Math.max(...a.popularRoutes.map((r) => r.searches), 1)) : "<p>Search some journeys to see popular routes.</p>";
  $("#anComponents").innerHTML = (c.fullyConnected ? "<p>✅ The network is fully connected.</p>" : "<p>⚠️ The network has separate parts.</p>") +
    c.components.map((x, i) => `<div class="kv"><span>Component ${i + 1} (${x.size})</span><span>${esc(x.stops.join(", "))}</span></div>`).join("");
  $("#anReach").innerHTML = bars(c.reachableByStop.map((r) => ({ n: r.name, v: r.reachable })), Math.max(...c.reachableByStop.map((r) => r.reachable), 1));
}

guard(async () => { await refreshRefs(); await loadDashboard(); })();
