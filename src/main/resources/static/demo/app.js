// Demo client of the bank. Plain JavaScript over the REST API: every call shows up in the log on the right.
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

/** One place for every request: token, idempotency key, the log, a token refresh on 401, readable errors. */
async function api(method, path, body, { idempotent = false, raw = false, retried = false, quiet = false } = {}) {
    const headers = {};
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
        throw new Error('The bank does not answer. Is it running?');
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

function writeLog(headers, method, path, status, ms, request, response) {
    const id = uuid();
    curls.set(id, [`curl -X ${method} '${location.origin}${path}'`,
        ...Object.entries(headers).map(([k, v]) => `-H '${k}: ${v}'`),
        ...(request !== undefined ? [`-d '${JSON.stringify(request)}'`] : [])].join(' \\\n  '));
    const entry = document.createElement('details');
    const s = String(status)[0] || '0';
    const time = new Date().toLocaleTimeString('uk-UA', { hour12: false });
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
    try { await navigator.clipboard.writeText(curls.get(id)); toast('Copied: paste it into a terminal'); }
    catch { window.prompt('Copy the command', curls.get(id)); }
}

let toastTimer;
function toast(message, kind = 'ok') {
    const t = document.getElementById('toast');
    t.textContent = message;
    t.className = `show ${kind}`;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => (t.className = ''), 3500);
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
const money = (amount, currency = '') => amount == null ? '—' : `${Number(amount).toLocaleString('uk-UA', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}`;
const when = (iso) => iso ? new Date(iso).toLocaleString('uk-UA') : '—';
const short = (id) => id ? String(id).slice(0, 8) : '—';
const badge = (text) => {
    const kind = /COMPLETED|APPROVED|ACTIVE|PAID|RESOLVED|SENT/.test(text) ? 'ok'
        : /FAILED|DECLINED|BLOCKED|OVERDUE|CLOSED|LOCKED/.test(text) ? 'bad'
            : /PROCESSING|UNKNOWN|MANUAL_REVIEW|OPEN|IN_PROGRESS|MINIMUM_PAID|PENDING/.test(text) ? 'warn' : 'info';
    return `<span class="badge ${kind}">${esc(text)}</span>`;
};
const options = (items, label, valueOf = (x) => x.id) => items.map((x) => `<option value="${esc(valueOf(x))}">${esc(label(x))}</option>`).join('');
const accountLabel = (a) => `${a.currency} · ${a.type} · ${money(a.balance, a.currency)} · …${a.number.slice(-6)}`;
const isStaff = () => state.me && (state.me.role === 'ADMIN' || state.me.role === 'SUPPORT');

function rememberPayment(payment) {
    state.payments = [payment, ...state.payments.filter((p) => p.id !== payment.id)].slice(0, 20);
    sessionStorage.setItem('payments', JSON.stringify(state.payments));
    return payment;
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
        toast('Welcome! Open an account to begin.');
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
        toast('If this email has an account, a link is on its way. Check Mailpit (localhost:8025).');
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
    document.getElementById('who').textContent = `${state.me.firstName} ${state.me.lastName} · ${state.me.role}`;
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

async function render() {
    const main = document.getElementById('view');
    try {
        main.innerHTML = await views[state.view]();
    } catch (e) {
        main.innerHTML = `<div class="card"><h2>Something went wrong</h2><p>${esc(e.message)}</p></div>`;
    }
    refreshBell();
}

const page = (title, lead, ...cards) => `<h1>${title}</h1><p class="lead">${lead}</p>${cards.join('')}`;
const card = (title, body, hint = '') => `<div class="card"><h2>${title}</h2>${hint ? `<p class="hint">${hint}</p>` : ''}${body}</div>`;
const table = (headers, rows, empty = 'Nothing yet') => rows.length
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
            <div class="row"><div><label>Top up ${esc(a.currency)} …${esc(a.number.slice(-6))} with</label>
            <input id="quickTopUpAmount" value="${a.currency === 'UAH' ? '60000' : '1000'}"></div>
            <div class="fit"><button onclick="quickTopUp('${a.id}')">💵 Top up</button></div>
            <div class="fit"><button class="secondary" onclick="topUpAccountId = null; render()">Cancel</button></div></div>
            <p class="hint below">Money from outside the bank, e.g. a card of another bank. Free.</p></td></tr>`;
    return page('🏠 Overview', 'Your accounts and cards. Every balance is read from the double-entry ledger.',
        empty ? `<div class="notice">💡 Your account is empty. Press <b>💵 Top up</b> next to it to bring in money, then try transfers, bills and exchange.</div>` : '',
        card('💰 Accounts', table(['Currency', 'Type', 'IBAN', 'Balance', ''], accounts.map((a) => `<tr>
                <td><b>${a.currency}</b></td><td>${badge(a.type)}</td><td class="mono">${esc(a.number)}</td>
                <td class="num"><b>${money(a.balance, a.currency)}</b>${a.creditLimit ? `<br><small>limit ${money(a.creditLimit)}</small>` : ''}</td>
                <td class="buttons">${a.type === 'CLIENT' ? `<button class="small" onclick="topUpAccountId = '${a.id}'; render()">💵 Top up</button>` : ''}
                    <button class="small secondary" onclick="showStatement('${a.id}')">Statement</button></td></tr>${topUpRow(a)}`),
            'No accounts yet: open one below') +
            `<div class="row" style="margin-top:12px"><div><label>Open an account in</label>
             <select id="newCurrency">${CURRENCIES.map((c) => `<option>${c}</option>`).join('')}</select></div>
             <div class="fit"><button onclick="openAccount()">Open account</button></div></div>
             <div id="statement"></div>`),
        card('💳 Cards', cards.length ? `<div class="wallet">${cards.map((c) => cardVisual(c, accounts)).join('')}</div>`
            : '<div class="empty">No cards yet: issue one on the Cards page</div>'));
};

async function openAccount() {
    await act(() => api('POST', '/accounts', { currency: val('newCurrency') }), (a) => `Account ${a.currency} opened`);
}

async function quickTopUp(accountId) {
    const payment = await pay('/payments/top-up', { accountId, amount: val('quickTopUpAmount') }, 'Top-up');
    if (payment) {
        topUpAccountId = null;
        render();
    }
}

async function showStatement(accountId) {
    try {
        const { content } = await api('GET', `/accounts/${accountId}/transactions?size=20`);
        document.getElementById('statement').innerHTML = `<h2 style="margin-top:16px">📜 Statement</h2>` + table(
            ['When', 'Operation', 'Amount', 'Balance after'],
            content.map((e) => `<tr><td>${when(e.createdAt)}</td><td>${badge(e.type)} ${esc(e.description)}</td>
                <td class="num ${e.direction === 'CREDIT' ? 'plus' : 'minus'}">${e.direction === 'CREDIT' ? '+' : '−'}${money(e.amount)}</td>
                <td class="num">${money(e.balanceAfter)}</td></tr>`),
            'No operations yet');
    } catch (e) { toast(e.message, 'bad'); }
}

// ---------- 🔁 Payments ----------

views.payments = async () => {
    const accounts = (await api('GET', '/accounts')).filter((a) => a.type !== 'CREDIT');
    const spending = await api('GET', '/accounts');
    const cards = await api('GET', '/cards');
    const accountOptions = options(accounts, accountLabel);
    return page('🔁 Transfers', 'Every payment carries a new Idempotency-Key: pressing a button twice can never pay twice. Utility bills are under 🧾 Bills & utilities.',
        `<div class="grid">`,
        card('💵 Top up', `<label>Account</label><select id="topUpAccount">${accountOptions}</select>
            <label>Amount</label><input id="topUpAmount" value="60000">
            <div class="actions"><button onclick="topUp()">Top up</button></div>`, 'Money from outside the bank. Free.'),
        card('➡️ Transfer by IBAN', `<label>From</label><select id="trFrom">${accountOptions}</select>
            <label>Recipient IBAN</label><input id="trTo" placeholder="UA…">
            <label>Amount</label><input id="trAmount" value="500">
            <label>Description</label><input id="trDesc" value="Rent">
            <div class="actions"><button onclick="transfer()">Send</button></div>`, '0.5%, at least 5.00 ₴.'),
        card('💳 Card to card', `<label>From card</label><select id="ctFrom">${options(cards, (c) => `${c.type} ${c.maskedNumber}`)}</select>
            <label>Recipient card number</label><input id="ctTo" placeholder="16 digits">
            <label>Amount</label><input id="ctAmount" value="250">
            <div class="actions"><button onclick="cardTransfer()">Send</button></div>`, 'Fee by card type: universal and payroll 0.5%, student 0%, virtual 1%, credit 2%.'),
        `</div>`,
        card('🧾 Payments of this session', table(['When', 'Type', 'Amount', 'Fee', 'Status', ''], state.payments.map((p) => `<tr>
                <td>${when(p.createdAt)}</td><td>${badge(p.type)}<br><small>${esc(p.description)}</small></td>
                <td class="num">${money(p.amount, p.currency)}</td><td class="num">${money(p.fee)}</td>
                <td>${badge(p.status)}${p.failureReason ? `<br><small>${esc(p.failureReason)}</small>` : ''}</td>
                <td><button class="small secondary" onclick="checkPayment('${p.id}')">Refresh</button>
                    <button class="small secondary" onclick="receipt('${p.id}')">PDF</button>
                    <button class="small secondary" onclick="emailReceipt('${p.id}')">Email</button></td></tr>`),
            'Make a payment above'), 'The bank keeps every payment; this list is only what you did in this browser tab.'));
};

const pay = (path, body, label) => act(async () => rememberPayment(await api('POST', path, body, { idempotent: true })),
    (p) => `${label}: ${p.status}${Number(p.fee) > 0 ? `, fee ${money(p.fee, p.currency)}` : ''}`);

const topUp = () => pay('/payments/top-up', { accountId: val('topUpAccount'), amount: val('topUpAmount') }, 'Top-up');
const transfer = () => pay('/payments/transfers', { fromAccountId: val('trFrom'), toAccountNumber: val('trTo'), amount: val('trAmount'), description: val('trDesc') }, 'Transfer');
const cardTransfer = () => pay('/payments/card-transfers', { fromCardId: val('ctFrom'), toCardNumber: val('ctTo'), amount: val('ctAmount') }, 'Card transfer');
// ---------- 🧾 Bills & utilities ----------

const PROVIDER_ICONS = { ELECTRICITY: '⚡', WATER: '💧', GAS: '🔥', HEATING: '♨️', INTERNET: '🌐', MOBILE: '📱' };
let selectedProvider = null;

views.bills = async () => {
    const providers = await api('GET', '/bill-payments/providers');
    const accounts = (await api('GET', '/accounts')).filter((a) => a.currency === 'UAH' && a.type !== 'SAVINGS');
    const provider = providers.find((p) => p.id === selectedProvider) || providers[0];
    selectedProvider = provider?.id;
    const mobile = provider?.category === 'MOBILE';
    const bills = state.payments.filter((p) => p.type === 'BILL_PAYMENT');

    const form = !accounts.length
        ? `<div class="empty-state"><div class="big">🧾</div><p>Bills are paid from a UAH account.</p>
            <button onclick="openUahAccount()">Open a UAH account</button></div>`
        : `<label>From</label><select id="billFrom">${options(accounts, accountLabel)}</select>
            <label>${mobile ? 'Phone number' : 'Your account number at the provider'}</label>
            <input id="billAccount" value="${mobile ? '+380671234567' : '4410012345'}">
            <label>Amount, UAH</label><input id="billAmount" value="${mobile ? '200' : '300'}">
            <div class="actions"><button onclick="payBill()">Pay ${esc(provider?.name || '')}</button></div>`;

    return page('🧾 Bills & utilities', 'Paid asynchronously: the bank holds the money at once (PROCESSING), a background worker calls the provider, and the payment becomes COMPLETED or FAILED. Watch it in the API log.',
        card('1. Choose a provider', `<div class="providers">${providers.map((p) => `
            <button type="button" class="provider ${p.id === selectedProvider ? 'selected' : ''}" onclick="selectProvider('${p.id}')">
                <span class="ico">${PROVIDER_ICONS[p.category] || '🧾'}</span><span>${esc(p.name)}</span><span class="cat">${esc(p.category)}</span>
            </button>`).join('')}</div>`),
        card('2. Pay', `<div class="narrow">${form}</div>`, 'Fee: 0.5% for utilities and internet, 1% for mobile, at least 1.00 ₴. Cashback comes once the provider accepts.'),
        card('🧾 My bill payments', table(['When', 'Provider', 'Amount', 'Status', ''], bills.map((p) => `<tr>
                <td>${when(p.createdAt)}</td><td>${esc(providers.find((x) => x.id === p.providerId)?.name || p.description)}</td>
                <td class="num">${money(p.amount, p.currency)}</td>
                <td>${badge(p.status)}${p.failureReason ? `<br><small>${esc(p.failureReason)}</small>` : ''}</td>
                <td><button class="small secondary" onclick="receipt('${p.id}')">PDF</button></td></tr>`),
            'Nothing paid in this tab yet'), 'Only what you paid in this browser tab.'));
};

function selectProvider(id) {
    selectedProvider = id;
    render();
}

const openUahAccount = () => act(() => api('POST', '/accounts', { currency: 'UAH' }), 'UAH account opened');

async function payBill() {
    const payment = await pay('/bill-payments', { providerId: selectedProvider, accountNumber: val('billAccount'), fromAccountId: val('billFrom'), amount: val('billAmount') }, 'Bill payment');
    if (payment) followPayment(payment.id);
}

/** The provider is called in the background: watch PROCESSING turn into COMPLETED or FAILED by itself. */
async function followPayment(id) {
    for (let attempt = 0; attempt < 20; attempt++) {
        await new Promise((resolve) => setTimeout(resolve, 1500));
        const p = rememberPayment(await api('GET', `/payments/${id}`, undefined, { quiet: true }));
        if (p.status !== 'PROCESSING' && p.status !== 'UNKNOWN') {
            toast(`Bill payment ${p.status}${p.failureReason ? `: ${p.failureReason}` : ''}`, p.status === 'COMPLETED' ? 'ok' : 'bad');
            if (state.view === 'payments' || state.view === 'bills') render();
            return;
        }
    }
}

async function checkPayment(id) {
    await act(async () => rememberPayment(await api('GET', `/payments/${id}`)), (p) => `Payment is ${p.status}`);
}

async function receipt(id) {
    try {
        const response = await api('GET', `/payments/${id}/receipt`, undefined, { raw: true });
        window.open(URL.createObjectURL(await response.blob()), '_blank');
    } catch (e) { toast(e.message, 'bad'); }
}

async function emailReceipt(id) {
    await act(() => api('POST', `/payments/${id}/receipt/email`), 'Receipt sent: see it in Mailpit (localhost:8025)');
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
            ? `Own ${money(Math.max(Number(account.balance), 0), account.currency)} · limit ${money(account.creditLimit, account.currency)}`
            : `${account.currency} · ${money(account.balance, account.currency)}`}</div>
        <div class="bottom"><span>${esc(holder)}</span><span>${esc(c.expiry)}</span><span class="kind">${esc(c.type)}</span></div>
        ${off ? `<div class="stamp">${esc(c.status.replace('_', ' '))}</div>` : ''}</div>`;
};

views.cards = async () => {
    const all = await api('GET', '/accounts');
    const accounts = all.filter((a) => a.type === 'CLIENT');
    const cards = await api('GET', '/cards');
    return page('💳 Cards', 'Card numbers are stored encrypted (AES-256-GCM), the PIN as a keyed hash, the CVV not at all.',
        card('➕ Issue a card', `<div class="row">
            <div><label>Account</label><select id="cardAccount">${options(accounts, accountLabel)}</select></div>
            <div><label>Type</label><select id="cardType"><option>UNIVERSAL</option><option>PAYROLL</option><option>STUDENT</option><option>VIRTUAL</option></select></div>
            <div class="fit"><button onclick="issueCard()">Issue</button></div></div>`,
            'Universal: the main everyday card, 0.5% per card transfer. Payroll 0.5%, student 0%, virtual 1% (no plastic, so no chip). A credit limit lives on the UNIVERSAL card: see 💳 Credit limit in the menu.'),
        card('👛 My cards', cards.length ? `<div class="wallet">${cards.map((c) => `<div class="slot">${cardVisual(c, all)}
            <div class="actions">
                ${c.status !== 'CLOSED' ? `<button class="small secondary" onclick="cardDetails('${c.id}')">Show number</button>` : ''}
                ${c.status === 'ACTIVE' ? `<button class="small secondary" onclick="setPin('${c.id}', ${c.pinSet})">${c.pinSet ? 'Change' : 'Set'} PIN</button>` : ''}
                ${c.pinSet && c.status === 'ACTIVE' ? `<button class="small secondary" onclick="verifyPin('${c.id}')">Try PIN</button>` : ''}
                ${c.status === 'ACTIVE' ? `<button class="small secondary" onclick="cardAction('${c.id}','block')">Block</button>` : ''}
                ${c.status === 'BLOCKED' ? `<button class="small secondary" onclick="cardAction('${c.id}','unblock')">Unblock</button>` : ''}
                ${c.status !== 'CLOSED' ? `<button class="small danger" onclick="cardAction('${c.id}','close')">Close</button>` : ''}
            </div></div>`).join('')}</div>` : '<div class="empty">No cards yet</div>'));
};

const issueCard = () => act(() => api('POST', '/cards', { accountId: val('cardAccount'), type: val('cardType') }), (c) => `${c.type} card issued`);
const cardAction = (id, action) => act(() => api('POST', `/cards/${id}/${action}`), `Card: ${action}`);

async function cardDetails(id) {
    try {
        const d = await api('GET', `/cards/${id}/details`);
        document.getElementById(`num-${id}`).textContent = d.number;
        document.getElementById(`cvv-${id}`).textContent = `CVV ${d.cvv} · copy the number into "Card to card"`;
    } catch (e) { toast(e.message, 'bad'); }
}

async function setPin(id, pinSet) {
    const currentPin = pinSet ? window.prompt('Current PIN') : null;
    const newPin = window.prompt('New PIN (4 digits; 1111, 1234 and the like are refused)');
    if (newPin) await act(() => api('PUT', `/cards/${id}/pin`, { currentPin, newPin }), 'PIN saved');
}

async function verifyPin(id) {
    const pin = window.prompt('PIN, as at a terminal. Three wrong ones lock the card.');
    if (pin) await act(() => api('POST', `/cards/${id}/pin/verification`, { pin }), 'PIN is correct');
}

// ---------- 💱 Exchange ----------

let currentQuote = null;

views.exchange = async () => {
    const rates = await api('GET', '/exchange-rates');
    const accounts = (await api('GET', '/accounts')).filter((a) => a.type === 'CLIENT');
    return page('💱 Exchange', 'Ask for a quote, then accept it: the price holds for 30 seconds, even if the rate changes meanwhile.',
        card('📈 Rates', table(['Currency', 'NBU', 'Bank buys', 'Bank sells', 'Mode'], rates.map((r) => `<tr>
            <td><b>${r.currency}</b></td><td class="num">${r.officialRate ?? '—'}${r.stale ? ' ⚠️' : ''}</td>
            <td class="num">${r.buy ?? '—'}</td><td class="num">${r.sell ?? '—'}</td><td>${badge(r.mode || '—')}</td></tr>`),
            'No rates yet: the bank loads them from the NBU at start-up')),
        card('🔄 Exchange between my accounts', `<div class="row">
            <div><label>Sell from</label><select id="fxFrom">${options(accounts, accountLabel)}</select></div>
            <div><label>Buy into</label><select id="fxTo">${options([...accounts].reverse(), accountLabel)}</select></div>
            <div><label>Amount to sell</label><input id="fxAmount" value="1000"></div>
            <div class="fit"><button onclick="quote()">Get a quote</button></div></div>
            <div id="quoteBox">${currentQuote ? quoteHtml(currentQuote) : ''}</div>`, 'You need accounts in two currencies.'));
};

const quoteHtml = (q) => `<div class="stats" style="margin-top:12px">
    <div class="stat"><div class="k">You sell</div><div class="v">${money(q.sellAmount, q.fromCurrency)}</div></div>
    <div class="stat"><div class="k">You get</div><div class="v">${money(q.buyAmount, q.toCurrency)}</div></div>
    <div class="stat"><div class="k">Rate</div><div class="v">${q.rate}</div></div>
    <div class="stat"><div class="k">Price holds</div><div class="v countdown" id="quoteLeft">…</div></div></div>
    <div class="actions"><button id="acceptQuote" onclick="acceptQuote('${q.id}')">Accept this price</button></div>`;

let quoteTimer;
function startCountdown(q) {
    clearInterval(quoteTimer);
    const tick = () => {
        const left = Math.max(0, Math.round((new Date(q.expiresAt) - Date.now()) / 1000));
        const el = document.getElementById('quoteLeft');
        if (!el) return clearInterval(quoteTimer);
        el.textContent = left > 0 ? `${left} s` : 'expired';
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
    }, (p) => `Exchanged: got ${money(p.creditedAmount)}`);
}

