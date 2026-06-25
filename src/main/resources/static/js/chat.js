'use strict';

/* ── STATE ── */
let stompClient = null;
let username = null;
let userLanguage = null;
let currentRoomCode = null;

/* ── DOM REFS ── */
const joinScreen = document.getElementById('join-screen');
const chatScreen = document.getElementById('chat-screen');
const joinForm = document.getElementById('join-form');
const messageForm = document.getElementById('message-form');
const messageInput = document.getElementById('message-input');
const messageList = document.getElementById('message-list');
const messageArea = document.getElementById('message-area');
const headerName = document.getElementById('header-name');
const userAvatar = document.getElementById('user-avatar');
const roomBadge = document.getElementById('room-code-badge');
const roomBadgeCode = document.getElementById('room-badge-code');

/* NEW-ROOM FORM  (tab: "New Room")
   POST /api/rooms → get code → connect */

joinForm.addEventListener('submit', async(e) => {
    e.preventDefault();
    username = document.getElementById('username').value.trim();
    userLanguage = document.getElementById('language').value;
    if (!username) return;

    const btn = joinForm.querySelector('button[type="submit"]');
    btn.disabled = true;
    btn.textContent = 'Creating…';

    try {
        const res = await fetch('/api/rooms', { method: 'POST' });
        const data = await res.json();
        currentRoomCode = data.code;
        connect();
    } catch (err) {
        console.error('Failed to create room:', err);
        alert('Could not create a room. Please refresh and try again.');
        btn.disabled = false;
        btn.innerHTML = 'Create private room <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>';
    }
});

/* JOIN-ROOM FORM  (tab: "Join Room")
   GET /api/rooms/{code} → validate → connect*/

const joinRoomForm = document.getElementById('join-room-form');
if (joinRoomForm) {
    joinRoomForm.addEventListener('submit', async(e) => {
        e.preventDefault();
        const nameVal = document.getElementById('username-join').value.trim();
        const langVal = document.getElementById('language-join').value;
        const codeVal = document.getElementById('room-code-join').value.trim().toUpperCase();

        if (!nameVal || !codeVal) return;

        const btn = joinRoomForm.querySelector('button[type="submit"]');
        btn.disabled = true;
        btn.textContent = 'Joining…';

        try {
            const res = await fetch(`/api/rooms/${codeVal}`);
            if (!res.ok) {
                showCodeError('Room not found. Check the code and try again.');
                btn.disabled = false;
                btn.innerHTML = 'Join room <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path><polyline points="10 17 15 12 10 7"></polyline><line x1="15" y1="12" x2="3" y2="12"></line></svg>';
                return;
            }
            username = nameVal;
            userLanguage = langVal;
            currentRoomCode = codeVal;
            connect();
        } catch (err) {
            console.error('Failed to join room:', err);
            alert('Could not reach server. Please refresh and try again.');
            btn.disabled = false;
        }
    });
}

/* WEBSOCKET / STOMP*/
function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, onConnected, onError);
}

function onConnected() {
    stompClient.subscribe(`/topic/room/${currentRoomCode}`, onMessageReceived);

    stompClient.send('/app/room.join', {}, JSON.stringify({
        sender: username,
        senderLanguage: userLanguage,
        roomCode: currentRoomCode,
        type: 'JOIN'
    }));

    roomBadgeCode.textContent = currentRoomCode;
    roomBadge.classList.remove('hidden');

    joinScreen.classList.add('hidden');
    chatScreen.classList.remove('hidden');

    headerName.textContent = username;
    userAvatar.textContent = username.charAt(0).toUpperCase();
    messageInput.focus();
}

function onError(error) {
    console.error('WebSocket error:', error);
    alert('Could not connect to chat server. Please refresh and try again.');
}

/* SEND MESSAGE*/
messageForm.addEventListener('submit', (e) => {
    e.preventDefault();
    sendMessage();
});

function sendMessage() {
    const content = messageInput.value.trim();
    if (!content || !stompClient) return;

    stompClient.send('/app/room.send', {}, JSON.stringify({
        sender: username,
        senderLanguage: userLanguage,
        roomCode: currentRoomCode,
        content: content,
        type: 'CHAT'
    }));

    messageInput.value = '';
}

/* RECEIVE & DISPLAY MESSAGES*/
function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);
    displayMessage(message);
}

function displayMessage(message) {
    const li = document.createElement('li');

    if (message.type === 'JOIN' || message.type === 'LEAVE') {
        li.classList.add('message-item', 'system');
        li.innerHTML = `<div class="message-bubble">${escapeHtml(message.content)}</div>`;
    } else {
        const isSent = message.sender === username;
        li.classList.add('message-item', isSent ? 'sent' : 'received');

        let bubbleHtml = '';

        if (!isSent) {
            bubbleHtml += `<div class="sender-name">${escapeHtml(message.sender)}</div>`;
        }

        bubbleHtml += '<div class="message-bubble">';

        if (!isSent && message.translations && message.translations[userLanguage]) {
            bubbleHtml += `<div class="translated-text">${escapeHtml(message.translations[userLanguage])}</div>`;
        } else {
            const defaultText = message.content || message.originalText || '';
            bubbleHtml += `<div class="translated-text">${escapeHtml(defaultText)}</div>`;
        }

        bubbleHtml += '</div>';
        li.innerHTML = bubbleHtml;
    }

    messageList.appendChild(li);
    messageArea.scrollTop = messageArea.scrollHeight;
}

/* ROOM CODE BADGE — copy to clipboard */

function copyRoomCode() {
    if (!currentRoomCode) return;
    navigator.clipboard.writeText(currentRoomCode).then(() => {
        roomBadge.classList.add('copied');
        setTimeout(() => roomBadge.classList.remove('copied'), 1800);
    }).catch(() => {
        // Fallback for browsers without Clipboard API
        const ta = document.createElement('textarea');
        ta.value = currentRoomCode;
        ta.style.cssText = 'position:fixed;opacity:0';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        roomBadge.classList.add('copied');
        setTimeout(() => roomBadge.classList.remove('copied'), 1800);
    });
}

/* HELPERS */
function showCodeError(msg) {
    const errEl = document.getElementById('room-code-error');
    if (errEl) {
        errEl.textContent = msg;
        errEl.style.display = 'block';
        setTimeout(() => { errEl.style.display = 'none'; }, 3500);
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}