// ========== НАСТРОЙКИ ==========
let allParts = [];
let currentPart = 0;
let isSending = false;
let retryScheduled = false;
let delay = 1.0;
let autoSend = true;
let totalParts = 0;

// ========== DOM-ЭЛЕМЕНТЫ ПАНЕЛИ ==========
let panel, queueLabel, progressBar, delayInput, autoCheckbox, actionBtn, prevBtn, nextBtn, statusLabel, miniIcon, projectLabel, refreshBtn;

function getTextarea() {
    return document.querySelector('textarea');
}

// ========== ПАНЕЛЬ УПРАВЛЕНИЯ ==========
function createPanel() {
    panel = document.createElement('div');
    panel.id = 'c2p-panel';
    panel.innerHTML = `
        <div id="c2p-header">
            <span>Code2Prompt</span>
            <button id="c2p-refresh" title="Обновить данные с сервера">🔄</button>
            <button id="c2p-minimize" title="Свернуть">–</button>
        </div>
        <div id="c2p-body">
            <div class="c2p-project" id="c2p-project" title="Название проекта">—</div>
            <div class="c2p-row">
                <span>Очередь:</span>
                <span id="c2p-queue">0/0</span>
            </div>
            <div class="c2p-progress">
                <div id="c2p-progress-bar"></div>
            </div>
            <div class="c2p-row">
                <span>Задержка (сек):</span>
                <input id="c2p-delay" type="number" value="1.0" step="0.5" min="0.5" max="10" style="width:50px">
            </div>
            <div class="c2p-row">
                <label><input id="c2p-auto" type="checkbox" checked> Автоотправка</label>
            </div>
            <div class="c2p-buttons">
                <button id="c2p-prev" title="Предыдущая часть">◀</button>
                <button id="c2p-action">▶ Подключиться</button>
                <button id="c2p-next" title="Следующая часть">▶</button>
            </div>
            <div class="c2p-status" id="c2p-status">Готов</div>
        </div>
    `;
    document.body.appendChild(panel);

    queueLabel = document.getElementById('c2p-queue');
    progressBar = document.getElementById('c2p-progress-bar');
    delayInput = document.getElementById('c2p-delay');
    autoCheckbox = document.getElementById('c2p-auto');
    actionBtn = document.getElementById('c2p-action');
    prevBtn = document.getElementById('c2p-prev');
    nextBtn = document.getElementById('c2p-next');
    statusLabel = document.getElementById('c2p-status');
    projectLabel = document.getElementById('c2p-project');
    refreshBtn = document.getElementById('c2p-refresh');

    delayInput.addEventListener('change', () => delay = parseFloat(delayInput.value) || 1.0);
    autoCheckbox.addEventListener('change', () => autoSend = autoCheckbox.checked);

    // Обновить данные с сервера (без отправки)
    refreshBtn.addEventListener('click', async () => {
        setStatus('Проверка сервера...');
        try {
            const response = await fetch('http://localhost:9090/context/parts?id=0');
            if (!response.ok) throw new Error('Server returned ' + response.status);
            
            const firstPart = await response.json();
            totalParts = firstPart.total;
            allParts = [];
            
            for (let i = 0; i < totalParts; i++) {
                const part = await fetch(`http://localhost:9090/context/parts?id=${i}`).then(r => r.json());
                allParts.push(part);
            }
            
            currentPart = 0;
            isSending = false;
            retryScheduled = false;
            updateQueue();
            updateButton();
            await loadProjectName();
            setStatus('Готово. Загружено ' + totalParts + ' частей');
        } catch(e) {
            setStatus('⚠ Сервер недоступен');
            if (actionBtn) {
                actionBtn.textContent = '⚠ Сервер недоступен';
                actionBtn.style.background = '#f38ba8';
                actionBtn.style.color = '#1e1e2e';
                setTimeout(() => updateButton(), 3000);
            }
        }
    });

    document.getElementById('c2p-minimize').addEventListener('click', () => {
        document.getElementById('c2p-body').style.display = 'none';
        panel.classList.add('c2p-minimized');
        panel.style.display = 'none';
        showMiniIcon();
    });

    // Кнопка Подключиться/Пауза/Продолжить/Готово
    actionBtn.addEventListener('click', () => {
        if (!isSending && currentPart >= totalParts && totalParts > 0) {
            currentPart = 0;
            totalParts = 0;
            allParts = [];
            updateQueue();
            updateButton();
            setStatus('Готов');
            return;
        }
        
        if (isSending) {
            isSending = false;
            updateButton();
            setStatus('Пауза');
        } else {
            if (allParts.length === 0) {
                loadParts();
            } else {
                isSending = true;
                updateButton();
                insertAndSend();
            }
        }
    });

    prevBtn.addEventListener('click', () => {
        if (allParts.length === 0) return;
        currentPart = Math.max(0, currentPart - 1);
        updateQueue();
        showPartInTextarea();
        setStatus('Часть ' + (currentPart + 1) + '/' + totalParts);
    });

    nextBtn.addEventListener('click', () => {
        if (allParts.length === 0) return;
        currentPart = Math.min(totalParts - 1, currentPart + 1);
        updateQueue();
        showPartInTextarea();
        setStatus('Часть ' + (currentPart + 1) + '/' + totalParts);
    });

    makeDraggable(panel);
}

