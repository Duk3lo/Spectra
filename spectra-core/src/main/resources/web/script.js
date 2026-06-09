const CONFIG = {
    ranges: {
        bassLimit: 0.15,
        snareLimit: 0.50
    },
    colors: {
        peak:   { color: '#ffffff', shadow: '0 0 15px #fff' },
        bass:   { high: '#ff3366', low: '#881133' },
        mid:    { high: '#00ffcc', low: '#006655' },
        high:   { high: '#33ccff', low: '#115588' }
    },
    throttleMs: 50
};

const DOM = {
    container: document.getElementById('bars-container'),
    kickText: document.getElementById('kick-text'),
    snareText: document.getElementById('snare-text'),
    hatText: document.getElementById('hat-text'),
    slider: document.getElementById('vol-slider'),
    volLabel: document.getElementById('vol-label'),
    overlay: document.getElementById('overlay'),
    status: document.getElementById('status'),
    energyVal: document.getElementById('energy-val'),
    comboVal: document.getElementById('combo-val'),
    speedVal: document.getElementById('speed-val'),
    progressFill: document.getElementById('progress-fill'),
    timeText: document.getElementById('time-text')
};

let barElements = [];
let currentNumBars = 0;
let volTimeout = null;
let isUserDraggingVol = false;
let isShuttingDown = false;
let lastAudioData = null;

if (DOM.slider) {
    DOM.slider.addEventListener('pointerdown', () => { isUserDraggingVol = true; });
    DOM.slider.addEventListener('pointerup', () => { isUserDraggingVol = false; });
    DOM.slider.addEventListener('touchstart', () => { isUserDraggingVol = true; }, { passive: true });
    DOM.slider.addEventListener('touchend', () => { isUserDraggingVol = false; }, { passive: true });
    DOM.slider.addEventListener('blur', () => { isUserDraggingVol = false; });
}

if (DOM.slider && DOM.volLabel) {
    DOM.slider.oninput = function() {
        const val = this.value;
        DOM.volLabel.innerText = Math.round(Number(val) * 100) + '%';
        clearTimeout(volTimeout);
        volTimeout = setTimeout(() => {
            fetch('/volume?level=' + val).catch(err => console.warn(err));
        }, CONFIG.throttleMs);
    };
}

function formatTime(secs) {
    if (isNaN(secs) || secs < 0) return "00:00";
    let m = Math.floor(secs / 60);
    let s = Math.floor(secs % 60);
    return (m < 10 ? "0" + m : m) + ":" + (s < 10 ? "0" + s : s);
}

