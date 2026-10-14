(function () {
  const cfg = window.WW || {};
  const roomCode = cfg.roomCode;
  let currentPhase = cfg.phase;
  let phaseEndsAtMs = Number(cfg.phaseEndsAtMs || 0);
  let serverSkew = Number(cfg.serverNowMs || 0) - Date.now();

  let lastEventId = 0;
  let lastChatId = 0;

  const countdownEl = document.getElementById("countdown");
  const eventsEl = document.getElementById("events");
  const chatEl = document.getElementById("chat");

  const audioNight = document.getElementById("audioNight");
  const audioDay = document.getElementById("audioDay");
  const audioClick = document.getElementById("audioClick");
  const musicToggle = document.getElementById("musicToggle");

  let musicEnabled = localStorage.getItem("ww_music") === "1";

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

  function isNightPhase(phase) {
    return phase && phase.startsWith("NIGHT");
  }
  function isDayPhase(phase) {
    return phase === "DAY_CHAT" || phase === "DAY_VOTE";
  }

  function stopAllMusic() {
    [audioNight, audioDay].forEach(a => {
      if (!a) return;
      a.pause();
      a.currentTime = 0;
    });
  }

  function refreshMusic() {
    if (!audioNight || !audioDay || !musicToggle) return;
    musicToggle.textContent = musicEnabled ? "🔊 Musique" : "🔇 Musique";
    if (!musicEnabled) {
      stopAllMusic();
      return;
    }
    // Try to play
    try {
      if (isNightPhase(currentPhase)) {
        audioDay.pause();
        audioNight.play().catch(()=>{});
      } else if (isDayPhase(currentPhase)) {
        audioNight.pause();
        audioDay.play().catch(()=>{});
      } else {
        stopAllMusic();
      }
    } catch (e) {}
  }

  if (musicToggle) {
    musicToggle.addEventListener("click", () => {
      musicEnabled = !musicEnabled;
      localStorage.setItem("ww_music", musicEnabled ? "1" : "0");
      refreshMusic();
    });
  }

  // Play click sound on any submit
  document.addEventListener("submit", (e) => {
    if (audioClick) {
      try { audioClick.currentTime = 0; audioClick.play().catch(()=>{}); } catch (err) {}
    }
  });

  function appendEvent(ev) {
    if (!eventsEl) return;
    if (eventsEl.textContent === "Chargement...") eventsEl.textContent = "";
    const line = (ev.isPrivate ? "🔒 " : "") + ev.message;
    eventsEl.textContent += (eventsEl.textContent ? "\n" : "") + line;
    eventsEl.scrollTop = eventsEl.scrollHeight;
  }

  function appendChat(msg) {
    if (!chatEl) return;
    const wrapper = document.createElement("div");
    wrapper.className = "msg";

    const img = document.createElement("img");
    img.className = "avatar";
    img.src = "/avatars/" + msg.avatar;
    img.alt = "avatar";

    const bubble = document.createElement("div");
    bubble.className = "bubble";
    const name = document.createElement("div");
    name.innerHTML = "<b>" + escapeHtml(msg.username) + "</b>";
    const text = document.createElement("div");
    text.textContent = msg.message;
    const meta = document.createElement("div");
    meta.className = "meta";
    meta.textContent = new Date(msg.createdAtMs).toLocaleTimeString();

    bubble.appendChild(name);
    bubble.appendChild(text);
    bubble.appendChild(meta);

    wrapper.appendChild(img);
    wrapper.appendChild(bubble);

    chatEl.appendChild(wrapper);
    chatEl.scrollTop = chatEl.scrollHeight;
  }

  function escapeHtml(s) {
    return (s || "").replace(/[&<>"']/g, (c) => ({
      "&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#039;"
    }[c]));
  }

  async function pollState() {
    try {
      const res = await fetch("/api/game/" + roomCode + "/state", {cache: "no-store"});
      if (!res.ok) return;
      const st = await res.json();
      serverSkew = st.serverNowMs - Date.now();
      phaseEndsAtMs = st.phaseEndsAtMs || 0;

      if (st.phase && st.phase !== currentPhase) {
        // phase changed -> reload to show correct Thymeleaf UI
        window.location.reload();
        return;
      }
      currentPhase = st.phase;
      refreshMusic();
    } catch (e) {}
  }

  async function pollEvents() {
    try {
      const res = await fetch("/api/game/" + roomCode + "/events?afterId=" + lastEventId, {cache:"no-store"});
      if (!res.ok) return;
      const events = await res.json();
      for (const ev of events) {
        lastEventId = Math.max(lastEventId, ev.id);
        appendEvent(ev);
      }
    } catch (e) {}
  }

  async function pollChat() {
    if (!chatEl) return;
    try {
      const res = await fetch("/api/game/" + roomCode + "/chat?afterId=" + lastChatId, {cache:"no-store"});
      if (!res.ok) return;
      const msgs = await res.json();
      for (const m of msgs) {
        lastChatId = Math.max(lastChatId, m.id);
        appendChat(m);
      }
    } catch (e) {}
  }

  // initial load
  refreshMusic();
  updateCountdown();
  pollEvents();
  pollChat();

  setInterval(updateCountdown, 250);
  setInterval(pollState, 2000);
  setInterval(pollEvents, 2000);
  setInterval(pollChat, 2000);
})();