// ---------- 🐷 Jars & cashback ----------

views.savings = async () => {
    const jars = await api('GET', '/savings');
    const accounts = (await api('GET', '/accounts')).filter((a) => a.type === 'CLIENT');
    const cashback = await api('GET', '/cashback');
    return page('🐷 Jars & cashback', 'A jar (банка) keeps savings apart from your account and earns interest every day, paid monthly. Cashback comes from mobile, internet and utility bills.',
        card('🐷 My jars', table(['Currency', 'Balance', 'Rate', 'Accrued, not paid yet'], jars.map((j) => `<tr>
                <td><b>${j.currency}</b></td><td class="num">${money(j.balance, j.currency)}</td>
                <td class="num">${j.annualRate}% / year</td><td class="num">${money(j.accruedInterest, j.currency)}</td></tr>`), 'No jars yet') +
            `<div class="row" style="margin-top:12px">
                <div><label>Open a jar in</label><select id="jarCurrency">${CURRENCIES.map((c) => `<option>${c}</option>`).join('')}</select></div>
                <div class="fit"><button onclick="openJar()">Open jar</button></div></div>
             <div class="row" style="margin-top:8px">
                <div><label>Jar</label><select id="jarId">${options(jars, (j) => `${j.currency} jar · ${money(j.balance, j.currency)}`)}</select></div>
                <div><label>Current account</label><select id="jarAccount">${options(accounts, accountLabel)}</select></div>
                <div><label>Amount</label><input id="jarAmount" value="1000"></div></div>
             <div class="actions"><button onclick="jarMove('deposits')">Put in</button><button class="secondary" onclick="jarMove('withdrawals')">Take out</button></div>`),
        card(`🎁 Cashback for ${esc(cashback.period)}`, `<div class="stats">
                <div class="stat"><div class="k">Earned</div><div class="v">${money(cashback.earned, 'UAH')}</div></div>
                <div class="stat"><div class="k">Monthly cap</div><div class="v">${money(cashback.monthlyCap, 'UAH')}</div></div>
                <div class="stat"><div class="k">Left this month</div><div class="v">${money(cashback.remaining, 'UAH')}</div></div></div>
            <p class="hint" style="margin-top:10px">Rates: ${Object.entries(cashback.rates || {}).map(([k, v]) => `${k} ${v}%`).join(' · ')}</p>`,
            'Earned when a bill payment completes, paid on the 1st of next month.'));
};

