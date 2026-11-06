// Demo client of the bank. Plain JavaScript over the REST API: every call shows up in the log on the right.
// Every text a person reads comes from i18n.js: t() for texts, label() for status names, and dates and money formatted
// for the chosen language (Kyiv time).
'use strict';

const state = {
    access: sessionStorage.getItem('access'),
    refresh: sessionStorage.getItem('refresh'),
    me: null,
    payments: JSON.parse(sessionStorage.getItem('payments') || '[]'), // payments made in this session
    view: 'overview',
};

// ---------- HTTP ----------

const uuid = () => crypto.randomUUID();

/** One place for every request: token, idempotency key, language, the log, a token refresh on 401, readable errors. */
async function api(method, path, body, { idempotent = false, raw = false, retried = false, quiet = false } = {}) {
    const headers = { 'Accept-Language': currentLang() };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (state.access) headers.Authorization = `Bearer ${state.access}`;
    if (idempotent) headers['Idempotency-Key'] = uuid();
    const log = quiet ? () => {} : (...args) => writeLog(headers, ...args);

    const started = performance.now();
    let response;
    try {
        response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    } catch (e) {
        log(method, path, 0, Math.round(performance.now() - started), body, String(e));
        throw new Error(t('error.noAnswer'));
    }
    const ms = Math.round(performance.now() - started);

    if (response.status === 401 && state.refresh && !retried && !path.startsWith('/authentication')) {
        log(method, path, 401, ms, body, '(access token expired, refreshing)');
        if (await refreshTokens()) return api(method, path, body, { idempotent, raw, quiet, retried: true });
    }

    if (raw && response.ok) {
        log(method, path, response.status, ms, body, `(${response.headers.get('Content-Type')})`);
        return response;
    }

    const text = await response.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = text; }
    log(method, path, response.status, ms, body, data);

    if (!response.ok) {
        const message = data && (data.detail || data.title) || `${response.status} ${response.statusText}`;
        throw new Error(message);
    }
    return data;
}

async function refreshTokens() {
    try {
        const tokens = await api('POST', '/authentication/refresh', { refreshToken: state.refresh }, { retried: true });
        saveTokens(tokens);
        return true;
    } catch {
        logout(true);
        return false;
    }
}

function saveTokens(tokens) {
    state.access = tokens.accessToken;
    state.refresh = tokens.refreshToken;
    sessionStorage.setItem('access', state.access);
    sessionStorage.setItem('refresh', state.refresh);
}

// ---------- Log, toasts, helpers ----------

const curls = new Map();

/** The API log is a developer's tool (paths, JSON, curl), so it stays in English whatever the language. */
function writeLog(headers, method, path, status, ms, request, response) {
    const id = uuid();
    curls.set(id, [`curl -X ${method} '${location.origin}${path}'`,
        ...Object.entries(headers).map(([k, v]) => `-H '${k}: ${v}'`),
        ...(request !== undefined ? [`-d '${JSON.stringify(request)}'`] : [])].join(' \\\n  '));
    const entry = document.createElement('details');
    const s = String(status)[0] || '0';
    const time = new Date().toLocaleTimeString('uk-UA', { hour12: false, timeZone: 'Europe/Kyiv' });
    entry.innerHTML = `<summary><span class="t">${time}</span><span class="m">${method}</span><span class="path">${esc(path)}</span>
        <span class="s${s}">${status || 'ERR'}</span><span class="ms">${ms} ms</span></summary>
        ${request !== undefined ? `<div class="dir">→ request</div><pre>${jsonHtml(request)}</pre>` : ''}
        <div class="dir">← response ${status || ''}</div><pre>${typeof response === 'string' ? esc(response) : jsonHtml(response)}</pre>
        <button class="curl" onclick="copyCurl('${id}')">📋 copy as curl</button>`;
    const list = document.getElementById('log');
    list.querySelector('.log-empty')?.remove();
    // Only the newest call is open: what just happened is visible without a click.
    list.querySelectorAll('details[open]').forEach((d) => d.removeAttribute('open'));
    entry.open = true;
    list.prepend(entry);
    while (list.children.length > 60) list.lastChild.remove();
    document.getElementById('logCount').textContent = list.children.length;
}

/**
 * Pretty JSON with colours for keys, strings, numbers and true/false/null. Split into tokens first, then each token is
 * escaped on its own: escaping the whole text first would break strings that contain an escaped quote.
 */
function jsonHtml(value) {
    return JSON.stringify(value, null, 2).replace(
        /("(?:\\.|[^"\\])*")(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g,
        (match, str, colon, literal) => str ? `<span class="${colon ? 'k' : 'str'}">${esc(str)}</span>${colon || ''}`
            : literal ? `<span class="lit">${literal}</span>` : `<span class="num">${match}</span>`);
}

function clearLog() {
    document.getElementById('log').innerHTML = '';
    document.getElementById('logCount').textContent = '0';
    curls.clear();
}

const LOG_OPEN = 'apiLogOpen';

/** Shows or hides the API log. Remembered in this browser; open by default on wide screens. */
function toggleLog(force) {
    const layout = document.getElementById('layout');
    const open = force ?? !layout.classList.contains('with-log');
    layout.classList.toggle('with-log', open);
    document.getElementById('logToggle').classList.toggle('on', open);
    try { localStorage.setItem(LOG_OPEN, String(open)); } catch { /* not critical */ }
}

function initialLogState() {
    try {
        const saved = localStorage.getItem(LOG_OPEN);
        if (saved !== null) return saved === 'true';
    } catch { /* not critical */ }
    return window.innerWidth > 1280;
}

async function copyCurl(id) {
    try { await navigator.clipboard.writeText(curls.get(id)); toast(t('toast.curlCopied')); }
    catch { window.prompt(t('prompt.copyCommand'), curls.get(id)); }
}

let toastTimer;
function toast(message, kind = 'ok') {
    const el = document.getElementById('toast');
    el.textContent = message;
    el.className = `show ${kind}`;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => (el.className = ''), 3500);
}

/** Runs an action from a button: shows the error as a toast instead of failing silently, then redraws the view. */
async function act(action, success) {
    try {
        const result = await action();
        if (success) toast(typeof success === 'function' ? success(result) : success);
        await render();
        return result;
    } catch (e) {
        toast(e.message, 'bad');
    }
}

const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const val = (id) => document.getElementById(id).value.trim();
const money = (amount, currency = '') => formatMoney(amount, currency);
/** A moment (createdAt, ...): in Kyiv time, as the chosen language writes it. */
const when = (iso) => formatDateTime(iso);
/** A calendar day (a due date, ...): no time zone, so it never moves to the day before. */
const day = (isoDate) => formatDate(isoDate);
const rate = (value) => formatRate(value);
const short = (id) => id ? String(id).slice(0, 8) : '—';
/** A status or type: the colour comes from the code, the words from the dictionary. */
const badge = (code) => {
    const kind = /COMPLETED|APPROVED|ACTIVE|PAID|RESOLVED|SENT/.test(code) ? 'ok'
        : /FAILED|DECLINED|BLOCKED|OVERDUE|CLOSED|LOCKED/.test(code) ? 'bad'
            : /PROCESSING|UNKNOWN|MANUAL_REVIEW|OPEN|IN_PROGRESS|MINIMUM_PAID|PENDING/.test(code) ? 'warn' : 'info';
    return `<span class="badge ${kind}">${esc(label(code))}</span>`;
};
const options = (items, text, valueOf = (x) => x.id) => items.map((x) => `<option value="${esc(valueOf(x))}">${esc(text(x))}</option>`).join('');
const accountLabel = (a) => `${a.currency} · ${label(a.type)} · ${money(a.balance, a.currency)} · …${a.number.slice(-6)}`;
const isStaff = () => state.me && (state.me.role === 'ADMIN' || state.me.role === 'SUPPORT');

