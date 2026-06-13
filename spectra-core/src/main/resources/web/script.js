
const CONFIG = {
    ranges: { bassLimit: 0.15, snareLimit: 0.50 },
    colors: {
        peak:  { color: '#ffffff', shadow: '0 0 15px #fff' },
        bass:  { high: '#ff3366', low: '#881133' },
        mid:   { high: '#00ffcc', low: '#006655' },
        high:  { high: '#33ccff', low: '#115588' }
    },
    throttleMs: 50
};

const DOM = {
    container:  document.getElementById('bars-container'),
    kickText:   document.getElementById('kick-text'),
    snareText:  document.getElementById('snare-text'),
    hatText:    document.getElementById('hat-text'),
    slider:     document.getElementById('vol-slider'),
    volLabel:   document.getElementById('vol-label'),
    overlay:    document.getElementById('overlay'),
    audioUnlock:document.getElementById('audio-unlock'),
    status:     document.getElementById('status'),
    energyVal:  document.getElementById('energy-val'),
    comboVal:   document.getElementById('combo-val'),
    speedVal:   document.getElementById('speed-val'),
    progressFill: document.getElementById('progress-fill'),
    timeText:   document.getElementById('time-text')
};

const webAudio = new Audio();
let audioUnlocked  = false;
let webAudioEnabled = true;
let currentTrackId = 0;

document.body.addEventListener('click', () => {
    if (!audioUnlocked) {
        audioUnlocked = true;
        if (webAudioEnabled) {
            webAudio.play().catch(() => console.log("Audio unlock initializing..."));
        }
        if (DOM.audioUnlock) DOM.audioUnlock.style.display = 'none';
    }
}, { once: true });

let barElements    = [];
let volTimeout     = null;
let isUserDraggingVol = false;
let isShuttingDown = false;
let lastSeekTime   = 0;

let targetData  = null;
let currentData = null;

let cachedBassEnd  = 0;
let cachedSnareEnd = 0;
let cachedNumBars  = 0;

if (DOM.slider) {
    DOM.slider.addEventListener('pointerdown', () => { isUserDraggingVol = true; });
    DOM.slider.addEventListener('pointerup',   () => { isUserDraggingVol = false; });
    DOM.slider.addEventListener('touchstart',  () => { isUserDraggingVol = true; },  { passive: true });
    DOM.slider.addEventListener('touchend',    () => { isUserDraggingVol = false; }, { passive: true });
    DOM.slider.addEventListener('blur',        () => { isUserDraggingVol = false; });
}

if (DOM.slider && DOM.volLabel) {
    DOM.slider.oninput = function() {
        const val = this.value;
        DOM.volLabel.innerText = Math.round(Number(val) * 100) + '%';
        if (webAudioEnabled) webAudio.volume = Number(val);

        clearTimeout(volTimeout);
        volTimeout = setTimeout(() => {
            fetch('/volume?level=' + val).catch(err => console.warn(err));
        }, CONFIG.throttleMs);
    };
}

function formatTime(secs) {
    if (isNaN(secs) || secs < 0) return "00:00";
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return (m < 10 ? "0" + m : m) + ":" + (s < 10 ? "0" + s : s);
}

function rebuildVisualizer(numBars) {
    if (!DOM.container) return;
    DOM.container.innerHTML = '';
    barElements = [];
    if (numBars > 32) DOM.container.classList.add('dense');
    else              DOM.container.classList.remove('dense');

    const fragment = document.createDocumentFragment();
    for (let i = 0; i < numBars; i++) {
        const wrap = document.createElement('div');
        wrap.className = 'bar-wrapper';
        const bar = document.createElement('div');
        bar.className = 'bar';
        if (numBars <= 32) {
            const lbl = document.createElement('div');
            lbl.className = 'label';
            lbl.innerText = String(i + 1);
            wrap.appendChild(bar);
            wrap.appendChild(lbl);
        } else {
            wrap.appendChild(bar);
        }
        fragment.appendChild(wrap);
        barElements.push(bar);
    }
    DOM.container.appendChild(fragment);

    cachedBassEnd  = Math.max(1, Math.floor(numBars * CONFIG.ranges.bassLimit));
    cachedSnareEnd = Math.max(cachedBassEnd + 1, Math.floor(numBars * CONFIG.ranges.snareLimit));
    cachedNumBars  = numBars;
}

