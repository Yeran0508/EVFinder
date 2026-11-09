const state = {
  query: { city: "Chicago", connectorType: "", sort: "power", order: "desc", limit: 12 },
  nextCursor: null,
  activeStationId: null,
  loading: false
};

const elements = {
  form: document.querySelector("#search-form"),
  city: document.querySelector("#city"),
  connector: document.querySelector("#connector"),
  sort: document.querySelector("#sort"),
  grid: document.querySelector("#station-grid"),
  summary: document.querySelector("#result-summary"),
  notice: document.querySelector("#notice"),
  loadMore: document.querySelector("#load-more"),
  drawer: document.querySelector("#station-drawer"),
  drawerContent: document.querySelector("#drawer-content"),
  backdrop: document.querySelector("#drawer-backdrop"),
  closeDrawer: document.querySelector("#close-drawer"),
  apiStatus: document.querySelector("#api-status"),
  loadingTemplate: document.querySelector("#loading-template")
};

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers || {}) }
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.error || `Request failed (${response.status})`);
  return { body, status: response.status };
}

function showNotice(message) {
  elements.notice.textContent = message;
  elements.notice.classList.toggle("hidden", !message);
}

function showSkeletons() {
  elements.grid.replaceChildren();
  for (let i = 0; i < 6; i += 1) {
    elements.grid.append(elements.loadingTemplate.content.cloneNode(true));
  }
}

function ratingLabel(station) {
  if (station.averageRating == null) return "New · No reviews";
  return `★ ${Number(station.averageRating).toFixed(1)} · ${station.reviewCount} review${station.reviewCount === 1 ? "" : "s"}`;
}

function stationCard(station) {
  const article = document.createElement("article");
  article.className = "station-card";
  const connectors = station.connectorTypes.length
    ? station.connectorTypes.map(type => `<span class="connector-chip">${escapeHtml(type)}</span>`).join("")
    : '<span class="connector-chip">Connector unknown</span>';
  article.innerHTML = `
    <div class="card-topline">
      <span class="power">${station.maxPowerKw == null ? "Power unknown" : `${escapeHtml(station.maxPowerKw)} kW`}</span>
      <span class="rating">${escapeHtml(ratingLabel(station))}</span>
    </div>
    <h3>${escapeHtml(station.name)}</h3>
    <p class="station-address">${escapeHtml(station.address)}</p>
    <div class="connector-list">${connectors}</div>
    <button class="card-action" type="button">View station</button>`;
  article.querySelector("button").addEventListener("click", () => openStation(station.id));
  return article;
}

function buildSearchUrl(cursor = null) {
  const params = new URLSearchParams({ city: state.query.city, limit: String(state.query.limit) });
  if (state.query.connectorType) params.set("connectorType", state.query.connectorType);
  if (state.query.sort) {
    params.set("sort", state.query.sort);
    params.set("order", state.query.order);
  }
  if (cursor) params.set("cursor", cursor);
  return `/api/v1/stations?${params}`;
}

async function searchStations({ append = false } = {}) {
  if (state.loading) return;
  state.loading = true;
  showNotice("");
  elements.loadMore.disabled = true;
  if (!append) {
    showSkeletons();
    elements.summary.textContent = `Searching ${state.query.city}…`;
  }

  try {
    const { body } = await api(buildSearchUrl(append ? state.nextCursor : null));
    if (!append) elements.grid.replaceChildren();
    body.items.forEach(station => elements.grid.append(stationCard(station)));
    state.nextCursor = body.nextCursor;
    elements.loadMore.classList.toggle("hidden", !state.nextCursor);
    const count = elements.grid.querySelectorAll(".station-card:not(.skeleton-card)").length;
    elements.summary.textContent = body.items.length
      ? `${count} station${count === 1 ? "" : "s"} shown${state.nextCursor ? " · More available" : ""}`
      : "No matching stations";
  } catch (error) {
    if (!append) elements.grid.replaceChildren();
    showNotice(error.message);
    elements.summary.textContent = "Search unavailable";
  } finally {
    state.loading = false;
    elements.loadMore.disabled = false;
  }
}

function connectorDetails(connectors) {
  if (!connectors.length) return '<p class="empty-copy">Connector details are unavailable.</p>';
  return connectors.map(connector => `
    <div class="connector-detail">
      <strong>${escapeHtml(connector.type)}</strong>
      <span>${connector.count == null ? "Count unknown" : `${connector.count} connector${connector.count === 1 ? "" : "s"}`}<br>
      ${connector.maxPowerKw == null ? "Power unknown" : `${escapeHtml(connector.maxPowerKw)} kW max`}</span>
    </div>`).join("");
}

