// API base URL
const API_BASE = '/api/todos';

// State
let currentFilter = 'all';
let currentSearch = '';
let todos = [];
let searchDebounceTimer = null;

// DOM Elements
const todoForm = document.getElementById('todo-form');
const todoTitleInput = document.getElementById('todo-title');
const todoDescInput = document.getElementById('todo-desc');
const todoPriorityInput = document.getElementById('todo-priority');
const todoDueDateInput = document.getElementById('todo-due-date');

const todoList = document.getElementById('todo-list');
const emptyState = document.getElementById('empty-state');
const searchInput = document.getElementById('search-input');
const clearSearchBtn = document.getElementById('clear-search');
const filterTabs = document.querySelectorAll('.filter-tab');

const totalCountElem = document.getElementById('total-count');
const pendingCountElem = document.getElementById('pending-count');
const completedCountElem = document.getElementById('completed-count');

// Modal Elements
const editModal = document.getElementById('edit-modal');
const editForm = document.getElementById('edit-form');
const editIdInput = document.getElementById('edit-id');
const editTitleInput = document.getElementById('edit-title');
const editDescInput = document.getElementById('edit-desc');
const editPriorityInput = document.getElementById('edit-priority');
const editDueDateInput = document.getElementById('edit-due-date');
const closeModalBtn = document.getElementById('close-modal-btn');
const cancelModalBtn = document.getElementById('cancel-modal-btn');

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
    // Set default date picker min to today
    const today = new Date().toISOString().split('T')[0];
    todoDueDateInput.min = today;
    editDueDateInput.min = today;

    fetchTodos();
    setupEventListeners();
});

// --- Event Listeners ---
function setupEventListeners() {
    // Form Submit (Create)
    todoForm.addEventListener('submit', handleAddTodo);

    // Search input
    searchInput.addEventListener('input', (e) => {
        currentSearch = e.target.value.trim();
        clearSearchBtn.style.display = currentSearch ? 'block' : 'none';
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = setTimeout(fetchTodos, 250);
    });

    clearSearchBtn.addEventListener('click', () => {
        searchInput.value = '';
        currentSearch = '';
        clearSearchBtn.style.display = 'none';
        fetchTodos();
    });

    // Filter tabs
    filterTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            filterTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            currentFilter = tab.dataset.filter;
            fetchTodos();
        });
    });

    // Edit Modal close handlers
    closeModalBtn.addEventListener('click', closeEditModal);
    cancelModalBtn.addEventListener('click', closeEditModal);
    editModal.addEventListener('click', (e) => {
        if (e.target === editModal) closeEditModal();
    });
    editForm.addEventListener('submit', handleEditTodo);
}

// --- API Calls ---

// 1. Fetch Todos (Read All)
async function fetchTodos() {
    try {
        let url = `${API_BASE}?`;
        if (currentFilter !== 'all') {
            url += `status=${encodeURIComponent(currentFilter)}&`;
        }
        if (currentSearch) {
            url += `search=${encodeURIComponent(currentSearch)}&`;
        }

        const res = await fetch(url);
        if (!res.ok) throw new Error('Failed to fetch tasks');
        todos = await res.json();
        
        renderTodos(todos);
        updateStats();
    } catch (err) {
        console.error('Error loading todos:', err);
        showToast('Error loading tasks from server', 'error');
    }
}

// 2. Add Todo (Create)
async function handleAddTodo(e) {
    e.preventDefault();
    const title = todoTitleInput.value.trim();
    if (!title) return;

    const newTodo = {
        title: title,
        description: todoDescInput.value.trim(),
        priority: todoPriorityInput.value,
        dueDate: todoDueDateInput.value || null,
        completed: false
    };

    try {
        const res = await fetch(API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(newTodo)
        });

        if (!res.ok) throw new Error('Failed to create task');

        todoForm.reset();
        todoPriorityInput.value = 'MEDIUM';
        showToast('Task added successfully!', 'success');
        fetchTodos();
    } catch (err) {
        console.error('Error creating todo:', err);
        showToast('Error creating task', 'error');
    }
}