function renderLoop() {
    if (targetData) {
        const numBars = targetData.bars.length;

        if (!currentData || currentData.bars.length !== numBars) {
            currentData = {
                bars:        new Float32Array(numBars),
                intensities: new Float32Array(numBars),
                energy: targetData.energy,
                speed:  targetData.speed,
                current: targetData.current,
                total:   targetData.total,
                combo:   targetData.combo,
                paused:  targetData.paused
            };
            for (let i = 0; i < numBars; i++) {
                currentData.bars[i]        = targetData.bars[i];
                currentData.intensities[i] = targetData.intensities[i];
            }
            rebuildVisualizer(numBars);
        } else {
            const spd = 0.35;
            const tb = targetData.bars;
            const cb = currentData.bars;
            const ti = targetData.intensities;
            const ci = currentData.intensities;

            for (let i = 0; i < numBars; i++) {
                cb[i] += (tb[i] - cb[i]) * spd;
                ci[i] += (ti[i] - ci[i]) * spd;
            }

            currentData.energy  += (targetData.energy - currentData.energy) * spd;
            currentData.speed   += (targetData.speed  - currentData.speed)  * spd;
            currentData.current = targetData.current;
            currentData.total   = targetData.total;
            currentData.combo   = targetData.combo;
            currentData.paused  = targetData.paused;
        }

        const d = currentData;

        if (DOM.status) {
            const txt = d.paused ? '⏸ PAUSED' : '▶ PLAYING';
            if (DOM.status.innerText !== txt) {
                DOM.status.innerText = txt;
                DOM.status.style.color = d.paused ? '#ff3366' : '#00ffcc';
            }
        }

        if (DOM.energyVal) DOM.energyVal.innerText = Math.round(d.energy * 100) + '%';
        if (DOM.comboVal)  DOM.comboVal.innerText  = d.combo + 'x';
        if (DOM.speedVal)  DOM.speedVal.innerText  = d.speed.toFixed(2) + 'x';

        if (DOM.progressFill) {
            const pct = (d.current / d.total) * 100;
            DOM.progressFill.style.width = (isNaN(pct) ? 0 : Math.min(pct, 100)) + '%';
        }
        if (DOM.timeText) DOM.timeText.innerText = formatTime(d.current) + ' / ' + formatTime(d.total);

        const ci = d.intensities;
        let maxBass = 0, maxSnare = 0, maxHigh = 0;
        for (let i = 0;              i < cachedBassEnd;  i++) { if (ci[i] > maxBass)  maxBass  = ci[i]; }
        for (let i = cachedBassEnd;  i < cachedSnareEnd; i++) { if (ci[i] > maxSnare) maxSnare = ci[i]; }
        for (let i = cachedSnareEnd; i < numBars;        i++) { if (ci[i] > maxHigh)  maxHigh  = ci[i]; }

        if (DOM.kickText) {
            DOM.kickText.style.opacity = `${0.2 + maxBass * 0.8}`;
            DOM.kickText.style.transform = `scale(${1 + maxBass * 0.25})`;
        }

        if (DOM.snareText) {
            DOM.snareText.style.opacity = `${0.2 + maxSnare * 0.8}`;
            DOM.snareText.style.transform = `scale(${1 + maxSnare * 0.20})`;
        }

        if (DOM.hatText) {
            DOM.hatText.style.opacity = `${0.2 + maxHigh * 0.8}`;
            DOM.hatText.style.transform = `scale(${1 + maxHigh * 0.15})`;
        }

        const cb = d.bars;
        for (let i = 0; i < numBars; i++) {
            const intensity = ci[i];
            const barVal    = cb[i] < 1.0 ? cb[i] : 1.0;

            let h = (Math.pow(barVal, 1.5) * 55) + (Math.pow(intensity, 1.2) * 45);
            if (h < 2)   h = 2;
            if (h > 100) h = 100;

            const bs        = barElements[i].style;
            bs.height       = h + '%';

            const isBass    = i < cachedBassEnd;
            const colorObj  = isBass ? CONFIG.colors.bass
                : (i < cachedSnareEnd ? CONFIG.colors.mid : CONFIG.colors.high);

            if (intensity > 0.8) {
                bs.backgroundColor = CONFIG.colors.peak.color;
                bs.boxShadow       = CONFIG.colors.peak.shadow;
            } else if (intensity > 0.4) {
                bs.backgroundColor = colorObj.high;
                bs.boxShadow       = '0 0 15px ' + colorObj.high;
            } else {
                bs.backgroundColor = colorObj.low;
                bs.boxShadow       = 'none';
            }
        }
    }

    if (!isShuttingDown) requestAnimationFrame(renderLoop);
}
let source = null;

