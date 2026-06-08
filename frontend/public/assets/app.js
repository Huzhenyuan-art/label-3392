(() => {
  function ensureHost() {
    let host = document.querySelector(".toast-host");
    if (!host) {
      host = document.createElement("div");
      host.className = "toast-host";
      document.body.appendChild(host);
    }
    return host;
  }

  function toast(message, type) {
    const host = ensureHost();
    const el = document.createElement("div");
    el.className = `toast ${type || ""}`.trim();
    el.textContent = message;
    host.appendChild(el);
    setTimeout(() => el.remove(), 3200);
  }

  window.AppToast = {
    ok: (m) => toast(m, "ok"),
    bad: (m) => toast(m, "bad"),
  };

  document.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-confirm]");
    if (!btn) return;
    const msg = btn.getAttribute("data-confirm") || "确定要继续吗？";
    if (!confirm(msg)) {
      e.preventDefault();
      e.stopPropagation();
    }
  });
})();

