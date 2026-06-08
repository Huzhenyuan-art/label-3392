(() => {
  document.addEventListener("click", (e) => {
    const toggleBtn = e.target.closest(".toggle-snapshot");
    if (toggleBtn) {
      const targetId = toggleBtn.getAttribute("data-target-id");
      const panel = document.getElementById(targetId);
      if (panel) {
        panel.classList.toggle("open");
        const isOpen = panel.classList.contains("open");
        toggleBtn.textContent = isOpen ? "收起详情" : "查看详情";
      }
    }

    const closeBtn = e.target.closest(".close-snapshot");
    if (closeBtn) {
      const targetId = closeBtn.getAttribute("data-target-id");
      const panel = document.getElementById(targetId);
      if (panel) {
        panel.classList.remove("open");
        const toggleBtn = document.querySelector(`.toggle-snapshot[data-target-id="${targetId}"]`);
        if (toggleBtn) {
          toggleBtn.textContent = "查看详情";
        }
      }
    }
  });
})();
