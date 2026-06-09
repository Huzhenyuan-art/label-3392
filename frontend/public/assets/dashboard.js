(() => {
  const API = '/api/dashboard/stats';

  let statusChart = null;
  let trendChart = null;

  function renderStats(data) {
    document.getElementById('totalCount').textContent = data.totalCount;
    document.getElementById('activeCount').textContent = data.activeCount;
    document.getElementById('inactiveCount').textContent = data.inactiveCount;
    document.getElementById('activePercent').textContent = data.activePercent + '%';
    document.getElementById('inactivePercent').textContent = data.inactivePercent + '%';
  }

  function renderStatusChart(data) {
    const ctx = document.getElementById('statusChart').getContext('2d');
    if (statusChart) statusChart.destroy();

    statusChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['启用', '停用'],
        datasets: [{
          data: [data.activeCount, data.inactiveCount],
          backgroundColor: ['rgba(22,163,74,0.75)', 'rgba(107,114,128,0.55)'],
          borderColor: ['rgba(22,163,74,1)', 'rgba(107,114,128,1)'],
          borderWidth: 2,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        cutout: '62%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              padding: 18,
              usePointStyle: true,
              pointStyleWidth: 10,
              font: { size: 13 }
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                const pct = total > 0 ? ((context.parsed / total) * 100).toFixed(1) : 0;
                return context.label + ': ' + context.parsed + ' (' + pct + '%)';
              }
            }
          }
        }
      }
    });
  }

  function renderTrendChart(data) {
    const ctx = document.getElementById('trendChart').getContext('2d');
    if (trendChart) trendChart.destroy();

    const labels = data.trend.map(d => d.date.substring(5));
    const values = data.trend.map(d => d.count);

    trendChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: '新增产品数',
          data: values,
          borderColor: '#2563eb',
          backgroundColor: 'rgba(37,99,235,0.12)',
          fill: true,
          tension: 0.35,
          pointBackgroundColor: '#2563eb',
          pointBorderColor: '#fff',
          pointBorderWidth: 2,
          pointRadius: 5,
          pointHoverRadius: 7
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              stepSize: 1,
              font: { size: 12 }
            },
            grid: {
              color: 'rgba(23,37,84,0.06)'
            }
          },
          x: {
            ticks: { font: { size: 12 } },
            grid: { display: false }
          }
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: 'rgba(11,18,32,0.88)',
            titleFont: { size: 13 },
            bodyFont: { size: 13 },
            padding: 10,
            cornerRadius: 8
          }
        }
      }
    });
  }

  function renderLowStock(products) {
    const tbody = document.getElementById('lowStockBody');
    if (!products || products.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" class="hint">暂无库存预警产品</td></tr>';
      return;
    }

    tbody.innerHTML = products.map(p => {
      const stockClass = p.stock <= 3 ? 'danger-text' : '';
      return '<tr>' +
        '<td class="col-id">' + p.id + '</td>' +
        '<td class="col-name"><a class="link" href="/products/' + p.id + '">' + escapeHtml(p.name) + '</a></td>' +
        '<td class="col-category">' + escapeHtml(p.categoryName || '-') + '</td>' +
        '<td class="col-stock ' + stockClass + '" style="font-weight:700">' + p.stock + '</td>' +
        '<td><span class="pill status-active" style="border-color:rgba(239,68,68,.35);background:rgba(239,68,68,.06)"><span class="spark" style="background:linear-gradient(135deg,#ef4444,#f87171);box-shadow:0 0 0 5px rgba(239,68,68,.18)"></span>库存不足</span></td>' +
        '</tr>';
    }).join('');
  }

  function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  async function loadDashboard() {
    try {
      const resp = await fetch(API, { credentials: 'same-origin' });
      if (!resp.ok) throw new Error('HTTP ' + resp.status);
      const data = await resp.json();

      renderStats(data);
      renderStatusChart(data);
      renderTrendChart(data);
      renderLowStock(data.lowStockProducts);
    } catch (e) {
      console.error('Dashboard load failed:', e);
      const hint = document.querySelector('#lowStockBody td');
      if (hint) hint.textContent = '加载失败，请刷新重试';
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadDashboard);
  } else {
    loadDashboard();
  }
})();