const openJar = () => act(() => api('POST', '/savings', { currency: val('jarCurrency') }), 'Jar opened');
const jarMove = (kind) => act(async () => rememberPayment(await api('POST', `/savings/${val('jarId')}/${kind}`,
    { accountId: val('jarAccount'), amount: val('jarAmount') }, { idempotent: true })), kind === 'deposits' ? 'Put into the jar' : 'Taken out of the jar');

// ---------- 💼 Sole proprietor (ФОП) ----------

const TAX_GROUPS = {
    GROUP_1: 'Group 1: fixed tax monthly (retail at markets, household services)',
    GROUP_2: 'Group 2: fixed tax monthly (services and goods to people and single tax payers)',
    GROUP_3: 'Group 3: 5% of every income, or 3% for a VAT payer',
};

views.business = async () => {
    let business = null;
    try { business = await api('GET', '/business'); } catch { /* 404: not a sole proprietor yet */ }

    if (!business) {
        const name = state.me ? `ФОП ${state.me.lastName} ${state.me.firstName}` : 'ФОП';
        const taxId = String(Math.floor(1e9 + Math.random() * 9e9));
        return page('💼 Sole proprietor (ФОП)', 'Open a business account for your ФОП: it comes with a BUSINESS card and a tax jar, apart from your own money.',
            card('📝 Register your ФОП', `<div class="narrow">
                <label>Name as registered</label><input id="fopName" value="${esc(name)}">
                <label>РНОКПП (tax number, 10 digits)</label><input id="fopTaxId" value="${taxId}" maxlength="10">
                <label>КВЕД codes, the main one first</label><input id="fopKved" value="62.01, 63.11">
                <label>Single tax group</label><select id="fopGroup" onchange="document.getElementById('fopVat').disabled = this.value !== 'GROUP_3'; if (this.value !== 'GROUP_3') document.getElementById('fopVat').checked = false">
                    ${Object.entries(TAX_GROUPS).map(([k, v]) => `<option value="${k}" ${k === 'GROUP_3' ? 'selected' : ''}>${esc(v)}</option>`).join('')}</select>
                <label class="check"><input type="checkbox" id="fopVat"> VAT payer (group 3 only)</label>
                <label>Registered on</label><input id="fopDate" type="date" value="${new Date().toISOString().slice(0, 10)}">
                <div class="actions"><button onclick="openBusiness()">Open ФОП account</button></div></div>`,
                'Demo: the tax number is made up and only checked to be 10 digits.'));
    }

    const incomes = await api('GET', '/business/incomes');
    const periods = await api('GET', '/tax/periods');
    const cards = (await api('GET', '/cards')).filter((c) => c.accountId === business.businessAccountId);
    const accounts = await api('GET', '/accounts');
    const percent = Number(business.taxRatePercent) > 0;
    return page('💼 Sole proprietor (ФОП)', `${esc(business.name)} · РНОКПП ${esc(business.taxId)} · КВЕД ${business.kvedCodes.map(esc).join(', ')}`,
        card('📊 This quarter, ' + esc(business.quarter), `<div class="stats">
            <div class="stat"><div class="k">Business account</div><div class="v">${money(business.businessBalance, 'UAH')}</div></div>
            <div class="stat"><div class="k">🐷 Tax jar</div><div class="v">${money(business.taxReserve, 'UAH')}</div></div>
            <div class="stat"><div class="k">Income</div><div class="v">${money(business.quarterIncome, 'UAH')}</div></div>
            <div class="stat"><div class="k">${percent ? 'Tax put aside' : 'Fixed tax for the quarter'}</div><div class="v">${money(business.quarterTax, 'UAH')}</div></div></div>
            <p class="hint" style="margin-top:10px">${esc(TAX_GROUPS[business.taxGroup])}${business.vatPayer ? ' · VAT payer' : ''}.
            ${percent ? `<b>${Number(business.taxRatePercent)}% of every income goes into the tax jar the moment it arrives</b>, in the same ledger transaction, so it can never be spent by mistake. Your own money moved in from a personal account is not income.`
                : `Fixed tax: <b>${money(business.monthlyTax, 'UAH')}</b> a month, whatever the income.`}
            IBAN for customers: <span class="mono">${esc(business.businessAccountNumber)}</span></p>`),
        card('🧾 Single tax', `<label class="check"><input type="checkbox" ${business.autoPayTax ? 'checked' : ''} onchange="setTaxAutoPay(this.checked)">
                <span><b>Auto-pay</b>: on the due date the bank pays from the tax jar (or the business account if the jar is short) and emails the receipt</span></label>
            <div style="margin-top:12px">` + table(['Period', 'Due by', 'Tax', 'Paid', 'Left', 'Status', ''], periods.map((p) => `<tr>
                <td>${esc(p.label)}</td><td>${esc(p.dueDate)}</td><td class="num">${money(p.amount, 'UAH')}</td>
                <td class="num">${money(p.paid, 'UAH')}</td><td class="num"><b>${money(p.left, 'UAH')}</b></td><td>${badge(p.status)}</td>
                <td>${Number(p.left) > 0 ? `<button class="small" onclick="payTax('${p.period}')">Pay now</button>` : ''}</td></tr>`),
                'No tax periods yet') + '</div>',
            `${business.taxGroup === 'GROUP_3'
                ? 'Group 3 pays for a quarter within 50 days after it ends; a quarter still going on (ACCRUING) may be paid early for the tax so far.'
                : 'Groups 1 and 2 pay for each month in advance, by the 20th of that month, whatever the income.'}
             A reminder comes 5 days before the due date. The bank never pays part of the tax.`),
        `<div class="grid">`,
        card('💵 Receive a payment from a customer', `<label>Amount, UAH</label><input id="fopIncome" value="10000">
            <div class="actions"><button onclick="receiveIncome('${business.businessAccountId}')">Receive</button></div>`,
            'Money from a customer outside the bank. Or ask another client to pay your IBAN above.'),
        card('💳 Your BUSINESS card', cards.length ? `<div class="wallet">${cards.map((c) => cardVisual(c, accounts)).join('')}</div>` : '<div class="empty">No card</div>'),
        `</div>`,
        card('🧾 Recent income', table(['When', 'Income', 'Tax put aside'], incomes.map((i) => `<tr>
            <td>${when(i.createdAt)}</td><td class="num">${money(i.amount, 'UAH')}</td><td class="num">${money(i.tax, 'UAH')}</td></tr>`),
            'No income yet')));
};

