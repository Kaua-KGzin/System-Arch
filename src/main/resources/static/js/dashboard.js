const POLL_INTERVAL_MS = 5000;

const state = {
    systems: [],
    connections: [],
    events: [],
    architecture: [],
    filters: { q: "", tag: "", status: "" },
};

const els = {
    stats: document.getElementById("stats"),
    grid: document.getElementById("systems"),
    emptyState: document.getElementById("empty-state"),
    search: document.getElementById("search"),
    tagFilter: document.getElementById("tag-filter"),
    statusFilter: document.getElementById("status-filter"),
    graph: document.getElementById("graph"),
    events: document.getElementById("events"),
    architecture: document.getElementById("architecture"),
    architectureEmpty: document.getElementById("architecture-empty"),
    eventsEmpty: document.getElementById("events-empty"),
    mobileToggle: document.getElementById("mobile-toggle"),
    mobilePanel: document.getElementById("mobile-panel"),
    mobileQr: document.getElementById("mobile-qr"),
    mobileUrl: document.getElementById("mobile-url"),
};

// Everything below comes from systems that registered themselves (the Hub may be
// open), so it is untrusted: escape every value and only link http(s) URLs.
function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, (ch) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[ch]);
}

function safeUrl(url) {
    return /^https?:\/\//i.test(url ?? "") ? url : null;
}

async function fetchJson(path) {
    const response = await fetch(path);
    if (!response.ok) {
        throw new Error(`GET ${path} failed: ${response.status}`);
    }
    return response.json();
}

function formatRelativeTime(isoTimestamp) {
    if (!isoTimestamp) {
        return "nunca";
    }
    const seconds = Math.round((Date.now() - new Date(isoTimestamp).getTime()) / 1000);
    if (seconds < 5) return "agora mesmo";
    if (seconds < 60) return `há ${seconds}s`;
    if (seconds < 3600) return `há ${Math.round(seconds / 60)}min`;
    return `há ${Math.round(seconds / 3600)}h`;
}

// ---- Tabs -----------------------------------------------------------

document.querySelectorAll(".tab-button").forEach((button) => {
    button.addEventListener("click", () => {
        document.querySelectorAll(".tab-button").forEach((b) => b.classList.remove("active"));
        document.querySelectorAll(".tab-panel").forEach((p) => p.classList.remove("active"));
        button.classList.add("active");
        document.getElementById(`tab-${button.dataset.tab}`).classList.add("active");
        if (button.dataset.tab === "graph") {
            renderGraph();
        }
    });
});

// ---- Stats ------------------------------------------------------------

function renderStats(stats) {
    els.stats.innerHTML = `
        <div class="stat-tile"><div class="value">${stats.total}</div><div class="label">Sistemas</div></div>
        <div class="stat-tile up"><div class="value">${stats.up}</div><div class="label">Online</div></div>
        <div class="stat-tile down"><div class="value">${stats.down}</div><div class="label">Offline</div></div>
        <div class="stat-tile unknown"><div class="value">${stats.unknown}</div><div class="label">Desconhecido</div></div>
        <div class="stat-tile"><div class="value">${stats.connections}</div><div class="label">Conexões</div></div>
    `;
}

// ---- Systems grid + filters --------------------------------------------

function populateTagOptions(systems) {
    const currentValue = els.tagFilter.value;
    const tags = [...new Set(systems.flatMap((s) => s.tags || []))].sort();
    els.tagFilter.innerHTML = '<option value="">Todas as tags</option>' +
        tags.map((tag) => `<option value="${escapeHtml(tag)}">${escapeHtml(tag)}</option>`).join("");
    els.tagFilter.value = tags.includes(currentValue) ? currentValue : "";
}

function applyFilters(systems) {
    const { q, tag, status } = state.filters;
    return systems.filter((system) => {
        const matchesQuery = !q ||
            system.id.toLowerCase().includes(q) ||
            system.name.toLowerCase().includes(q) ||
            (system.description || "").toLowerCase().includes(q);
        const matchesTag = !tag || (system.tags || []).includes(tag);
        const matchesStatus = !status || system.status === status;
        return matchesQuery && matchesTag && matchesStatus;
    });
}

function plural(count, one, many) {
    const n = Number(count) || 0;
    return `${n} ${n === 1 ? one : many}`;
}

function tagHtml(tag) {
    const role = tag.startsWith("role:") ? ` role role-${escapeHtml(tag.slice(5))}` : "";
    return `<span class="tag${role}">${escapeHtml(tag)}</span>`;
}