function rememberPayment(payment) {
    state.payments = [payment, ...state.payments.filter((p) => p.id !== payment.id)].slice(0, 20);
    sessionStorage.setItem('payments', JSON.stringify(state.payments));
    return payment;
}

/** Called by i18n.js after the language changed: everything drawn by code is drawn again. */
function onLanguageChanged() {
    if (state.me) {
        document.getElementById('who').textContent = `${state.me.firstName} ${state.me.lastName} · ${t(`role.${state.me.role}`)}`;
        render();
    }
}

// ---------- Session ----------

function fillAdmin() {
    document.getElementById('loginEmail').value = 'admin@bank.local';
    document.getElementById('loginPassword').value = 'demo-admin-password';
}

// "Remember me" keeps only the email, and only in this browser: never a password, never a token. The session itself
// stays in sessionStorage, so closing the tab still logs out.
const REMEMBERED_EMAIL = 'rememberedEmail';

function rememberEmail(email, remember) {
    try {
        if (remember) localStorage.setItem(REMEMBERED_EMAIL, email);
        else localStorage.removeItem(REMEMBERED_EMAIL);
    } catch { /* storage blocked: nothing to remember */ }
}

function rememberedEmail() {
    try { return localStorage.getItem(REMEMBERED_EMAIL); } catch { return null; }
}

/** Fills the login form for a returning user and puts the cursor where they type next. */
function showRememberedEmail() {
    const email = rememberedEmail();
    document.getElementById('rememberedAs').hidden = !email;
    document.getElementById('loginRemember').checked = Boolean(email);
    document.getElementById('loginPassword').value = '';
    if (email) {
        document.getElementById('rememberedEmail').textContent = email;
        document.getElementById('loginEmail').value = email;
        document.getElementById('loginPassword').focus();
    }
}

function forgetMe(event) {
    event.preventDefault();
    rememberEmail(null, false);
    document.getElementById('loginEmail').value = '';
    showRememberedEmail();
    document.getElementById('loginEmail').focus();
}

async function register(event) {
    event.preventDefault();
    try {
        const email = val('regEmail');
        const password = document.getElementById('regPassword').value;
        await api('POST', '/authentication/register', { email, password, firstName: val('regFirst'), lastName: val('regLast') });
        saveTokens(await api('POST', '/authentication/login', { email, password }));
        rememberEmail(email, document.getElementById('regRemember').checked);
        await start();
        toast(t('toast.welcome'));
    } catch (e) { toast(e.message, 'bad'); }
}

async function login(event) {
    event.preventDefault();
    try {
        const email = val('loginEmail');
        saveTokens(await api('POST', '/authentication/login', { email, password: document.getElementById('loginPassword').value }));
        rememberEmail(email, document.getElementById('loginRemember').checked);
        await start();
    } catch (e) { toast(e.message, 'bad'); }
}

/** One card, three faces: log in, open an account, forgot password. */
function showAuth(mode, event) {
    event?.preventDefault();
    document.getElementById('loginForm').hidden = mode !== 'login';
    document.getElementById('registerForm').hidden = mode !== 'register';
    document.getElementById('forgotForm').hidden = mode !== 'forgot';
    document.querySelector('.tabs').hidden = mode === 'forgot';
    document.getElementById('tabLogin').setAttribute('aria-selected', String(mode === 'login'));
    document.getElementById('tabRegister').setAttribute('aria-selected', String(mode === 'register'));
    if (mode === 'forgot') {
        document.getElementById('forgotEmail').value = val('loginEmail');
        document.getElementById('forgotEmail').focus();
    }
}

/** The bank answers the same for every address, so the page does too: it never says whether the account exists. */
async function requestPasswordReset(event) {
    event.preventDefault();
    try {
        await api('POST', '/authentication/password-reset', { email: val('forgotEmail') });
        showAuth('login');
        toast(t('toast.resetSent'));
    } catch (e) { toast(e.message, 'bad'); }
}

async function logout(silent) {
    if (!silent && state.refresh) {
        try { await api('POST', '/authentication/logout', { refreshToken: state.refresh }); } catch { /* already gone */ }
    }
    clearInterval(state.bell);
    state.access = state.refresh = state.me = null;
    sessionStorage.clear();
    state.payments = [];
    document.getElementById('app').hidden = true;
    document.getElementById('auth').hidden = false;
    showRememberedEmail();
}

async function start() {
    state.me = await api('GET', '/users/me');
    document.getElementById('auth').hidden = true;
    document.getElementById('app').hidden = false;
    toggleLog(initialLogState());
    document.getElementById('who').textContent = `${state.me.firstName} ${state.me.lastName} · ${t(`role.${state.me.role}`)}`;
    document.getElementById('staffNav').hidden = !isStaff();
    document.querySelectorAll('[data-admin]').forEach((a) => (a.hidden = state.me.role !== 'ADMIN'));
    clearInterval(state.bell);
    state.bell = setInterval(refreshBell, 5000);
    await go(isStaff() && state.view === 'overview' ? 'staffCredit' : state.view);
}

async function refreshBell() {
    try {
        const { unread } = await api('GET', '/notifications/unread-count', undefined, { quiet: true });
        const el = document.getElementById('unread');
        el.textContent = unread;
        el.hidden = unread === 0;
    } catch { /* not critical */ }
}

// ---------- Navigation ----------

const views = {};

async function go(name) {
    state.view = name;
    document.querySelectorAll('nav a').forEach((a) => a.classList.toggle('active', a.dataset.view === name));
    await render();
}

/** Each drawing gets a number: a slow one that finishes after a newer one has started is thrown away. */
let renderTicket = 0;

/**
 * Draws the current view. A view waits for the API, so two drawings can overlap (a language switch, then a quick
 * click in the menu): without the ticket the older, slower one would finish last and put the old page back.
 */
async function render() {
    if (!state.me) return;
    const ticket = ++renderTicket;
    const main = document.getElementById('view');
    let html;
    try {
        html = await views[state.view]();
    } catch (e) {
        html = `<div class="card"><h2>${t('error.title')}</h2><p>${esc(e.message)}</p></div>`;
    }
    if (ticket !== renderTicket) return;
    main.innerHTML = html;
    refreshBell();
}

const page = (title, lead, ...cards) => `<h1>${title}</h1><p class="lead">${lead}</p>${cards.join('')}`;
const card = (title, body, hint = '') => `<div class="card"><h2>${title}</h2>${hint ? `<p class="hint">${hint}</p>` : ''}${body}</div>`;
const table = (headers, rows, empty = t('common.nothingYet')) => rows.length
    ? `<div class="table-wrap"><table><tr>${headers.map((h) => `<th>${h}</th>`).join('')}</tr>${rows.join('')}</table></div>`
    : `<div class="empty">${empty}</div>`;
const CURRENCIES = ['UAH', 'USD', 'EUR'];

// ---------- 🏠 Overview ----------

// The account whose "Top up" row is open on the overview, if any.
let topUpAccountId = null;