function showMiniIcon() {
    if (miniIcon) return;
    miniIcon = document.createElement('div');
    miniIcon.id = 'c2p-mini';
    miniIcon.innerHTML = 'C2P';
    miniIcon.title = 'Развернуть Code2Prompt (двойной клик)';
    miniIcon.style.left = panel.style.left || 'auto';
    miniIcon.style.top = panel.style.top || '100px';
    miniIcon.style.right = panel.style.right || '20px';
    miniIcon.addEventListener('dblclick', () => {
        document.getElementById('c2p-body').style.display = '';
        panel.classList.remove('c2p-minimized');
        panel.style.display = '';
        panel.style.top = miniIcon.style.top;
        panel.style.left = miniIcon.style.left;
        panel.style.right = miniIcon.style.right;
        miniIcon.remove();
        miniIcon = null;
    });
    document.body.appendChild(miniIcon);
    makeDraggable(miniIcon);
}

function setStatus(text) {
    if (statusLabel) statusLabel.textContent = text;
}

function showPartInTextarea() {
    const textarea = getTextarea();
    if (!textarea || currentPart >= allParts.length) return;
    const part = allParts[currentPart];
    const prefix = `[ЧАСТЬ ${part.index}/${totalParts}]\n\n`;
    setNativeValue(textarea, prefix + part.content);
}

function addStyles() {
    const style = document.createElement('style');
    style.textContent = `
        #c2p-panel {
            position: fixed; top: 100px; right: 20px; z-index: 9999;
            width: 240px; background: #1e1e2e; color: #cdd6f4;
            border-radius: 12px; font-family: sans-serif; font-size: 13px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.4); overflow: hidden;
        }
        #c2p-header {
            background: #313244; padding: 10px 14px; font-weight: bold;
            cursor: move; user-select: none; font-size: 14px;
            display: flex; justify-content: space-between; align-items: center; gap: 4px;
        }
        #c2p-refresh {
            background: none; border: none; color: #cdd6f4; cursor: pointer;
            font-size: 14px; padding: 0 2px; line-height: 1;
        }
        #c2p-refresh:hover { color: #89b4fa; }
        #c2p-minimize {
            background: none; border: none; color: #cdd6f4; cursor: pointer;
            font-size: 18px; padding: 0 4px; line-height: 1;
        }
        #c2p-minimize:hover { color: #f38ba8; }
        #c2p-body { padding: 10px 14px; }
        .c2p-project {
            font-size: 12px; color: #89b4fa; text-align: center;
            padding: 4px 8px; margin-bottom: 6px;
            background: #313244; border-radius: 4px;
            white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
        }
        .c2p-minimized { width: auto !important; }
        .c2p-row { display: flex; justify-content: space-between; align-items: center; margin: 6px 0; }
        .c2p-progress { height: 6px; background: #313244; border-radius: 3px; margin: 8px 0; overflow: hidden; }
        #c2p-progress-bar { height: 100%; width: 0%; background: #89b4fa; transition: width 0.3s; }
        .c2p-buttons { display: flex; gap: 4px; margin-top: 8px; }
        .c2p-buttons button { padding: 8px; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: bold; transition: transform 0.1s; }
        .c2p-buttons button:active { transform: scale(0.95); }
        #c2p-prev, #c2p-next { width: 32px; background: #45475a; color: #cdd6f4; flex: 0; }
        #c2p-action { flex: 1; }
        #c2p-delay { background: #313244; color: #cdd6f4; border: 1px solid #45475a; border-radius: 4px; padding: 2px 6px; width: 50px; }
        .c2p-status {
            margin-top: 8px;
            padding: 4px 8px;
            background: #313244;
            border-radius: 4px;
            font-size: 11px;
            text-align: center;
            color: #a6adc8;
        }
        #c2p-mini {
            position: fixed; top: 100px; right: 20px; z-index: 9999;
            width: 36px; height: 36px; background: #1e1e2e; color: #89b4fa;
            border-radius: 50%; display: flex; align-items: center; justify-content: center;
            cursor: pointer; font-weight: bold; font-size: 11px;
            box-shadow: 0 4px 16px rgba(0,0,0,0.4);
        }
        #c2p-mini:hover { background: #313244; }
    `;
    document.head.appendChild(style);
}

