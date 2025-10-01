// Configuration - use relative URLs for Spring Boot integration
const API_BASE_URL = '/api/passengers';
const DEFAULT_PAGE_SIZE = 20;

// Global state
let currentPage = 0;
let currentPageSize = DEFAULT_PAGE_SIZE;
let currentFilters = {};
let buses = [];
let stops = [];
let isEditMode = false;
let editingId = null;

// API Service
class ApiService {
    static async request(url, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
            },
        };

        const response = await fetch(url, { ...defaultOptions, ...options });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: 'Network error' }));
            throw new Error(errorData.message || `HTTP ${response.status}`);
        }

        return response.json();
    }

    static async getRecords(page = 0, size = DEFAULT_PAGE_SIZE, filters = {}) {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
            ...Object.fromEntries(
                Object.entries(filters).filter(([_, value]) => value !== '' && value !== null)
            )
        });

        return this.request(`${API_BASE_URL}?${params}`);
    }

    static async getRecord(id) {
        return this.request(`${API_BASE_URL}/${id}`);
    }

    static async createRecord(data) {
        return this.request(`${API_BASE_URL}`, {
            method: 'POST',
            body: JSON.stringify(data),
        });
    }

    static async updateRecord(id, data) {
        return this.request(`${API_BASE_URL}/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data),
        });
    }

    static async deleteRecord(id) {
        const response = await fetch(`${API_BASE_URL}/${id}`, {
            method: 'DELETE',
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
    }

    static async getBuses() {
        return this.request(`${API_BASE_URL}/buses`);
    }

    static async getStops() {
        return this.request(`${API_BASE_URL}/stops`);
    }
}

// UI Manager
class UIManager {
    static showLoading() {
        document.getElementById('loadingIndicator').style.display = 'flex';
        document.getElementById('dataTable').style.opacity = '0.5';
    }

    static hideLoading() {
        document.getElementById('loadingIndicator').style.display = 'none';
        document.getElementById('dataTable').style.opacity = '1';
    }

    static showModal(mode = 'create', data = null) {
        isEditMode = mode === 'edit';
        editingId = data?.id || null;

        const modal = document.getElementById('modalOverlay');
        const title = document.getElementById('modalTitle');
        const saveBtn = document.getElementById('modalSave');

        title.textContent = isEditMode ? 'Редактировать запись' : 'Добавить запись';
        saveBtn.querySelector('.btn-text').textContent = isEditMode ? 'Обновить' : 'Сохранить';

        if (isEditMode && data) {
            this.populateForm(data);
        } else {
            this.clearForm();
            // Set current datetime as default
            document.getElementById('modalTimestamp').value = new Date().toISOString().slice(0, 16);
        }

        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    static hideModal() {
        document.getElementById('modalOverlay').style.display = 'none';
        document.body.style.overflow = '';
        this.clearFormErrors();
    }

    static populateForm(data) {
        document.getElementById('modalBusId').value = data.busId || '';
        document.getElementById('modalStopId').value = data.stopId || '';
        document.getElementById('modalEntered').value = data.entered || '';
        document.getElementById('modalExited').value = data.exited || '';
        document.getElementById('modalTimestamp').value = data.timestamp ?
            new Date(data.timestamp).toISOString().slice(0, 16) : '';
    }

    static clearForm() {
        document.getElementById('recordForm').reset();
    }

    static clearFormErrors() {
        document.querySelectorAll('.error-message').forEach(el => {
            el.classList.remove('show');
            el.textContent = '';
        });
        document.querySelectorAll('.form-group').forEach(el => {
            el.classList.remove('error');
        });
    }

    static showFieldError(field, message) {
        const errorEl = document.getElementById(`${field}Error`);
        const groupEl = errorEl.closest('.form-group');

        errorEl.textContent = message;
        errorEl.classList.add('show');
        groupEl.classList.add('error');
    }

    static showConfirmationDialog(id, callback) {
        const overlay = document.getElementById('confirmationOverlay');
        overlay.style.display = 'flex';

        document.getElementById('confirmDelete').onclick = () => {
            overlay.style.display = 'none';
            callback(id);
        };
    }

    static hideConfirmationDialog() {
        document.getElementById('confirmationOverlay').style.display = 'none';
    }

    static renderTable(data) {
        const tbody = document.getElementById('tableBody');
        const emptyState = document.getElementById('emptyState');

        if (data.content.length === 0) {
            tbody.innerHTML = '';
            emptyState.style.display = 'block';
            return;
        }

        emptyState.style.display = 'none';

        tbody.innerHTML = data.content.map(record => `
            <tr>
                <td>${record.id}</td>
                <td>${record.busModel || 'N/A'}</td>
                <td>${record.stopName || 'N/A'}</td>
                <td>${record.routeName || 'N/A'}</td>
                <td>${record.entered}</td>
                <td>${record.exited}</td>
                <td>${this.formatDateTime(record.timestamp)}</td>
                <td>
                    <div class="table-actions">
                        <button class="action-btn edit-btn" onclick="handleEditClick(${record.id})">
                            Изм.
                        </button>
                        <button class="action-btn delete-btn" onclick="handleDeleteClick(${record.id})">
                            Удал.
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    static renderPagination(data) {
        const info = document.getElementById('paginationInfo');
        const pageNumbers = document.getElementById('pageNumbers');
        const firstBtn = document.getElementById('firstPage');
        const prevBtn = document.getElementById('prevPage');
        const nextBtn = document.getElementById('nextPage');
        const lastBtn = document.getElementById('lastPage');

        const { number, size, totalElements, totalPages } = data;
        const start = number * size + 1;
        const end = Math.min((number + 1) * size, totalElements);

        info.textContent = `Записи ${start}-${end} из ${totalElements}`;

        // Enable/disable navigation buttons
        firstBtn.disabled = number === 0;
        prevBtn.disabled = number === 0;
        nextBtn.disabled = number >= totalPages - 1;
        lastBtn.disabled = number >= totalPages - 1;

        // Generate page numbers
        pageNumbers.innerHTML = '';
        const maxVisiblePages = 5;
        let startPage = Math.max(0, number - Math.floor(maxVisiblePages / 2));
        let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);

        if (endPage - startPage < maxVisiblePages - 1) {
            startPage = Math.max(0, endPage - maxVisiblePages + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            const pageBtn = document.createElement('button');
            pageBtn.className = `btn btn-outline page-number ${i === number ? 'active' : ''}`;
            pageBtn.textContent = i + 1;
            pageBtn.onclick = () => loadData(i);
            pageNumbers.appendChild(pageBtn);
        }
    }

    static formatDateTime(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleString('ru-RU', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    static populateSelect(selectId, items, valueField = 'id', textField = 'name', defaultText = '') {
        const select = document.getElementById(selectId);
        const currentValue = select.value;

        select.innerHTML = defaultText ? `<option value="">${defaultText}</option>` : '';

        items.forEach(item => {
            const option = document.createElement('option');
            option.value = item[valueField];
            option.textContent = textField.includes('.') ?
                this.getNestedProperty(item, textField) :
                item[textField];
            select.appendChild(option);
        });

        // Restore previous value if it exists
        if (currentValue) {
            select.value = currentValue;
        }
    }

    static getNestedProperty(obj, path) {
        return path.split('.').reduce((current, prop) => current?.[prop], obj);
    }

    static showBusInfo(bus) {
        return `${bus.model} (${bus.routeName})`;
    }

    static showStopInfo(stop) {
        return `${stop.name} (${stop.routeName})`;
    }
}

// Toast Notifications
class ToastManager {
    static show(message, type = 'info', title = '', duration = 5000) {
        const container = document.getElementById('toastContainer');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;

        const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        };

        toast.innerHTML = `
            <div class="toast-icon">${icons[type] || icons.info}</div>
            <div class="toast-content">
                ${title ? `<div class="toast-title">${title}</div>` : ''}
                <div class="toast-message">${message}</div>
            </div>
            <button class="toast-close">&times;</button>
        `;

        container.appendChild(toast);

        // Auto remove
        const timeout = setTimeout(() => {
            this.remove(toast);
        }, duration);

        // Manual close
        toast.querySelector('.toast-close').onclick = () => {
            clearTimeout(timeout);
            this.remove(toast);
        };
    }

    static remove(toast) {
        toast.style.animation = 'slideOutRight 0.3s ease forwards';
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }

    static success(message, title = 'Успешно') {
        this.show(message, 'success', title);
    }

    static error(message, title = 'Ошибка') {
        this.show(message, 'error', title);
    }

    static warning(message, title = 'Предупреждение') {
        this.show(message, 'warning', title);
    }

    static info(message, title = 'Информация') {
        this.show(message, 'info', title);
    }
}

// Validation Manager
class ValidationManager {
    static validateForm(formData) {
        const errors = {};

        if (!formData.busId) {
            errors.busId = 'Выберите автобус';
        }

        if (!formData.stopId) {
            errors.stopId = 'Выберите остановку';
        }

        if (formData.entered === '' || formData.entered < 0) {
            errors.entered = 'Введите корректное количество вошедших';
        }

        if (formData.exited === '' || formData.exited < 0) {
            errors.exited = 'Введите корректное количество вышедших';
        }

        if (!formData.timestamp) {
            errors.timestamp = 'Выберите время';
        } else {
            const selectedDate = new Date(formData.timestamp);
            const now = new Date();
            if (selectedDate > now) {
                errors.timestamp = 'Время не может быть в будущем';
            }
        }

        return errors;
    }

    static showErrors(errors) {
        UIManager.clearFormErrors();
        Object.entries(errors).forEach(([field, message]) => {
            UIManager.showFieldError(field, message);
        });
    }
}

// Main Application Logic
async function loadData(page = currentPage, size = currentPageSize, filters = currentFilters) {
    try {
        UIManager.showLoading();

        const data = await ApiService.getRecords(page, size, filters);

        currentPage = page;
        currentPageSize = size;
        currentFilters = filters;

        UIManager.renderTable(data);
        UIManager.renderPagination(data);

    } catch (error) {
        console.error('Error loading data:', error);
        ToastManager.error('Не удалось загрузить данные: ' + error.message);
    } finally {
        UIManager.hideLoading();
    }
}

async function loadReferenceData() {
    try {
        const [busesData, stopsData] = await Promise.all([
            ApiService.getBuses(),
            ApiService.getStops()
        ]);

        buses = busesData;
        stops = stopsData;

        // Populate filter selects
        UIManager.populateSelect('busFilter', buses, 'id', 'model', 'Все автобусы');
        UIManager.populateSelect('stopFilter', stops, 'id', 'name', 'Все остановки');

        // Populate modal selects with more detailed info
        const modalBusSelect = document.getElementById('modalBusId');
        const modalStopSelect = document.getElementById('modalStopId');

        modalBusSelect.innerHTML = '<option value="">Выберите автобус</option>';
        buses.forEach(bus => {
            const option = document.createElement('option');
            option.value = bus.id;
            option.textContent = `${bus.model} (Маршрут: ${bus.routeName})`;
            modalBusSelect.appendChild(option);
        });

        modalStopSelect.innerHTML = '<option value="">Выберите остановку</option>';
        stops.forEach(stop => {
            const option = document.createElement('option');
            option.value = stop.id;
            option.textContent = `${stop.name} (Маршрут: ${stop.routeName})`;
            modalStopSelect.appendChild(option);
        });

    } catch (error) {
        console.error('Error loading reference data:', error);
        ToastManager.error('Не удалось загрузить справочные данные: ' + error.message);
    }
}

// Event Handlers
async function handleAddClick() {
    UIManager.showModal('create');
}

async function handleEditClick(id) {
    try {
        const record = await ApiService.getRecord(id);
        UIManager.showModal('edit', record);
    } catch (error) {
        console.error('Error loading record:', error);
        ToastManager.error('Не удалось загрузить запись: ' + error.message);
    }
}

function handleDeleteClick(id) {
    UIManager.showConfirmationDialog(id, async (recordId) => {
        try {
            await ApiService.deleteRecord(recordId);
            ToastManager.success('Запись успешно удалена');
            await loadData();
        } catch (error) {
            console.error('Error deleting record:', error);
            ToastManager.error('Не удалось удалить запись: ' + error.message);
        }
    });
}

async function handleFormSubmit(event) {
    event.preventDefault();

    const saveBtn = document.getElementById('modalSave');
    const btnText = saveBtn.querySelector('.btn-text');
    const btnSpinner = saveBtn.querySelector('.btn-spinner');

    // Show loading state
    saveBtn.disabled = true;
    btnText.style.display = 'none';
    btnSpinner.style.display = 'block';

    try {
        const formData = new FormData(event.target);
        const data = {
            busId: parseInt(formData.get('busId')),
            stopId: parseInt(formData.get('stopId')),
            entered: parseInt(formData.get('entered')),
            exited: parseInt(formData.get('exited')),
            timestamp: formData.get('timestamp')
        };

        // Validate
        const errors = ValidationManager.validateForm(data);
        if (Object.keys(errors).length > 0) {
            ValidationManager.showErrors(errors);
            return;
        }

        // Save
        if (isEditMode) {
            await ApiService.updateRecord(editingId, data);
            ToastManager.success('Запись успешно обновлена');
        } else {
            await ApiService.createRecord(data);
            ToastManager.success('Запись успешно создана');
        }

        UIManager.hideModal();
        await loadData();

    } catch (error) {
        console.error('Error saving record:', error);
        ToastManager.error('Не удалось сохранить запись: ' + error.message);
    } finally {
        // Reset button state
        saveBtn.disabled = false;
        btnText.style.display = 'inline';
        btnSpinner.style.display = 'none';
    }
}

function handleFiltersApply() {
    const filters = {
        busId: document.getElementById('busFilter').value || null,
        stopId: document.getElementById('stopFilter').value || null,
        startTime: document.getElementById('startTimeFilter').value || null,
        endTime: document.getElementById('endTimeFilter').value || null
    };

    loadData(0, currentPageSize, filters);
}

function handleFiltersClear() {
    document.getElementById('busFilter').value = '';
    document.getElementById('stopFilter').value = '';
    document.getElementById('startTimeFilter').value = '';
    document.getElementById('endTimeFilter').value = '';

    loadData(0, currentPageSize, {});
}

function handleRefreshData() {
    loadData(currentPage, currentPageSize, currentFilters);
}

function handlePageSizeChange() {
    const newSize = parseInt(document.getElementById('pageSize').value);
    loadData(0, newSize, currentFilters);
}

// Initialize Application
document.addEventListener('DOMContentLoaded', async () => {
    // Set up event listeners
    document.getElementById('addNewRecord').onclick = handleAddClick;
    document.getElementById('recordForm').onsubmit = handleFormSubmit;
    document.getElementById('modalClose').onclick = UIManager.hideModal;
    document.getElementById('modalCancel').onclick = UIManager.hideModal;
    document.getElementById('confirmCancel').onclick = UIManager.hideConfirmationDialog;

    document.getElementById('applyFilters').onclick = handleFiltersApply;
    document.getElementById('clearFilters').onclick = handleFiltersClear;
    document.getElementById('refreshData').onclick = handleRefreshData;
    document.getElementById('pageSize').onchange = handlePageSizeChange;

    // Pagination controls
    document.getElementById('firstPage').onclick = () => loadData(0);
    document.getElementById('prevPage').onclick = () => loadData(Math.max(0, currentPage - 1));
    document.getElementById('nextPage').onclick = () => loadData(currentPage + 1);
    document.getElementById('lastPage').onclick = () => {
        // This will be set properly after first load
    };

    // Close modals on overlay click
    document.getElementById('modalOverlay').onclick = (e) => {
        if (e.target === e.currentTarget) UIManager.hideModal();
    };
    document.getElementById('confirmationOverlay').onclick = (e) => {
        if (e.target === e.currentTarget) UIManager.hideConfirmationDialog();
    };

    // Close modals on Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            UIManager.hideModal();
            UIManager.hideConfirmationDialog();
        }
    });

    // Load initial data
    await loadReferenceData();
    await loadData();
});

// CSS for slide out animation
const style = document.createElement('style');
style.textContent = `
    @keyframes slideOutRight {
        to {
            opacity: 0;
            transform: translateX(100%);
        }
    }
`;
document.head.appendChild(style);