function connectAudioEngine() {
    if (source) source.close();
    source = new EventSource('/stream');

    source.onopen = function() {
        if (DOM.overlay && !isShuttingDown) DOM.overlay.style.display = 'none';
    };

    source.onerror = function() {
        if (isShuttingDown) return;

        if (source.readyState === EventSource.CONNECTING) {
            return;
        }

        if (source.readyState === EventSource.CLOSED) {
            source.close();
            setTimeout(connectAudioEngine, 3000);
        }
    };

    source.onmessage = function(event) {
        try {
            const data = JSON.parse(event.data);

            if (data.type === 'shutdown') {
                isShuttingDown = true;
                if (source) source.close();
                webAudio.pause();

                if (DOM.overlay) {
                    DOM.overlay.style.display       = 'flex';
                    DOM.overlay.style.backdropFilter = 'blur(15px)';
                    DOM.overlay.style.webkitBackdropFilter = 'blur(15px)';
                    DOM.overlay.innerHTML = `
                        <div style="text-align:center;background:rgba(0,0,0,0.4);padding:40px;border-radius:20px;border:1px solid rgba(255,51,102,0.3);">
                            <h1 style="color:#ff3366;font-size:2.5rem;margin:0;text-transform:uppercase;letter-spacing:4px;text-shadow:0 0 20px rgba(255,51,102,0.5);">Engine Stopped</h1>
                            <p style="color:#ccc;font-size:1.1rem;margin-top:15px;">Closing visualizer in <span id="countdown" style="color:#fff;font-weight:bold;">3</span>...</p>
                        </div>`;

                    let t = 3;
                    const timer = setInterval(() => {
                        t--;
                        const cd = document.getElementById('countdown');
                        if (cd) cd.innerText = String(t);
                        if (t <= 0) {
                            clearInterval(timer);
                            window.opener = null;
                            window.open('', '_self', '');
                            window.close();
                            const c = DOM.overlay.querySelector('div');
                            if (c) c.innerHTML = '<h1 style="color:#ff3366;">OFFLINE</h1><p style="color:#888;">You can now close this tab.</p>';
                        }
                    }, 1000);
                }
                return;
            }

            if (data.type === 'volume_change') {
                if (DOM.slider && !isUserDraggingVol) {
                    DOM.slider.value = data.value;
                    if (DOM.volLabel) DOM.volLabel.innerText = Math.round(Number(data.value) * 100) + '%';
                    if (webAudioEnabled) webAudio.volume = Number(data.value);
                }
                return;
            }

            if (data.type === 'audio_mode') {
                webAudioEnabled = data.enabled;
                if (!webAudioEnabled) {
                    webAudio.pause();
                    webAudio.src = '';
                    currentTrackId = 0;
                }
                return;
            }

            targetData = data;

            if (webAudioEnabled && data.audioId !== undefined && data.audioId !== 0) {
                if (data.audioId !== currentTrackId) {
                    currentTrackId = data.audioId;
                    webAudio.src = '/audio_track?id=' + currentTrackId;

                    webAudio.onloadedmetadata = function() {
                        webAudio.currentTime = data.current || 0;
                        if (audioUnlocked && !data.paused) {
                            webAudio.play().catch(e => console.log("Play error:", e));
                        }
                    };
                    webAudio.load();
                }

                if (audioUnlocked && webAudio.readyState > 0) {
                    if (!isUserDraggingVol && data.volume !== undefined) {
                        webAudio.volume = data.volume;
                        if (DOM.slider) DOM.slider.value = data.volume;
                    }

                    if (data.paused && !webAudio.paused) {
                        webAudio.pause();
                    } else if (!data.paused && webAudio.paused) {
                        webAudio.play().catch(() => console.log("Autoplay blocked, needs user interaction"));
                    }

                    if (!data.paused) {
                        const drift    = webAudio.currentTime - data.current;
                        const absDrift = Math.abs(drift);

                        if (absDrift > 0.8 && absDrift < data.total && (Date.now() - lastSeekTime > 2000)) {
                            webAudio.currentTime = data.current;
                            lastSeekTime = Date.now();
                        } else if (absDrift > 0.05 && absDrift <= 0.8) {
                            webAudio.playbackRate = drift < 0 ? 1.05 : 0.95;
                        } else {
                            webAudio.playbackRate = 1.0;
                        }
                    }
                }
            }

        } catch (e) {
        }
    };
}

connectAudioEngine();
requestAnimationFrame(renderLoop);

document.addEventListener('visibilitychange', function() {
    if (document.visibilityState === 'visible' && !isShuttingDown) {
        if (!source || source.readyState === EventSource.CLOSED) {
            console.log("Reconnecting visualizer after visibility change...");
            connectAudioEngine();
        }
    }
});