function makeDraggable(el) {
    let offsetX, offsetY, startX, startY;

    el.addEventListener('mousedown', (e) => {
        startX = e.clientX; startY = e.clientY;
        offsetX = el.offsetLeft; offsetY = el.offsetTop;
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
    });

    function onMove(e) {
        el.style.left = (offsetX + e.clientX - startX) + 'px';
        el.style.top = (offsetY + e.clientY - startY) + 'px';
        el.style.right = 'auto';
    }
    function onUp() {
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
    }
}

function updateQueue() {
    if (queueLabel) queueLabel.textContent = `${currentPart + 1}/${totalParts}`;
    if (progressBar) progressBar.style.width = totalParts > 0 ? `${(currentPart / totalParts) * 100}%` : '0%';
}

function updateButton() {
    if (!actionBtn) return;
    if (totalParts > 0 && currentPart >= totalParts && !isSending) {
        actionBtn.textContent = '✓ Готово';
        actionBtn.style.background = '#6c7086';
        actionBtn.style.color = '#cdd6f4';
    } else if (isSending) {
        actionBtn.textContent = '⏸ Пауза';
        actionBtn.style.background = '#f9e2af';
        actionBtn.style.color = '#1e1e2e';
    } else if (allParts.length > 0) {
        actionBtn.textContent = '▶ Продолжить';
        actionBtn.style.background = '#a6e3a1';
        actionBtn.style.color = '#1e1e2e';
    } else {
        actionBtn.textContent = '▶ Подключиться';
        actionBtn.style.background = '#89b4fa';
        actionBtn.style.color = '#1e1e2e';
    }
}

// ========== ЛОГИКА ОТПРАВКИ ==========
function setNativeValue(element, value) {
    const prototypeValueSetter = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')?.set;
    if (prototypeValueSetter) {
        prototypeValueSetter.call(element, value);
    } else {
        element.value = value;
    }
    element.dispatchEvent(new Event('input', { bubbles: true }));
}

function clickSendButton() {
    const sendBtn = document.querySelector(
        'div[role="button"]:not(.ds-button--disabled) svg path[d*="M8.3125"]'
    )?.closest('div[role="button"]');
    
    if (sendBtn) {
        sendBtn.click();
        console.log('Code2Prompt: send button clicked');
        return true;
    }
    return false;
}

async function loadProjectName() {
    try {
        const res = await fetch('http://localhost:9090/project');
        const data = await res.json();
        if (projectLabel && data.name) {
            projectLabel.textContent = data.name;
            projectLabel.title = data.name;
        }
    } catch(e) {
        if (projectLabel) projectLabel.textContent = '—';
    }
}