// 3. Toggle Completion (Update)
async function toggleTodo(id, currentCompleted) {
    try {
        const res = await fetch(`${API_BASE}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ completed: !currentCompleted })
        });

        if (!res.ok) throw new Error('Failed to update task status');

        const updated = await res.json();
        showToast(updated.completed ? 'Task completed! 🎉' : 'Task marked as pending', 'info');
        fetchTodos();
    } catch (err) {
        console.error('Error toggling todo:', err);
        showToast('Failed to update status', 'error');
    }
}

// 4. Open Edit Modal
function openEditModal(todo) {
    editIdInput.value = todo.id;
    editTitleInput.value = todo.title || '';
    editDescInput.value = todo.description || '';
    editPriorityInput.value = todo.priority || 'MEDIUM';
    editDueDateInput.value = todo.dueDate || '';

    editModal.style.display = 'flex';
    editTitleInput.focus();
}

function closeEditModal() {
    editModal.style.display = 'none';
    editForm.reset();
}

// 5. Submit Edit (Update)
async function handleEditTodo(e) {
    e.preventDefault();
    const id = editIdInput.value;
    const title = editTitleInput.value.trim();
    if (!title) return;

    const updatedData = {
        title: title,
        description: editDescInput.value.trim(),
        priority: editPriorityInput.value,
        dueDate: editDueDateInput.value || null
    };

    try {
        const res = await fetch(`${API_BASE}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updatedData)
        });

        if (!res.ok) throw new Error('Failed to save changes');

        closeEditModal();
        showToast('Task updated successfully', 'success');
        fetchTodos();
    } catch (err) {
        console.error('Error updating todo:', err);
        showToast('Failed to save changes', 'error');
    }
}

// 6. Delete Todo (Delete)
async function deleteTodo(id, title) {
    if (!confirm(`Are you sure you want to delete "${title}"?`)) return;

    try {
        const res = await fetch(`${API_BASE}/${id}`, {
            method: 'DELETE'
        });

        if (!res.ok) throw new Error('Failed to delete task');

        showToast('Task deleted', 'info');
        fetchTodos();
    } catch (err) {
        console.error('Error deleting todo:', err);
        showToast('Failed to delete task', 'error');
    }
}

// --- Render Logic ---
function renderTodos(items) {
    todoList.innerHTML = '';

    if (!items || items.length === 0) {
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';

    items.forEach(todo => {
        const itemDiv = document.createElement('div');
        itemDiv.className = `todo-item ${todo.completed ? 'completed' : ''}`;

        // Priority class
        const priorityClass = `badge-${(todo.priority || 'medium').toLowerCase()}`;

        // Due date formatting
        let dueDateHtml = '';
        if (todo.dueDate) {
            dueDateHtml = `<span class="date-badge">📅 ${escapeHtml(todo.dueDate)}</span>`;
        }

        itemDiv.innerHTML = `
            <div class="todo-checkbox-wrapper">
                <input type="checkbox" class="todo-checkbox" ${todo.completed ? 'checked' : ''} title="Mark completed">
            </div>
            <div class="todo-content">
                <div class="todo-title">${escapeHtml(todo.title)}</div>
                ${todo.description ? `<div class="todo-desc">${escapeHtml(todo.description)}</div>` : ''}
                <div class="todo-meta">
                    <span class="badge ${priorityClass}">${escapeHtml(todo.priority || 'MEDIUM')}</span>
                    ${dueDateHtml}
                </div>
            </div>
            <div class="todo-actions">
                <button class="btn-icon-action btn-edit" title="Edit task">✏️</button>
                <button class="btn-icon-action btn-delete" title="Delete task">🗑️</button>
            </div>
        `;

        // Attach item events
        const checkbox = itemDiv.querySelector('.todo-checkbox');
        checkbox.addEventListener('change', () => toggleTodo(todo.id, todo.completed));

        const editBtn = itemDiv.querySelector('.btn-edit');
        editBtn.addEventListener('click', () => openEditModal(todo));

        const deleteBtn = itemDiv.querySelector('.btn-delete');
        deleteBtn.addEventListener('click', () => deleteTodo(todo.id, todo.title));

        todoList.appendChild(itemDiv);
    });
}

// Update stats by querying total counts or calculating from all todos
async function updateStats() {
    try {
        const res = await fetch(API_BASE);
        if (!res.ok) return;
        const all = await res.json();

        const total = all.length;
        const completed = all.filter(t => t.completed).length;
        const pending = total - completed;

        totalCountElem.textContent = total;
        pendingCountElem.textContent = pending;
        completedCountElem.textContent = completed;
    } catch (e) {
        // Silently ignore stats fetch failure
    }
}

// --- Helpers ---
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '⚠️';

    toast.innerHTML = `<span>${icon}</span> <span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}
