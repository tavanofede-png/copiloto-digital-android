const helperBtn = document.getElementById('helperBtn');
    const helperPop = document.getElementById('helperPop');
    const closeHelper = document.getElementById('closeHelper');
    helperBtn.addEventListener('click', () => {
      const show = !helperPop.classList.contains('show');
      helperPop.classList.toggle('show', show);
      helperBtn.setAttribute('aria-expanded', String(show));
    });
    closeHelper.addEventListener('click', () => {
      helperPop.classList.remove('show');
      helperBtn.setAttribute('aria-expanded', 'false');
      helperBtn.focus();
    });