views.overview = async () => {
    const accounts = await api('GET', '/accounts');
    const cards = await api('GET', '/cards');
    const own = accounts.filter((a) => a.type === 'CLIENT');
    const empty = own.length > 0 && own.every((a) => Number(a.balance) === 0);
    const topUpRow = (a) => a.id !== topUpAccountId ? '' : `<tr class="inline-form"><td colspan="5">
            <div class="row"><div><label>${t('overview.topUpWith', { account: `${esc(a.currency)} …${esc(a.number.slice(-6))}` })}</label>
            <input id="quickTopUpAmount" value="${a.currency === 'UAH' ? '60000' : '1000'}"></div>
            <div class="fit"><button onclick="quickTopUp('${a.id}')">💵 ${t('overview.topUp')}</button></div>
            <div class="fit"><button class="secondary" onclick="topUpAccountId = null; render()">${t('common.cancel')}</button></div></div>
            <p class="hint below">${t('overview.topUpHint')}</p></td></tr>`;
    return page(`🏠 ${t('nav.overview')}`, t('overview.lead'),
        empty ? `<div class="notice">${t('overview.emptyNotice')}</div>` : '',
        card(`💰 ${t('overview.accounts')}`, table([t('col.currency'), t('col.type'), 'IBAN', t('col.balance'), ''], accounts.map((a) => `<tr>
                <td><b>${a.currency}</b></td><td>${badge(a.type)}</td><td class="mono">${esc(a.number)}</td>
                <td class="num"><b>${money(a.balance, a.currency)}</b>${a.creditLimit ? `<br><small>${t('overview.limit')} ${money(a.creditLimit, 'UAH')}</small>` : ''}</td>
                <td class="buttons">${a.type === 'CLIENT' ? `<button class="small" onclick="topUpAccountId = '${a.id}'; render()">💵 ${t('overview.topUp')}</button>` : ''}
                    <button class="small secondary" onclick="showStatement('${a.id}')">${t('overview.statement')}</button></td></tr>${topUpRow(a)}`),
            t('overview.noAccounts')) +
            `<div class="row" style="margin-top:12px"><div><label>${t('overview.openIn')}</label>
             <select id="newCurrency">${CURRENCIES.map((c) => `<option>${c}</option>`).join('')}</select></div>
             <div class="fit"><button onclick="openAccount()">${t('overview.openAccount')}</button></div></div>
             <div id="statement"></div>`),
        card(`💳 ${t('nav.cards')}`, cards.length ? `<div class="wallet">${cards.map((c) => cardVisual(c, accounts)).join('')}</div>`
            : `<div class="empty">${t('overview.noCards')}</div>`));
};

async function openAccount() {
    await act(() => api('POST', '/accounts', { currency: val('newCurrency') }), (a) => t('toast.accountOpened', { currency: a.currency }));
}

async function quickTopUp(accountId) {
    const payment = await pay('/payments/top-up', { accountId, amount: val('quickTopUpAmount') }, t('payment.topUp'));
    if (payment) {
        topUpAccountId = null;
        render();
    }
}

async function showStatement(accountId) {
    try {
        const { content } = await api('GET', `/accounts/${accountId}/transactions?size=20`);
        document.getElementById('statement').innerHTML = `<h2 style="margin-top:16px">📜 ${t('overview.statement')}</h2>` + table(
            [t('col.when'), t('col.operation'), t('col.amount'), t('col.balanceAfter')],
            content.map((e) => `<tr><td>${when(e.createdAt)}</td><td>${badge(e.type)} ${esc(e.description)}</td>
                <td class="num ${e.direction === 'CREDIT' ? 'plus' : 'minus'}">${e.direction === 'CREDIT' ? '+' : '−'}${money(e.amount)}</td>
                <td class="num">${money(e.balanceAfter)}</td></tr>`),
            t('overview.noOperations'));
    } catch (e) { toast(e.message, 'bad'); }
}

// ---------- 🔁 Transfers ----------

views.payments = async () => {
    const accounts = (await api('GET', '/accounts')).filter((a) => a.type !== 'CREDIT');
    const cards = await api('GET', '/cards');
    const accountOptions = options(accounts, accountLabel);
    return page(`🔁 ${t('nav.payments')}`, t('payments.lead'),
        `<div class="grid">`,
        card(`💵 ${t('overview.topUp')}`, `<label>${t('col.account')}</label><select id="topUpAccount">${accountOptions}</select>
            <label>${t('col.amount')}</label><input id="topUpAmount" value="60000">
            <div class="actions"><button onclick="topUp()">${t('overview.topUp')}</button></div>`, t('payments.topUpHint')),
        card(`➡️ ${t('payments.byIban')}`, `<label>${t('payments.from')}</label><select id="trFrom">${accountOptions}</select>
            <label>${t('payments.recipientIban')}</label><input id="trTo" placeholder="UA…">
            <label>${t('col.amount')}</label><input id="trAmount" value="500">
            <label>${t('payments.description')}</label><input id="trDesc" value="${esc(t('payments.descriptionDefault'))}">
            <div class="actions"><button onclick="transfer()">${t('common.send')}</button></div>`, t('payments.byIbanHint')),
        card(`💳 ${t('payments.cardToCard')}`, `<label>${t('payments.fromCard')}</label><select id="ctFrom">${options(cards, (c) => `${label(c.type)} ${c.maskedNumber}`)}</select>
            <label>${t('payments.recipientCard')}</label><input id="ctTo" placeholder="${esc(t('payments.sixteenDigits'))}">
            <label>${t('col.amount')}</label><input id="ctAmount" value="250">
            <div class="actions"><button onclick="cardTransfer()">${t('common.send')}</button></div>`, t('payments.cardToCardHint')),
        `</div>`,
        card(`🧾 ${t('payments.session')}`, table([t('col.when'), t('col.type'), t('col.amount'), t('col.fee'), t('col.status'), ''], state.payments.map((p) => `<tr>
                <td>${when(p.createdAt)}</td><td>${badge(p.type)}<br><small>${esc(p.description)}</small></td>
                <td class="num">${money(p.amount, p.currency)}</td><td class="num">${money(p.fee, p.currency)}</td>
                <td>${badge(p.status)}${p.failureReason ? `<br><small>${esc(p.failureReason)}</small>` : ''}</td>
                <td><button class="small secondary" onclick="checkPayment('${p.id}')">${t('common.refresh')}</button>
                    <button class="small secondary" onclick="receipt('${p.id}')">PDF</button>
                    <button class="small secondary" onclick="emailReceipt('${p.id}')">Email</button></td></tr>`),
            t('payments.sessionEmpty')), t('payments.sessionHint')));
};

const pay = (path, body, what) => act(async () => rememberPayment(await api('POST', path, body, { idempotent: true })),
    (p) => `${what}: ${label(p.status)}${Number(p.fee) > 0 ? `, ${t('payment.fee')} ${money(p.fee, p.currency)}` : ''}`);

const topUp = () => pay('/payments/top-up', { accountId: val('topUpAccount'), amount: val('topUpAmount') }, t('payment.topUp'));
const transfer = () => pay('/payments/transfers', { fromAccountId: val('trFrom'), toAccountNumber: val('trTo'), amount: val('trAmount'), description: val('trDesc') }, t('payment.transfer'));
const cardTransfer = () => pay('/payments/card-transfers', { fromCardId: val('ctFrom'), toCardNumber: val('ctTo'), amount: val('ctAmount') }, t('payment.cardTransfer'));

// ---------- 🧾 Bills & utilities ----------

const PROVIDER_ICONS = { ELECTRICITY: '⚡', WATER: '💧', GAS: '🔥', HEATING: '♨️', INTERNET: '🌐', MOBILE: '📱' };
let selectedProvider = null;

