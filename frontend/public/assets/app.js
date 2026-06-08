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

  function showConfirm(message, onConfirm) {
    const overlay = document.createElement("div");
    overlay.className = "confirm-overlay";

    const dialog = document.createElement("div");
    dialog.className = "confirm-dialog";

    const title = document.createElement("h3");
    title.className = "confirm-title";
    title.textContent = "操作确认";

    const msg = document.createElement("p");
    msg.className = "confirm-message";
    msg.textContent = message;

    const actions = document.createElement("div");
    actions.className = "confirm-actions";

    const cancelBtn = document.createElement("button");
    cancelBtn.className = "btn";
    cancelBtn.type = "button";
    cancelBtn.textContent = "取消";

    const confirmBtn = document.createElement("button");
    confirmBtn.className = "btn primary";
    confirmBtn.type = "button";
    confirmBtn.textContent = "确认";

    function close() {
      overlay.remove();
      document.removeEventListener("keydown", onKeydown);
    }

    function onKeydown(e) {
      if (e.key === "Escape") {
        close();
      }
    }

    cancelBtn.addEventListener("click", close);
    confirmBtn.addEventListener("click", () => {
      close();
      onConfirm();
    });
    overlay.addEventListener("click", (e) => {
      if (e.target === overlay) close();
    });
    document.addEventListener("keydown", onKeydown);

    actions.appendChild(cancelBtn);
    actions.appendChild(confirmBtn);
    dialog.appendChild(title);
    dialog.appendChild(msg);
    dialog.appendChild(actions);
    overlay.appendChild(dialog);
    document.body.appendChild(overlay);

    setTimeout(() => confirmBtn.focus(), 50);
  }

  document.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-confirm]");
    if (!btn) return;

    if (btn.hasAttribute("data-confirm-skip")) {
      return;
    }

    e.preventDefault();
    e.stopPropagation();

    const msg = btn.getAttribute("data-confirm") || "确定要继续吗？";
    const form = btn.closest("form");

    showConfirm(msg, () => {
      btn.setAttribute("data-confirm-skip", "");
      if (form) {
        form.submit();
      } else {
        btn.click();
      }
    });
  });
})();

