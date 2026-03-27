const container = document.getElementById('bars-container');
const kickText = document.getElementById('kick-text');
const snareText = document.getElementById('snare-text');
const hatText = document.getElementById('hat-text');

let barElements = [];
let currentNumBars = 0;

const source = new EventSource('/stream');

source.onerror = function() {
    source.close();
    const overlay = document.getElementById('overlay');
    if (overlay) overlay.style.display = 'flex';
    setTimeout(() => window.close(), 1500);
};

const slider = document.getElementById('vol-slider');
const volLabel = document.getElementById('vol-label');

if (slider && volLabel) {
    slider.oninput = function() {
        const val = this.value;
        volLabel.innerText = Math.round(parseFloat(val) * 100) + '%';
        // Corregido: Manejando la promesa del fetch
        fetch('/volume?level=' + val).catch(err => console.error("Error al cambiar volumen:", err));
    };
}

function formatTime(secs) {
    if (isNaN(secs) || secs < 0) return "00:00";
    let m = Math.floor(secs / 60);
    let s = Math.floor(secs % 60);
    return (m < 10 ? "0" + m : m) + ":" + (s < 10 ? "0" + s : s);
}

source.onmessage = function(event) {
    /** * Definimos el tipo de datos para que el IDE no marque "Unresolved variable"
     * @type {{ bars: number[], intensities: number[], energy: number, combo: number, speed: number, paused: boolean, current: number, total: number }}
     */
    const data = JSON.parse(event.data);
    const numBars = data.bars.length;

    if (numBars !== currentNumBars) {
        console.log("Reconstruyendo visualizador para " + numBars + " barras.");
        if (container) container.innerHTML = '';
        barElements = [];
        for(let i=0; i<numBars; i++) {
            let wrap = document.createElement('div'); wrap.className = 'bar-wrapper';
            let bar = document.createElement('div'); bar.className = 'bar';
            let lbl = document.createElement('div'); lbl.className = 'label';
            // Corregido: Convertido a String para evitar error de tipos
            lbl.innerText = String(i + 1);
            wrap.appendChild(bar); wrap.appendChild(lbl);
            if (container) container.appendChild(wrap);
            barElements.push(bar);
        }
        currentNumBars = numBars;
    }

    // Actualización de UI con conversiones a String para el IDE
    const statusElem = document.getElementById('status');
    if (statusElem) {
        statusElem.innerText = data.paused ? '⏸ EN PAUSA' : '▶ REPRODUCIENDO';
        statusElem.style.color = data.paused ? '#ff3366' : '#00ffcc';
    }

    const energyElem = document.getElementById('energy-val');
    if (energyElem) energyElem.innerText = Math.round(data.energy * 100) + '%';

    const comboElem = document.getElementById('combo-val');
    if (comboElem) comboElem.innerText = data.combo + 'x';

    const speedElem = document.getElementById('speed-val');
    if (speedElem) speedElem.innerText = data.speed.toFixed(2) + 'x';

    let percent = (data.current / data.total) * 100;
    if (isNaN(percent)) percent = 0;

    const progressFill = document.getElementById('progress-fill');
    if (progressFill) progressFill.style.width = percent + '%';

    const timeText = document.getElementById('time-text');
    if (timeText) timeText.innerText = formatTime(data.current) + " / " + formatTime(data.total);

    // Cálculos de rangos
    let bassEnd = Math.max(1, Math.floor(numBars * 0.15));
    let snareEnd = Math.max(bassEnd + 1, Math.floor(numBars * 0.50));

    let maxBassHit = 0, maxSnareHit = 0, maxHighHit = 0;

    for(let i=0; i<bassEnd; i++) maxBassHit = Math.max(maxBassHit, data.intensities[i] || 0);
    for(let i=bassEnd; i<snareEnd; i++) maxSnareHit = Math.max(maxSnareHit, data.intensities[i] || 0);
    for(let i=snareEnd; i<numBars; i++) maxHighHit = Math.max(maxHighHit, data.intensities[i] || 0);

    // Corregido: Todas las opacidades y escalas convertidas a String explícitamente
    if (kickText) {
        kickText.style.opacity = String(0.2 + (maxBassHit * 0.8));
        kickText.style.transform = `scale(${1 + (maxBassHit * 0.25)})`;
        kickText.style.textShadow = maxBassHit > 0.4 ? `0 0 ${maxBassHit * 15}px #ff3366` : 'none';
    }

    if (snareText) {
        snareText.style.opacity = String(0.2 + (maxSnareHit * 0.8));
        snareText.style.transform = `scale(${1 + (maxSnareHit * 0.20)})`;
        snareText.style.textShadow = maxSnareHit > 0.4 ? `0 0 ${maxSnareHit * 15}px #00ffcc` : 'none';
    }

    if (hatText) {
        hatText.style.opacity = String(0.2 + (maxHighHit * 0.8));
        hatText.style.transform = `scale(${1 + (maxHighHit * 0.15)})`;
        hatText.style.textShadow = maxHighHit > 0.4 ? `0 0 ${maxHighHit * 15}px #33ccff` : 'none';
    }

    // Dibujar barras
    for(let i=0; i<numBars; i++) {
        let intensity = data.intensities[i] || 0;
        let h = (Math.pow(Math.min(1.0, data.bars[i]), 1.5) * 55) + (Math.pow(intensity, 1.2) * 45);
        if (h < 2) h = 2; if (h > 100) h = 100;

        barElements[i].style.height = h + '%';

        let color, shadow;
        let isBass = i < bassEnd;
        let isMid = i < snareEnd;

        if (intensity > 0.8) { color = '#ffffff'; shadow = '0 0 15px #fff'; }
        else if (intensity > 0.4) {
            color = isBass ? '#ff3366' : (isMid ? '#00ffcc' : '#33ccff');
            shadow = `0 0 15px ${color}`;
        } else {
            color = isBass ? '#881133' : (isMid ? '#006655' : '#115588');
            shadow = 'none';
        }
        barElements[i].style.backgroundColor = color;
        barElements[i].style.boxShadow = shadow;
    }
};