function rebuildVisualizer(numBars) {
    if (!DOM.container) return;
    DOM.container.innerHTML = '';
    barElements = [];
    if (numBars > 32) DOM.container.classList.add('dense');
    else DOM.container.classList.remove('dense');

    const fragment = document.createDocumentFragment();
    for (let i = 0; i < numBars; i++) {
        let wrap = document.createElement('div');
        wrap.className = 'bar-wrapper';
        let bar = document.createElement('div');
        bar.className = 'bar';
        if (numBars <= 32) {
            let lbl = document.createElement('div');
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
    currentNumBars = numBars;
}

function renderLoop() {
    if (lastAudioData) {
        const data = lastAudioData;
        lastAudioData = null;

        const numBars = data.bars.length;
        if (numBars !== currentNumBars) rebuildVisualizer(numBars);

        if (DOM.status) {
            DOM.status.innerText = data.paused ? '⏸ PAUSED' : '▶ PLAYING';
            DOM.status.style.color = data.paused ? '#ff3366' : '#00ffcc';
        }

        if (DOM.energyVal) DOM.energyVal.innerText = Math.round(data.energy * 100) + '%';
        if (DOM.comboVal) DOM.comboVal.innerText = data.combo + 'x';
        if (DOM.speedVal) DOM.speedVal.innerText = data.speed.toFixed(2) + 'x';

        if (DOM.progressFill) {
            let percent = (data.current / data.total) * 100;
            DOM.progressFill.style.width = (isNaN(percent) ? 0 : percent) + '%';
        }
        if (DOM.timeText) DOM.timeText.innerText = `${formatTime(data.current)} / ${formatTime(data.total)}`;

        let bassEnd = Math.max(1, Math.floor(numBars * CONFIG.ranges.bassLimit));
        let snareEnd = Math.max(bassEnd + 1, Math.floor(numBars * CONFIG.ranges.snareLimit));

        let maxBassHit = 0, maxSnareHit = 0, maxHighHit = 0;
        for (let i = 0; i < bassEnd; i++) maxBassHit = Math.max(maxBassHit, data.intensities[i] || 0);
        for (let i = bassEnd; i < snareEnd; i++) maxSnareHit = Math.max(maxSnareHit, data.intensities[i] || 0);
        for (let i = snareEnd; i < numBars; i++) maxHighHit = Math.max(maxHighHit, data.intensities[i] || 0);

        if (DOM.kickText) {
            DOM.kickText.style.opacity = String(0.2 + (maxBassHit * 0.8));
            DOM.kickText.style.transform = `scale(${1 + (maxBassHit * 0.25)})`;
        }
        if (DOM.snareText) {
            DOM.snareText.style.opacity = String(0.2 + (maxSnareHit * 0.8));
            DOM.snareText.style.transform = `scale(${1 + (maxSnareHit * 0.20)})`;
        }
        if (DOM.hatText) {
            DOM.hatText.style.opacity = String(0.2 + (maxHighHit * 0.8));
            DOM.hatText.style.transform = `scale(${1 + (maxHighHit * 0.15)})`;
        }

        for (let i = 0; i < numBars; i++) {
            let intensity = data.intensities[i] || 0;
            let barValue = Math.min(1.0, data.bars[i] || 0);
            let h = (Math.pow(barValue, 1.5) * 55) + (Math.pow(intensity, 1.2) * 45);
            h = Math.max(2, Math.min(h, 100));

            let barStyle = barElements[i].style;
            barStyle.height = h + '%';

            let isBass = i < bassEnd;
            let isMid = i >= bassEnd && i < snareEnd;
            let colorObj = isBass ? CONFIG.colors.bass : (isMid ? CONFIG.colors.mid : CONFIG.colors.high);

            if (intensity > 0.8) {
                barStyle.backgroundColor = CONFIG.colors.peak.color;
                barStyle.boxShadow = CONFIG.colors.peak.shadow;
            } else if (intensity > 0.4) {
                barStyle.backgroundColor = colorObj.high;
                barStyle.boxShadow = `0 0 15px ${colorObj.high}`;
            } else {
                barStyle.backgroundColor = colorObj.low;
                barStyle.boxShadow = 'none';
            }
        }
    }

    if (!isShuttingDown) {
        requestAnimationFrame(renderLoop);
    }
}

let source = null;

function conectarAudioEngine() {
    if (source) source.close();
    source = new EventSource('/stream');

    source.onopen = function() {
        if (DOM.overlay && !isShuttingDown) DOM.overlay.style.display = 'none';
    };

    source.onerror = function() {
        if (isShuttingDown) return;


        if (source.readyState === EventSource.CONNECTING) {
            console.log("Intentando reconectar automáticamente...");
            return;
        }

        source.close();
        if (DOM.overlay) {
            DOM.overlay.style.display = 'flex';
            DOM.overlay.innerHTML = '<div class="loading-content"><h1>CONECTANDO...</h1><p>Esperando señal del motor de audio</p></div>';
        }
        setTimeout(conectarAudioEngine, 2000);
    };

    source.onmessage = function(event) {
        try {
            const data = JSON.parse(event.data);

            if (data.type === 'shutdown') {
                isShuttingDown = true;
                if (source) source.close();

                if (DOM.overlay) {
                    DOM.overlay.style.display = 'flex';
                    DOM.overlay.style.backdropFilter = 'blur(15px)';
                    DOM.overlay.style.webkitBackdropFilter = 'blur(15px)';
                    DOM.overlay.innerHTML = `
                        <div style="text-align: center; background: rgba(0,0,0,0.4); padding: 40px; border-radius: 20px; border: 1px solid rgba(255, 51, 102, 0.3);">
                            <h1 style="color: #ff3366; font-size: 2.5rem; margin: 0; text-transform: uppercase; letter-spacing: 4px; text-shadow: 0 0 20px rgba(255,51,102,0.5);">Engine Stopped</h1>
                            <p style="color: #ccc; font-size: 1.1rem; margin-top: 15px;">Cerrando visualizador en <span id="countdown" style="color: #fff; font-weight: bold;">3</span>...</p>
                        </div>
                    `;

                    let timeLeft = 3;
                    const timer = setInterval(() => {
                        timeLeft--;
                        const cdSpan = document.getElementById("countdown");
                        if (cdSpan) cdSpan.innerText = timeLeft.toString();

                        if (timeLeft <= 0) {
                            clearInterval(timer);
                            window.opener = null;
                            window.open('', '_self', '');
                            window.close();

                            const content = DOM.overlay.querySelector('div');
                            if(content) content.innerHTML = '<h1 style="color: #ff3366;">OFFLINE</h1><p style="color: #888;">Ya puedes cerrar esta pestaña.</p>';
                        }
                    }, 1000);
                }
                return;
            }

            if (data.type === 'volume_change') {
                if (DOM.slider && !isUserDraggingVol) {
                    DOM.slider.value = data.value;
                    if (DOM.volLabel) DOM.volLabel.innerText = Math.round(Number(data.value) * 100) + '%';
                }
                return;
            }

            lastAudioData = data;

        } catch (e) {}
    };
}

conectarAudioEngine();
requestAnimationFrame(renderLoop);

document.addEventListener("visibilitychange", function() {
    if (document.visibilityState === 'visible' && !isShuttingDown) {
        if (!source || source.readyState === EventSource.CLOSED) {
            console.log("Reconectando visualizador por cambio de visibilidad...");
            conectarAudioEngine();
        }
    }
});