const openBusiness = () => act(() => api('POST', '/business', {
    name: val('fopName'), taxId: val('fopTaxId'),
    kvedCodes: val('fopKved').split(',').map((k) => k.trim()).filter(Boolean),
    taxGroup: val('fopGroup'), vatPayer: document.getElementById('fopVat').checked, registeredOn: val('fopDate'),
}), 'ФОП account opened, with a BUSINESS card and a tax jar');

const setTaxAutoPay = (enabled) => act(() => api('PUT', '/business/tax-auto-pay', { enabled }),
    enabled ? 'Auto-pay on: the bank pays the tax on its due date' : 'Auto-pay off');

const payTax = (period) => act(async () => rememberPayment(await api('POST', `/tax/periods/${period}/payments`)),
    (p) => `Tax paid: ${money(p.amount, 'UAH')}. The receipt is under Transfers → Payments of this session`);

async function receiveIncome(accountId) {
    await pay('/payments/top-up', { accountId, amount: val('fopIncome') }, 'Income');
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
        ? card('📝 Get a credit limit', `<div class="empty-state"><div class="big">💳</div>
            <p>The limit lives on your <b>UNIVERSAL</b> card, next to your own money. Issue the card on your UAH account first.</p>
            <button onclick="go('cards')">Go to Cards</button></div>`)
        : card('📝 Get a credit limit on your UNIVERSAL card', `<div class="row"><div><label>Limit, UAH</label><input id="creditLimit" value="8000"></div>
            <div class="fit"><button onclick="applyCredit()">Apply</button></div></div>`,
            'Limit = 50% of your monthly income (top-ups and transfers from others, last 90 days). Up to 10 000 ₴ within it: approved at once; up to 1.5×: a person decides.');

    const lineCard = !line ? '' : card('💳 Credit limit on your UNIVERSAL card', `<div class="stats">
            <div class="stat"><div class="k">Own money</div><div class="v">${money(line.ownFunds, 'UAH')}</div></div>
            <div class="stat"><div class="k">Credit limit</div><div class="v">${money(line.creditLimit, 'UAH')}</div></div>
            <div class="stat"><div class="k">Available</div><div class="v">${money(line.available, 'UAH')}</div></div>
            <div class="stat"><div class="k">Debt</div><div class="v">${money(line.debt, 'UAH')}</div></div>
            <div class="stat"><div class="k">Last statement</div><div class="v">${money(line.statementDebt, 'UAH')}</div></div>
            <div class="stat"><div class="k">To pay no interest</div><div class="v">${money(line.leftToRepayInterestFree, 'UAH')}</div></div>
            <div class="stat"><div class="k">Minimum payment left</div><div class="v">${money(line.leftForMinimumPayment, 'UAH')}</div></div></div>
        <p class="hint" style="margin-top:10px">Your own money is spent first, then the limit. Card payments and bills may use the limit;
           transfers by IBAN, jars and exchange use only your own money. <b>Any money that arrives on the account repays the debt</b>,
           so there is no separate repay button: just top up (Overview → 💵 Top up).<br>
           Last statement interest-free until <b>${esc(line.statementInterestFreeUntil)}</b>; spending this month until <b>${esc(line.newSpendingInterestFreeUntil)}</b>.</p>
        <h2 style="margin-top:16px">📅 Statements</h2>` + table(['Month', 'Debt', 'Minimum', 'Due', 'Status', 'Charged'], statements.map((s) => `<tr>
            <td>${esc(s.period)}</td><td class="num">${money(s.closingDebt)}</td><td class="num">${money(s.minimumPayment)}</td>
            <td>${esc(s.dueDate)}</td><td>${badge(s.status)}</td><td class="num">${s.interest == null ? '—' : money(Number(s.interest) + Number(s.lateFee))}</td></tr>`),
        'The first statement closes on the 1st of next month'));

    return page('💳 Credit limit', 'Like monobank: one UNIVERSAL card holds your own money and the bank\'s credit limit. Rules decide the clear cases at once; the grey zone waits for a bank employee.',
        lineCard, applyCard,
        card('🗂️ My applications', table(['When', 'Asked', 'Affordable', 'Approved', 'Decision', 'Why'], applications.map((x) => `<tr>
            <td>${when(x.createdAt)}</td><td class="num">${money(x.requestedLimit)}</td><td class="num">${money(x.affordableLimit)}</td>
            <td class="num">${money(x.approvedLimit)}</td><td>${badge(x.status)}<br><small>${esc(x.decisionSource || '')}</small></td>
            <td><small>${(x.reasons || []).map(esc).join('<br>')}${x.decisionComment ? `<br>💬 ${esc(x.decisionComment)}` : ''}</small></td></tr>`), 'No applications yet')));
};

