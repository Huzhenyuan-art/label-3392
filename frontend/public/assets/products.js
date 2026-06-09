(() => {
  function isBlank(s) {
    return !s || !s.trim();
  }

  function setupCsvImportExport() {
    const importModal = document.getElementById("importModal");
    if (!importModal) return;

    const btnImportCsv = document.getElementById("btnImportCsv");
    const btnExportCsv = document.getElementById("btnExportCsv");
    const btnConfirmImport = document.getElementById("btnConfirmImport");
    const csvFileInput = document.getElementById("csvFileInput");
    const importProgress = document.getElementById("importProgress");
    const exportForm = document.getElementById("exportForm");
    const importForm = document.getElementById("importForm");
    const importFormFile = document.getElementById("importFormFile");
    const btnCloseResult = document.getElementById("btnCloseResult");
    const importResultCard = document.getElementById("importResultCard");

    if (btnExportCsv && exportForm) {
      btnExportCsv.addEventListener("click", () => {
        exportForm.submit();
      });
    }

    function openModal() {
      if (importModal) {
        importModal.style.display = "flex";
      }
    }

    function closeModal() {
      if (importModal) {
        importModal.style.display = "none";
      }
    }

    if (btnImportCsv) {
      btnImportCsv.addEventListener("click", openModal);
    }

    importModal.querySelectorAll("[data-close-modal]").forEach((el) => {
      el.addEventListener("click", closeModal);
    });

    if (btnCloseResult && importResultCard) {
      btnCloseResult.addEventListener("click", () => {
        importResultCard.style.display = "none";
      });
    }

    if (btnConfirmImport && csvFileInput && importForm && importFormFile) {
      btnConfirmImport.addEventListener("click", () => {
        const file = csvFileInput.files[0];
        if (!file) {
          if (window.AppToast && window.AppToast.bad) {
            window.AppToast.bad("请选择CSV文件");
          }
          return;
        }

        if (!file.name.toLowerCase().endsWith(".csv")) {
          if (window.AppToast && window.AppToast.bad) {
            window.AppToast.bad("请上传CSV格式的文件");
          }
          return;
        }

        importFormFile.files = csvFileInput.files;

        if (importProgress) {
          importProgress.style.display = "block";
          const progressBar = importProgress.querySelector(".progress-bar");
          if (progressBar) progressBar.style.width = "30%";
        }
        btnConfirmImport.disabled = true;
        btnConfirmImport.textContent = "导入中...";

        importForm.submit();
      });
    }
  }

  function isValidDecimal(s) {
    const t = s.trim();
    if (!t) return false;
    return /^\d+(\.\d{1,2})?$/.test(t);
  }

  function isValidInteger(s) {
    const t = s.trim();
    if (!t) return false;
    return /^\d+$/.test(t);
  }

  function showFieldError(form, fieldName, msg) {
    const err = form.querySelector(`[data-role="${fieldName}-error"]`);
    if (err) {
      err.textContent = msg;
      err.style.display = "block";
    }
    const input = form.querySelector(`[name="${fieldName}"]`);
    if (input) {
      input.classList.add("invalid");
    }
  }

  function clearFieldError(form, fieldName) {
    const err = form.querySelector(`[data-role="${fieldName}-error"]`);
    if (err) {
      err.textContent = "";
      err.style.display = "none";
    }
    const input = form.querySelector(`[name="${fieldName}"]`);
    if (input) {
      input.classList.remove("invalid");
    }
  }

  function clearAllFormErrors(form) {
    ["categoryId", "name", "description", "price", "stock", "status"].forEach((field) => {
      clearFieldError(form, field);
    });
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

  function highlightNameMatches() {
    var nameInput = document.querySelector('form.searchbar[action="/products"] input[name="name"]');
    if (!nameInput) return;
    var keyword = (nameInput.value || "").trim();
    if (!keyword) return;

    var cells = document.querySelectorAll('.cell-name .label.link');
    var escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    var re = new RegExp('(' + escaped + ')', 'gi');

    cells.forEach(function(el) {
      var text = el.textContent || '';
      if (!re.test(text)) return;
      re.lastIndex = 0;
      el.innerHTML = text.replace(re, '<mark>$1</mark>');
    });
  }

  window.addEventListener("DOMContentLoaded", () => {
    setupCsvImportExport();
    highlightNameMatches();

    const form = document.querySelector('form.searchbar[action="/products"]');
    if (!form) return;

    const pageSizeSelect = document.getElementById("pageSizeSelect");
    if (pageSizeSelect) {
      pageSizeSelect.addEventListener("change", () => {
        const pageInput = form.querySelector('input[name="page"]');
        if (pageInput) {
          pageInput.value = "1";
        } else {
          const newPageInput = document.createElement("input");
          newPageInput.type = "hidden";
          newPageInput.name = "page";
          newPageInput.value = "1";
          form.appendChild(newPageInput);
        }
        form.submit();
      });
    }

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

  window.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector('form.grid:not(.searchbar)');
    if (!form) return;
    if (!form.querySelector('input[name="name"], select[name="categoryId"]')) return;

    const validateForm = () => {
      let hasError = false;

      clearAllFormErrors(form);

      const categoryId = form.querySelector('[name="categoryId"]').value;
      if (isBlank(categoryId)) {
        showFieldError(form, "categoryId", "请选择分类");
        hasError = true;
      }

      const name = form.querySelector('[name="name"]').value || "";
      if (isBlank(name)) {
        showFieldError(form, "name", "名称不能为空");
        hasError = true;
      } else if (name.length < 1 || name.length > 50) {
        showFieldError(form, "name", "名称长度需为 1-50");
        hasError = true;
      }

      const description = form.querySelector('[name="description"]').value || "";
      if (description.length > 255) {
        showFieldError(form, "description", "描述最长 255");
        hasError = true;
      }

      const price = form.querySelector('[name="price"]').value || "";
      if (isBlank(price)) {
        showFieldError(form, "price", "价格不能为空");
        hasError = true;
      } else if (!isValidDecimal(price)) {
        showFieldError(form, "price", "价格请输入有效数字（最多 2 位小数）");
        hasError = true;
      } else {
        const priceN = Number(price);
        if (priceN < 0) {
          showFieldError(form, "price", "价格不能小于 0");
          hasError = true;
        }
      }

      const stock = form.querySelector('[name="stock"]').value || "";
      if (isBlank(stock)) {
        showFieldError(form, "stock", "库存不能为空");
        hasError = true;
      } else if (!isValidInteger(stock)) {
        showFieldError(form, "stock", "库存请输入有效整数");
        hasError = true;
      } else {
        const stockN = Number(stock);
        if (stockN < 0) {
          showFieldError(form, "stock", "库存不能小于 0");
          hasError = true;
        }
      }

      const status = form.querySelector('[name="status"]').value;
      if (isBlank(status)) {
        showFieldError(form, "status", "状态不能为空");
        hasError = true;
      }

      return !hasError;
    };

    ["categoryId", "name", "description", "price", "stock", "status"].forEach((field) => {
      const input = form.querySelector(`[name="${field}"]`);
      if (input) {
        input.addEventListener("input", () => clearFieldError(form, field));
        input.addEventListener("change", () => clearFieldError(form, field));
      }
    });

    form.addEventListener("submit", (e) => {
      if (!validateForm()) {
        e.preventDefault();
        if (window.AppToast && window.AppToast.bad) {
          window.AppToast.bad("请检查表单中的错误");
        }
      }
    });
  });
})();