views.bills = async () => {
    const providers = await api('GET', '/bill-payments/providers');
    const accounts = (await api('GET', '/accounts')).filter((a) => a.currency === 'UAH' && a.type !== 'SAVINGS' && a.type !== 'TAX_RESERVE');
    const provider = providers.find((p) => p.id === selectedProvider) || providers[0];
    selectedProvider = provider?.id;
    const mobile = provider?.category === 'MOBILE';
    const bills = state.payments.filter((p) => p.type === 'BILL_PAYMENT');

    const form = !accounts.length
        ? `<div class="empty-state"><div class="big">🧾</div><p>${t('bills.needUah')}</p>
            <button onclick="openUahAccount()">${t('bills.openUah')}</button></div>`
        : `<label>${t('payments.from')}</label><select id="billFrom">${options(accounts, accountLabel)}</select>
            <label>${mobile ? t('bills.phone') : t('bills.providerAccount')}</label>
            <input id="billAccount" value="${mobile ? '+380671234567' : '4410012345'}">
            <label>${t('bills.amountUah')}</label><input id="billAmount" value="${mobile ? '200' : '300'}">
            <div class="actions"><button onclick="payBill()">${t('bills.pay', { provider: esc(provider?.name || '') })}</button></div>`;

    return page(`🧾 ${t('nav.bills')}`, t('bills.lead'),
        card(t('bills.choose'), `<div class="providers">${providers.map((p) => `
            <button type="button" class="provider ${p.id === selectedProvider ? 'selected' : ''}" onclick="selectProvider('${p.id}')">
                <span class="ico">${PROVIDER_ICONS[p.category] || '🧾'}</span><span>${esc(p.name)}</span><span class="cat">${esc(label(p.category))}</span>
            </button>`).join('')}</div>`),
        card(t('bills.payStep'), `<div class="narrow">${form}</div>`, t('bills.feeHint')),
        card(`🧾 ${t('bills.mine')}`, table([t('col.when'), t('col.provider'), t('col.amount'), t('col.status'), ''], bills.map((p) => `<tr>
                <td>${when(p.createdAt)}</td><td>${esc(providers.find((x) => x.id === p.providerId)?.name || p.description)}</td>
                <td class="num">${money(p.amount, p.currency)}</td>
                <td>${badge(p.status)}${p.failureReason ? `<br><small>${esc(p.failureReason)}</small>` : ''}</td>
                <td><button class="small secondary" onclick="receipt('${p.id}')">PDF</button></td></tr>`),
            t('bills.mineEmpty')), t('bills.mineHint')));
};

function selectProvider(id) {
    selectedProvider = id;
    render();
}

const openUahAccount = () => act(() => api('POST', '/accounts', { currency: 'UAH' }), t('toast.accountOpened', { currency: 'UAH' }));

async function payBill() {
    const payment = await pay('/bill-payments', { providerId: selectedProvider, accountNumber: val('billAccount'), fromAccountId: val('billFrom'), amount: val('billAmount') }, t('payment.bill'));
    if (payment) followPayment(payment.id);
}

/** The provider is called in the background: watch PROCESSING turn into COMPLETED or FAILED by itself. */
async function followPayment(id) {
    for (let attempt = 0; attempt < 20; attempt++) {
        await new Promise((resolve) => setTimeout(resolve, 1500));
        const p = rememberPayment(await api('GET', `/payments/${id}`, undefined, { quiet: true }));
        if (p.status !== 'PROCESSING' && p.status !== 'UNKNOWN') {
            toast(`${t('payment.bill')}: ${label(p.status)}${p.failureReason ? `: ${p.failureReason}` : ''}`, p.status === 'COMPLETED' ? 'ok' : 'bad');
            if (state.view === 'payments' || state.view === 'bills') render();
            return;
        }
    }
}

async function checkPayment(id) {
    await act(async () => rememberPayment(await api('GET', `/payments/${id}`)), (p) => t('toast.paymentIs', { status: label(p.status) }));
}

async function receipt(id) {
    try {
        const response = await api('GET', `/payments/${id}/receipt`, undefined, { raw: true });
        window.open(URL.createObjectURL(await response.blob()), '_blank');
    } catch (e) { toast(e.message, 'bad'); }
}

async function emailReceipt(id) {
    await act(() => api('POST', `/payments/${id}/receipt/email`), t('toast.receiptSent'));
}

// ---------- 💳 Cards ----------

/** A card as the client would see it in the app: its own colours per type, the holder, a stamp when it is off. */
const cardVisual = (c, accounts = []) => {
    const account = accounts.find((a) => a.id === c.accountId);
    const holder = state.me ? `${state.me.firstName} ${state.me.lastName}`.toUpperCase() : '';
    const off = c.status !== 'ACTIVE';
    return `<div class="bank-card ${c.type.toLowerCase()} ${off ? 'off' : ''}">
        <div class="top"><span class="bank"><span class="mark" aria-hidden="true"><i></i><i></i><i></i></span>TABOROVETS UNIVERSAL BANK</span></div>
        <div class="chip"></div>
        <div class="number" id="num-${c.id}">${esc(c.maskedNumber)}</div>
        <div class="secret" id="cvv-${c.id}">${!account ? '' : account.creditLimit
            ? t('cards.ownAndLimit', { own: money(Math.max(Number(account.balance), 0), account.currency), limit: money(account.creditLimit, account.currency) })
            : `${account.currency} · ${money(account.balance, account.currency)}`}</div>
        <div class="bottom"><span>${esc(holder)}</span><span>${esc(c.expiry)}</span><span class="kind">${esc(c.type)}</span></div>
        ${off ? `<div class="stamp">${esc(label(c.status))}</div>` : ''}</div>`;
};

views.cards = async () => {
    const all = await api('GET', '/accounts');
    const accounts = all.filter((a) => a.type === 'CLIENT');
    const cards = await api('GET', '/cards');
    const types = ['UNIVERSAL', 'PAYROLL', 'STUDENT', 'VIRTUAL'];
    return page(`💳 ${t('nav.cards')}`, t('cards.lead'),
        card(`➕ ${t('cards.issueTitle')}`, `<div class="row">
            <div><label>${t('col.account')}</label><select id="cardAccount">${options(accounts, accountLabel)}</select></div>
            <div><label>${t('col.type')}</label><select id="cardType">${types.map((k) => `<option value="${k}">${esc(label(k))}</option>`).join('')}</select></div>
            <div class="fit"><button onclick="issueCard()">${t('cards.issue')}</button></div></div>`,
            t('cards.issueHint')),
        card(`👛 ${t('cards.mine')}`, cards.length ? `<div class="wallet">${cards.map((c) => `<div class="slot">${cardVisual(c, all)}
            <div class="actions">
                ${c.status !== 'CLOSED' ? `<button class="small secondary" onclick="cardDetails('${c.id}')">${t('cards.showNumber')}</button>` : ''}
                ${c.status === 'ACTIVE' ? `<button class="small secondary" onclick="setPin('${c.id}', ${c.pinSet})">${c.pinSet ? t('cards.changePin') : t('cards.setPin')}</button>` : ''}
                ${c.pinSet && c.status === 'ACTIVE' ? `<button class="small secondary" onclick="verifyPin('${c.id}')">${t('cards.tryPin')}</button>` : ''}
                ${c.status === 'ACTIVE' ? `<button class="small secondary" onclick="cardAction('${c.id}','block')">${t('cards.block')}</button>` : ''}
                ${c.status === 'BLOCKED' ? `<button class="small secondary" onclick="cardAction('${c.id}','unblock')">${t('cards.unblock')}</button>` : ''}
                ${c.status !== 'CLOSED' ? `<button class="small danger" onclick="cardAction('${c.id}','close')">${t('cards.close')}</button>` : ''}
            </div></div>`).join('')}</div>` : `<div class="empty">${t('cards.none')}</div>`));
};

const issueCard = () => act(() => api('POST', '/cards', { accountId: val('cardAccount'), type: val('cardType') }), (c) => t('toast.cardIssued', { type: label(c.type) }));
const cardAction = (id, action) => act(() => api('POST', `/cards/${id}/${action}`), t(`toast.card.${action}`));

async function cardDetails(id) {
    try {
        const d = await api('GET', `/cards/${id}/details`);
        document.getElementById(`num-${id}`).textContent = d.number;
        document.getElementById(`cvv-${id}`).textContent = t('cards.cvvHint', { cvv: d.cvv });
    } catch (e) { toast(e.message, 'bad'); }
}

async function setPin(id, pinSet) {
    const currentPin = pinSet ? window.prompt(t('prompt.currentPin')) : null;
    const newPin = window.prompt(t('prompt.newPin'));
    if (newPin) await act(() => api('PUT', `/cards/${id}/pin`, { currentPin, newPin }), t('toast.pinSaved'));
}