const applyCredit = () => act(() => api('POST', '/credit-applications', { requestedLimit: val('creditLimit') }),
    (x) => `Application: ${x.status}${x.status === 'MANUAL_REVIEW' ? ' (log in as the demo admin to decide)' : ''}`);

// ---------- 🔔 Notifications ----------

views.notifications = async () => {
    const { content } = await api('GET', '/notifications?size=50');
    return page('🔔 Notifications', 'Made from the bank\'s events (outbox → RabbitMQ → consumer). Important ones also go by email.',
        card('Inbox', table(['When', 'What', ''], content.map((n) => `<tr style="${n.readAt ? 'opacity:.6' : ''}">
            <td>${when(n.createdAt)}</td><td><b>${esc(n.title)}</b><br>${esc(n.body)}<br><small class="mono">${esc(n.eventType)}</small></td>
            <td>${n.readAt ? '✓' : `<button class="small secondary" onclick="readNotification('${n.id}')">Read</button>`}</td></tr>`),
            'Nothing yet: make a payment and wait a second') +
        `<div class="actions"><button class="secondary" onclick="readAll()">Mark all as read</button><button class="secondary" onclick="render()">Refresh</button></div>`));
};

const readNotification = (id) => act(() => api('POST', `/notifications/${id}/read`));
const readAll = () => act(() => api('POST', '/notifications/read-all'), 'All read');

