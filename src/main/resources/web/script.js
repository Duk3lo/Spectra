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

/* AGREGADO: bloqueo real mientras el usuario arrastra */
let isUserDraggingVol = false;

if (DOM.slider) {
    DOM.slider.addEventListener('pointerdown', () => {
        isUserDraggingVol = true;
    });

    DOM.slider.addEventListener('pointerup', () => {
        isUserDraggingVol = false;
    });

    DOM.slider.addEventListener('touchstart', () => {
        isUserDraggingVol = true;
    }, { passive: true });

    DOM.slider.addEventListener('touchend', () => {
        isUserDraggingVol = false;
    }, { passive: true });

    DOM.slider.addEventListener('blur', () => {
        isUserDraggingVol = false;
    });
}

if (DOM.slider && DOM.volLabel) {
    DOM.slider.oninput = function() {
        const val = this.value;

        DOM.volLabel.innerText = Math.round(Number(val) * 100) + '%';

        clearTimeout(volTimeout);
        volTimeout = setTimeout(() => {
            fetch('/volume?level=' + val).catch(err => console.warn("Error enviando volumen:", err));
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
    console.log("Reconstruyendo visualizador para " + numBars + " barras.");

    DOM.container.innerHTML = '';
    barElements = [];

    const fragment = document.createDocumentFragment();

    for (let i = 0; i < numBars; i++) {
        let wrap = document.createElement('div');
        wrap.className = 'bar-wrapper';

        let bar = document.createElement('div');
        bar.className = 'bar';

        let lbl = document.createElement('div');
        lbl.className = 'label';
        lbl.innerText = String(i + 1);

        wrap.appendChild(bar);
        wrap.appendChild(lbl);
        fragment.appendChild(wrap);
        barElements.push(bar);
    }

    DOM.container.appendChild(fragment);
    currentNumBars = numBars;
}

const source = new EventSource('/stream');

source.onerror = function() {
    source.close();
    if (DOM.overlay) DOM.overlay.style.display = 'flex';
    setTimeout(() => window.close(), 1500);
};

source.onmessage = function(event) {
    try {
        /**
         * @type {{
         *  type: string, value: number, bars: number[], intensities: number[],
         *  energy: number, combo: number, speed: number, paused: boolean,
         *  current: number, total: number
         * }}
         */
        const data = JSON.parse(event.data);

        if (data.type === 'volume_change') {
            /* AGREGADO: no sobrescribir mientras el usuario está moviendo */
            if (DOM.slider && !isUserDraggingVol) {
                DOM.slider.value = data.value;
                DOM.volLabel.innerText = Math.round(Number(data.value) * 100) + '%';
            }
            return;
        }

        const numBars = data.bars.length;
        if (numBars !== currentNumBars) {
            rebuildVisualizer(numBars);
        }

        if (DOM.status) {
            DOM.status.innerText = data.paused ? '⏸ EN PAUSA' : '▶ REPRODUCIENDO';
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
            DOM.kickText.style.textShadow = maxBassHit > 0.4 ? `0 0 ${maxBassHit * 15}px ${CONFIG.colors.bass.high}` : 'none';
        }
        if (DOM.snareText) {
            DOM.snareText.style.opacity = String(0.2 + (maxSnareHit * 0.8));
            DOM.snareText.style.transform = `scale(${1 + (maxSnareHit * 0.20)})`;
            DOM.snareText.style.textShadow = maxSnareHit > 0.4 ? `0 0 ${maxSnareHit * 15}px ${CONFIG.colors.mid.high}` : 'none';
        }
        if (DOM.hatText) {
            DOM.hatText.style.opacity = String(0.2 + (maxHighHit * 0.8));
            DOM.hatText.style.transform = `scale(${1 + (maxHighHit * 0.15)})`;
            DOM.hatText.style.textShadow = maxHighHit > 0.4 ? `0 0 ${maxHighHit * 15}px ${CONFIG.colors.high.high}` : 'none';
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

    } catch (e) {
        console.error("Error procesando actualización del servidor:", e);
    }
};