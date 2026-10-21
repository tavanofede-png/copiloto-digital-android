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
    sharing: { activity: true, screen: true, location: true, alerts: true, contact: true },
    message: null,
  };
  const tabs = all('[data-gf-tab]');
  const text = (selector, value) => { get(selector).textContent = value; };
  const voiceDialog = get('#gf-voice-dialog');
  const voiceInput = get('#gf-voice-text');
  const speech = 'speechSynthesis' in window ? window.speechSynthesis : null;
  let activeSpeech = null;

  function audioStatus(message) {
    for (const output of all('[data-gf-audio-status]')) output.textContent = message;
  }

  function stopAudio(message) {
    const wasSpeaking = activeSpeech !== null;
    activeSpeech = null;
    if (speech && wasSpeaking) speech.cancel();
    for (const button of all('[data-gf-play]')) {
      button.classList.remove('is-playing');
      button.setAttribute('aria-pressed', 'false');
      button.querySelector('span').textContent = 'Escuchar ejemplo';
    }
    if (message !== undefined) audioStatus(message);
  }

  function playAudio(button) {
    if (activeSpeech && activeSpeech.button === button) {
      stopAudio('Reproducción detenida.');
      return;
    }
    stopAudio('');
    const message = button.dataset.gfPlay === 'composer' ? voiceInput.value.trim() : state.message?.content;
    if (!message) return;
    if (!speech || !('SpeechSynthesisUtterance' in window)) {
      audioStatus('La reproducción de voz no está disponible en este navegador. El mensaje sigue visible para leerlo.');
      return;
    }
    const voice = speech.getVoices().find(item => item.localService && item.lang.toLowerCase().startsWith('es'));
    if (!voice) {
      audioStatus('Este navegador no tiene una voz local en español disponible. Podés leer el ejemplo y simular el envío.');
      return;
    }
    const utterance = new SpeechSynthesisUtterance(message);
    utterance.voice = voice;
    utterance.lang = voice.lang;
    utterance.rate = 0.95;
    activeSpeech = { utterance, button };
    button.setAttribute('aria-pressed', 'true');
    button.querySelector('span').textContent = 'Detener audio';
    audioStatus('Preparando el ejemplo de voz en este dispositivo…');
    utterance.onstart = () => {
      if (activeSpeech?.utterance !== utterance) return;
      button.classList.add('is-playing');
      audioStatus('Reproduciendo el ejemplo acá. No se envía a ningún teléfono.');
    };
    utterance.onend = () => {
      if (activeSpeech?.utterance === utterance) stopAudio('Ejemplo de audio finalizado.');
    };
    utterance.onerror = () => {
      if (activeSpeech?.utterance === utterance) stopAudio('No se pudo reproducir el audio. Podés leer el mensaje y simular su envío.');
    };
    try { speech.speak(utterance); }
    catch { stopAudio('No se pudo reproducir el audio en este navegador.'); }
  }

  function renderContact() {
    get('#gf-send-ping').disabled = !state.sharing.contact;
    get('#gf-open-voice').disabled = !state.sharing.contact;
    get('#gf-contact-permission-note').hidden = state.sharing.contact;
    get('#gf-contact-preview').hidden = !state.message || !state.sharing.contact;
    text('#gf-voice-send-note', state.scenario === 'offline'
      ? 'Nieto está desactivado en el ejemplo. El aviso quedará pendiente hasta que vuelva a activarse.'
      : 'Se mostrará una vista previa. No se contactará ningún teléfono.');
    if (!state.message) return;
    const queued = state.message.status === 'queued';
    text('#gf-contact-message-title', state.message.type === 'voice' ? 'Un aviso con la voz de Nieto' : 'Un ping de tu hijo');
    text('#gf-contact-message-text', state.message.content);
    text('#gf-contact-status', queued ? 'Pendiente · Nieto desactivado · demo' : 'Recibido por Mamá · demo');
    get('#gf-contact-status').classList.toggle('is-queued', queued);
    get('#gf-contact-play').hidden = state.message.type !== 'voice';
  }

  function sendContact(type, content) {
    if (!state.sharing.contact) return;
    stopAudio('');
    const queued = state.scenario === 'offline';
    state.message = { type, content, status: queued ? 'queued' : 'delivered' };
    renderContact();
    announce(queued
      ? 'Aviso de ejemplo pendiente. Se mostrará como recibido cuando actives Nieto en la simulación. No se envió ningún mensaje real.'
      : 'Vista previa lista: así recibiría Mamá el aviso. No se contactó a ningún teléfono.');
  }

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
    renderContact();
  }

  function useScenario(name) {
    const deliverQueued = name !== 'offline' && state.sharing.contact && state.message?.status === 'queued';
    state.scenario = name;
    state.pendingAlert = name === 'alert';
    state.reviewed = false;
    if (deliverQueued) state.message.status = 'delivered';
    render();
    let message;
    if (name === 'offline') message = 'Ejemplo: Nieto se desactivó. La ubicación y la actividad no reciben nuevas actualizaciones.';
    else if (name === 'alert' && state.sharing.alerts) {
      message = state.notifications
        ? 'Notificación de ejemplo: Mamá abrió un enlace sospechoso. Podés revisarlo en Alertas. No se envió ninguna notificación real.'
        : 'Alerta de ejemplo disponible en la pestaña Alertas. Los avisos están desactivados en esta demostración.';
    } else if (name === 'alert') message = 'Mamá no comparte sus alertas de seguridad en este ejemplo.';
    else message = 'Ejemplo: Nieto activado. Se muestran datos ficticios y los permisos elegidos.';
    if (deliverQueued) message += ' El aviso pendiente ahora figura como recibido en esta demo.';
    announce(message);
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
      if (event.target.dataset.gfPermission === 'contact' && !event.target.checked) {
        state.message = null;
        stopAudio('');
        if (voiceDialog.open) voiceDialog.close();
      }
      render();
      announce('Permisos actualizados solo en este ejemplo. Tu familiar mantendrá el control desde su propio celular.');
    });
  }
  get('#gf-send-ping').addEventListener('click', () => sendContact('ping', 'Hola Martha, tu hijo te mandó un ping para estar cerca.'));
  get('#gf-open-voice').addEventListener('click', () => {
    if (!state.sharing.contact) return;
    stopAudio('');
    renderContact();
    voiceDialog.showModal();
    voiceInput.focus();
  });
  get('#gf-close-contact-preview').addEventListener('click', () => {
    stopAudio('');
    state.message = null;
    renderContact();
    get('#gf-send-ping').focus({ preventScroll: true });
  });
  for (const button of all('[data-gf-play]')) button.addEventListener('click', () => playAudio(button));
  for (const button of [get('#gf-close-voice'), get('#gf-cancel-voice')]) button.addEventListener('click', () => voiceDialog.close());
  voiceDialog.addEventListener('close', () => stopAudio(''));
  voiceInput.addEventListener('input', () => {
    stopAudio('');
    text('#gf-voice-count', voiceInput.value.length + '/180');
    get('#gf-submit-voice').disabled = !voiceInput.value.trim();
  });
  get('#gf-voice-form').addEventListener('submit', event => {
    event.preventDefault();
    const content = voiceInput.value.trim();
    if (!content || !state.sharing.contact) return;
    sendContact('voice', content);
    voiceDialog.close();
  });
  if (speech) speech.getVoices();
  window.addEventListener('pagehide', () => stopAudio());
  text('#gf-voice-count', voiceInput.value.length + '/180');
  render();
  selectTab('summary');
})();
