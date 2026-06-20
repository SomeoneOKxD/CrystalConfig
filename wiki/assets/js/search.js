(function () {
  const form = document.querySelector(".site-search");
  const input = document.getElementById("wiki-search-input");
  const results = document.getElementById("wiki-search-results");

  if (!form || !input || !results) {
    return;
  }

  const indexUrl = form.getAttribute("data-search-index") || "/search.json";
  let pages = null;
  let activeIndex = -1;
  let lastQuery = "";

  const normalize = (value) => String(value || "").toLowerCase().replace(/[^a-z0-9_#+.\-/\s]/g, " ").replace(/\s+/g, " ").trim();
  const escapeHtml = (value) => String(value || "").replace(/[&<>'"]/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "'": "&#39;",
    '"': "&quot;"
  })[char]);

  function tokenize(query) {
    return normalize(query).split(" ").filter((token) => token.length > 1);
  }

  async function loadIndex() {
    if (pages) {
      return pages;
    }

    try {
      const response = await fetch(indexUrl, { cache: "force-cache" });
      if (!response.ok) {
        throw new Error(`Failed to load search index: ${response.status}`);
      }

      const data = await response.json();
      pages = data.map((page) => ({
        title: page.title || "Untitled",
        description: page.description || "",
        url: page.url || "#",
        content: page.content || "",
        titleSearch: normalize(page.title),
        descriptionSearch: normalize(page.description),
        contentSearch: normalize(page.content)
      }));
    } catch (error) {
      pages = [];
      results.hidden = false;
      results.innerHTML = `<div class="search-empty">Search is unavailable because the index could not be loaded.</div>`;
      // Keep the error visible to developers without breaking the page for users.
      console.warn(error);
    }

    return pages;
  }

  function scorePage(page, tokens, rawQuery) {
    let score = 0;

    if (page.titleSearch.includes(rawQuery)) {
      score += 60;
    }
    if (page.descriptionSearch.includes(rawQuery)) {
      score += 24;
    }
    if (page.contentSearch.includes(rawQuery)) {
      score += 8;
    }

    for (const token of tokens) {
      if (page.titleSearch.includes(token)) {
        score += 18;
      }
      if (page.descriptionSearch.includes(token)) {
        score += 8;
      }
      if (page.contentSearch.includes(token)) {
        score += 2;
      }
    }

    return score;
  }

  function makeSnippet(page, tokens) {
    const text = String(page.content || page.description || "").replace(/\s+/g, " ").trim();
    if (!text) {
      return page.description || "Open this page.";
    }

    const lower = text.toLowerCase();
    let index = -1;
    for (const token of tokens) {
      index = lower.indexOf(token.toLowerCase());
      if (index >= 0) {
        break;
      }
    }

    if (index < 0) {
      return text.slice(0, 150) + (text.length > 150 ? "…" : "");
    }

    const start = Math.max(0, index - 54);
    const end = Math.min(text.length, index + 132);
    return `${start > 0 ? "…" : ""}${text.slice(start, end)}${end < text.length ? "…" : ""}`;
  }

  function closeResults() {
    results.hidden = true;
    input.setAttribute("aria-expanded", "false");
    activeIndex = -1;
  }

  function openResults() {
    results.hidden = false;
    input.setAttribute("aria-expanded", "true");
  }

  function setActive(index) {
    const items = Array.from(results.querySelectorAll(".search-result"));
    if (!items.length) {
      activeIndex = -1;
      return;
    }

    activeIndex = Math.max(0, Math.min(index, items.length - 1));
    items.forEach((item, itemIndex) => {
      item.setAttribute("aria-selected", String(itemIndex === activeIndex));
      if (itemIndex === activeIndex) {
        item.scrollIntoView({ block: "nearest" });
      }
    });
  }

  async function runSearch() {
    const rawQuery = normalize(input.value);
    lastQuery = rawQuery;

    if (!rawQuery) {
      results.innerHTML = `<div class="search-empty">Type to search the wiki pages.</div>`;
      openResults();
      return;
    }

    const tokens = tokenize(rawQuery);
    const index = await loadIndex();
    if (lastQuery !== rawQuery) {
      return;
    }

    const matches = index
      .map((page) => ({ page, score: scorePage(page, tokens, rawQuery) }))
      .filter((match) => match.score > 0)
      .sort((a, b) => b.score - a.score || a.page.title.localeCompare(b.page.title))
      .slice(0, 8);

    if (!matches.length) {
      results.innerHTML = `<div class="search-empty">No results for <strong>${escapeHtml(input.value)}</strong>.</div>`;
      openResults();
      return;
    }

    results.innerHTML = matches.map(({ page }, index) => `
      <a class="search-result" role="option" aria-selected="false" data-index="${index}" href="${escapeHtml(page.url)}">
        <span class="search-result-title">${escapeHtml(page.title)}</span>
        <span class="search-result-snippet">${escapeHtml(makeSnippet(page, tokens))}</span>
        <span class="search-result-url">${escapeHtml(page.url)}</span>
      </a>
    `).join("");

    openResults();
    setActive(0);
  }

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const active = results.querySelector('.search-result[aria-selected="true"]') || results.querySelector(".search-result");
    if (active) {
      window.location.href = active.getAttribute("href");
    }
  });

  input.addEventListener("focus", () => {
    loadIndex();
    if (!input.value.trim()) {
      results.innerHTML = `<div class="search-empty">Type to search the wiki pages.</div>`;
      openResults();
    } else {
      runSearch();
    }
  });

  input.addEventListener("input", runSearch);

  input.addEventListener("keydown", (event) => {
    const items = results.querySelectorAll(".search-result");

    if (event.key === "Escape") {
      closeResults();
      input.blur();
      return;
    }

    if (event.key === "ArrowDown" && items.length) {
      event.preventDefault();
      setActive(activeIndex + 1);
      return;
    }

    if (event.key === "ArrowUp" && items.length) {
      event.preventDefault();
      setActive(activeIndex - 1);
    }
  });

  results.addEventListener("mousemove", (event) => {
    const item = event.target.closest(".search-result");
    if (item) {
      setActive(Number(item.getAttribute("data-index")) || 0);
    }
  });

  document.addEventListener("keydown", (event) => {
    const isShortcut = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k";
    if (!isShortcut) {
      return;
    }

    event.preventDefault();
    input.focus();
    input.select();
  });

  document.addEventListener("click", (event) => {
    if (!form.contains(event.target)) {
      closeResults();
    }
  });
})();
