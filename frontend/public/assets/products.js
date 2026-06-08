(() => {
  function isBlank(s) {
    return !s || !s.trim();
  }

  function isValidDecimal(s) {
    const t = s.trim();
    if (!t) return false;
    // allow: 12, 12.3, 12.34
    return /^\d+(\.\d{1,2})?$/.test(t);
  }

  function showError(form, msg) {
    const err = form.querySelector('[data-role="price-error"]') || null;
    if (err) {
      err.textContent = msg;
      err.style.display = "block";
    }
    form.querySelectorAll('input[name="minPrice"], input[name="maxPrice"]').forEach((el) => {
      el.classList.add("invalid");
    });
    if (window.AppToast && window.AppToast.bad) window.AppToast.bad(msg);
  }

  function clearError(form) {
    const err = form.querySelector('[data-role="price-error"]') || null;
    if (err) {
      err.textContent = "";
      err.style.display = "none";
    }
    form.querySelectorAll('input[name="minPrice"], input[name="maxPrice"]').forEach((el) => {
      el.classList.remove("invalid");
    });
  }

  window.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector('form.searchbar[action="/products"]');
    if (!form) return;

    const minEl = form.querySelector('input[name="minPrice"]');
    const maxEl = form.querySelector('input[name="maxPrice"]');
    if (!minEl || !maxEl) return;

    // Ensure error placeholder exists for JS-only validation.
    let err = form.querySelector('[data-role="price-error"]');
    if (!err) {
      err = document.createElement("div");
      err.className = "err-inline";
      err.dataset.role = "price-error";
      err.style.display = "none";
      const field = maxEl.closest(".field") || form;
      field.appendChild(err);
    }

    const onInput = () => {
      // Only clear JS-created errors; server-rendered errors should persist until next submit.
      if (err && err.style.display !== "none") clearError(form);
    };
    minEl.addEventListener("input", onInput);
    maxEl.addEventListener("input", onInput);

    form.addEventListener("submit", (e) => {
      const min = (minEl.value || "").trim();
      const max = (maxEl.value || "").trim();

      const minBlank = isBlank(min);
      const maxBlank = isBlank(max);

      if (minBlank && maxBlank) return; // ok
      if (minBlank !== maxBlank) {
        e.preventDefault();
        showError(form, "价格区间需同时填写最小价和最大价（或同时留空）");
        return;
      }

      if (!isValidDecimal(min) || !isValidDecimal(max)) {
        e.preventDefault();
        showError(form, "价格区间请输入数字（最多 2 位小数），例如：199.00");
        return;
      }

      const minN = Number(min);
      const maxN = Number(max);
      if (!Number.isFinite(minN) || !Number.isFinite(maxN)) {
        e.preventDefault();
        showError(form, "价格区间请输入有效数字，例如：199.00");
        return;
      }

      if (minN > maxN) {
        e.preventDefault();
        showError(form, "价格区间不合法：最小价不能大于最大价");
      }
    });
  });
})();