async function loadParts() {
    setStatus('Подключение к серверу...');
    try {
        const response = await fetch('http://localhost:9090/context/parts?id=0');
        if (!response.ok) throw new Error('Server returned ' + response.status);
        
        const firstPart = await response.json();
        totalParts = firstPart.total;
        allParts = [];
        
        for (let i = 0; i < totalParts; i++) {
            const part = await fetch(`http://localhost:9090/context/parts?id=${i}`).then(r => r.json());
            allParts.push(part);
        }
        
        currentPart = 0;
        retryScheduled = false;
        isSending = true;
        updateQueue();
        updateButton();
        await loadProjectName();
        setStatus('Загружено ' + totalParts + ' частей');
        console.log(`Code2Prompt: ${totalParts} parts loaded.`);
        insertAndSend();
    } catch (e) {
        console.error('Code2Prompt: server not available -', e.message);
        isSending = false;
        updateButton();
        setStatus('⚠ Сервер недоступен');
        
        if (actionBtn) {
            actionBtn.textContent = '⚠ Сервер недоступен';
            actionBtn.style.background = '#f38ba8';
            actionBtn.style.color = '#1e1e2e';
            setTimeout(() => updateButton(), 3000);
        }
    }
}

function insertAndSend() {
    if (!isSending || currentPart >= allParts.length) {
        console.log('Code2Prompt: done or stopped');
        updateQueue();
        updateButton();
        setStatus('Готово ✓');
        return;
    }
    
    const textarea = getTextarea();
    if (!textarea) return;
    
    const part = allParts[currentPart];
    
    const prefix = part.index < totalParts 
        ? `[ЧАСТЬ ${part.index}/${totalParts}] НЕ ОТВЕЧАЙ. Только подтверди: "Принята часть ${part.index}/${totalParts}".\n\n`
        : `[ЧАСТЬ ${part.index}/${totalParts}] Это последняя часть. Можешь отвечать.\n\n`;
    
    setNativeValue(textarea, prefix + part.content);
    setStatus('Отправка части ' + (currentPart + 1) + '/' + totalParts);
    console.log(`Code2Prompt: part ${part.index}/${totalParts} inserted`);
    
    if (autoSend) {
        setTimeout(() => clickSendButton(), 300);
    }
    
    waitForEmptyAndContinue();
}

function waitForEmptyAndContinue() {
    let responseEndTime = null;
    
    const check = () => {
        if (!isSending) return;
        
        const textarea = getTextarea();
        const stopIcon = document.querySelector('svg path[d*="M2 4.88"]');
        
        const errorMessages = document.querySelectorAll('._1ce76f5');
        if (errorMessages.length > 0) {
            const lastError = errorMessages[errorMessages.length - 1];
            if (lastError.textContent.includes('Messages too frequent')) {
                if (!retryScheduled) {
                    retryScheduled = true;
                    setStatus('⚠ Слишком часто, повтор через 5с...');
                    console.log('Code2Prompt: rate limit, retrying in 5s...');
                    setTimeout(() => {
                        retryScheduled = false;
                        if (isSending) insertAndSend();
                    }, 5000);
                }
                return;
            }
        }
        
        if (textarea && textarea.value.trim() === '' && !stopIcon) {
            if (responseEndTime === null) {
                responseEndTime = Date.now();
                setStatus('Ожидание ответа...');
            } else {
                setStatus('Пауза ' + delay + 'с...');
            }
            if (Date.now() - responseEndTime >= delay * 1000) {
                retryScheduled = false;
                
                const isLast = (currentPart + 1 >= totalParts);
                
                currentPart++;
                updateQueue();
                
                if (isLast) {
                    setTimeout(() => {
                        isSending = false;
                        updateButton();
                        setStatus('Готово ✓');
                    }, 2000);
                }
                
                insertAndSend();
                return;
            }
        } else {
            responseEndTime = null;
            if (stopIcon) {
                setStatus('Идёт ответ ИИ...');
            }
        }
        
        setTimeout(check, 300);
    };
    
    check();
}

// ========== ИНИЦИАЛИЗАЦИЯ ==========
function attachToTextarea(textarea) {
    textarea.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && e.ctrlKey) {
            e.preventDefault();
            e.stopPropagation();
            loadParts();
        }
    }, true);
}

function init() {
    addStyles();
    createPanel();
    
    const observer = new MutationObserver(() => {
        const textarea = getTextarea();
        if (textarea && !textarea.dataset.c2pAttached) {
            textarea.dataset.c2pAttached = 'true';
            attachToTextarea(textarea);
        }
    });
    observer.observe(document.body, { childList: true, subtree: true });
}

init();