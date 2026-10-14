(function () {
  const cfg = window.WW || {};
  const roomCode = cfg.roomCode;
  const myUserId = cfg.myUserId;
  let currentPhase = cfg.phase;
  let phaseEndsAtMs = Number(cfg.phaseEndsAtMs || 0);
  let serverSkew = Number(cfg.serverNowMs || 0) - Date.now();

  let lastEventId = 0;
  let lastChatId = 0;

  const countdownEl = document.getElementById("countdown");
  const eventsEl = document.getElementById("events");
  const chatEl = document.getElementById("chat");
  const wsIndicator = document.getElementById("wsIndicator");

  const audioNight = document.getElementById("audioNight");
  const audioDay = document.getElementById("audioDay");
  const audioClick = document.getElementById("audioClick");
  const musicToggle = document.getElementById("musicToggle");

  let musicEnabled = localStorage.getItem("ww_music") === "1";

  // ─── Countdown ────────────────────────────────────────────────────
  function fmtSeconds(s) {
    s = Math.max(0, Math.floor(s));
    const m = Math.floor(s / 60);
    const r = s % 60;
    return m > 0 ? (m + ":" + String(r).padStart(2, "0")) : String(r);
  }

  function updateCountdown() {
    if (!countdownEl || !phaseEndsAtMs) return;
    const now = Date.now() + serverSkew;
    const remaining = Math.max(0, phaseEndsAtMs - now);
    countdownEl.textContent = fmtSeconds(remaining / 1000);
  }

  setInterval(updateCountdown, 250);
  updateCountdown();

  // ─── Music ────────────────────────────────────────────────────────
  function isNightPhase(p) { return p && p.startsWith("NIGHT"); }
  function isDayPhase(p) { return p === "DAY_CHAT" || p === "DAY_VOTE"; }

  function stopAllMusic() {
    [audioNight, audioDay].forEach(a => { if (!a) return; a.pause(); a.currentTime = 0; });
  }

  function refreshMusic() {
    if (!audioNight || !audioDay || !musicToggle) return;
    musicToggle.textContent = musicEnabled ? "🔊 Musique" : "🔇 Musique";
    if (!musicEnabled) { stopAllMusic(); return; }
    try {
      if (isNightPhase(currentPhase)) { audioDay.pause(); audioNight.play().catch(() => {}); }
      else if (isDayPhase(currentPhase)) { audioNight.pause(); audioDay.play().catch(() => {}); }
      else { stopAllMusic(); }
    } catch (e) {}
  }

  if (musicToggle) {
    musicToggle.addEventListener("click", () => {
      musicEnabled = !musicEnabled;
      localStorage.setItem("ww_music", musicEnabled ? "1" : "0");
      refreshMusic();
    });
  }

  document.addEventListener("submit", () => {
    if (audioClick) { try { audioClick.currentTime = 0; audioClick.play().catch(() => {}); } catch (e) {} }
  });

  // ─── DOM helpers ──────────────────────────────────────────────────
  function escapeHtml(s) {
    return (s || "").replace(/[&<>"']/g, c =>
      ({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#039;"}[c]));
  }

  function appendEvent(ev) {
    if (!eventsEl) return;
    if (eventsEl.textContent === "Chargement...") eventsEl.textContent = "";
    if (ev.id && ev.id <= lastEventId) return;
    if (ev.id) lastEventId = Math.max(lastEventId, ev.id);
    const line = (ev.isPrivate ? "🔒 " : "") + ev.message;
    eventsEl.textContent += (eventsEl.textContent ? "\n" : "") + line;
    eventsEl.scrollTop = eventsEl.scrollHeight;
  }

  function appendChat(msg) {
    if (!chatEl) return;
    if (msg.id && msg.id <= lastChatId) return;
    if (msg.id) lastChatId = Math.max(lastChatId, msg.id);
    const wrapper = document.createElement("div");
    wrapper.className = "msg";
    const img = document.createElement("img");
    img.className = "avatar"; img.src = "/avatars/" + msg.avatar; img.alt = "avatar";
    const bubble = document.createElement("div"); bubble.className = "bubble";
    const name = document.createElement("div");
    name.innerHTML = "<b>" + escapeHtml(msg.username) + "</b>";
    const text = document.createElement("div"); text.textContent = msg.message;
    const meta = document.createElement("div"); meta.className = "meta";
    meta.textContent = new Date(msg.createdAtMs).toLocaleTimeString();
    bubble.appendChild(name); bubble.appendChild(text); bubble.appendChild(meta);
    wrapper.appendChild(img); wrapper.appendChild(bubble);
    chatEl.appendChild(wrapper);
    chatEl.scrollTop = chatEl.scrollHeight;
  }

  // ─── Phase change → reload ────────────────────────────────────────
  function onStateUpdate(st) {
    serverSkew = (st.serverNowMs || Date.now()) - Date.now();
    phaseEndsAtMs = st.phaseEndsAtMs || 0;
    if (st.phase && st.phase !== currentPhase) {
      window.location.reload();
      return;
    }
    refreshMusic();
  }

  // ─── WS status indicator ──────────────────────────────────────────
  function setWsStatus(connected) {
    if (!wsIndicator) return;
    wsIndicator.textContent = connected ? "🟢 Live" : "🔴 Reconnexion...";
    wsIndicator.style.color = connected ? "#4ade80" : "#f87171";
  }

  // ─── HTTP fallback ────────────────────────────────────────────────
  let fallbackInterval = null;

  function startFallback() {
    if (fallbackInterval) return;
    setWsStatus(false);
    fallbackInterval = setInterval(() => {
      fetch("/api/game/" + roomCode + "/state", {cache: "no-store"})
        .then(r => r.ok ? r.json() : null).then(st => { if (st) onStateUpdate(st); }).catch(() => {});
      fetch("/api/game/" + roomCode + "/events?afterId=" + lastEventId, {cache: "no-store"})
        .then(r => r.ok ? r.json() : []).then(evs => evs.forEach(appendEvent)).catch(() => {});
      if (chatEl) {
        fetch("/api/game/" + roomCode + "/chat?afterId=" + lastChatId, {cache: "no-store"})
          .then(r => r.ok ? r.json() : []).then(msgs => msgs.forEach(appendChat)).catch(() => {});
      }
    }, 3000);
  }

  function stopFallback() {
    if (fallbackInterval) { clearInterval(fallbackInterval); fallbackInterval = null; }
  }

  // ─── Initial HTTP load ────────────────────────────────────────────
  refreshMusic();

  fetch("/api/game/" + roomCode + "/events?afterId=0", {cache: "no-store"})
    .then(r => r.ok ? r.json() : []).then(evs => evs.forEach(appendEvent)).catch(() => {});
  if (chatEl) {
    fetch("/api/game/" + roomCode + "/chat?afterId=0", {cache: "no-store"})
      .then(r => r.ok ? r.json() : []).then(msgs => msgs.forEach(appendChat)).catch(() => {});
  }

  // ─── STOMP WebSocket ──────────────────────────────────────────────
  function connectWS() {
    // @stomp/stompjs v7 uses Client class directly
    if (typeof StompJs === "undefined" || typeof SockJS === "undefined") {
      startFallback();
      return;
    }

    const client = new StompJs.Client({
      webSocketFactory: () => new SockJS("/ws"),
      reconnectDelay: 5000,
      debug: () => {},
      onConnect: () => {
        setWsStatus(true);
        stopFallback();

        client.subscribe("/topic/game/" + roomCode + "/state", frame => {
          try { onStateUpdate(JSON.parse(frame.body)); } catch (e) {}
        });

        client.subscribe("/topic/game/" + roomCode + "/events", frame => {
          try { appendEvent(JSON.parse(frame.body)); } catch (e) {}
        });

        client.subscribe("/topic/game/" + roomCode + "/chat", frame => {
          try { appendChat(JSON.parse(frame.body)); } catch (e) {}
        });

        if (myUserId) {
          client.subscribe("/topic/private/" + myUserId, frame => {
            try {
              const ev = JSON.parse(frame.body);
              ev.isPrivate = true;
              appendEvent(ev);
            } catch (e) {}
          });
        }
      },
      onDisconnect: () => {
        setWsStatus(false);
        startFallback();
      },
      onStompError: () => {
        setWsStatus(false);
        startFallback();
      }
    });

    client.activate();
  }

  // Small delay to ensure SockJS + StompJs are fully evaluated
  setTimeout(connectWS, 200);

  // Safety net: if WS never connects after 3s, use HTTP fallback
  setTimeout(() => {
    if (fallbackInterval === null && wsIndicator && wsIndicator.textContent !== "🟢 Live") {
      startFallback();
    }
  }, 3000);

})();
