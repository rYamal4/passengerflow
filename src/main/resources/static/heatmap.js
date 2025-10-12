let allStops = [];
let routeStops = [];
let predictions = [];
let currentRoute = '';
let currentHour = 12;

const SVG_WIDTH = 1000;
const SVG_HEIGHT = 600;
const MARGIN = { top: 40, right: 100, bottom: 40, left: 100 };
const NODE_RADIUS = 20;

class ApiService {
    static async request(url) {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        return response.json();
    }

    static async getAllStops() {
        return this.request('/api/stops');
    }

    static async getPredictions(route) {
        return this.request(`/api/predictions?route=${encodeURIComponent(route)}`);
    }
}

class ToastManager {
    static show(message, type = 'info', title = '', duration = 5000) {
        const container = document.getElementById('toastContainer');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;

        const icons = {
            success: '✅',
            error: '❌',
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

        const timeout = setTimeout(() => {
            this.remove(toast);
        }, duration);

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

    static error(message, title = 'Ошибка') {
        this.show(message, 'error', title);
    }

    static success(message, title = 'Успешно') {
        this.show(message, 'success', title);
    }

    static info(message, title = 'Информация') {
        this.show(message, 'info', title);
    }
}

function showLoading() {
    document.getElementById('loadingIndicator').style.display = 'flex';
}

function hideLoading() {
    document.getElementById('loadingIndicator').style.display = 'none';
}

function getOccupancyColor(occupancy) {
    if (occupancy === null || occupancy === undefined) {
        return '#9E9E9E';
    }
    if (occupancy < 50) return '#4CAF50';
    if (occupancy < 80) return '#FFEB3B';
    if (occupancy < 100) return '#FF9800';
    if (occupancy < 120) return '#F44336';
    return '#B71C1C';
}

function getOccupancyForStop(stopName, hour) {
    const prediction = predictions.find(p => {
        const predTime = p.time.split(':');
        const predHour = parseInt(predTime[0]);
        return p.stopName === stopName && predHour === hour;
    });
    return prediction ? prediction.occupancyPercentage : null;
}

async function loadStops() {
    try {
        showLoading();
        allStops = await ApiService.getAllStops();
        populateRouteSelect();
    } catch (error) {
        console.error('Error loading stops:', error);
        ToastManager.error('Не удалось загрузить остановки');
    } finally {
        hideLoading();
    }
}

function populateRouteSelect() {
    const routeSelect = document.getElementById('routeSelect');
    const routes = [...new Set(allStops.map(stop => stop.routeName))].sort();

    routeSelect.innerHTML = '<option value="">Выберите маршрут...</option>';
    routes.forEach(route => {
        const option = document.createElement('option');
        option.value = route;
        option.textContent = route;
        routeSelect.appendChild(option);
    });
}

async function handleRouteChange() {
    const routeSelect = document.getElementById('routeSelect');
    currentRoute = routeSelect.value;

    if (!currentRoute) {
        showEmptyState();
        return;
    }

    try {
        showLoading();
        routeStops = allStops.filter(stop => stop.routeName === currentRoute)
                              .sort((a, b) => a.id - b.id);

        predictions = await ApiService.getPredictions(currentRoute);

        hideEmptyState();
        renderHeatmap();
    } catch (error) {
        console.error('Error loading predictions:', error);
        ToastManager.error('Не удалось загрузить прогнозы для маршрута');
    } finally {
        hideLoading();
    }
}

function showEmptyState() {
    document.getElementById('emptyState').style.display = 'block';
    document.getElementById('heatmapSvg').style.display = 'none';
}

function hideEmptyState() {
    document.getElementById('emptyState').style.display = 'none';
    document.getElementById('heatmapSvg').style.display = 'block';
}

function renderHeatmap() {
    const svg = document.getElementById('heatmapSvg');
    svg.innerHTML = '';

    if (routeStops.length === 0) {
        return;
    }

    const availableHeight = SVG_HEIGHT - MARGIN.top - MARGIN.bottom;
    const spacing = routeStops.length > 1 ? availableHeight / (routeStops.length - 1) : 0;
    const centerX = SVG_WIDTH / 2;

    const svgNS = 'http://www.w3.org/2000/svg';

    for (let i = 0; i < routeStops.length - 1; i++) {
        const y1 = MARGIN.top + i * spacing;
        const y2 = MARGIN.top + (i + 1) * spacing;

        const line = document.createElementNS(svgNS, 'line');
        line.setAttribute('class', 'connection-line');
        line.setAttribute('x1', centerX);
        line.setAttribute('y1', y1);
        line.setAttribute('x2', centerX);
        line.setAttribute('y2', y2);
        svg.appendChild(line);
    }

    routeStops.forEach((stop, index) => {
        const y = MARGIN.top + index * spacing;
        const occupancy = getOccupancyForStop(stop.name, currentHour);
        const color = getOccupancyColor(occupancy);

        const g = document.createElementNS(svgNS, 'g');
        g.setAttribute('class', 'stop-node');
        g.setAttribute('data-stop-name', stop.name);
        g.setAttribute('data-occupancy', occupancy !== null ? occupancy.toFixed(1) : 'N/A');

        const circle = document.createElementNS(svgNS, 'circle');
        circle.setAttribute('cx', centerX);
        circle.setAttribute('cy', y);
        circle.setAttribute('r', NODE_RADIUS);
        circle.setAttribute('fill', color);
        g.appendChild(circle);

        const text = document.createElementNS(svgNS, 'text');
        text.setAttribute('class', 'stop-label');
        text.setAttribute('x', centerX + NODE_RADIUS + 15);
        text.setAttribute('y', y);
        text.setAttribute('dominant-baseline', 'middle');
        text.textContent = stop.name;
        g.appendChild(text);

        g.addEventListener('mouseenter', (e) => showTooltip(e, stop, occupancy));
        g.addEventListener('mouseleave', hideTooltip);
        g.addEventListener('mousemove', updateTooltipPosition);

        svg.appendChild(g);
    });

    const title = document.createElementNS(svgNS, 'text');
    title.setAttribute('x', SVG_WIDTH / 2);
    title.setAttribute('y', 20);
    title.setAttribute('text-anchor', 'middle');
    title.setAttribute('style', 'font-size: 18px; font-weight: 600; fill: #333;');
    title.textContent = `Маршрут ${currentRoute} в ${currentHour}:00`;
    svg.appendChild(title);
}

function showTooltip(event, stop, occupancy) {
    const tooltip = document.getElementById('tooltip');
    const stopName = document.getElementById('tooltipStopName');
    const time = document.getElementById('tooltipTime');
    const occupancyText = document.getElementById('tooltipOccupancy');

    stopName.textContent = stop.name;
    time.textContent = `Время: ${currentHour}:00`;

    if (occupancy !== null) {
        occupancyText.textContent = `Загруженность: ${occupancy.toFixed(1)}%`;
        occupancyText.style.color = getTextColorForOccupancy(occupancy);
    } else {
        occupancyText.textContent = 'Загруженность: нет данных';
        occupancyText.style.color = '#ccc';
    }

    tooltip.style.display = 'block';
    updateTooltipPosition(event);
}

function hideTooltip() {
    document.getElementById('tooltip').style.display = 'none';
}

function updateTooltipPosition(event) {
    const tooltip = document.getElementById('tooltip');
    tooltip.style.left = (event.pageX + 15) + 'px';
    tooltip.style.top = (event.pageY + 15) + 'px';
}

function getTextColorForOccupancy(occupancy) {
    if (occupancy < 50) return '#4CAF50';
    if (occupancy < 80) return '#F9A825';
    if (occupancy < 100) return '#FF9800';
    if (occupancy < 120) return '#F44336';
    return '#B71C1C';
}

function handleHourChange() {
    const slider = document.getElementById('hourSlider');
    const hourValue = document.getElementById('hourValue');
    currentHour = parseInt(slider.value);
    hourValue.textContent = currentHour;

    if (currentRoute && routeStops.length > 0) {
        renderHeatmap();
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('routeSelect').addEventListener('change', handleRouteChange);
    document.getElementById('hourSlider').addEventListener('input', handleHourChange);

    await loadStops();
});
