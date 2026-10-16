/*
 * Helpdesk frontend: a dependency-free single page app served by Spring Boot.
 * Hash routing, fetch-based API client, JWT kept in sessionStorage (cleared when the tab closes).
 * All server text is escaped before it is placed in HTML.
 */
(() => {
    'use strict';

    const app = document.getElementById('app');
    const PAGE_SIZE = 10;

    const DEMO_ACCOUNTS = [
        { org: 'acme', email: 'admin@acme.com', label: 'Acme admin' },
        { org: 'acme', email: 'agent1@acme.com', label: 'Acme agent' },
        { org: 'acme', email: 'customer1@acme.com', label: 'Acme customer' },
        { org: 'acme', email: 'customer2@acme.com', label: 'Acme customer 2' },
        { org: 'globex', email: 'admin@globex.com', label: 'Globex admin' },
        { org: 'globex', email: 'customer@globex.com', label: 'Globex customer' },
    ];

    const STATUS_LABEL = { OPEN: 'Open', IN_PROGRESS: 'In progress', RESOLVED: 'Resolved', CLOSED: 'Closed' };
    const PRIORITY_LABEL = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', URGENT: 'Urgent' };
    const ROLE_LABEL = { ORG_ADMIN: 'Admin', AGENT: 'Agent', CUSTOMER: 'Customer' };

    const listState = { q: '', status: '', priority: '', assignee: '', sort: 'createdAt,desc', page: 0 };

    // ---------------------------------------------------------------- session

    const session = {
        read() {
            try { return JSON.parse(sessionStorage.getItem('helpdesk.session')) || null; } catch { return null; }
        },
        write(value) {
            try { sessionStorage.setItem('helpdesk.session', JSON.stringify(value)); } catch { /* storage unavailable */ }
        },
        clear() {
            try { sessionStorage.removeItem('helpdesk.session'); } catch { /* storage unavailable */ }
        },
    };

    let current = session.read();

    const isStaff = () => current && current.user.role !== 'CUSTOMER';
    const isAdmin = () => current && current.user.role === 'ORG_ADMIN';

    function signIn(auth) {
        current = { token: auth.accessToken, user: auth.user };
        session.write(current);
    }

    function signOut(message) {
        current = null;
        session.clear();
        location.hash = '#/login';
        if (message) toast(message, true);
    }

    // ---------------------------------------------------------------- api

    class ApiError extends Error {}

    async function api(path, { method = 'GET', body } = {}) {
        const headers = { Accept: 'application/json' };
        if (body !== undefined) headers['Content-Type'] = 'application/json';
        if (current) headers.Authorization = 'Bearer ' + current.token;

        const response = await fetch(path, {
            method,
            headers,
            body: body !== undefined ? JSON.stringify(body) : undefined,
        });

        if (response.status === 401 && current) {
            signOut('Your session has ended. Log in again.');
            throw new ApiError('Unauthorized');
        }

        const data = response.status === 204 ? null : await response.json().catch(() => null);
        if (!response.ok) throw new ApiError(problemMessage(data, response.status));
        return data;
    }

    function problemMessage(problem, status) {
        if (!problem) return 'Request failed with status ' + status + '.';
        if (Array.isArray(problem.errors) && problem.errors.length) {
            return problem.errors.map((e) => e.field + ': ' + e.message).join('. ');
        }
        return problem.detail || problem.title || 'Request failed with status ' + status + '.';
    }

    // ---------------------------------------------------------------- helpers

    function esc(value) {
        return String(value ?? '').replace(/[&<>"']/g, (c) => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
        })[c]);
    }

    const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });

    function relative(iso) {
        if (!iso) return '';
        const seconds = (new Date(iso).getTime() - Date.now()) / 1000;
        const units = [['year', 31536000], ['month', 2592000], ['day', 86400], ['hour', 3600], ['minute', 60]];
        for (const [unit, size] of units) {
            if (Math.abs(seconds) >= size) return rtf.format(Math.round(seconds / size), unit);
        }
        return 'just now';
    }

    function absolute(iso) {
        return iso ? new Date(iso).toLocaleString(undefined, { day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit' }) : '';
    }

    const statusPill = (s) => `<span class="status" data-s="${esc(s)}">${esc(STATUS_LABEL[s] || s)}</span>`;
    const priorityText = (p) => `<span class="priority" data-p="${esc(p)}">${esc(PRIORITY_LABEL[p] || p)}</span>`;
    const options = (map, selected) => Object.entries(map)
        .map(([value, label]) => `<option value="${value}" ${value === selected ? 'selected' : ''}>${esc(label)}</option>`)
        .join('');

    let toastTimer;
    function toast(message, isError = false) {
        const el = document.getElementById('toast');
        el.textContent = message;
        el.classList.toggle('error', isError);
        el.hidden = false;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => { el.hidden = true; }, 4000);
    }

    function formData(form) {
        return Object.fromEntries(new FormData(form).entries());
    }

    /** Wraps a submit handler: disables the submit button while the request runs and reports errors. */
    function onSubmit(form, handler) {
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const button = form.querySelector('button[type="submit"]');
            if (button) button.disabled = true;
            try {
                await handler(formData(form), form);
            } catch (error) {
                if (error.message !== 'Unauthorized') toast(error.message, true);
            } finally {
                if (button) button.disabled = false;
            }
        });
    }

    let staffCache = null;
    async function staffMembers() {
        if (!staffCache) {
            const page = await api('/api/v1/users?size=100&sort=fullName');
            staffCache = page.content.filter((u) => u.role !== 'CUSTOMER' && u.active);
        }
        return staffCache;
    }

    // ---------------------------------------------------------------- chrome

    function renderChrome(section) {
        const bar = document.getElementById('topbar');
        bar.hidden = !current;
        if (!current) return;
        document.getElementById('org-name').textContent = current.user.organizationName;
        document.getElementById('user-name').textContent = current.user.fullName;
        document.getElementById('user-role').textContent = ROLE_LABEL[current.user.role];
        document.getElementById('nav-team').hidden = !isAdmin();
        document.querySelectorAll('[data-nav]').forEach((a) => {
            if (a.dataset.nav === section) a.setAttribute('aria-current', 'page');
            else a.removeAttribute('aria-current');
        });
    }

    document.getElementById('logout').addEventListener('click', () => {
        staffCache = null;
        signOut();
    });

    // ---------------------------------------------------------------- views: auth

    function viewLogin() {
        renderChrome();
        app.innerHTML = `
            <section class="auth">
                <div class="auth-intro">
                    <h1>Support tickets for every team, kept apart.</h1>
                    <p>Each organization signs in to its own workspace. Agents see their organization's queue,
                       customers see only the tickets they raised, and internal notes never reach customers.</p>
                    <div class="demo">
                        <h2>Try a demo account</h2>
                        <p class="hint">All demo accounts use the password <strong>Password@123</strong>.</p>
                        <div class="demo-list">
                            ${DEMO_ACCOUNTS.map((d, i) => `
                                <button type="button" data-demo="${i}">${esc(d.label)} <span>${esc(d.org)}</span></button>`).join('')}
                        </div>
                    </div>
                </div>
                <form class="panel panel-body" id="login-form" novalidate>
                    <h2 style="margin-bottom:16px">Log in</h2>
                    <div class="field">
                        <label for="org">Organization</label>
                        <input id="org" name="organizationSlug" autocomplete="organization" required placeholder="acme">
                    </div>
                    <div class="field">
                        <label for="email">Email</label>
                        <input id="email" name="email" type="email" autocomplete="username" required>
                    </div>
                    <div class="field">
                        <label for="password">Password</label>
                        <input id="password" name="password" type="password" autocomplete="current-password" required>
                    </div>
                    <button class="primary" type="submit" style="width:100%;justify-content:center">Log in</button>
                    <p class="hint" style="text-align:center">New organization? <a href="#/signup">Create a workspace</a></p>
                </form>
            </section>`;

        const form = document.getElementById('login-form');
        app.querySelectorAll('[data-demo]').forEach((button) => button.addEventListener('click', () => {
            const demo = DEMO_ACCOUNTS[Number(button.dataset.demo)];
            form.organizationSlug.value = demo.org;
            form.email.value = demo.email;
            form.password.value = 'Password@123';
            form.password.focus();
        }));

        onSubmit(form, async (data) => {
            signIn(await api('/api/v1/auth/login', { method: 'POST', body: data }));
            location.hash = '#/tickets';
        });
    }

    function viewSignup() {
        renderChrome();
        app.innerHTML = `
            <section class="auth">
                <div class="auth-intro">
                    <h1>Create a workspace for your organization.</h1>
                    <p>You become the organization admin. From the Team page you can add agents and customers,
                       and none of your data is visible to other organizations.</p>
                </div>
                <form class="panel panel-body" id="signup-form" novalidate>
                    <div class="field"><label for="on">Organization name</label><input id="on" name="organizationName" required></div>
                    <div class="field">
                        <label for="os">Organization ID</label>
                        <input id="os" name="organizationSlug" required pattern="[a-z0-9]+(-[a-z0-9]+)*">
                        <p class="hint">Used when logging in. Lowercase letters, digits and hyphens.</p>
                    </div>
                    <div class="field"><label for="an">Your name</label><input id="an" name="adminFullName" required></div>
                    <div class="field"><label for="ae">Your email</label><input id="ae" name="adminEmail" type="email" required></div>
                    <div class="field"><label for="ap">Password</label><input id="ap" name="adminPassword" type="password" minlength="8" required></div>
                    <button class="primary" type="submit" style="width:100%;justify-content:center">Create workspace</button>
                    <p class="hint" style="text-align:center"><a href="#/login">Back to log in</a></p>
                </form>
            </section>`;

        const form = document.getElementById('signup-form');
        form.organizationName.addEventListener('input', () => {
            form.organizationSlug.value = form.organizationName.value.toLowerCase().trim()
                .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 60);
        });
        onSubmit(form, async (data) => {
            signIn(await api('/api/v1/organizations', { method: 'POST', body: data }));
            toast('Workspace created.');
            location.hash = '#/tickets';
        });
    }

    // ---------------------------------------------------------------- views: ticket list

    async function viewTickets() {
        renderChrome('tickets');
        const staff = isStaff() ? await staffMembers() : [];
        const assigneeOptions = isStaff() ? `
            <select name="assignee" aria-label="Assignee">
                <option value="">Any assignee</option>
                <option value="unassigned" ${listState.assignee === 'unassigned' ? 'selected' : ''}>Unassigned</option>
                <option value="${current.user.id}" ${listState.assignee === String(current.user.id) ? 'selected' : ''}>Assigned to me</option>
                ${staff.filter((u) => u.id !== current.user.id).map((u) => `
                    <option value="${u.id}" ${listState.assignee === String(u.id) ? 'selected' : ''}>${esc(u.fullName)}</option>`).join('')}
            </select>` : '<span></span>';

        app.innerHTML = `
            <div class="page-head">
                <div>
                    <h1>${isStaff() ? 'Ticket queue' : 'My tickets'}</h1>
                    <p>${isStaff() ? 'Every ticket in ' + esc(current.user.organizationName) + '.' : 'Tickets you have raised with support.'}</p>
                </div>
                <a class="button primary" href="#/tickets/new">New ticket</a>
            </div>
            <section class="panel">
                <form class="filters" id="filters">
                    <input class="search" name="q" type="search" placeholder="Search subject or description" value="${esc(listState.q)}" aria-label="Search">
                    <select name="status" aria-label="Status"><option value="">Any status</option>${options(STATUS_LABEL, listState.status)}</select>
                    <select name="priority" aria-label="Priority"><option value="">Any priority</option>${options(PRIORITY_LABEL, listState.priority)}</select>
                    ${assigneeOptions}
                    <select name="sort" aria-label="Sort">
                        ${options({ 'createdAt,desc': 'Newest first', 'createdAt,asc': 'Oldest first', 'updatedAt,desc': 'Recently updated' }, listState.sort)}
                    </select>
                </form>
                <div id="results"><div class="empty">Loading tickets</div></div>
            </section>`;

        const form = document.getElementById('filters');
        let debounce;
        const apply = () => {
            Object.assign(listState, formData(form), { page: 0 });
            loadTicketPage();
        };
        form.addEventListener('change', apply);
        form.q.addEventListener('input', () => { clearTimeout(debounce); debounce = setTimeout(apply, 300); });
        form.addEventListener('submit', (e) => { e.preventDefault(); apply(); });
        await loadTicketPage();
    }

    async function loadTicketPage() {
        const params = new URLSearchParams({ page: listState.page, size: PAGE_SIZE, sort: listState.sort });
        if (listState.q.trim()) params.set('q', listState.q.trim());
        if (listState.status) params.set('status', listState.status);
        if (listState.priority) params.set('priority', listState.priority);
        if (listState.assignee === 'unassigned') params.set('unassigned', 'true');
        else if (listState.assignee) params.set('assigneeId', listState.assignee);

        const results = document.getElementById('results');
        try {
            const page = await api('/api/v1/tickets?' + params);
            if (!page.content.length) {
                results.innerHTML = `<div class="empty">No tickets match these filters. Clear a filter or <a href="#/tickets/new">raise a new ticket</a>.</div>`;
                return;
            }
            results.innerHTML = `
                <div class="table-wrap">
                    <table>
                        <thead><tr>
                            <th scope="col">#</th><th scope="col">Subject</th><th scope="col">Status</th>
                            <th scope="col">Priority</th>${isStaff() ? '<th scope="col">Requester</th>' : ''}
                            <th scope="col">Assignee</th><th scope="col">Updated</th>
                        </tr></thead>
                        <tbody>
                            ${page.content.map((t) => `
                                <tr data-priority="${esc(t.priority)}">
                                    <td class="id">${t.id}</td>
                                    <td class="subject"><a href="#/tickets/${t.id}">${esc(t.subject)}</a></td>
                                    <td>${statusPill(t.status)}</td>
                                    <td>${priorityText(t.priority)}</td>
                                    ${isStaff() ? `<td>${esc(t.requester.fullName)}</td>` : ''}
                                    <td>${t.assignee ? esc(t.assignee.fullName) : '<span class="muted">Unassigned</span>'}</td>
                                    <td class="muted" title="${esc(absolute(t.updatedAt))}">${esc(relative(t.updatedAt))}</td>
                                </tr>`).join('')}
                        </tbody>
                    </table>
                </div>
                <div class="pager">
                    <span>Page ${page.page + 1} of ${page.totalPages}, ${page.totalElements} ticket${page.totalElements === 1 ? '' : 's'}</span>
                    <div>
                        <button type="button" data-page="-1" ${page.first ? 'disabled' : ''}>Previous</button>
                        <button type="button" data-page="1" ${page.last ? 'disabled' : ''}>Next</button>
                    </div>
                </div>`;
            results.querySelectorAll('[data-page]').forEach((b) => b.addEventListener('click', () => {
                listState.page += Number(b.dataset.page);
                loadTicketPage();
            }));
        } catch (error) {
            if (error.message !== 'Unauthorized') results.innerHTML = `<div class="empty">${esc(error.message)}</div>`;
        }
    }

    // ---------------------------------------------------------------- views: new ticket

    function viewNewTicket() {
        renderChrome('tickets');
        app.innerHTML = `
            <div class="page-head"><div><h1>New ticket</h1><p>Describe the problem. An agent will pick it up from the queue.</p></div></div>
            <form class="panel panel-body" id="new-ticket" style="max-width:720px" novalidate>
                <div class="field"><label for="s">Subject</label><input id="s" name="subject" maxlength="200" required></div>
                <div class="field"><label for="d">Description</label><textarea id="d" name="description" rows="7" required></textarea></div>
                <div class="field" style="max-width:220px"><label for="p">Priority</label><select id="p" name="priority">${options(PRIORITY_LABEL, 'MEDIUM')}</select></div>
                <div style="display:flex;gap:8px">
                    <button class="primary" type="submit">Create ticket</button>
                    <a class="button" href="#/tickets">Cancel</a>
                </div>
            </form>`;
        onSubmit(document.getElementById('new-ticket'), async (data) => {
            const ticket = await api('/api/v1/tickets', { method: 'POST', body: data });
            toast('Ticket #' + ticket.id + ' created.');
            location.hash = '#/tickets/' + ticket.id;
        });
    }

    // ---------------------------------------------------------------- views: ticket detail

    function transitionLabel(target, from) {
        if (target === 'OPEN') return from === 'CLOSED' || from === 'RESOLVED' ? 'Reopen' : 'Move back to open';
        return { IN_PROGRESS: 'Start progress', RESOLVED: 'Mark resolved', CLOSED: 'Close ticket' }[target];
    }

    function historyLine(h) {
        const who = esc(h.changedBy.fullName);
        switch (h.action) {
            case 'CREATED': return `${who} opened the ticket`;
            case 'STATUS_CHANGED': return `${who} changed status from ${esc(STATUS_LABEL[h.fromValue])} to ${esc(STATUS_LABEL[h.toValue])}`;
            case 'PRIORITY_CHANGED': return `${who} changed priority from ${esc(PRIORITY_LABEL[h.fromValue])} to ${esc(PRIORITY_LABEL[h.toValue])}`;
            case 'ASSIGNEE_CHANGED':
                if (!h.toValue) return `${who} unassigned ${esc(h.fromValue)}`;
                if (!h.fromValue) return `${who} assigned it to ${esc(h.toValue)}`;
                return `${who} reassigned it from ${esc(h.fromValue)} to ${esc(h.toValue)}`;
            default: return who;
        }
    }

    async function viewTicket(id) {
        renderChrome('tickets');
        app.innerHTML = '<div class="empty">Loading ticket</div>';
        let ticket, comments, history, staff;
        try {
            [ticket, comments, history, staff] = await Promise.all([
                api(`/api/v1/tickets/${id}`),
                api(`/api/v1/tickets/${id}/comments`),
                api(`/api/v1/tickets/${id}/history`),
                isStaff() ? staffMembers() : Promise.resolve([]),
            ]);
        } catch (error) {
            if (error.message !== 'Unauthorized') {
                app.innerHTML = `<div class="empty">${esc(error.message)} <a href="#/tickets">Back to tickets</a></div>`;
            }
            return;
        }

        const closed = ticket.status === 'CLOSED';
        app.innerHTML = `
            <p style="margin:0 0 12px"><a href="#/tickets">All tickets</a></p>
            <div class="detail">
                <div class="stack">
                    <article class="panel">
                        <div class="panel-body" id="summary">
                            <div class="ticket-title"><h1><span class="id">#${ticket.id}</span> ${esc(ticket.subject)}</h1>${statusPill(ticket.status)}</div>
                            <p class="meta">${esc(ticket.requester.fullName)} opened this ${esc(relative(ticket.createdAt))}</p>
                            <p class="description">${esc(ticket.description)}</p>
                            ${ticket.editable ? '<div class="actions"><button type="button" id="edit">Edit details</button></div>' : ''}
                        </div>
                        <ol class="thread" aria-label="Conversation">
                            ${comments.map((c) => `
                                <li class="comment ${c.internal ? 'internal' : ''}">
                                    <div class="comment-head">
                                        <strong>${esc(c.author.fullName)}</strong>
                                        <span class="muted">${esc(ROLE_LABEL[c.author.role])}</span>
                                        <span class="muted" title="${esc(absolute(c.createdAt))}">${esc(relative(c.createdAt))}</span>
                                    </div>
                                    ${c.internal ? '<div class="note-flag">Internal note. Only agents and admins can see this.</div>' : ''}
                                    <p>${esc(c.body)}</p>
                                </li>`).join('')}
                        </ol>
                        ${closed
                            ? '<div class="composer muted">This ticket is closed. Reopen it to continue the conversation.</div>'
                            : `<form class="composer" id="composer">
                                   <label for="body">${ticket.canAddInternalNotes ? 'Reply or add a note' : 'Reply'}</label>
                                   <textarea id="body" name="body" rows="4" required></textarea>
                                   <div class="composer-actions">
                                       ${ticket.canAddInternalNotes
                                           ? '<label class="check"><input type="checkbox" name="internal" value="true"> Internal note, hidden from the customer</label>'
                                           : '<span></span>'}
                                       <button class="primary" type="submit" id="send">Send reply</button>
                                   </div>
                               </form>`}
                    </article>
                </div>

                <aside class="stack">
                    <section class="panel panel-body props">
                        <h2 style="margin-bottom:12px">Details</h2>
                        <dl>
                            <dt>Status</dt><dd>${statusPill(ticket.status)}</dd>
                            <dt>Priority</dt><dd>${priorityText(ticket.priority)}</dd>
                            <dt>Requester</dt><dd>${esc(ticket.requester.fullName)}</dd>
                            <dt>Assignee</dt>
                            <dd>${ticket.assignable
                                ? `<select id="assignee" aria-label="Assignee">
                                       <option value="">Unassigned</option>
                                       ${staff.map((u) => `<option value="${u.id}" ${ticket.assignee && ticket.assignee.id === u.id ? 'selected' : ''}>${esc(u.fullName)}</option>`).join('')}
                                   </select>`
                                : (ticket.assignee ? esc(ticket.assignee.fullName) : '<span class="muted">Unassigned</span>')}</dd>
                            <dt>Created</dt><dd>${esc(absolute(ticket.createdAt))}</dd>
                            <dt>Updated</dt><dd>${esc(absolute(ticket.updatedAt))}</dd>
                            ${ticket.closedAt ? `<dt>Closed</dt><dd>${esc(absolute(ticket.closedAt))}</dd>` : ''}
                        </dl>
                        ${ticket.allowedTransitions.length ? `
                            <div class="actions">
                                ${ticket.allowedTransitions.map((s) => `
                                    <button type="button" data-status="${s}" class="${s === 'CLOSED' ? 'danger' : ''}">${esc(transitionLabel(s, ticket.status))}</button>`).join('')}
                            </div>` : ''}
                    </section>
                    <section class="panel panel-body">
                        <h2 style="margin-bottom:8px">History</h2>
                        <ol class="timeline">
                            ${history.map((h) => `<li>${historyLine(h)}<time datetime="${esc(h.changedAt)}">${esc(absolute(h.changedAt))}</time></li>`).join('')}
                        </ol>
                    </section>
                </aside>
            </div>`;

        const reload = () => viewTicket(id);

        app.querySelectorAll('[data-status]').forEach((button) => button.addEventListener('click', async () => {
            button.disabled = true;
            try {
                await api(`/api/v1/tickets/${id}/status`, { method: 'PATCH', body: { status: button.dataset.status } });
                toast('Status updated to ' + STATUS_LABEL[button.dataset.status] + '.');
                reload();
            } catch (error) {
                button.disabled = false;
                if (error.message !== 'Unauthorized') toast(error.message, true);
            }
        }));

        const assignee = document.getElementById('assignee');
        if (assignee) assignee.addEventListener('change', async () => {
            try {
                const value = assignee.value ? Number(assignee.value) : null;
                await api(`/api/v1/tickets/${id}/assignee`, { method: 'PATCH', body: { assigneeId: value } });
                toast(value ? 'Ticket assigned.' : 'Ticket unassigned.');
                reload();
            } catch (error) {
                if (error.message !== 'Unauthorized') toast(error.message, true);
            }
        });

        const composer = document.getElementById('composer');
        if (composer) {
            const internalBox = composer.querySelector('input[name="internal"]');
            if (internalBox) internalBox.addEventListener('change', () => {
                composer.classList.toggle('internal', internalBox.checked);
                document.getElementById('send').textContent = internalBox.checked ? 'Add internal note' : 'Send reply';
            });
            onSubmit(composer, async (data) => {
                await api(`/api/v1/tickets/${id}/comments`, {
                    method: 'POST',
                    body: { body: data.body, internal: data.internal === 'true' },
                });
                toast(data.internal === 'true' ? 'Internal note added.' : 'Reply sent.');
                reload();
            });
        }

        const edit = document.getElementById('edit');
        if (edit) edit.addEventListener('click', () => {
            document.getElementById('summary').innerHTML = `
                <form id="edit-form" novalidate>
                    <div class="field"><label for="es">Subject</label><input id="es" name="subject" maxlength="200" value="${esc(ticket.subject)}" required></div>
                    <div class="field"><label for="ed">Description</label><textarea id="ed" name="description" rows="6" required>${esc(ticket.description)}</textarea></div>
                    <div class="field" style="max-width:220px"><label for="ep">Priority</label><select id="ep" name="priority">${options(PRIORITY_LABEL, ticket.priority)}</select></div>
                    <div style="display:flex;gap:8px"><button class="primary" type="submit">Save changes</button><button type="button" id="cancel-edit">Cancel</button></div>
                </form>`;
            document.getElementById('cancel-edit').addEventListener('click', reload);
            onSubmit(document.getElementById('edit-form'), async (data) => {
                await api(`/api/v1/tickets/${id}`, { method: 'PATCH', body: data });
                toast('Changes saved.');
                reload();
            });
        });
    }

    // ---------------------------------------------------------------- views: team (admin)

    async function viewTeam() {
        renderChrome('team');
        if (!isAdmin()) { location.hash = '#/tickets'; return; }

        const page = await api('/api/v1/users?size=100&sort=fullName');
        app.innerHTML = `
            <div class="page-head"><div><h1>Team</h1><p>People who can sign in to ${esc(current.user.organizationName)}.</p></div></div>
            <div class="detail">
                <section class="panel">
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th scope="col">Name</th><th scope="col">Email</th><th scope="col">Role</th><th scope="col">Access</th></tr></thead>
                            <tbody>
                                ${page.content.map((u) => `
                                    <tr>
                                        <td>${esc(u.fullName)}</td>
                                        <td class="muted">${esc(u.email)}</td>
                                        <td>${esc(ROLE_LABEL[u.role])}</td>
                                        <td>${u.id === current.user.id
                                            ? '<span class="muted">You</span>'
                                            : `<button type="button" class="${u.active ? 'danger' : ''}" data-user="${u.id}" data-active="${!u.active}">${u.active ? 'Deactivate' : 'Reactivate'}</button>`}</td>
                                    </tr>`).join('')}
                            </tbody>
                        </table>
                    </div>
                </section>
                <form class="panel panel-body" id="add-user" novalidate>
                    <h2 style="margin-bottom:12px">Add a person</h2>
                    <div class="field"><label for="un">Full name</label><input id="un" name="fullName" required></div>
                    <div class="field"><label for="ue">Email</label><input id="ue" name="email" type="email" required></div>
                    <div class="field"><label for="up">Temporary password</label><input id="up" name="password" type="password" minlength="8" required></div>
                    <div class="field"><label for="ur">Role</label><select id="ur" name="role">${options(ROLE_LABEL, 'AGENT')}</select></div>
                    <button class="primary" type="submit">Add person</button>
                </form>
            </div>`;

        app.querySelectorAll('[data-user]').forEach((button) => button.addEventListener('click', async () => {
            button.disabled = true;
            try {
                await api(`/api/v1/users/${button.dataset.user}/status`, {
                    method: 'PATCH', body: { active: button.dataset.active === 'true' },
                });
                staffCache = null;
                toast(button.dataset.active === 'true' ? 'Access restored.' : 'Access removed.');
                viewTeam();
            } catch (error) {
                button.disabled = false;
                if (error.message !== 'Unauthorized') toast(error.message, true);
            }
        }));

        onSubmit(document.getElementById('add-user'), async (data) => {
            await api('/api/v1/users', { method: 'POST', body: data });
            staffCache = null;
            toast(data.fullName + ' added.');
            viewTeam();
        });
    }

    // ---------------------------------------------------------------- router

    async function route() {
        const hash = location.hash.replace(/^#/, '') || '/login';
        const publicRoute = hash === '/login' || hash === '/signup';

        if (!current && !publicRoute) { location.hash = '#/login'; return; }
        if (current && publicRoute) { location.hash = '#/tickets'; return; }

        try {
            const ticketMatch = hash.match(/^\/tickets\/(\d+)$/);
            if (hash === '/login') viewLogin();
            else if (hash === '/signup') viewSignup();
            else if (hash === '/tickets') await viewTickets();
            else if (hash === '/tickets/new') viewNewTicket();
            else if (ticketMatch) await viewTicket(ticketMatch[1]);
            else if (hash === '/team') await viewTeam();
            else location.hash = '#/tickets';
        } catch (error) {
            if (error.message !== 'Unauthorized') toast(error.message, true);
        }
        app.focus({ preventScroll: true });
    }

    window.addEventListener('hashchange', route);
    route();
})();