function sparkline(trend) {
    if (!trend || trend.length < 2) return "";
    const width = 80;
    const height = 20;
    const step = width / (trend.length - 1);
    // Scale to the trend's own range (min 10 points) so small moves stay visible.
    const min = Math.min(...trend);
    const range = Math.max(Math.max(...trend) - min, 10);
    const y = (score) => (height - 2 - ((score - min) / range) * (height - 4)).toFixed(1);
    const points = trend.map((score, i) => `${(i * step).toFixed(1)},${y(score)}`);
    return `<svg class="sparkline" viewBox="0 0 ${width} ${height}" width="${width}" height="${height}" aria-hidden="true">
        <polyline points="${points.join(" ")}" fill="none" stroke="currentColor" stroke-width="1.5"/></svg>`;
}

function architectureHtml(architecture) {
    if (!architecture || !architecture.latest) return "";
    const latest = architecture.latest;
    const grade = escapeHtml((latest.grade || "?").toLowerCase());
    const delta = architecture.delta == null || architecture.delta === 0 ? ""
        : `<span class="delta ${architecture.delta > 0 ? "up" : "down"}">${architecture.delta > 0 ? "▲" : "▼"}${Math.abs(architecture.delta)}</span>`;
    const report = safeUrl(latest.reportUrl);
    const label = `${Number(latest.score)}/100 ${escapeHtml(latest.grade || "")}`;
    return `
        <div class="architecture">
            <span class="grade grade-${grade}" title="Score de arquitetura (ArchMAP)">${report
                ? `<a href="${escapeHtml(report)}" target="_blank" rel="noopener">${label}</a>` : label}</span>
            ${delta}${sparkline(architecture.trend)}
            <span class="architecture-meta">${plural(latest.files, "arquivo", "arquivos")} · ${plural(latest.cycles, "ciclo", "ciclos")} · ${formatRelativeTime(latest.analyzedAt)}</span>
        </div>`;
}

function renderCard(system) {
    const card = document.createElement("article");
    card.className = "card";

    const tags = (system.tags || []).map(tagHtml).join("");
    const url = safeUrl(system.baseUrl);
    const status = escapeHtml(system.status.toLowerCase());

    card.innerHTML = `
        <div class="card-header">
            <h2>${escapeHtml(system.name)}</h2>
            <span class="badge ${status}">${escapeHtml(system.status)}</span>
        </div>
        <p>${url ? `<a href="${escapeHtml(url)}" target="_blank" rel="noopener">${escapeHtml(url)}</a>` : escapeHtml(system.baseUrl)}</p>
        <p>${escapeHtml(system.description)}</p>
        ${architectureHtml(system.architecture)}
        <p>Último sinal: ${formatRelativeTime(system.lastSeen)}</p>
        <div class="tags">${tags}</div>
    `;
    return card;
}

function renderArchitecture() {
    els.architectureEmpty.hidden = state.architecture.length > 0;
    els.architecture.innerHTML = state.architecture.map((entry) => `
        <article class="card">
            <div class="card-header">
                <h2>${escapeHtml(entry.systemId)}</h2>
                <span class="badge ${entry.registered ? "up" : "unknown"}">${entry.registered ? "no Hub" : "só CI"}</span>
            </div>
            ${architectureHtml(entry.architecture)}
            <p class="hint">${escapeHtml(entry.architecture.latest.ref || "")} ${escapeHtml((entry.architecture.latest.commit || "").slice(0, 7))}</p>
        </article>
    `).join("");
}

function renderSystems() {
    const filtered = applyFilters(state.systems);
    els.grid.innerHTML = "";
    els.emptyState.hidden = filtered.length > 0;
    filtered.forEach((system) => els.grid.appendChild(renderCard(system)));
}

els.search.addEventListener("input", (e) => {
    state.filters.q = e.target.value.trim().toLowerCase();
    renderSystems();
});
els.tagFilter.addEventListener("change", (e) => {
    state.filters.tag = e.target.value;
    renderSystems();
});
els.statusFilter.addEventListener("change", (e) => {
    state.filters.status = e.target.value;
    renderSystems();
});

// ---- Connections graph --------------------------------------------------

function statusOf(id) {
    const system = state.systems.find((s) => s.id === id);
    return system ? system.status.toLowerCase() : "unknown";
}

