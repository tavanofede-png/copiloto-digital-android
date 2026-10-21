// Local, fictional product demo. No device data, GPS or push notifications.
(() => {
  const root = document.getElementById('familias');
  if (!root) return;
  const get = selector => root.querySelector(selector);
  const all = selector => Array.from(root.querySelectorAll(selector));
  const state = {
    scenario: 'alert',
    tab: 'summary',
    pendingAlert: true,
    reviewed: false,
    notifications: true,
    sharing: { activity: true, screen: true, location: true, alerts: true, contact: true },
    message: null,
    activityFilter: 'all',
  };
  const tabs = all('[data-gf-tab]');
  const text = (selector, value) => { get(selector).textContent = value; };
  const voiceDialog = get('#gf-voice-dialog');
  const voiceInput = get('#gf-voice-text');
  const speech = 'speechSynthesis' in window ? window.speechSynthesis : null;
  let activeSpeech = null;
  const explorer = get('#gf-explorer');
  const demo = get('#familia-demo');
  const demoMarker = document.createComment('family-demo-home');
  demo.before(demoMarker);
  const eventDialog = get('#gf-event-dialog');
  const wideMockup = window.matchMedia('(min-width:851px)');
  const activityEvents = {
    recipe: { title:'Buscó una receta de cocina', time:'10:42', app:'Navegador', duration:'8 min', site:'paulinacocina.net', description:'Mamá abrió una receta de cocina y consultó cómo encontrar sus ingredientes.', guidance:'Nieto le explicó dónde tocar para seguir leyendo. Mamá realizó cada paso en su propio celular.' },
    whatsapp: { title:'Practicó cómo crear un grupo', time:'10:31', app:'WhatsApp', duration:'10 min', site:'No aplica', description:'Mamá practicó la creación de un grupo en WhatsApp y repitió los pasos para aprender el proceso.', guidance:'La acompañó paso a paso y repitió las instrucciones con paciencia.' },
    news: { title:'Leyó las noticias', time:'10:21', app:'Navegador', duration:'6 min', site:'lanacion.com.ar', description:'Mamá visitó un sitio de noticias desde el navegador de su celular.', guidance:'Realizó esta actividad por su cuenta.' },
    session: { title:'Activó el acompañamiento', time:'10:15', app:'Nieto', duration:'No aplica', site:'No aplica', description:'Mamá inició el acompañamiento y mantuvo habilitados los permisos elegidos para su grupo.', guidance:'Quedó disponible para responder sus preguntas y guiarla con paciencia.' },
    risk: { title:'Abrió un enlace sospechoso', time:'10:44', app:'Navegador', duration:'Sin registro', site:'Enlace por verificar', description:'Mamá abrió un enlace que requiere revisión. Conviene comprobar el sitio antes de continuar.', guidance:'Le sugirió no continuar y dejó una alerta para que su familia pueda acompañarla.' },
  };

  function updateMockupOrientation() {
    get('.gf-tabs').setAttribute('aria-orientation', explorer.open && wideMockup.matches ? 'vertical' : 'horizontal');
  }

  function renderActivity() {
    let count = 0;
    for (const control of all('[data-gf-filter]')) control.setAttribute('aria-pressed', String(control.dataset.gfFilter === state.activityFilter));
    for (const item of all('[data-gf-event-category]')) {
      const riskAllowed = item.id !== 'gf-risk-event' || (state.scenario === 'alert' && state.sharing.alerts);
      const visible = riskAllowed && (state.activityFilter === 'all' || item.dataset.gfEventCategory.split(' ').includes(state.activityFilter));
      item.hidden = !visible;
      if (visible) count++;
    }
    text('#gf-activity-count', count + (count === 1 ? ' evento' : ' eventos'));
    text('#gf-risk-status', state.reviewed ? 'Revisado' : 'Requiere revisión');
  }

  function openEvent(name) {
    const event = activityEvents[name];
    if (!event || !state.sharing.activity || (name === 'risk' && !state.sharing.alerts)) return;
    text('#gf-event-title', event.title);
    text('#gf-event-time', event.time);
    text('#gf-event-app', event.app);
    text('#gf-event-duration', state.sharing.screen ? event.duration : 'No compartido');
    text('#gf-event-site', event.site);
    text('#gf-event-description', event.description);
    text('#gf-event-guidance', event.guidance);
    get('#gf-event-see-alerts').hidden = name !== 'risk';
    eventDialog.showModal();
    get('#gf-close-event').focus();
  }

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
      button.querySelector('span').textContent = 'Escuchar aviso';
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
      audioStatus('Audio no disponible en este dispositivo. Podés leer el mensaje.');
      return;
    }
    const voice = speech.getVoices().find(item => item.localService && item.lang.toLowerCase().startsWith('es'));
    if (!voice) {
      audioStatus('Elegí una voz en español en tu dispositivo para escuchar el aviso.');
      return;
    }
    const utterance = new SpeechSynthesisUtterance(message);
    utterance.voice = voice;
    utterance.lang = voice.lang;
    utterance.rate = 0.95;
    activeSpeech = { utterance, button };
    button.setAttribute('aria-pressed', 'true');
    button.querySelector('span').textContent = 'Detener audio';
    audioStatus('Preparando audio…');
    utterance.onstart = () => {
      if (activeSpeech?.utterance !== utterance) return;
      button.classList.add('is-playing');
      audioStatus('Reproduciendo aviso.');
    };
    utterance.onend = () => {
      if (activeSpeech?.utterance === utterance) stopAudio('Audio finalizado.');
    };
    utterance.onerror = () => {
      if (activeSpeech?.utterance === utterance) stopAudio('No se pudo reproducir el audio. Intentá nuevamente.');
    };
    try { speech.speak(utterance); }
    catch { stopAudio('No se pudo reproducir el audio. Intentá nuevamente.'); }
  }

  function renderContact() {
    get('#gf-send-ping').disabled = !state.sharing.contact;
    get('#gf-open-voice').disabled = !state.sharing.contact;
    get('#gf-contact-permission-note').hidden = state.sharing.contact;
    get('#gf-contact-preview').hidden = !state.message || !state.sharing.contact;
    text('#gf-voice-send-note', state.scenario === 'offline'
      ? 'El aviso quedará pendiente hasta que Nieto vuelva a activarse.'
      : 'Revisá tu mensaje antes de continuar.');
    if (!state.message) return;
    const queued = state.message.status === 'queued';
    text('#gf-contact-message-title', state.message.type === 'voice' ? 'Un aviso con la voz de Nieto' : 'Un ping de tu hijo');
    text('#gf-contact-message-text', state.message.content);
    text('#gf-contact-status', queued ? 'Pendiente · Nieto desactivado' : 'Aviso preparado');
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
      ? 'Aviso pendiente. Nieto está desactivado.'
      : 'Tu aviso está listo.');
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
    if (explorer.open) explorer.scrollTop = 0;
  }

  function render() {
    const active = state.scenario !== 'offline';
    const pending = state.pendingAlert && state.sharing.alerts;
    for (const button of all('[data-gf-scenario]')) {
      button.setAttribute('aria-pressed', String(button.dataset.gfScenario === state.scenario));
    }
    get('#gf-scenario-select').value = state.scenario;
    get('#gf-session').classList.toggle('is-offline', !active);
    text('#gf-session-label', active ? 'Nieto activado' : 'Nieto desactivado');
    get('#gf-session-note').hidden = active;
    text('#gf-last-activity', active ? (state.scenario === 'alert' ? 'Hace 3 min' : 'Hace 5 min') : '10:44');
    text('#gf-last-activity-note', state.scenario === 'alert' ? 'Revisó un enlace con Nieto.' : 'Consultó una receta con Nieto.');
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
        : state.reviewed ? 'La alerta fue revisada.' : 'Sin alertas pendientes.');
    get('#gf-review-alert').hidden = !pending;
    get('#gf-alert-card').hidden = !pending;
    get('#gf-alert-empty').hidden = pending;
    text('#gf-alert-empty-title', !state.sharing.alerts ? 'Alertas no compartidas' : state.reviewed ? 'Alerta revisada' : 'Todo en orden por ahora');
    text('#gf-alert-empty-text', !state.sharing.alerts
      ? 'Mamá eligió no compartir avisos de seguridad con su grupo.'
      : state.reviewed ? 'Marcaste esta alerta como revisada.' : 'No hay alertas pendientes.');
    get('.gf-notifications').hidden = !state.sharing.alerts;
    get('#gf-simulate-alert').disabled = !state.sharing.activity;
    get('#gf-simulate-alert').textContent = 'Ver actividad reciente →';
    get('#gf-notifications').checked = state.notifications;
    renderActivity();
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
    if (name === 'offline') message = 'Nieto está desactivado. Consultá los últimos registros compartidos.';
    else if (name === 'alert' && state.sharing.alerts) {
      message = state.notifications
        ? 'Hay un enlace sospechoso para revisar en Alertas.'
        : 'Alerta disponible en la pestaña Alertas. Tenés los avisos desactivados.';
    } else if (name === 'alert') message = 'Mamá no comparte sus alertas de seguridad.';
    else message = 'Nieto activado. Podés consultar la actividad compartida.';
    if (deliverQueued) message += ' Tu aviso está listo.';
    announce(message);
  }

  for (const tab of tabs) {
    tab.addEventListener('click', () => selectTab(tab.dataset.gfTab));
    tab.addEventListener('keydown', event => {
      const index = tabs.indexOf(tab);
      let next;
      if (event.key === 'ArrowRight' || event.key === 'ArrowDown') next = (index + 1) % tabs.length;
      else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') next = (index - 1 + tabs.length) % tabs.length;
      else if (event.key === 'Home') next = 0;
      else if (event.key === 'End') next = tabs.length - 1;
      else return;
      event.preventDefault();
      selectTab(tabs[next].dataset.gfTab, true);
    });
  }
  for (const button of all('[data-gf-go]')) button.addEventListener('click', () => selectTab(button.dataset.gfGo, true));
  get('#gf-open-mockup').addEventListener('click', () => {
    get('#gf-explorer-mount').append(demo);
    explorer.showModal();
    document.body.classList.add('gf-explorer-open');
    updateMockupOrientation();
    selectTab('activity', true);
  });
  get('#gf-close-explorer').addEventListener('click', () => explorer.close());
  explorer.addEventListener('close', () => {
    stopAudio('');
    demoMarker.after(demo);
    document.body.classList.remove('gf-explorer-open');
    updateMockupOrientation();
    get('#gf-open-mockup').focus({ preventScroll:true });
  });
  wideMockup.addEventListener('change', updateMockupOrientation);
  for (const button of all('[data-gf-filter]')) button.addEventListener('click', () => {
    state.activityFilter = button.dataset.gfFilter;
    renderActivity();
  });
  for (const button of all('[data-gf-event]')) button.addEventListener('click', () => openEvent(button.dataset.gfEvent));
  get('#gf-close-event').addEventListener('click', () => eventDialog.close());
  get('#gf-event-see-alerts').addEventListener('click', () => {
    eventDialog.close();
    selectTab('alerts', true);
  });
  for (const button of all('[data-gf-scenario]')) button.addEventListener('click', () => useScenario(button.dataset.gfScenario));
  get('#gf-scenario-select').addEventListener('change', event => useScenario(event.target.value));
  get('#gf-mark-reviewed').addEventListener('click', () => {
    state.pendingAlert = false;
    state.reviewed = true;
    render();
    get('#gf-tab-alerts').focus({ preventScroll: true });
    announce('Alerta marcada como revisada.');
  });
  get('#gf-simulate-alert').addEventListener('click', () => selectTab('activity', true));
  get('#gf-notifications').addEventListener('change', event => {
    state.notifications = event.target.checked;
    announce(state.notifications
      ? 'Preferencia de notificaciones activada.'
      : 'Preferencia de notificaciones desactivada. Podés seguir consultando las alertas.');
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
      announce('Preferencias actualizadas.');
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