async function verifyPin(id) {
    const pin = window.prompt(t('prompt.tryPin'));
    if (pin) await act(() => api('POST', `/cards/${id}/pin/verification`, { pin }), t('toast.pinCorrect'));
}

// ---------- 💱 Exchange ----------

let currentQuote = null;

views.exchange = async () => {
    const rates = await api('GET', '/exchange-rates');
    const accounts = (await api('GET', '/accounts')).filter((a) => a.type === 'CLIENT');
    return page(`💱 ${t('nav.exchange')}`, t('exchange.lead'),
        card(`📈 ${t('exchange.rates')}`, table([t('col.currency'), t('exchange.nbu'), t('exchange.bankBuys'), t('exchange.bankSells'), t('exchange.mode')], rates.map((r) => `<tr>
            <td><b>${r.currency}</b></td><td class="num">${rate(r.officialRate)}${r.stale ? ' ⚠️' : ''}</td>
            <td class="num">${rate(r.buy)}</td><td class="num">${rate(r.sell)}</td><td>${badge(r.mode || '—')}</td></tr>`),
            t('exchange.noRates'))),
        card(`🔄 ${t('exchange.between')}`, `<div class="row">
            <div><label>${t('exchange.sellFrom')}</label><select id="fxFrom">${options(accounts, accountLabel)}</select></div>
            <div><label>${t('exchange.buyInto')}</label><select id="fxTo">${options([...accounts].reverse(), accountLabel)}</select></div>
            <div><label>${t('exchange.amountToSell')}</label><input id="fxAmount" value="1000"></div>
            <div class="fit"><button onclick="quote()">${t('exchange.getQuote')}</button></div></div>
            <div id="quoteBox">${currentQuote ? quoteHtml(currentQuote) : ''}</div>`, t('exchange.needTwo')));
};

/**
 * The quote's rate the way people read it: "1 $ = 41,85 ₴", never "1 ₴ = 0,0239 $". The API keeps the exact
 * rate (8 decimals) because the amounts are computed from it; the screen only shows it.
 */
const quoteRate = (q) => {
    const sign = (c) => CURRENCY_SIGNS[c] || c;
    return q.fromCurrency === 'UAH'
        ? `1 ${sign(q.toCurrency)} = ${rate(1 / Number(q.rate))} ${sign(q.fromCurrency)}`
        : `1 ${sign(q.fromCurrency)} = ${rate(q.rate)} ${sign(q.toCurrency)}`;
};

const quoteHtml = (q) => `<div class="stats" style="margin-top:12px">
    <div class="stat"><div class="k">${t('exchange.youSell')}</div><div class="v">${money(q.sellAmount, q.fromCurrency)}</div></div>
    <div class="stat"><div class="k">${t('exchange.youGet')}</div><div class="v">${money(q.buyAmount, q.toCurrency)}</div></div>
    <div class="stat"><div class="k">${t('exchange.rate')}</div><div class="v">${quoteRate(q)}</div></div>
    <div class="stat"><div class="k">${t('exchange.priceHolds')}</div><div class="v countdown" id="quoteLeft">…</div></div></div>
    <div class="actions"><button id="acceptQuote" onclick="acceptQuote('${q.id}')">${t('exchange.accept')}</button></div>`;

let quoteTimer;
function startCountdown(q) {
    clearInterval(quoteTimer);
    const tick = () => {
        const left = Math.max(0, Math.round((new Date(q.expiresAt) - Date.now()) / 1000));
        const el = document.getElementById('quoteLeft');
        if (!el) return clearInterval(quoteTimer);
        el.textContent = left > 0 ? t('exchange.seconds', { seconds: left }) : t('exchange.expired');
        document.getElementById('acceptQuote').disabled = left === 0;
        if (left === 0) clearInterval(quoteTimer);
    };
    tick();
    quoteTimer = setInterval(tick, 1000);
}

async function quote() {
    try {
        currentQuote = await api('POST', '/exchange/quotes', { fromAccountId: val('fxFrom'), toAccountId: val('fxTo'), amount: val('fxAmount') });
        document.getElementById('quoteBox').innerHTML = quoteHtml(currentQuote);
        startCountdown(currentQuote);
    } catch (e) { toast(e.message, 'bad'); }
}

async function acceptQuote(quoteId) {
    await act(async () => {
        const p = rememberPayment(await api('POST', '/exchange', { quoteId }, { idempotent: true }));
        currentQuote = null;
        return p;
    }, (p) => t('toast.exchanged', { amount: money(p.creditedAmount) }));
}

// ---------- 🐷 Jars & cashback ----------

views.savings = async () => {
    const jars = await api('GET', '/savings');
    const accounts = (await api('GET', '/accounts')).filter((a) => a.type === 'CLIENT');
    const cashback = await api('GET', '/cashback');
    return page(`🐷 ${t('nav.savings')}`, t('savings.lead'),
        card(`🐷 ${t('savings.mine')}`, table([t('col.currency'), t('col.balance'), t('savings.rate'), t('savings.accrued')], jars.map((j) => `<tr>
                <td><b>${j.currency}</b></td><td class="num">${money(j.balance, j.currency)}</td>
                <td class="num">${t('savings.perYear', { rate: j.annualRate })}</td><td class="num">${money(j.accruedInterest, j.currency)}</td></tr>`), t('savings.none')) +
            `<div class="row" style="margin-top:12px">
                <div><label>${t('savings.openIn')}</label><select id="jarCurrency">${CURRENCIES.map((c) => `<option>${c}</option>`).join('')}</select></div>
                <div class="fit"><button onclick="openJar()">${t('savings.open')}</button></div></div>
             <div class="row" style="margin-top:8px">
                <div><label>${t('savings.jar')}</label><select id="jarId">${options(jars, (j) => t('savings.jarOption', { currency: j.currency, balance: money(j.balance, j.currency) }))}</select></div>
                <div><label>${t('savings.currentAccount')}</label><select id="jarAccount">${options(accounts, accountLabel)}</select></div>
                <div><label>${t('col.amount')}</label><input id="jarAmount" value="1000"></div></div>
             <div class="actions"><button onclick="jarMove('deposits')">${t('savings.putIn')}</button><button class="secondary" onclick="jarMove('withdrawals')">${t('savings.takeOut')}</button></div>`),
        card(`🎁 ${t('savings.cashbackFor', { period: esc(cashback.period) })}`, `<div class="stats">
                <div class="stat"><div class="k">${t('savings.earned')}</div><div class="v">${money(cashback.earned, 'UAH')}</div></div>
                <div class="stat"><div class="k">${t('savings.cap')}</div><div class="v">${money(cashback.monthlyCap, 'UAH')}</div></div>
                <div class="stat"><div class="k">${t('savings.left')}</div><div class="v">${money(cashback.remaining, 'UAH')}</div></div></div>
            <p class="hint" style="margin-top:10px">${t('savings.rates')}: ${Object.entries(cashback.rates || {}).map(([k, v]) => `${esc(label(k))} ${v}%`).join(' · ')}</p>`,
            t('savings.cashbackHint')));
};

const openJar = () => act(() => api('POST', '/savings', { currency: val('jarCurrency') }), t('toast.jarOpened'));
const jarMove = (kind) => act(async () => rememberPayment(await api('POST', `/savings/${val('jarId')}/${kind}`,
    { accountId: val('jarAccount'), amount: val('jarAmount') }, { idempotent: true })), kind === 'deposits' ? t('toast.jarIn') : t('toast.jarOut'));

// ---------- 💼 Sole proprietor (ФОП) ----------

