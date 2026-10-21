// Local, fictional product demo. No device data, GPS or push notifications.
(() => {
  const root = document.getElementById('familias');
  if (!root) return;
  const get = selector => root.querySelector(selector);
  const all = selector => Array.from(root.querySelectorAll(selector));
  const state = {
    scenario: 'normal',
    tab: 'summary',
    pendingAlert: false,
    reviewed: false,
    notifications: true,
    sharing: { activity: true, screen: true, location: true, alerts: true },
  };
  const tabs = all('[data-gf-tab]');
  const text = (selector, value) => { get(selector).textContent = value; };

  function announce(message) {
    const output = get('#gf-announcement');
    output.hidden = false;
    output.textContent = message;
  }

  function selectTab(name, focus = false) {
    if (!tabs.some(tab => tab.dataset.gfTab === name)) return;
    state.tab = name;
    for (const tab of tabs) {
      const selected = tab.dataset.gfTab === name;
      tab.setAttribute('aria-selected', String(selected));
      tab.tabIndex = selected ? 0 : -1;
      get('#' + tab.getAttribute('aria-controls')).hidden = !selected;
      if (selected && focus) tab.focus({ preventScroll: true });
    }
  }

  function render() {
    const active = state.scenario !== 'offline';
    const pending = state.pendingAlert && state.sharing.alerts;
    for (const button of all('[data-gf-scenario]')) {
      button.setAttribute('aria-pressed', String(button.dataset.gfScenario === state.scenario));
    }
    get('#gf-session').classList.toggle('is-offline', !active);
    text('#gf-session-label', active ? 'Nieto activado' : 'Nieto desactivado');
    get('#gf-session-note').hidden = active;
    text('#gf-last-activity', active ? 'Hace 5 min' : '10:42');
    for (const element of all('[data-gf-data]')) element.hidden = !state.sharing[element.dataset.gfData];
    for (const element of all('[data-gf-private]')) element.hidden = state.sharing[element.dataset.gfPrivate];

    text('#gf-location-title', active ? 'Ubicación compartida' : 'Última ubicación compartida');
    text('#gf-location-updated', active ? 'Hace 2 min' : 'Al cerrar la sesión');
    text('#gf-location-note', active
      ? 'Mamá está compartiendo su ubicación mientras Nieto está activo.'
      : 'Último registro antes de desactivar Nieto. Esta ubicación ya no se actualiza.');
    get('.gf-map').classList.toggle('is-offline', !active);
    text('#gf-summary-location', state.sharing.location
      ? (active ? 'Ubicación compartida' : 'Última ubicación compartida')
      : 'Ubicación no compartida');
    text('#gf-summary-location-note', state.sharing.location
      ? (active ? 'En casa · actualizada hace 2 min' : 'En casa · registro anterior, sin actualización')
      : 'Mamá eligió mantener este dato privado.');

    get('#gf-alert-count').hidden = !pending;
    get('#gf-safety-line').classList.toggle('is-alert', pending);
    text('#gf-safety-text', !state.sharing.alerts
      ? 'Mamá no comparte sus alertas de seguridad.'
      : pending ? 'Un enlace sospechoso para revisar juntos.'
        : state.reviewed ? 'La alerta del ejemplo fue revisada.' : 'Sin alertas pendientes en este ejemplo.');
    get('#gf-review-alert').hidden = !pending;
    get('#gf-alert-card').hidden = !pending;
    get('#gf-alert-empty').hidden = pending;
    text('#gf-alert-empty-title', !state.sharing.alerts ? 'Alertas no compartidas' : state.reviewed ? 'Alerta revisada' : 'Todo en orden por ahora');
    text('#gf-alert-empty-text', !state.sharing.alerts
      ? 'Mamá eligió no compartir avisos de seguridad con su grupo.'
      : state.reviewed ? 'Marcaste como revisada la alerta de esta demostración.' : 'No hay alertas pendientes en este ejemplo.');
    get('.gf-notifications').hidden = !state.sharing.alerts;
    get('#gf-simulate-alert').disabled = !active;
    get('#gf-simulate-alert').textContent = active ? 'Probar una alerta de ejemplo →' : 'Activá Nieto en el ejemplo para probar una alerta';
    get('#gf-notifications').checked = state.notifications;
  }

  function useScenario(name) {
    state.scenario = name;
    state.pendingAlert = name === 'alert';
    state.reviewed = false;
    render();
    if (name === 'offline') announce('Ejemplo: Nieto se desactivó. La ubicación y la actividad no reciben nuevas actualizaciones.');
    else if (name === 'alert' && state.sharing.alerts) {
      announce(state.notifications
        ? 'Notificación de ejemplo: Mamá abrió un enlace sospechoso. Podés revisarlo en Alertas. No se envió ninguna notificación real.'
        : 'Alerta de ejemplo disponible en la pestaña Alertas. Los avisos están desactivados en esta demostración.');
    } else if (name === 'alert') announce('Mamá no comparte sus alertas de seguridad en este ejemplo.');
    else announce('Ejemplo: Nieto activado. Se muestran datos ficticios y los permisos elegidos.');
  }

  for (const tab of tabs) {
    tab.addEventListener('click', () => selectTab(tab.dataset.gfTab));
    tab.addEventListener('keydown', event => {
      const index = tabs.indexOf(tab);
      let next;
      if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
      else if (event.key === 'ArrowLeft') next = (index - 1 + tabs.length) % tabs.length;
      else if (event.key === 'Home') next = 0;
      else if (event.key === 'End') next = tabs.length - 1;
      else return;
      event.preventDefault();
      selectTab(tabs[next].dataset.gfTab, true);
    });
  }
  for (const button of all('[data-gf-go]')) button.addEventListener('click', () => selectTab(button.dataset.gfGo, true));
  for (const button of all('[data-gf-scenario]')) button.addEventListener('click', () => useScenario(button.dataset.gfScenario));
  get('#gf-mark-reviewed').addEventListener('click', () => {
    state.pendingAlert = false;
    state.reviewed = true;
    render();
    get('#gf-tab-alerts').focus({ preventScroll: true });
    announce('La alerta del ejemplo quedó revisada. No se contactó a tu familiar.');
  });
  get('#gf-simulate-alert').addEventListener('click', () => useScenario('alert'));
  get('#gf-notifications').addEventListener('change', event => {
    state.notifications = event.target.checked;
    announce(state.notifications
      ? 'Notificaciones activadas en esta demo. No se enviarán avisos reales.'
      : 'Notificaciones desactivadas en esta demo. Las alertas seguirán disponibles para consultar.');
  });
  get('#gf-permissions-button').addEventListener('click', event => {
    const permissions = get('#gf-permissions');
    permissions.hidden = !permissions.hidden;
    event.currentTarget.setAttribute('aria-expanded', String(!permissions.hidden));
    event.currentTarget.textContent = permissions.hidden ? 'Ver permisos' : 'Ocultar permisos';
  });
  for (const control of all('[data-gf-permission]')) {
    control.addEventListener('change', event => {
      state.sharing[event.target.dataset.gfPermission] = event.target.checked;
      render();
      announce('Permisos actualizados solo en este ejemplo. Tu familiar mantendrá el control desde su propio celular.');
    });
  }
  render();
  selectTab('summary');
})();
