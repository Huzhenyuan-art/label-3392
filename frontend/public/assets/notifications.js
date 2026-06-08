(() => {
  const UNREAD_COUNT_API = '/api/notifications/unread-count';
  const REFRESH_INTERVAL = 30000;

  function updateUnreadBadge(count) {
    const badges = document.querySelectorAll('[data-role="unread-badge"]');

    badges.forEach(badge => {
      if (count > 0) {
        badge.style.display = 'flex';
        badge.textContent = count > 99 ? '99+' : count;
      } else {
        badge.style.display = 'none';
      }
    });

    if (document.body.classList.contains('page-notifications')) {
      const originalTitle = document.title.replace(/^\(\d+\)\s*/, '');
      document.title = count > 0
        ? `(${count}) ${originalTitle}`
        : originalTitle;
    }
  }

  async function fetchUnreadCount() {
    try {
      const response = await fetch(UNREAD_COUNT_API, {
        credentials: 'same-origin',
        headers: {
          'Accept': 'application/json',
        },
      });
      if (response.ok) {
        const data = await response.json();
        updateUnreadBadge(data.count || 0);
      }
    } catch (e) {
      console.warn('Failed to fetch unread count:', e);
    }
  }

  function init() {
    fetchUnreadCount();
    setInterval(fetchUnreadCount, REFRESH_INTERVAL);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

  window.NotificationCenter = {
    refresh: fetchUnreadCount,
    updateBadge: updateUnreadBadge,
  };
})();
