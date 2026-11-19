// ========== НАСТРОЙКИ ==========
const SERVER_URL = 'https://localhost:9090';
const MAX_FILES_PER_MESSAGE = 10;

let allParts = [];
let projectName = 'code2prompt';
let currentPart = 0;
let isSending = false;
let isBusy = false;
let retryScheduled = false;
let delay = 1.0;
let autoSend = false;
let totalParts = 0;
let activeTab = 'files'; // 'files' | 'text'
let lastAttachTimestamp = 0;

// ========== DOM-ЭЛЕМЕНТЫ ПАНЕЛИ ==========
let panel, queueLabel, progressBar, delayInput, autoCheckbox, actionBtn, prevBtn, nextBtn;
let statusLabel, miniIcon, projectLabel, refreshBtn, tabFilesBtn, tabTextBtn;
let filesView, textView, composerInfoLabel;

// ========== АДАПТЕР ПЛАТФОРМ (DEEPSEEK / GEMINI / GOOGLE AI MODE) ==========

const PlatformAdapter = {
    getMode() {
        const host = window.location.hostname;
        if (host.includes('gemini.google.com')) return 'gemini';
        if (host.includes('google.com') && window.location.pathname.startsWith('/search')) return 'google_ai';
        return 'deepseek';
    },

    isSupportedPage() {
        if (this.getMode() === 'google_ai') {
            return window.location.search.includes('udm=50');
        }
        return true;
    },

    getName() {
        const mode = this.getMode();
        if (mode === 'gemini') return 'Gemini';
        if (mode === 'google_ai') return 'Google AI';
        return 'DeepSeek';
    },

    getInput() {
        const mode = this.getMode();

        if (mode === 'deepseek') {
            return document.querySelector('textarea');
        }

        if (mode === 'gemini') {
            return document.querySelector('rich-textarea div[contenteditable="true"], div.ql-editor[contenteditable="true"]');
        }

        // Google AI Mode: исключаем верхнюю строку поиска (name="q" и form[action="/search"])
        const candidates = Array.from(
            document.querySelectorAll('textarea:not([name="q"]), div[contenteditable="true"][role="textbox"], div[contenteditable="true"]')
        ).filter(el => {
            if (el.closest('form[action="/search"]')) return false;
            if (el.closest('#c2p-panel')) return false;
            const rect = el.getBoundingClientRect();
            return rect.width > 80 && rect.height > 15 && el.offsetParent !== null;
        });

        return candidates.length > 0 ? candidates[candidates.length - 1] : null;
    },

    isInputEmpty(el) {
        if (!el) return true;
        if (el.tagName === 'TEXTAREA' || el.tagName === 'INPUT') {
            return el.value.trim() === '';
        }
        return el.textContent.trim() === '';
    },

    setInputValue(el, text) {
        if (!el) return;

        if (this.getMode() === 'deepseek') {
            const prototypeValueSetter = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')?.set;
            if (prototypeValueSetter) {
                prototypeValueSetter.call(el, text);
            } else {
                el.value = text;
            }
            el.dispatchEvent(new Event('input', { bubbles: true }));
            return;
        }

        el.focus();
        if (el.tagName === 'TEXTAREA' || el.tagName === 'INPUT') {
            const prototypeValueSetter = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')?.set;
            if (prototypeValueSetter) {
                prototypeValueSetter.call(el, text);
            } else {
                el.value = text;
            }
            el.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'insertText', data: text }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
            el.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true }));
        } else {
            el.textContent = text;
            el.dispatchEvent(new InputEvent('input', {
                bubbles: true,
                cancelable: true,
                inputType: 'insertText',
                data: text
            }));
        }
    },

    // Поиск единственного целевого <input type="file"> для текстовых документов
    findSingleFileInput() {
        const allInputs = Array.from(document.querySelectorAll('input[type="file"]'))
            .filter(inp => !inp.closest('#c2p-panel') && !inp.disabled);

        if (allInputs.length === 0) return null;

        // Исключаем инпуты, принимающие исключительно изображения/аудио/видео
        const docInputs = allInputs.filter(inp => {
            const accept = (inp.getAttribute('accept') || '').toLowerCase();
            if (!accept) return true;
            if (accept.includes('.txt') || accept.includes('text/') || accept.includes('*/*')) return true;
            const mediaOnly = (accept.includes('image/') || accept.includes('video/') || accept.includes('audio/')) &&
                !accept.includes('pdf') && !accept.includes('text') && !accept.includes('.txt');
            return !mediaOnly;
        });

        const pool = docInputs.length > 0 ? docInputs : allInputs;
        const multi = pool.find(inp => inp.multiple);
        if (multi) return multi;

        return pool[pool.length - 1];
    },

    // Прикрепление массива файлов в чат без дублирования
    async attachFiles(files) {
        if (!files || files.length === 0) return false;

        const dt = new DataTransfer();
        files.forEach(f => dt.items.add(f));

        // 1. Пробуем нативный <input type="file">
        const fileInput = this.findSingleFileInput();
        if (fileInput) {
            try {
                fileInput.files = dt.files;
                fileInput.dispatchEvent(new Event('change', { bubbles: true }));
                fileInput.dispatchEvent(new Event('input', { bubbles: true }));
                console.log(`📎 Code2Prompt [${this.getName()}]: attached ${files.length} file(s) via <input type="file">`);
                return true;
            } catch (e) {
                console.warn('⚠️ File input assignment failed, trying paste fallback:', e);
            }
        }

        // 2. Запасной вариант: эмуляция ClipboardEvent('paste') в поле ввода
        const inputEl = this.getInput();
        if (inputEl) {
            inputEl.focus();
            const pasteEvent = new ClipboardEvent('paste', {
                bubbles: true,
                cancelable: true,
                clipboardData: dt
            });
            inputEl.dispatchEvent(pasteEvent);
            console.log(`📎 Code2Prompt [${this.getName()}]: dispatched paste with ${files.length} file(s)`);
            return true;
        }

        return false;
    },

    // Элементы нижней панели ввода (исключая историю переписки выше и саму панель C2P)
    getBottomComposerElements() {
        const inputEl = this.getInput();
        if (!inputEl) return [];

        const inputRect = inputEl.getBoundingClientRect();
        const topBound = Math.max(0, inputRect.top - 260);
        const bottomBound = Math.min(window.innerHeight, inputRect.bottom + 90);

        return Array.from(document.querySelectorAll('*')).filter(el => {
            if (el.closest('#c2p-panel') || el.closest('#c2p-mini')) return false;
            if (el.getAttribute('role') === 'tooltip') return false;
            const rect = el.getBoundingClientRect();
            if (rect.width === 0 || rect.height === 0) return false;
            return rect.top >= topBound && rect.bottom <= bottomBound;
        });
    },

    // Поиск имён уже прикреплённых файлов проекта в DOM нижней панели ввода
    getStagedProjectFileNames(expectedNames) {
        const found = new Set();
        if (!expectedNames || expectedNames.length === 0) return found;

        const lowerMap = new Map();
        expectedNames.forEach(name => lowerMap.set(name.toLowerCase(), name));

        const elements = this.getBottomComposerElements();
        for (const el of elements) {
            const combined = [
                el.textContent || '',
                el.getAttribute('title') || '',
                el.getAttribute('aria-label') || '',
                el.getAttribute('data-tooltip') || ''
            ].join(' ').toLowerCase();

            if (!combined.includes('.txt')) continue;

            for (const [lowerName, originalName] of lowerMap.entries()) {
                if (!found.has(originalName) && combined.includes(lowerName)) {
                    found.add(originalName);
                }
            }
        }

        return found;
    },

    // Точный подсчёт уникальных чипов вложений в поле ввода (без удвоения на вложенных тегах и тултипах)
    countTotalAttachmentsInComposer(expectedNames) {
        const stagedProjectFiles = this.getStagedProjectFileNames(expectedNames);
        const elements = this.getBottomComposerElements();

        // Собираем кандидаты на отдельные чипы вложений (кнопки удаления чипа или контейнеры с расширением файла)
        const rawCandidates = [];
        for (const el of elements) {
            const rect = el.getBoundingClientRect();
            // Игнорируем крупные обёртки всей формы ввода
            if (rect.width > 420 || rect.height > 120) continue;

            const aria = (el.getAttribute('aria-label') || '').toLowerCase();
            const title = (el.getAttribute('title') || '').toLowerCase();
            const text = (el.textContent || '').trim().toLowerCase();

            const isRemoveBtn = (el.tagName === 'BUTTON' || el.getAttribute('role') === 'button') &&
                (aria.includes('удалить') || aria.includes('remove') || aria.includes('delete') ||
                    title.includes('удалить') || title.includes('remove')) &&
                !aria.includes('запрос') && !aria.includes('query') && !aria.includes('поиск');

            const hasFileBadge = /\.(txt|pdf|docx?|xlsx?|csv|json|xml|md|java|js|ts|py|png|jpe?g|webp)\b/.test(
                `${title} ${aria} ${text}`
            );

            if (isRemoveBtn || hasFileBadge) {
                rawCandidates.push(el);
            }
        }

        // Дедупликация: если один элемент вложен в другой или находится в пределах того же чипа, считаем как 1
        const uniqueChips = [];
        for (const el of rawCandidates) {
            const rect = el.getBoundingClientRect();
            const isDuplicate = uniqueChips.some(existing => {
                if (existing.contains(el) || el.contains(existing)) return true;
                const eRect = existing.getBoundingClientRect();
                const sameRow = Math.abs(rect.top - eRect.top) < 35;
                const closeX = Math.abs(rect.left - eRect.left) < 150;
                return sameRow && closeX;
            });
            if (!isDuplicate) {
                uniqueChips.push(el);
            }
        }

        return Math.max(stagedProjectFiles.size, uniqueChips.length);
    },

    clickSend() {
        const mode = this.getMode();

        if (mode === 'deepseek') {
            const sendBtn = document.querySelector(
                'div[role="button"]:not(.ds-button--disabled) svg path[d*="M8.3125"]'
            )?.closest('div[role="button"]');
            if (sendBtn) {
                sendBtn.click();
                return true;
            }
            return false;
        }

        if (mode === 'gemini') {
            const sendBtn = document.querySelector(
                'button.send-button:not([disabled]), button[aria-label*="Send"]:not([disabled]), button[aria-label*="Отправить"]:not([disabled])'
            );
            if (sendBtn) {
                sendBtn.click();
                return true;
            }
            return false;
        }

        const inputEl = this.getInput();
        if (!inputEl) return false;

        let container = inputEl.parentElement;
        for (let i = 0; i < 6 && container?.parentElement; i++) {
            if (container.querySelectorAll('button, div[role="button"]').length >= 2) break;
            container = container.parentElement;
        }

        const inputRect = inputEl.getBoundingClientRect();
        const ignoreWords = ['микрофон', 'mic', 'voice', 'голос', 'изображ', 'image', 'camera', 'файл', 'file', 'attach', 'прикреп', 'очист', 'clear', 'close', 'закрыть'];

        const buttons = Array.from((container || document).querySelectorAll('button:not([disabled]), div[role="button"]:not([aria-disabled="true"])'))
            .filter(btn => {
                if (btn.closest('#c2p-panel')) return false;
                const rect = btn.getBoundingClientRect();
                if (rect.width === 0 || rect.height === 0) return false;
                if (rect.left < inputRect.left) return false;

                const label = (btn.getAttribute('aria-label') || btn.title || btn.textContent || '').toLowerCase();
                if (ignoreWords.some(w => label.includes(w))) return false;

                return label.includes('отправить') ||
                    label.includes('send') ||
                    label.includes('спросить') ||
                    label.includes('ask') ||
                    label.includes('поиск') ||
                    label.includes('search') ||
                    btn.type === 'submit' ||
                    btn.querySelector('svg') !== null;
            });

        if (buttons.length > 0) {
            buttons[buttons.length - 1].click();
        }

        setTimeout(() => {
            if (!this.isInputEmpty(inputEl)) {
                ['keydown', 'keypress', 'keyup'].forEach(eventType => {
                    inputEl.dispatchEvent(new KeyboardEvent(eventType, {
                        key: 'Enter',
                        code: 'Enter',
                        keyCode: 13,
                        which: 13,
                        shiftKey: false,
                        bubbles: true,
                        cancelable: true
                    }));
                });
            }
        }, 250);

        return true;
    },

    isGenerating() {
        const mode = this.getMode();

        if (mode === 'deepseek') {
            return Boolean(document.querySelector('svg path[d*="M2 4.88"]'));
        }

        const stopBtn = document.querySelector(
            'button[aria-label*="Stop"], button[aria-label*="Остановить"], button[aria-label*="Прекратить"], div[role="button"][aria-label*="Stop"], div[role="button"][aria-label*="Остановить"], .stop-icon'
        );
        if (stopBtn && stopBtn.offsetParent !== null) {
            return true;
        }

        if (mode === 'google_ai') {
            const inputEl = this.getInput();
            if (inputEl && (inputEl.disabled || inputEl.getAttribute('aria-disabled') === 'true')) {
                return true;
            }
        }

        return false;
    },

    hasRateLimitError() {
        if (this.getMode() === 'deepseek') {
            const errorMessages = document.querySelectorAll('._1ce76f5');
            if (errorMessages.length > 0) {
                const lastError = errorMessages[errorMessages.length - 1];
                return lastError.textContent.includes('Messages too frequent');
            }
        }
        return false;
    }
};