// ---------- 🎧 Support ----------

let openTicketId = null;

views.support = async () => {
    const tickets = await api('GET', '/support/tickets');
    const opened = openTicketId ? await api('GET', `/support/tickets/${openTicketId}`) : null;
    return page('🎧 Support', 'Ask the bank; an employee takes your ticket and answers here (and by email).',
        `<div class="grid">`,
        card('✉️ New ticket', `<label>Subject</label><input id="tSubject" value="Transfer did not arrive">
            <label>Message</label><textarea id="tMessage">I sent 500 UAH yesterday and the recipient does not see it.</textarea>
            <div class="actions"><button onclick="openTicket()">Send</button></div>`),
        card('🗂️ My tickets', table(['Subject', 'Status', ''], tickets.map((t) => `<tr>
            <td>${esc(t.subject)}<br><small>${when(t.createdAt)}</small></td><td>${badge(t.status)}</td>
            <td><button class="small secondary" onclick="showTicket('${t.id}')">Open</button></td></tr>`), 'No tickets yet')),
        `</div>`,
        opened ? card(`💬 ${esc(opened.ticket.subject)} ${badge(opened.ticket.status)}`, conversation(opened.messages) +
            (opened.ticket.status !== 'RESOLVED' ? `<label>Reply</label><textarea id="tReply"></textarea>
            <div class="actions"><button onclick="replyTicket('${opened.ticket.id}')">Send</button></div>` : '')) : '');
};

