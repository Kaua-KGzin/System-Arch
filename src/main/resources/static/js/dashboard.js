const POLL_INTERVAL_MS = 5000;

const grid = document.getElementById("systems");
const summary = document.getElementById("summary");
const emptyState = document.getElementById("empty-state");

async function fetchSystems() {
    const response = await fetch("/api/v1/systems");
    if (!response.ok) {
        throw new Error(`Failed to load systems: ${response.status}`);
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

function renderCard(system) {
    const card = document.createElement("article");
    card.className = "card";

    const tags = (system.tags || [])
        .map((tag) => `<span class="tag">${tag}</span>`)
        .join("");

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

function renderSummary(systems) {
    const counts = { UP: 0, DOWN: 0, UNKNOWN: 0 };
    systems.forEach((s) => counts[s.status]++);
    summary.innerHTML = `
        <span><strong>${systems.length}</strong> sistemas</span>
        <span><strong>${counts.UP}</strong> online</span>
        <span><strong>${counts.DOWN}</strong> offline</span>
        <span><strong>${counts.UNKNOWN}</strong> desconhecido</span>
    `;
}

async function refresh() {
    try {
        const systems = await fetchSystems();
        grid.innerHTML = "";
        emptyState.hidden = systems.length > 0;
        renderSummary(systems);
        systems.forEach((system) => grid.appendChild(renderCard(system)));
    } catch (err) {
        console.error(err);
    }
}

refresh();
setInterval(refresh, POLL_INTERVAL_MS);