// ========== API-ЗАПРОСЫ К ЛОКАЛЬНОМУ СЕРВЕРУ ==========

async function apiRequest(endpoint, params = {}) {
    const urlParams = new URLSearchParams(params);
    const url = `${SERVER_URL}${endpoint}${urlParams.toString() ? '?' + urlParams : ''}`;

    const response = await fetch(url, {
        headers: {
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache',
            'Expires': '0'
        }
    });

    if (!response.ok) {
        const text = await response.text();
        throw new Error(`Server returned ${response.status}: ${text}`);
    }

    return await response.json();
}

function sanitizeFileName(name) {
    return (name || 'code2prompt').replace(/[\\/:*?"<>|]/g, '_').trim() || 'code2prompt';
}

function getExpectedFileNames() {
    const base = sanitizeFileName(projectName);
    return allParts.map((part, idx) => part.filename || part.fileName || `${base}_part${idx + 1}.txt`);
}

async function syncFromServer() {
    const [projData, firstPart] = await Promise.all([
        apiRequest('/project').catch(() => ({ name: 'Code2Prompt' })),
        apiRequest('/context/parts', { id: 0 })
    ]);

    if (projData && projData.name) {
        projectName = projData.name;
        if (projectLabel) {
            projectLabel.textContent = projectName;
            projectLabel.title = projectName;
        }
    }

    totalParts = firstPart.total || 0;
    const loaded = [];
    for (let i = 0; i < totalParts; i++) {
        const part = i === 0 ? firstPart : await apiRequest('/context/parts', { id: i });
        loaded.push(part);
    }
    allParts = loaded;
}

// ========== ПАНЕЛЬ УПРАВЛЕНИЯ ==========

function createPanel() {
    if (document.getElementById('c2p-panel')) return;

    panel = document.createElement('div');
    panel.id = 'c2p-panel';
    panel.innerHTML = `
        <div id="c2p-header">
            <span>Code2Prompt <small style="font-weight:normal;opacity:0.7;">(${PlatformAdapter.getName()})</small></span>
            <div>
                <button id="c2p-refresh" title="Обновить данные с сервера">🔄</button>
                <button id="c2p-minimize" title="Свернуть">–</button>
            </div>
        </div>
        <div class="c2p-tabs">
            <button id="c2p-tab-files" class="c2p-tab active">📎 Файлами</button>
            <button id="c2p-tab-text" class="c2p-tab">💬 Текстом</button>
        </div>
        <div id="c2p-body">
            <div class="c2p-project" id="c2p-project" title="Название проекта">—</div>
            <div class="c2p-row">
                <span id="c2p-queue-title">Файлов в чате:</span>
                <span id="c2p-queue">0/0</span>
            </div>
            <div class="c2p-progress">
                <div id="c2p-progress-bar"></div>
            </div>

            <div id="c2p-files-view">
                <div class="c2p-hint" id="c2p-composer-info">В поле ввода: 0 (макс. ${MAX_FILES_PER_MESSAGE})</div>
            </div>

            <div id="c2p-text-view" style="display:none;">
                <div class="c2p-row">
                    <span>Задержка (сек):</span>
                    <input id="c2p-delay" type="number" value="1.0" step="0.5" min="0.5" max="10" style="width:52px">
                </div>
                <div class="c2p-row">
                    <label><input id="c2p-auto" type="checkbox"> Автоотправка</label>
                </div>
            </div>

            <div class="c2p-buttons">
                <button id="c2p-prev" title="Предыдущая часть" style="display:none;">◀</button>
                <button id="c2p-action">📎 Прикрепить файлы</button>
                <button id="c2p-next" title="Следующая часть" style="display:none;">▶</button>
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
    tabFilesBtn = document.getElementById('c2p-tab-files');
    tabTextBtn = document.getElementById('c2p-tab-text');
    filesView = document.getElementById('c2p-files-view');
    textView = document.getElementById('c2p-text-view');
    composerInfoLabel = document.getElementById('c2p-composer-info');

    tabFilesBtn.addEventListener('click', () => switchTab('files'));
    tabTextBtn.addEventListener('click', () => switchTab('text'));

    delayInput.addEventListener('change', () => {
        delay = parseFloat(delayInput.value) || 1.0;
    });

    autoCheckbox.addEventListener('change', () => {
        autoSend = autoCheckbox.checked;
    });

    // Обновление данных с сервера с гарантированным сбросом блокировки
    refreshBtn.addEventListener('click', async () => {
        if (isBusy) return;
        isBusy = true;
        isSending = false;
        retryScheduled = false;
        refreshBtn.disabled = true;
        setStatus('Обновление с сервера...');
        updateAllUI();

        try {
            await syncFromServer();
            currentPart = 0;
            setStatus(`Загружено частей: ${totalParts}`);
        } catch (e) {
            console.error('❌ Refresh error:', e);
            setStatus('⚠ Сервер недоступен');
        } finally {
            isBusy = false;
            refreshBtn.disabled = false;
            updateAllUI();
        }
    });

    document.getElementById('c2p-minimize').addEventListener('click', () => {
        panel.style.display = 'none';
        showMiniIcon();
    });

    actionBtn.addEventListener('click', async () => {
        if (isBusy) return;

        if (activeTab === 'files') {
            await handleAttachFilesAction();
        } else {
            handleTextAction();
        }
    });

    prevBtn.addEventListener('click', () => {
        if (allParts.length === 0 || currentPart <= 0) return;
        currentPart--;
        updateAllUI();
        showPartInInput();
        setStatus(`Часть ${currentPart + 1}/${totalParts}`);
    });

    nextBtn.addEventListener('click', () => {
        if (allParts.length === 0 || currentPart >= totalParts - 1) return;
        currentPart++;
        updateAllUI();
        showPartInInput();
        setStatus(`Часть ${currentPart + 1}/${totalParts}`);
    });

    makeDraggable(panel);
    updateAllUI();

    // Периодическая синхронизация UI с реальным состоянием вложений в поле ввода
    setInterval(() => {
        if (!isBusy && activeTab === 'files' && panel && panel.style.display !== 'none') {
            updateAllUI();
        }
    }, 900);
}

function switchTab(tab) {
    if (isBusy) return;
    activeTab = tab;
    if (tab !== 'text' && isSending) {
        isSending = false;
    }

    tabFilesBtn.classList.toggle('active', tab === 'files');
    tabTextBtn.classList.toggle('active', tab === 'text');
    filesView.style.display = tab === 'files' ? '' : 'none';
    textView.style.display = tab === 'text' ? '' : 'none';
    prevBtn.style.display = tab === 'text' ? '' : 'none';
    nextBtn.style.display = tab === 'text' ? '' : 'none';

    const qTitle = document.getElementById('c2p-queue-title');
    if (qTitle) {
        qTitle.textContent = tab === 'files' ? 'Файлов в чате:' : 'Очередь текста:';
    }

    updateAllUI();
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

function addStyles() {
    if (document.getElementById('c2p-styles')) return;
    const style = document.createElement('style');
    style.id = 'c2p-styles';
    style.textContent = `
        #c2p-panel {
            position: fixed; top: 90px; right: 20px; z-index: 999999;
            width: 256px; background: #1e1e2e; color: #cdd6f4;
            border-radius: 12px; font-family: sans-serif; font-size: 13px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.45); overflow: hidden;
        }
        #c2p-header {
            background: #313244; padding: 10px 14px; font-weight: bold;
            cursor: move; user-select: none; font-size: 14px;
            display: flex; justify-content: space-between; align-items: center; gap: 4px;
        }
        #c2p-refresh, #c2p-minimize {
            background: none; border: none; color: #cdd6f4; cursor: pointer;
            font-size: 15px; padding: 0 4px; line-height: 1;
        }
        #c2p-refresh:hover { color: #89b4fa; }
        #c2p-minimize:hover { color: #f38ba8; }
        .c2p-tabs {
            display: flex; background: #181825; border-bottom: 1px solid #313244;
        }
        .c2p-tab {
            flex: 1; padding: 7px 4px; background: none; border: none;
            color: #a6adc8; font-size: 12px; font-weight: bold; cursor: pointer;
            border-bottom: 2px solid transparent; transition: all 0.15s;
        }
        .c2p-tab:hover { color: #cdd6f4; background: rgba(255,255,255,0.03); }
        .c2p-tab.active {
            color: #89b4fa; border-bottom-color: #89b4fa; background: #1e1e2e;
        }
        #c2p-body { padding: 10px 14px; }
        .c2p-project {
            font-size: 12px; color: #89b4fa; text-align: center;
            padding: 4px 8px; margin-bottom: 8px;
            background: #313244; border-radius: 4px;
            white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
        }
        .c2p-row { display: flex; justify-content: space-between; align-items: center; margin: 6px 0; }
        .c2p-hint {
            font-size: 11px; color: #a6adc8; text-align: center; margin: 4px 0 2px;
        }
        .c2p-progress { height: 6px; background: #313244; border-radius: 3px; margin: 8px 0; overflow: hidden; }
        #c2p-progress-bar { height: 100%; width: 0%; background: #89b4fa; transition: width 0.25s; }
        .c2p-buttons { display: flex; gap: 4px; margin-top: 8px; }
        .c2p-buttons button {
            padding: 8px; border: none; border-radius: 6px; cursor: pointer;
            font-size: 13px; font-weight: bold; transition: transform 0.1s;
        }
        .c2p-buttons button:active:not(:disabled) { transform: scale(0.96); }
        .c2p-buttons button:disabled { opacity: 0.55; cursor: not-allowed; }
        #c2p-prev, #c2p-next { width: 32px; background: #45475a; color: #cdd6f4; flex: 0; }
        #c2p-prev:hover:not(:disabled), #c2p-next:hover:not(:disabled) { background: #585b70; }
        #c2p-action { flex: 1; }
        #c2p-delay {
            background: #313244; color: #cdd6f4; border: 1px solid #45475a;
            border-radius: 4px; padding: 2px 6px;
        }
        .c2p-status {
            margin-top: 8px; padding: 4px 8px; background: #313244;
            border-radius: 4px; font-size: 11px; text-align: center; color: #a6adc8;
        }
        #c2p-mini {
            position: fixed; top: 90px; right: 20px; z-index: 999999;
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
        if (e.target.tagName === 'BUTTON') return;
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

// ========== ОБНОВЛЕНИЕ UI ==========

function updateAllUI() {
    if (!actionBtn) return;

    if (isBusy) {
        actionBtn.disabled = true;
        actionBtn.textContent = '⏳ Загрузка...';
        actionBtn.style.background = '#9399b2';
        actionBtn.style.color = '#1e1e2e';
        return;
    }

    actionBtn.disabled = false;

    if (activeTab === 'files') {
        const expectedNames = getExpectedFileNames();
        const stagedNames = PlatformAdapter.getStagedProjectFileNames(expectedNames);
        const totalInBox = PlatformAdapter.countTotalAttachmentsInComposer(expectedNames);

        if (composerInfoLabel) {
            composerInfoLabel.textContent = `В поле ввода: ${totalInBox} (макс. ${MAX_FILES_PER_MESSAGE})`;
        }

        if (queueLabel) {
            queueLabel.textContent = totalParts === 0 ? '0/0' : `${stagedNames.size} в чате / ${totalParts}`;
        }

        if (progressBar) {
            const pct = totalParts > 0 ? Math.min((stagedNames.size / totalParts) * 100, 100) : 0;
            progressBar.style.width = pct + '%';
        }

        if (totalParts === 0) {
            actionBtn.textContent = '📎 Прикрепить файлы';
            actionBtn.style.background = '#89b4fa';
            actionBtn.style.color = '#1e1e2e';
        } else if (stagedNames.size >= totalParts) {
            actionBtn.textContent = '✓ Все файлы в чате';
            actionBtn.style.background = '#a6e3a1';
            actionBtn.style.color = '#1e1e2e';
        } else if (totalInBox >= MAX_FILES_PER_MESSAGE) {
            actionBtn.textContent = `⚠ Лимит ${MAX_FILES_PER_MESSAGE} в поле`;
            actionBtn.style.background = '#fab387';
            actionBtn.style.color = '#1e1e2e';
        } else {
            const missingCount = Math.min(totalParts - stagedNames.size, MAX_FILES_PER_MESSAGE - totalInBox);
            actionBtn.textContent = `📎 Прикрепить (${missingCount})`;
            actionBtn.style.background = '#89b4fa';
            actionBtn.style.color = '#1e1e2e';
        }
        return;
    }

    // Режим вкладки "Текстом"
    if (queueLabel) {
        if (totalParts === 0) {
            queueLabel.textContent = '0/0';
        } else if (currentPart >= totalParts) {
            queueLabel.textContent = `${totalParts}/${totalParts} ✓`;
        } else {
            queueLabel.textContent = `${currentPart + 1}/${totalParts}`;
        }
    }

    if (progressBar) {
        const pct = totalParts > 0 ? Math.min((currentPart / totalParts) * 100, 100) : 0;
        progressBar.style.width = pct + '%';
    }

    if (prevBtn) prevBtn.disabled = (currentPart <= 0 || totalParts === 0);
    if (nextBtn) nextBtn.disabled = (currentPart >= totalParts - 1 || totalParts === 0);

    if (totalParts > 0 && currentPart >= totalParts && !isSending) {
        actionBtn.textContent = '✓ Готово';
        actionBtn.style.background = '#6c7086';
        actionBtn.style.color = '#cdd6f4';
    } else if (isSending) {
        actionBtn.textContent = '⏸ Пауза';
        actionBtn.style.background = '#f9e2af';
        actionBtn.style.color = '#1e1e2e';
    } else if (allParts.length > 0) {
        actionBtn.textContent = autoSend ? '▶ Продолжить' : '📝 Вставить часть';
        actionBtn.style.background = '#a6e3a1';
        actionBtn.style.color = '#1e1e2e';
    } else {
        actionBtn.textContent = '▶ Загрузить и начать';
        actionBtn.style.background = '#89b4fa';
        actionBtn.style.color = '#1e1e2e';
    }
}

// ========== ЛОГИКА ВКЛАДКИ "ФАЙЛАМИ" ==========

async function handleAttachFilesAction() {
    if (Date.now() - lastAttachTimestamp < 1200) {
        return;
    }

    isBusy = true;
    updateAllUI();
    setStatus('Проверка данных сервера...');

    try {
        if (allParts.length === 0) {
            await syncFromServer();
        }

        if (totalParts === 0) {
            setStatus('⚠ Нет частей для отправки');
            return;
        }

        const expectedNames = getExpectedFileNames();
        const stagedNames = PlatformAdapter.getStagedProjectFileNames(expectedNames);
        const totalInBox = PlatformAdapter.countTotalAttachmentsInComposer(expectedNames);

        if (stagedNames.size >= totalParts) {
            setStatus(`✓ Все ${totalParts} файл(ов) уже в чате`);
            return;
        }

        const freeSlots = MAX_FILES_PER_MESSAGE - totalInBox;
        if (freeSlots <= 0) {
            setStatus(`⚠ Достигнут лимит ${MAX_FILES_PER_MESSAGE} файлов в поле`);
            return;
        }

        // Выбираем только те части проекта, которых сейчас НЕТ в поле ввода (например, удалённый файл №7)
        const filesToAttach = [];
        for (let i = 0; i < allParts.length; i++) {
            if (filesToAttach.length >= freeSlots) break;
            const fname = expectedNames[i];
            if (!stagedNames.has(fname)) {
                filesToAttach.push(new File([allParts[i].content], fname, { type: 'text/plain;charset=utf-8' }));
            }
        }

        if (filesToAttach.length === 0) {
            setStatus('✓ Дубликатов не найдено, всё в чате');
            return;
        }

        setStatus(`Прикрепление ${filesToAttach.length} файл(ов)...`);
        const attached = await PlatformAdapter.attachFiles(filesToAttach);
        lastAttachTimestamp = Date.now();

        if (!attached) {
            setStatus('⚠ Поле загрузки не найдено');
            return;
        }

        // Ждём 600мс, чтобы интерфейс чата успел отрисовать чипы файлов в DOM
        await new Promise(r => setTimeout(r, 600));
        const nowStaged = PlatformAdapter.getStagedProjectFileNames(expectedNames);
        setStatus(`Готово! В поле ${nowStaged.size} из ${totalParts} файл(ов) ✓`);
    } catch (e) {
        console.error('❌ Attach files error:', e);
        setStatus('⚠ Сервер недоступен');
    } finally {
        isBusy = false;
        updateAllUI();
    }
}

// ========== ЛОГИКА ВКЛАДКИ "ТЕКСТОМ" ==========

function showPartInInput() {
    const inputEl = PlatformAdapter.getInput();
    if (!inputEl || currentPart >= allParts.length) return;
    PlatformAdapter.setInputValue(inputEl, allParts[currentPart].content);
}

async function handleTextAction() {
    if (!isSending && currentPart >= totalParts && totalParts > 0) {
        currentPart = 0;
        updateAllUI();
        setStatus('Сброшено на 1-ю часть');
        return;
    }

    if (isSending) {
        isSending = false;
        updateAllUI();
        setStatus('Пауза');
        return;
    }

    if (allParts.length === 0) {
        isBusy = true;
        updateAllUI();
        setStatus('Загрузка с сервера...');
        try {
            await syncFromServer();
            currentPart = 0;
        } catch (e) {
            setStatus('⚠ Сервер недоступен');
            return;
        } finally {
            isBusy = false;
            updateAllUI();
        }
    }

    if (totalParts === 0) {
        setStatus('⚠ Нет данных');
        return;
    }

    if (!autoSend) {
        showPartInInput();
        setStatus(`Вставлена часть ${currentPart + 1}/${totalParts}`);
        if (currentPart < totalParts - 1) {
            currentPart++;
        }
        updateAllUI();
        return;
    }

    isSending = true;
    updateAllUI();
    insertAndSendText();
}

function insertAndSendText() {
    if (!isSending || activeTab !== 'text') return;

    if (currentPart >= allParts.length) {
        isSending = false;
        updateAllUI();
        setStatus('Готово ✓');
        return;
    }

    const inputEl = PlatformAdapter.getInput();
    if (!inputEl) {
        setTimeout(() => insertAndSendText(), 1000);
        return;
    }

    const part = allParts[currentPart];
    PlatformAdapter.setInputValue(inputEl, part.content);
    setStatus(`Отправка части ${currentPart + 1}/${totalParts}`);

    const clickDelay = PlatformAdapter.getMode() === 'deepseek' ? 300 : 500;
    setTimeout(() => PlatformAdapter.clickSend(), clickDelay);
    setTimeout(() => waitForEmptyAndContinue(), 800);
}

function waitForEmptyAndContinue() {
    // DeepSeek — оригинальная логика без изменений
    if (PlatformAdapter.getMode() === 'deepseek') {
        let responseEndTime = null;
        const checkDeepSeek = () => {
            if (!isSending || activeTab !== 'text') return;
            const inputEl = PlatformAdapter.getInput();
            const isGenerating = PlatformAdapter.isGenerating();

            if (PlatformAdapter.hasRateLimitError()) {
                if (!retryScheduled) {
                    retryScheduled = true;
                    setStatus('⚠ Слишком часто, повтор через 5с...');
                    setTimeout(() => {
                        retryScheduled = false;
                        if (isSending) insertAndSendText();
                    }, 5000);
                }
                return;
            }

            if (inputEl && PlatformAdapter.isInputEmpty(inputEl) && !isGenerating) {
                if (responseEndTime === null) {
                    responseEndTime = Date.now();
                    setStatus('Ожидание ответа...');
                } else {
                    setStatus('Пауза ' + delay + 'с...');
                }

                if (Date.now() - responseEndTime >= delay * 1000) {
                    retryScheduled = false;
                    currentPart++;
                    updateAllUI();
                    if (currentPart >= totalParts) {
                        isSending = false;
                        updateAllUI();
                        setStatus('Готово ✓');
                        return;
                    }
                    insertAndSendText();
                    return;
                }
            } else {
                responseEndTime = null;
                setStatus(isGenerating ? 'Идёт ответ ИИ...' : 'Получен ответ...');
            }
            setTimeout(checkDeepSeek, 300);
        };
        checkDeepSeek();
        return;
    }

    // Google AI / Gemini — отслеживание появления кнопок оценки и стабилизации длины текста
    let submitted = false;
    let lastTextLen = -1;
    let stableTicks = 0;
    let responseEndTime = null;
    const sentAt = Date.now();

    const checkGoogle = () => {
        if (!isSending || activeTab !== 'text') return;

        const inputEl = PlatformAdapter.getInput();
        const isGenerating = PlatformAdapter.isGenerating();
        const inputEmpty = inputEl && PlatformAdapter.isInputEmpty(inputEl);

        if (!submitted) {
            if (inputEmpty) {
                submitted = true;
                setStatus('Запрос отправлен...');
            } else {
                setStatus('Ожидание отправки...');
                setTimeout(checkGoogle, 300);
                return;
            }
        }

        const currentLen = (document.body.innerText || '').length;
        if (currentLen === lastTextLen) {
            stableTicks++;
        } else {
            lastTextLen = currentLen;
            stableTicks = 0;
        }

        const elapsed = Date.now() - sentAt;
        const settled = elapsed > 1800 && !isGenerating && stableTicks >= 4;

        if (inputEmpty && settled) {
            if (responseEndTime === null) {
                responseEndTime = Date.now();
            }
            setStatus('Пауза ' + delay + 'с...');

            if (Date.now() - responseEndTime >= delay * 1000) {
                retryScheduled = false;
                currentPart++;
                updateAllUI();
                if (currentPart >= totalParts) {
                    isSending = false;
                    updateAllUI();
                    setStatus('Готово ✓');
                    return;
                }
                insertAndSendText();
                return;
            }
        } else {
            responseEndTime = null;
            setStatus('Идёт ответ ИИ...');
        }

        setTimeout(checkGoogle, 350);
    };

    checkGoogle();
}

// ========== ИНИЦИАЛИЗАЦИЯ ==========

function attachToInput(inputEl) {
    inputEl.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && e.ctrlKey) {
            e.preventDefault();
            e.stopPropagation();
            if (activeTab === 'files') {
                handleAttachFilesAction();
            } else {
                handleTextAction();
            }
        }
    }, true);
}

function init() {
    if (!PlatformAdapter.isSupportedPage()) return;

    addStyles();
    createPanel();

    const observer = new MutationObserver(() => {
        const inputEl = PlatformAdapter.getInput();
        if (inputEl && !inputEl.dataset.c2pAttached) {
            inputEl.dataset.c2pAttached = 'true';
            attachToInput(inputEl);
        }
    });
    observer.observe(document.body, { childList: true, subtree: true });
}

init();