function renderGraph() {
    const svgNs = "http://www.w3.org/2000/svg";
    const size = 600;
    const center = size / 2;
    const radius = size / 2 - 70;

    const nodeIds = [...new Set([
        ...state.systems.map((s) => s.id),
        ...state.connections.flatMap((c) => [c.from, c.to]),
    ])];

    const positions = {};
    nodeIds.forEach((id, index) => {
        const angle = (2 * Math.PI * index) / Math.max(nodeIds.length, 1);
        positions[id] = {
            x: center + radius * Math.cos(angle),
            y: center + radius * Math.sin(angle),
        };
    });

    els.graph.innerHTML = "";
    els.graph.setAttribute("viewBox", `0 0 ${size} ${size}`);

    const defs = document.createElementNS(svgNs, "defs");
    defs.innerHTML = `
        <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M0,0 L10,5 L0,10 z" fill="currentColor" style="color: var(--muted)"></path>
        </marker>
    `;
    els.graph.appendChild(defs);

    state.connections.forEach((edge) => {
        const from = positions[edge.from];
        const to = positions[edge.to];
        if (!from || !to) return;
        const line = document.createElementNS(svgNs, "line");
        line.setAttribute("x1", from.x);
        line.setAttribute("y1", from.y);
        line.setAttribute("x2", to.x);
        line.setAttribute("y2", to.y);
        line.setAttribute("class", `edge${edge.toKnown ? "" : " unknown-target"}`);
        els.graph.appendChild(line);
    });

    nodeIds.forEach((id) => {
        const pos = positions[id];
        const group = document.createElementNS(svgNs, "g");
        group.setAttribute("class", `node ${statusOf(id)}`);
        group.setAttribute("transform", `translate(${pos.x}, ${pos.y})`);

        const circle = document.createElementNS(svgNs, "circle");
        circle.setAttribute("r", "14");
        group.appendChild(circle);

        const label = document.createElementNS(svgNs, "text");
        label.setAttribute("text-anchor", "middle");
        label.setAttribute("y", "-20");
        label.textContent = id;
        group.appendChild(label);

        els.graph.appendChild(group);
    });
}

// ---- Activity feed -----------------------------------------------------

const EVENT_LABELS = {
    REGISTERED: "Registro",
    DEREGISTERED: "Remoção",
    WENT_UP: "Ficou online",
    WENT_DOWN: "Ficou offline",
    ARCHITECTURE_REPORTED: "Arquitetura",
};

function renderEvents() {
    els.eventsEmpty.hidden = state.events.length > 0;
    els.events.innerHTML = state.events.map((event) => `
        <li class="event ${escapeHtml(event.type.toLowerCase())}">
            <span class="dot"></span>
            <span class="message"><strong>${escapeHtml(EVENT_LABELS[event.type] ?? event.type)}</strong> · ${escapeHtml(event.message)}</span>
            <span class="time">${formatRelativeTime(event.occurredAt)}</span>
        </li>
    `).join("");
}

// ---- Mobile access (QR code) ---------------------------------------------

async function initMobileAccess() {
    try {
        const network = await fetchJson("/api/v1/network");
        if (!network.primaryUrl) {
            return;
        }
        els.mobileQr.src = "/api/v1/network/qr.svg";
        els.mobileUrl.textContent = network.primaryUrl;
        els.mobileUrl.href = network.primaryUrl;
        els.mobileToggle.hidden = false;
        els.mobileToggle.addEventListener("click", () => {
            els.mobilePanel.hidden = !els.mobilePanel.hidden;
        });
    } catch (err) {
        console.error(err);
    }
}

// ---- Polling loop --------------------------------------------------------

async function refresh() {
    try {
        const [systems, connections, events, stats, architecture] = await Promise.all([
            fetchJson("/api/v1/systems"),
            fetchJson("/api/v1/connections"),
            fetchJson("/api/v1/events?limit=30"),
            fetchJson("/api/v1/stats"),
            fetchJson("/api/v1/architecture"),
        ]);

        state.systems = systems;
        state.connections = connections;
        state.events = events;
        state.architecture = architecture;

        renderStats(stats);
        populateTagOptions(systems);
        renderSystems();
        renderEvents();
        renderArchitecture();
        if (document.getElementById("tab-graph").classList.contains("active")) {
            renderGraph();
        }
    } catch (err) {
        console.error(err);
    }
}

refresh();
setInterval(refresh, POLL_INTERVAL_MS);
initMobileAccess();