const conversation = (messages) => messages.map((m) => `<div style="margin:6px 0;padding:8px 10px;border-radius:10px;max-width:80%;
    ${m.fromStaff ? 'background:var(--brand-soft);margin-left:auto' : 'background:var(--bg)'}">
    <small>${m.fromStaff ? '🏦 Support' : '👤 Client'} · ${when(m.createdAt)}</small><br>${esc(m.body)}</div>`).join('');

const openTicket = () => act(async () => { const t = await api('POST', '/support/tickets', { subject: val('tSubject'), message: val('tMessage') }); openTicketId = t.ticket.id; return t; }, 'Ticket sent');
const showTicket = (id) => { openTicketId = id; return render(); };
const replyTicket = (id) => act(() => api('POST', `/support/tickets/${id}/messages`, { message: val('tReply') }), 'Sent');

// ---------- 👀 Staff: credit queue ----------

views.staffCredit = async () => {
    const { content } = await api('GET', '/admin/credit-applications?size=50');
    return page('👀 Credit queue', 'Applications the rules left for a person. Nobody decides on their own application (four-eyes).',
        card('Waiting for a decision', table(['When', 'Client', 'Asked', 'Affordable', 'Why it is here', 'Decision'], content.map((a) => `<tr>
            <td>${when(a.createdAt)}</td><td class="mono">${short(a.userId)}</td><td class="num">${money(a.requestedLimit)}</td>
            <td class="num">${money(a.affordableLimit)}</td><td><small>${(a.reasons || []).map(esc).join('<br>')}</small></td>
            <td><input id="lim-${a.id}" value="${a.affordableLimit}" style="width:110px">
                <input id="cmt-${a.id}" placeholder="comment (required)" style="width:170px;margin-top:4px">
                <div class="actions" style="margin-top:6px"><button class="small" onclick="decide('${a.id}','approve')">Approve</button>
                <button class="small danger" onclick="decide('${a.id}','decline')">Decline</button></div></td></tr>`), 'The queue is empty')));
};