views.business = async () => {
    let business = null;
    try { business = await api('GET', '/business'); } catch { /* 404: not a sole proprietor yet */ }

    if (!business) {
        const name = state.me ? `ФОП ${state.me.lastName} ${state.me.firstName}` : 'ФОП';
        const taxId = String(Math.floor(1e9 + Math.random() * 9e9));
        return page(`💼 ${t('nav.business')}`, t('business.leadNew'),
            card(`📝 ${t('business.register')}`, `<div class="narrow">
                <label>${t('business.name')}</label><input id="fopName" value="${esc(name)}">
                <label>${t('business.taxId')}</label><input id="fopTaxId" value="${taxId}" maxlength="10">
                <label>${t('business.kved')}</label><input id="fopKved" value="62.01, 63.11">
                <label>${t('business.group')}</label><select id="fopGroup" onchange="document.getElementById('fopVat').disabled = this.value !== 'GROUP_3'; if (this.value !== 'GROUP_3') document.getElementById('fopVat').checked = false">
                    ${['GROUP_1', 'GROUP_2', 'GROUP_3'].map((k) => `<option value="${k}" ${k === 'GROUP_3' ? 'selected' : ''}>${esc(t(`business.groupDescription.${k}`))}</option>`).join('')}</select>
                <label class="check"><input type="checkbox" id="fopVat"> ${t('business.vat')}</label>
                <label>${t('business.registeredOn')}</label><input id="fopDate" type="date" value="${new Date().toISOString().slice(0, 10)}">
                <div class="actions"><button onclick="openBusiness()">${t('business.open')}</button></div></div>`,
                t('business.demoHint')));
    }

    const incomes = await api('GET', '/business/incomes');
    const periods = await api('GET', '/tax/periods');
    const cards = (await api('GET', '/cards')).filter((c) => c.accountId === business.businessAccountId);
    const accounts = await api('GET', '/accounts');
    const percent = Number(business.taxRatePercent) > 0;
    return page(`💼 ${t('nav.business')}`, `${esc(business.name)} · РНОКПП ${esc(business.taxId)} · КВЕД ${business.kvedCodes.map(esc).join(', ')}`,
        card(`📊 ${t('business.quarter', { quarter: esc(taxPeriodLabel(business.quarter)) })}`, `<div class="stats">
            <div class="stat"><div class="k">${t('business.account')}</div><div class="v">${money(business.businessBalance, 'UAH')}</div></div>
            <div class="stat"><div class="k">🐷 ${t('business.taxJar')}</div><div class="v">${money(business.taxReserve, 'UAH')}</div></div>
            <div class="stat"><div class="k">${t('business.income')}</div><div class="v">${money(business.quarterIncome, 'UAH')}</div></div>
            <div class="stat"><div class="k">${percent ? t('business.taxAside') : t('business.fixedQuarter')}</div><div class="v">${money(business.quarterTax, 'UAH')}</div></div></div>
            <p class="hint" style="margin-top:10px">${esc(t(`business.groupDescription.${business.taxGroup}`))}${business.vatPayer ? ` · ${t('business.vatPayer')}` : ''}.
            ${percent ? t('business.percentHint', { rate: Number(business.taxRatePercent) }) : t('business.fixedHint', { amount: money(business.monthlyTax, 'UAH') })}
            ${t('business.iban')}: <span class="mono">${esc(business.businessAccountNumber)}</span></p>`),
        card(`🧾 ${t('tax.title')}`, `<label class="check"><input type="checkbox" ${business.autoPayTax ? 'checked' : ''} onchange="setTaxAutoPay(this.checked)">
                <span>${t('tax.autoPay')}</span></label>
            <div style="margin-top:12px">` + table([t('tax.period'), t('tax.dueBy'), t('tax.tax'), t('tax.paid'), t('tax.left'), t('col.status'), ''], periods.map((p) => `<tr>
                <td>${esc(taxPeriodLabel(p.period))}</td><td>${day(p.dueDate)}</td><td class="num">${money(p.amount, 'UAH')}</td>
                <td class="num">${money(p.paid, 'UAH')}</td><td class="num"><b>${money(p.left, 'UAH')}</b></td><td>${badge(p.status)}</td>
                <td>${Number(p.left) > 0 ? `<button class="small" onclick="payTax('${p.period}')">${t('tax.payNow')}</button>` : ''}</td></tr>`),
                t('tax.none')) + '</div>',
            `${business.taxGroup === 'GROUP_3' ? t('tax.hintQuarter') : t('tax.hintMonth')} ${t('tax.hintCommon')}`),
        `<div class="grid">`,
        card(`💵 ${t('business.receive')}`, `<label>${t('bills.amountUah')}</label><input id="fopIncome" value="10000">
            <div class="actions"><button onclick="receiveIncome('${business.businessAccountId}')">${t('business.receiveButton')}</button></div>`,
            t('business.receiveHint')),
        card(`💳 ${t('business.card')}`, cards.length ? `<div class="wallet">${cards.map((c) => cardVisual(c, accounts)).join('')}</div>` : `<div class="empty">${t('cards.none')}</div>`),
        `</div>`,
        card(`🧾 ${t('business.recentIncome')}`, table([t('col.when'), t('business.income'), t('business.taxAside')], incomes.map((i) => `<tr>
            <td>${when(i.createdAt)}</td><td class="num">${money(i.amount, 'UAH')}</td><td class="num">${money(i.tax, 'UAH')}</td></tr>`),
            t('business.noIncome'))));
};

/** "2026-11" → "11.2026" / "11/2026"; "2026-Q4" → "IV квартал 2026" / "Q4 2026". */
function taxPeriodLabel(code) {
    const quarter = /^(\d{4})-Q([1-4])$/.exec(code);
    if (quarter) return t('tax.quarterLabel', { year: quarter[1], roman: ['I', 'II', 'III', 'IV'][quarter[2] - 1], number: quarter[2] });
    const [year, month] = code.split('-');
    return t('tax.monthLabel', { year, month });
}

const openBusiness = () => act(() => api('POST', '/business', {
    name: val('fopName'), taxId: val('fopTaxId'),
    kvedCodes: val('fopKved').split(',').map((k) => k.trim()).filter(Boolean),
    taxGroup: val('fopGroup'), vatPayer: document.getElementById('fopVat').checked, registeredOn: val('fopDate'),
}), t('toast.businessOpened'));

const setTaxAutoPay = (enabled) => act(() => api('PUT', '/business/tax-auto-pay', { enabled }),
    enabled ? t('toast.autoPayOn') : t('toast.autoPayOff'));

const payTax = (period) => act(async () => rememberPayment(await api('POST', `/tax/periods/${period}/payments`)),
    (p) => t('toast.taxPaid', { amount: money(p.amount, 'UAH') }));

async function receiveIncome(accountId) {
    await pay('/payments/top-up', { accountId, amount: val('fopIncome') }, t('business.income'));
}

// ---------- 💳 Credit limit ----------

