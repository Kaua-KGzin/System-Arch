const POLL_INTERVAL_MS = 5000;

const state = {
    systems: [],
    connections: [],
    events: [],
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
    eventsEmpty: document.getElementById("events-empty"),
    mobileToggle: document.getElementById("mobile-toggle"),
    mobilePanel: document.getElementById("mobile-panel"),
    mobileQr: document.getElementById("mobile-qr"),
    mobileUrl: document.getElementById("mobile-url"),
};

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
        tags.map((tag) => `<option value="${tag}">${tag}</option>`).join("");
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

function renderCard(system) {
    const card = document.createElement("article");
    card.className = "card";

    const tags = (system.tags || []).map((tag) => `<span class="tag">${tag}</span>`).join("");

    card.innerHTML = `
        <div class="card-header">
            <h2>${system.name}</h2>
            <span class="badge ${system.status.toLowerCase()}">${system.status}</span>
        </div>
        <p><a href="${system.baseUrl}" target="_blank" rel="noopener">${system.baseUrl}</a></p>
        <p>${system.description ?? ""}</p>
        <p>Último sinal: ${formatRelativeTime(system.lastSeen)}</p>
        <div class="tags">${tags}</div>
    `;
    return card;
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
};

function renderEvents() {
    els.eventsEmpty.hidden = state.events.length > 0;
    els.events.innerHTML = state.events.map((event) => `
        <li class="event ${event.type.toLowerCase()}">
            <span class="dot"></span>
            <span class="message"><strong>${EVENT_LABELS[event.type] ?? event.type}</strong> · ${event.message}</span>
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
        const [systems, connections, events, stats] = await Promise.all([
            fetchJson("/api/v1/systems"),
            fetchJson("/api/v1/connections"),
            fetchJson("/api/v1/events?limit=30"),
            fetchJson("/api/v1/stats"),
        ]);

        state.systems = systems;
        state.connections = connections;
        state.events = events;

        renderStats(stats);
        populateTagOptions(systems);
        renderSystems();
        renderEvents();
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