const decide = (id, decision) => act(() => api('POST', `/admin/credit-applications/${id}/${decision}`,
    { approvedLimit: decision === 'approve' ? val(`lim-${id}`) : null, comment: val(`cmt-${id}`) }), `Application: ${decision}d`);

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
            table(['Currency', 'Type', 'Balance'], view.accounts.map((a) => `<tr><td>${a.currency}</td><td>${badge(a.type)}</td><td class="num">${money(a.balance, a.currency)}</td></tr>`)),
            'Visible only because this ticket is yours, read-only, and this view is written to the audit log.');
    }
    return page('🎧 Support queue', 'Take a ticket: from then on only you answer it, resolve it, and see the client\'s accounts.',
        card('Tickets', `<div class="actions" style="margin:0 0 10px">${['OPEN', 'IN_PROGRESS', 'RESOLVED'].map((s) =>
            `<button class="small ${s === staffStatus ? '' : 'secondary'}" onclick="staffFilter('${s}')">${s}</button>`).join('')}</div>` +
            table(['When', 'Subject', 'Status', ''], content.map((t) => `<tr><td>${when(t.createdAt)}</td><td>${esc(t.subject)}</td><td>${badge(t.status)}</td>
            <td>${t.status === 'OPEN' ? `<button class="small" onclick="takeTicket('${t.id}')">Take</button>` : ''}
                <button class="small secondary" onclick="staffOpen('${t.id}')">Open</button></td></tr>`), 'Nothing here')),
        opened ? card(`💬 ${esc(opened.ticket.subject)} ${badge(opened.ticket.status)}`, conversation(opened.messages) +
            (opened.ticket.status === 'IN_PROGRESS' && opened.ticket.assigneeId === state.me.id ? `<label>Answer</label><textarea id="sReply">Hello! Thanks for waiting: the transfer is on its way and will arrive today.</textarea>
            <div class="actions"><button onclick="staffAnswer('${opened.ticket.id}')">Answer</button>
            <button class="secondary" onclick="staffResolve('${opened.ticket.id}')">Resolve</button></div>` : '')) : '',
        client);
};

const staffFilter = (s) => { staffStatus = s; return render(); };
const staffOpen = (id) => { staffTicketId = id; return render(); };
const takeTicket = (id) => act(async () => { await api('POST', `/admin/support/tickets/${id}/take`); staffTicketId = id; staffStatus = 'IN_PROGRESS'; }, 'The ticket is yours');
const staffAnswer = (id) => act(() => api('POST', `/admin/support/tickets/${id}/messages`, { message: val('sReply') }), 'Answered: the client gets a notification and an email');
const staffResolve = (id) => act(() => api('POST', `/admin/support/tickets/${id}/resolve`), 'Resolved');

// ---------- 💱 Staff: bank rates ----------

views.staffRates = async () => {
    const rates = await api('GET', '/exchange-rates');
    return page('💱 Bank rates', 'ADMIN only. The bank\'s buy/sell rates: a spread around the NBU rate, or set by hand (within ±10% of it).',
        card('Current', table(['Currency', 'NBU', 'Buy', 'Sell', 'Mode', 'Spread'], rates.map((r) => `<tr><td><b>${r.currency}</b></td>
            <td class="num">${r.officialRate ?? '—'}</td><td class="num">${r.buy ?? '—'}</td><td class="num">${r.sell ?? '—'}</td>
            <td>${badge(r.mode || '—')}</td><td class="num">${r.spreadPercent ?? '—'}</td></tr>`)) +
            `<div class="actions"><button class="secondary" onclick="syncRates()">Sync NBU rates now</button></div>`),
        card('Set a rate', `<div class="row">
            <div><label>Currency</label><select id="rCur"><option>USD</option><option>EUR</option></select></div>
            <div><label>Mode</label><select id="rMode"><option>SPREAD</option><option>MANUAL</option></select></div>
            <div><label>Spread % (SPREAD)</label><input id="rSpread" value="2.00"></div>
            <div><label>Buy (MANUAL)</label><input id="rBuy"></div><div><label>Sell (MANUAL)</label><input id="rSell"></div></div>
            <label>Comment</label><input id="rComment" value="Weekend spread">
            <div class="actions"><button onclick="setRate()">Save</button></div>`, 'Every change keeps its author and is written to the audit log.'));
};

const syncRates = () => act(() => api('POST', '/admin/exchange-rates/sync'), 'NBU rates synced');
function setRate() {
    const manual = val('rMode') === 'MANUAL';
    return act(() => api('PUT', `/admin/exchange-rates/${val('rCur')}`, {
        mode: val('rMode'), spreadPercent: manual ? null : val('rSpread'),
        buy: manual ? val('rBuy') : null, sell: manual ? val('rSell') : null, comment: val('rComment'),
    }), 'Rate saved');
}

// ---------- 🕵️ Staff: audit ----------

let auditAction = '';

views.staffAudit = async () => {
    const { content } = await api('GET', `/admin/audit?size=50${auditAction ? `&action=${auditAction}` : ''}`);
    const actions = ['', 'CREDIT_APPLICATION_APPROVED', 'CREDIT_APPLICATION_DECLINED', 'BANK_RATE_SET', 'TICKET_TAKEN', 'TICKET_ANSWERED', 'TICKET_RESOLVED', 'CLIENT_ACCOUNTS_VIEWED'];
    return page('🕵️ Audit log', 'ADMIN only. Every staff action, in the same transaction as the action itself. It cannot be edited, even with SQL.',
        card('Entries', `<div class="row" style="margin-bottom:10px"><div><label>Action</label>
            <select onchange="auditAction=this.value;render()">${actions.map((a) => `<option ${a === auditAction ? 'selected' : ''} value="${a}">${a || 'all actions'}</option>`).join('')}</select></div></div>` +
            table(['When', 'Who', 'Action', 'On', 'Details'], content.map((e) => `<tr><td>${when(e.occurredAt)}</td>
            <td class="mono">${short(e.actorId)}<br>${badge(e.actorRole)}</td><td>${badge(e.action)}</td>
            <td><small>${esc(e.targetType)}</small><br><span class="mono">${short(e.targetId)}</span></td>
            <td class="mono"><small>${esc(JSON.stringify(e.details))}</small></td></tr>`), 'Nothing audited yet')));
};

// ---------- Start ----------

document.getElementById('regEmail').value = `client-${Math.random().toString(36).slice(2, 7)}@bank.local`;
fillGeneratedPassword('regPassword');
showRememberedEmail();
showAuth(rememberedEmail() ? 'login' : 'register');
if (state.access) start().catch(() => logout(true));