function reviewMarkup(reviews) {
  if (!reviews.length) return '<p class="empty-copy">No reviews yet. Be the first to share your experience.</p>';
  return reviews.map(review => `
    <article class="review">
      <div class="review-head"><span class="review-user">${escapeHtml(review.userId)}</span><span class="review-stars">${"★".repeat(review.rating)}${"☆".repeat(5 - review.rating)}</span></div>
      <p>${escapeHtml(review.comment || "No written comment")}</p>
      <time datetime="${escapeHtml(review.createdAt)}">${new Date(review.createdAt).toLocaleString()}</time>
    </article>`).join("");
}

async function openStation(stationId) {
  state.activeStationId = stationId;
  elements.drawerContent.innerHTML = '<div class="drawer-hero"><p class="eyebrow">Loading station</p><h2 id="drawer-title">Please wait…</h2></div>';
  elements.drawer.classList.add("open");
  elements.drawer.setAttribute("aria-hidden", "false");
  elements.backdrop.classList.remove("hidden");
  document.body.style.overflow = "hidden";

  try {
    const [{ body: station }, { body: reviews }] = await Promise.all([
      api(`/api/v1/stations/${stationId}`),
      api(`/api/v1/stations/${stationId}/reviews?limit=20`)
    ]);
    elements.drawerContent.innerHTML = `
      <div class="drawer-hero">
        <p class="eyebrow">Station details</p>
        <h2 id="drawer-title">${escapeHtml(station.name)}</h2>
        <p class="drawer-meta">${escapeHtml(station.address)}</p>
      </div>
      <div class="metric-row">
        <div class="metric"><strong>${station.averageRating == null ? "—" : `${Number(station.averageRating).toFixed(1)} ★`}</strong><span>Average rating</span></div>
        <div class="metric"><strong>${station.reviewCount}</strong><span>Community reviews</span></div>
      </div>
      <section class="drawer-section"><h3>Charging options</h3>${connectorDetails(station.connectors)}</section>
      <section class="drawer-section"><h3>Recent reviews</h3><div id="review-list">${reviewMarkup(reviews.items)}</div></section>
      <section class="drawer-section">
        <h3>Share your experience</h3>
        <form class="review-form" id="review-form">
          <label>Your display name<input name="userId" maxlength="64" placeholder="e.g. yeran-demo" required></label>
          <label>Rating<select name="rating" required><option value="5">5 — Excellent</option><option value="4">4 — Good</option><option value="3">3 — Average</option><option value="2">2 — Poor</option><option value="1">1 — Bad</option></select></label>
          <label>Comment<textarea name="comment" maxlength="500" placeholder="What should other drivers know?"></textarea></label>
          <p class="form-message" id="form-message" role="status"></p>
          <button class="primary-button" type="submit">Submit review</button>
        </form>
      </section>`;
    document.querySelector("#review-form").addEventListener("submit", submitReview);
  } catch (error) {
    elements.drawerContent.innerHTML = `<div class="drawer-hero"><p class="eyebrow">Unable to load</p><h2 id="drawer-title">Station unavailable</h2><p>${escapeHtml(error.message)}</p></div>`;
  }
}

async function submitReview(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const button = form.querySelector("button");
  const message = form.querySelector("#form-message");
  const values = new FormData(form);
  button.disabled = true;
  message.textContent = "Saving your review…";
  try {
    const { status } = await api(`/api/v1/stations/${state.activeStationId}/reviews`, {
      method: "POST",
      body: JSON.stringify({
        userId: values.get("userId"),
        rating: Number(values.get("rating")),
        comment: values.get("comment")
      })
    });
    await openStation(state.activeStationId);
    const refreshedMessage = document.querySelector("#form-message");
    if (refreshedMessage) refreshedMessage.textContent = status === 201 ? "Review added. Thank you!" : "Your existing review was kept.";
    await searchStations();
  } catch (error) {
    message.textContent = error.message;
  } finally {
    button.disabled = false;
  }
}

function closeDrawer() {
  state.activeStationId = null;
  elements.drawer.classList.remove("open");
  elements.drawer.setAttribute("aria-hidden", "true");
  elements.backdrop.classList.add("hidden");
  document.body.style.overflow = "";
}

async function checkHealth() {
  try {
    await api("/api/v1/health");
    elements.apiStatus.className = "api-status online";
    elements.apiStatus.lastChild.textContent = " API online";
  } catch {
    elements.apiStatus.className = "api-status offline";
    elements.apiStatus.lastChild.textContent = " API offline";
  }
}

elements.form.addEventListener("submit", event => {
  event.preventDefault();
  state.query.city = elements.city.value.trim();
  state.query.connectorType = elements.connector.value;
  state.query.sort = elements.sort.value;
  state.nextCursor = null;
  searchStations();
});
elements.loadMore.addEventListener("click", () => searchStations({ append: true }));
elements.closeDrawer.addEventListener("click", closeDrawer);
elements.backdrop.addEventListener("click", closeDrawer);
document.addEventListener("keydown", event => { if (event.key === "Escape") closeDrawer(); });

checkHealth();
searchStations();
