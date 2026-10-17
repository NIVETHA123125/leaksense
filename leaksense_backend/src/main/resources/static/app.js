// LeakSense Landing Page Interactive Logic
document.addEventListener('DOMContentLoaded', () => {
  fetchLiveStatus();
  setupDemoForm();
});

async function fetchLiveStatus() {
  try {
    const res = await fetch('/api/public/status');
    if (!res.ok) return;
    const data = await res.json();

    const statusBadge = document.getElementById('liveStatusBadge');
    if (statusBadge && data.status) {
      const isOptimal = data.status === 'OPTIMAL_SAFETY';
      statusBadge.innerHTML = `
        <span class="pulse-dot" style="background-color: ${isOptimal ? '#10b981' : '#f59e0b'};"></span>
        System Status: ${data.status.replace('_', ' ')} (${data.onlineNodes || 0}/${data.totalNodes || 0} Nodes Online)
      `;
    }

    if (data.systemHealthScore) {
      const healthEl = document.getElementById('healthScoreVal');
      if (healthEl) healthEl.textContent = `${data.systemHealthScore}%`;
    }
  } catch (err) {
    console.debug('Public status fetch bypassed:', err);
  }
}

function setupDemoForm() {
  const form = document.getElementById('demoForm');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitBtn = form.querySelector('button[type="submit"]');
    const originalText = submitBtn.textContent;
    submitBtn.disabled = true;
    submitBtn.textContent = 'Submitting Request...';

    const payload = {
      name: document.getElementById('demoName')?.value || '',
      email: document.getElementById('demoEmail')?.value || '',
      company: document.getElementById('demoCompany')?.value || '',
      message: document.getElementById('demoMessage')?.value || ''
    };

    try {
      const res = await fetch('/api/public/demo-request', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await res.json();
      showToast(data.message || 'Demo request submitted successfully!');
      form.reset();
    } catch (err) {
      showToast('Thank you! Demo request received.');
      form.reset();
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = originalText;
    }
  });
}

function showToast(message) {
  let toast = document.getElementById('toastNotification');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'toastNotification';
    toast.className = 'toast';
    document.body.appendChild(toast);
  }

  toast.innerHTML = `
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2.5">
      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
      <polyline points="22 4 12 14.01 9 11.01"></polyline>
    </svg>
    <span>${message}</span>
  `;

  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 4500);
}