views.credit = async () => {
    const applications = await api('GET', '/credit-applications');
    let line = null;
    let statements = [];
    try {
        line = await api('GET', '/credit-line');
        statements = await api('GET', '/credit-line/statements');
    } catch { /* no credit line yet: a 404 is the normal answer */ }
    const uah = (await api('GET', '/accounts')).find((x) => x.type === 'CLIENT' && x.currency === 'UAH');
    const cards = await api('GET', '/cards');
    const universal = uah && cards.find((c) => c.type === 'UNIVERSAL' && c.accountId === uah.id && c.status !== 'CLOSED');

    const applyCard = line ? '' : !universal
        ? card(`📝 ${t('credit.get')}`, `<div class="empty-state"><div class="big">💳</div>
            <p>${t('credit.needCard')}</p>
            <button onclick="go('cards')">${t('credit.goToCards')}</button></div>`)
        : card(`📝 ${t('credit.getOnCard')}`, `<div class="row"><div><label>${t('credit.limitUah')}</label><input id="creditLimit" value="8000"></div>
            <div class="fit"><button onclick="applyCredit()">${t('credit.apply')}</button></div></div>`,
            t('credit.applyHint'));

    const lineCard = !line ? '' : card(`💳 ${t('credit.onCard')}`, `<div class="stats">
            <div class="stat"><div class="k">${t('credit.own')}</div><div class="v">${money(line.ownFunds, 'UAH')}</div></div>
            <div class="stat"><div class="k">${t('credit.limit')}</div><div class="v">${money(line.creditLimit, 'UAH')}</div></div>
            <div class="stat"><div class="k">${t('credit.available')}</div><div class="v">${money(line.available, 'UAH')}</div></div>
            <div class="stat"><div class="k">${t('credit.debt')}</div><div class="v">${money(line.debt, 'UAH')}</div></div>
            <div class="stat"><div class="k">${t('credit.lastStatement')}</div><div class="v">${money(line.statementDebt, 'UAH')}</div></div>
            <div class="stat"><div class="k">${t('credit.noInterest')}</div><div class="v">${money(line.leftToRepayInterestFree, 'UAH')}</div></div>
            <div class="stat"><div class="k">${t('credit.minimumLeft')}</div><div class="v">${money(line.leftForMinimumPayment, 'UAH')}</div></div></div>
        <p class="hint" style="margin-top:10px">${t('credit.rules')}<br>
           ${t('credit.graceDates', { statement: day(line.statementInterestFreeUntil), spending: day(line.newSpendingInterestFreeUntil) })}</p>
        <h2 style="margin-top:16px">📅 ${t('credit.statements')}</h2>` + table([t('credit.month'), t('credit.debt'), t('credit.minimum'), t('credit.due'), t('col.status'), t('credit.charged')], statements.map((s) => `<tr>
            <td>${esc(s.period)}</td><td class="num">${money(s.closingDebt, 'UAH')}</td><td class="num">${money(s.minimumPayment, 'UAH')}</td>
            <td>${day(s.dueDate)}</td><td>${badge(s.status)}</td><td class="num">${s.interest == null ? '—' : money(Number(s.interest) + Number(s.lateFee), 'UAH')}</td></tr>`),
        t('credit.firstStatement')));

    return page(`💳 ${t('nav.credit')}`, t('credit.lead'),
        lineCard, applyCard,
        card(`🗂️ ${t('credit.applications')}`, table([t('col.when'), t('credit.asked'), t('credit.affordable'), t('credit.approved'), t('credit.decision'), t('credit.why')], applications.map((x) => `<tr>
            <td>${when(x.createdAt)}</td><td class="num">${money(x.requestedLimit, 'UAH')}</td><td class="num">${money(x.affordableLimit, 'UAH')}</td>
            <td class="num">${money(x.approvedLimit, 'UAH')}</td><td>${badge(x.status)}<br><small>${esc(x.decisionSource ? label(x.decisionSource) : '')}</small></td>
            <td><small>${(x.reasons || []).map(esc).join('<br>')}${x.decisionComment ? `<br>💬 ${esc(x.decisionComment)}` : ''}</small></td></tr>`), t('credit.noApplications'))));
};

const applyCredit = () => act(() => api('POST', '/credit-applications', { requestedLimit: val('creditLimit') }),
    (x) => `${t('toast.application')}: ${label(x.status)}${x.status === 'MANUAL_REVIEW' ? ` ${t('toast.askAdmin')}` : ''}`);

// ---------- 🔔 Notifications ----------

views.notifications = async () => {
    const { content } = await api('GET', '/notifications?size=50');
    return page(`🔔 ${t('nav.notifications')}`, t('notifications.lead'),
        card(t('notifications.inbox'), table([t('col.when'), t('notifications.what'), ''], content.map((n) => `<tr style="${n.readAt ? 'opacity:.6' : ''}">
            <td>${when(n.createdAt)}</td><td><b>${esc(n.title)}</b><br>${esc(n.body)}<br><small class="mono">${esc(n.eventType)}</small></td>
            <td>${n.readAt ? '✓' : `<button class="small secondary" onclick="readNotification('${n.id}')">${t('notifications.read')}</button>`}</td></tr>`),
            t('notifications.none')) +
        `<div class="actions"><button class="secondary" onclick="readAll()">${t('notifications.readAll')}</button><button class="secondary" onclick="render()">${t('common.refresh')}</button></div>`));
};

const readNotification = (id) => act(() => api('POST', `/notifications/${id}/read`));
const readAll = () => act(() => api('POST', '/notifications/read-all'), t('toast.allRead'));

// ---------- 🎧 Support ----------

let openTicketId = null;

views.support = async () => {
    const tickets = await api('GET', '/support/tickets');
    const opened = openTicketId ? await api('GET', `/support/tickets/${openTicketId}`) : null;
    return page(`🎧 ${t('nav.support')}`, t('support.lead'),
        `<div class="grid">`,
        card(`✉️ ${t('support.newTicket')}`, `<label>${t('support.subject')}</label><input id="tSubject" value="${esc(t('support.subjectDefault'))}">
            <label>${t('support.message')}</label><textarea id="tMessage">${esc(t('support.messageDefault'))}</textarea>
            <div class="actions"><button onclick="openTicket()">${t('common.send')}</button></div>`),
        card(`🗂️ ${t('support.mine')}`, table([t('support.subject'), t('col.status'), ''], tickets.map((x) => `<tr>
            <td>${esc(x.subject)}<br><small>${when(x.createdAt)}</small></td><td>${badge(x.status)}</td>
            <td><button class="small secondary" onclick="showTicket('${x.id}')">${t('common.open')}</button></td></tr>`), t('support.none'))),
        `</div>`,
        opened ? card(`💬 ${esc(opened.ticket.subject)} ${badge(opened.ticket.status)}`, conversation(opened.messages) +
            (opened.ticket.status !== 'RESOLVED' ? `<label>${t('support.reply')}</label><textarea id="tReply"></textarea>
            <div class="actions"><button onclick="replyTicket('${opened.ticket.id}')">${t('common.send')}</button></div>` : '')) : '');
};

const conversation = (messages) => messages.map((m) => `<div style="margin:6px 0;padding:8px 10px;border-radius:10px;max-width:80%;
    ${m.fromStaff ? 'background:var(--brand-soft);margin-left:auto' : 'background:var(--bg)'}">
    <small>${m.fromStaff ? `🏦 ${t('support.bank')}` : `👤 ${t('support.client')}`} · ${when(m.createdAt)}</small><br>${esc(m.body)}</div>`).join('');

const openTicket = () => act(async () => { const x = await api('POST', '/support/tickets', { subject: val('tSubject'), message: val('tMessage') }); openTicketId = x.ticket.id; return x; }, t('toast.ticketSent'));
const showTicket = (id) => { openTicketId = id; return render(); };
const replyTicket = (id) => act(() => api('POST', `/support/tickets/${id}/messages`, { message: val('tReply') }), t('toast.sent'));

// ---------- 👀 Staff: credit queue ----------

views.staffCredit = async () => {
    const { content } = await api('GET', '/admin/credit-applications?size=50');
    return page(`👀 ${t('nav.staffCredit')}`, t('staffCredit.lead'),
        card(t('staffCredit.waiting'), table([t('col.when'), t('staffCredit.client'), t('credit.asked'), t('credit.affordable'), t('staffCredit.whyHere'), t('credit.decision')], content.map((a) => `<tr>
            <td>${when(a.createdAt)}</td><td class="mono">${short(a.userId)}</td><td class="num">${money(a.requestedLimit, 'UAH')}</td>
            <td class="num">${money(a.affordableLimit, 'UAH')}</td><td><small>${(a.reasons || []).map(esc).join('<br>')}</small></td>
            <td><input id="lim-${a.id}" value="${a.affordableLimit}" style="width:110px">
                <input id="cmt-${a.id}" placeholder="${esc(t('staffCredit.comment'))}" style="width:170px;margin-top:4px">
                <div class="actions" style="margin-top:6px"><button class="small" onclick="decide('${a.id}','approve')">${t('staffCredit.approve')}</button>
                <button class="small danger" onclick="decide('${a.id}','decline')">${t('staffCredit.decline')}</button></div></td></tr>`), t('staffCredit.empty'))));
};

const decide = (id, decision) => act(() => api('POST', `/admin/credit-applications/${id}/${decision}`,
    { approvedLimit: decision === 'approve' ? val(`lim-${id}`) : null, comment: val(`cmt-${id}`) }), t(`toast.decided.${decision}`));

// ---------- 🎧 Staff: support queue ----------

let staffTicketId = null;
let staffStatus = 'OPEN';

views.staffSupport = async () => {
    const { content } = await api('GET', `/admin/support/tickets?status=${staffStatus}&size=50`);
    const opened = staffTicketId ? await api('GET', `/admin/support/tickets/${staffTicketId}`) : null;
    let client = '';
    if (opened && opened.ticket.assigneeId === state.me.id) {
        const view = await api('GET', `/admin/support/tickets/${staffTicketId}/client`);
        client = card(`👤 ${esc(view.client.firstName)} ${esc(view.client.lastName)} · ${esc(view.client.email)}`,
            table([t('col.currency'), t('col.type'), t('col.balance')], view.accounts.map((a) => `<tr><td>${a.currency}</td><td>${badge(a.type)}</td><td class="num">${money(a.balance, a.currency)}</td></tr>`)),
            t('staffSupport.clientHint'));
    }
    return page(`🎧 ${t('nav.staffSupport')}`, t('staffSupport.lead'),
        card(t('staffSupport.tickets'), `<div class="actions" style="margin:0 0 10px">${['OPEN', 'IN_PROGRESS', 'RESOLVED'].map((s) =>
            `<button class="small ${s === staffStatus ? '' : 'secondary'}" onclick="staffFilter('${s}')">${esc(label(s))}</button>`).join('')}</div>` +
            table([t('col.when'), t('support.subject'), t('col.status'), ''], content.map((x) => `<tr><td>${when(x.createdAt)}</td><td>${esc(x.subject)}</td><td>${badge(x.status)}</td>
            <td>${x.status === 'OPEN' ? `<button class="small" onclick="takeTicket('${x.id}')">${t('staffSupport.take')}</button>` : ''}
                <button class="small secondary" onclick="staffOpen('${x.id}')">${t('common.open')}</button></td></tr>`), t('staffSupport.empty'))),
        opened ? card(`💬 ${esc(opened.ticket.subject)} ${badge(opened.ticket.status)}`, conversation(opened.messages) +
            (opened.ticket.status === 'IN_PROGRESS' && opened.ticket.assigneeId === state.me.id ? `<label>${t('staffSupport.answer')}</label><textarea id="sReply">${esc(t('staffSupport.answerDefault'))}</textarea>
            <div class="actions"><button onclick="staffAnswer('${opened.ticket.id}')">${t('staffSupport.answer')}</button>
            <button class="secondary" onclick="staffResolve('${opened.ticket.id}')">${t('staffSupport.resolve')}</button></div>` : '')) : '',
        client);
};

const staffFilter = (s) => { staffStatus = s; return render(); };
const staffOpen = (id) => { staffTicketId = id; return render(); };
const takeTicket = (id) => act(async () => { await api('POST', `/admin/support/tickets/${id}/take`); staffTicketId = id; staffStatus = 'IN_PROGRESS'; }, t('toast.ticketYours'));
const staffAnswer = (id) => act(() => api('POST', `/admin/support/tickets/${id}/messages`, { message: val('sReply') }), t('toast.answered'));
const staffResolve = (id) => act(() => api('POST', `/admin/support/tickets/${id}/resolve`), t('toast.resolved'));

// ---------- 💱 Staff: bank rates ----------

views.staffRates = async () => {
    const rates = await api('GET', '/exchange-rates');
    return page(`💱 ${t('nav.staffRates')}`, t('staffRates.lead'),
        card(t('staffRates.current'), table([t('col.currency'), t('exchange.nbu'), t('staffRates.buy'), t('staffRates.sell'), t('exchange.mode'), t('staffRates.spread')], rates.map((r) => `<tr><td><b>${r.currency}</b></td>
            <td class="num">${rate(r.officialRate)}</td><td class="num">${rate(r.buy)}</td><td class="num">${rate(r.sell)}</td>
            <td>${badge(r.mode || '—')}</td><td class="num">${r.spreadPercent ?? '—'}</td></tr>`)) +
            `<div class="actions"><button class="secondary" onclick="syncRates()">${t('staffRates.sync')}</button></div>`),
        card(t('staffRates.set'), `<div class="row">
            <div><label>${t('col.currency')}</label><select id="rCur"><option>USD</option><option>EUR</option></select></div>
            <div><label>${t('exchange.mode')}</label><select id="rMode"><option value="SPREAD">${esc(label('SPREAD'))}</option><option value="MANUAL">${esc(label('MANUAL'))}</option></select></div>
            <div><label>${t('staffRates.spreadField')}</label><input id="rSpread" value="2.00"></div>
            <div><label>${t('staffRates.buyField')}</label><input id="rBuy"></div><div><label>${t('staffRates.sellField')}</label><input id="rSell"></div></div>
            <label>${t('staffRates.comment')}</label><input id="rComment" value="${esc(t('staffRates.commentDefault'))}">
            <div class="actions"><button onclick="setRate()">${t('common.save')}</button></div>`, t('staffRates.hint')));
};

const syncRates = () => act(() => api('POST', '/admin/exchange-rates/sync'), t('toast.ratesSynced'));
function setRate() {
    const manual = val('rMode') === 'MANUAL';
    return act(() => api('PUT', `/admin/exchange-rates/${val('rCur')}`, {
        mode: val('rMode'), spreadPercent: manual ? null : val('rSpread'),
        buy: manual ? val('rBuy') : null, sell: manual ? val('rSell') : null, comment: val('rComment'),
    }), t('toast.rateSaved'));
}

// ---------- 🕵️ Staff: audit ----------

let auditAction = '';

views.staffAudit = async () => {
    const { content } = await api('GET', `/admin/audit?size=50${auditAction ? `&action=${auditAction}` : ''}`);
    const actions = ['', 'CREDIT_APPLICATION_APPROVED', 'CREDIT_APPLICATION_DECLINED', 'BANK_RATE_SET', 'TICKET_TAKEN', 'TICKET_ANSWERED', 'TICKET_RESOLVED', 'CLIENT_ACCOUNTS_VIEWED'];
    return page(`🕵️ ${t('nav.staffAudit')}`, t('staffAudit.lead'),
        card(t('staffAudit.entries'), `<div class="row" style="margin-bottom:10px"><div><label>${t('staffAudit.action')}</label>
            <select onchange="auditAction=this.value;render()">${actions.map((a) => `<option ${a === auditAction ? 'selected' : ''} value="${a}">${a ? esc(label(a)) : t('staffAudit.all')}</option>`).join('')}</select></div></div>` +
            table([t('col.when'), t('staffAudit.who'), t('staffAudit.action'), t('staffAudit.on'), t('staffAudit.details')], content.map((e) => `<tr><td>${when(e.occurredAt)}</td>
            <td class="mono">${short(e.actorId)}<br><span class="badge info">${esc(t(`role.${e.actorRole}`))}</span></td><td>${badge(e.action)}</td>
            <td><small>${esc(e.targetType)}</small><br><span class="mono">${short(e.targetId)}</span></td>
            <td class="mono"><small>${esc(JSON.stringify(e.details))}</small></td></tr>`), t('staffAudit.empty'))));
};

// ---------- Start ----------

document.getElementById('regEmail').value = `client-${Math.random().toString(36).slice(2, 7)}@bank.local`;
fillGeneratedPassword('regPassword');
showRememberedEmail();
showAuth(rememberedEmail() ? 'login' : 'register');
if (state.access) start().catch(() => logout(true));
