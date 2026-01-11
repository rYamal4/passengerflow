let allStops = [];
let routeStops = [];
let predictions = [];
let currentRoute = '';
let currentHour = 12;
let useWeather = true;
let viewMode = 'map';

const SVG_WIDTH = 1000;
const SVG_HEIGHT = 600;
const MARGIN = { top: 60, right: 80, bottom: 60, left: 80 };
const NODE_RADIUS = 18;

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

    static async getPredictions(route, useWeather = true) {
        return this.request(`/api/predictions?route=${encodeURIComponent(route)}&useWeather=${useWeather}`);
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
    if (occupancy < 50) return '#10b981';
    if (occupancy < 80) return '#f59e0b';
    if (occupancy < 100) return '#f97316';
    if (occupancy < 120) return '#ef4444';
    return '#dc2626';
}

function getOccupancyForStop(stopName, hour) {
    const prediction = predictions.find(p => {
        const predTime = p.time.split(':');
        const predHour = parseInt(predTime[0]);
        return p.stopName === stopName && predHour === hour;
    });
    return prediction ? prediction.occupancyPercentage : null;
}

function projectCoordinates(stops) {
    const lats = stops.map(s => s.lat);
    const lons = stops.map(s => s.lon);

    const minLat = Math.min(...lats);
    const maxLat = Math.max(...lats);
    const minLon = Math.min(...lons);
    const maxLon = Math.max(...lons);

    const latRange = maxLat - minLat || 1;
    const lonRange = maxLon - minLon || 1;

    const availableWidth = SVG_WIDTH - MARGIN.left - MARGIN.right;
    const availableHeight = SVG_HEIGHT - MARGIN.top - MARGIN.bottom;

    const scale = Math.min(availableWidth / lonRange, availableHeight / latRange) * 0.9;

    return stops.map(stop => {
        const x = MARGIN.left + (stop.lon - minLon) * scale + (availableWidth - lonRange * scale) / 2;
        const y = MARGIN.top + (maxLat - stop.lat) * scale + (availableHeight - latRange * scale) / 2;
        return { ...stop, x, y };
    });
}

function createCurvedPath(x1, y1, x2, y2) {
    const dx = x2 - x1;
    const dy = y2 - y1;
    const distance = Math.sqrt(dx * dx + dy * dy);

    const curve = distance * 0.15;
    const cx = (x1 + x2) / 2 - dy * curve / distance;
    const cy = (y1 + y2) / 2 + dx * curve / distance;

    return `M ${x1} ${y1} Q ${cx} ${cy} ${x2} ${y2}`;
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
    updateDownloadButton();

    if (!currentRoute) {
        showEmptyState();
        return;
    }

    try {
        showLoading();
        routeStops = allStops.filter(stop => stop.routeName === currentRoute)
                              .sort((a, b) => a.id - b.id);

        predictions = await ApiService.getPredictions(currentRoute, useWeather);

        hideEmptyState();
        if (viewMode === 'map') {
            showMapView();
        } else {
            showTableView();
        }
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
    document.getElementById('heatmapTable').style.display = 'none';
}

function hideEmptyState() {
    document.getElementById('emptyState').style.display = 'none';
}

function renderHeatmap() {
    const svg = document.getElementById('heatmapSvg');
    svg.innerHTML = '';

    if (routeStops.length === 0) {
        return;
    }

    const svgNS = 'http://www.w3.org/2000/svg';
    const projectedStops = projectCoordinates(routeStops);

    const defs = document.createElementNS(svgNS, 'defs');

    const gradient = document.createElementNS(svgNS, 'linearGradient');
    gradient.setAttribute('id', 'lineGradient');
    gradient.setAttribute('gradientUnits', 'userSpaceOnUse');
    const stop1 = document.createElementNS(svgNS, 'stop');
    stop1.setAttribute('offset', '0%');
    stop1.setAttribute('style', 'stop-color:#6366f1;stop-opacity:0.5');
    const stop2 = document.createElementNS(svgNS, 'stop');
    stop2.setAttribute('offset', '100%');
    stop2.setAttribute('style', 'stop-color:#8b5cf6;stop-opacity:0.5');
    gradient.appendChild(stop1);
    gradient.appendChild(stop2);
    defs.appendChild(gradient);

    const filter = document.createElementNS(svgNS, 'filter');
    filter.setAttribute('id', 'nodeShadow');
    filter.setAttribute('x', '-50%');
    filter.setAttribute('y', '-50%');
    filter.setAttribute('width', '200%');
    filter.setAttribute('height', '200%');

    const feGaussianBlur = document.createElementNS(svgNS, 'feGaussianBlur');
    feGaussianBlur.setAttribute('in', 'SourceAlpha');
    feGaussianBlur.setAttribute('stdDeviation', '3');

    const feOffset = document.createElementNS(svgNS, 'feOffset');
    feOffset.setAttribute('dx', '0');
    feOffset.setAttribute('dy', '2');
    feOffset.setAttribute('result', 'offsetblur');

    const feMerge = document.createElementNS(svgNS, 'feMerge');
    const feMergeNode1 = document.createElementNS(svgNS, 'feMergeNode');
    const feMergeNode2 = document.createElementNS(svgNS, 'feMergeNode');
    feMergeNode2.setAttribute('in', 'SourceGraphic');
    feMerge.appendChild(feMergeNode1);
    feMerge.appendChild(feMergeNode2);

    filter.appendChild(feGaussianBlur);
    filter.appendChild(feOffset);
    filter.appendChild(feMerge);
    defs.appendChild(filter);

    svg.appendChild(defs);

    const pathsGroup = document.createElementNS(svgNS, 'g');
    pathsGroup.setAttribute('class', 'paths-group');

    for (let i = 0; i < projectedStops.length; i++) {
        const stop1 = projectedStops[i];
        const stop2 = projectedStops[(i + 1) % projectedStops.length];

        const path = document.createElementNS(svgNS, 'path');
        path.setAttribute('class', 'connection-path');
        path.setAttribute('d', createCurvedPath(stop1.x, stop1.y, stop2.x, stop2.y));
        path.setAttribute('stroke', 'url(#lineGradient)');
        path.setAttribute('fill', 'none');
        pathsGroup.appendChild(path);

        const dx = stop2.x - stop1.x;
        const dy = stop2.y - stop1.y;
        const angle = Math.atan2(dy, dx) * 180 / Math.PI;
        const midX = (stop1.x + stop2.x) / 2;
        const midY = (stop1.y + stop2.y) / 2;

        const arrow = document.createElementNS(svgNS, 'polygon');
        arrow.setAttribute('class', 'direction-arrow');
        arrow.setAttribute('points', '-6,-4 6,0 -6,4');
        arrow.setAttribute('transform', `translate(${midX},${midY}) rotate(${angle})`);
        arrow.setAttribute('fill', '#6366f1');
        arrow.setAttribute('opacity', '0.7');
        pathsGroup.appendChild(arrow);
    }

    svg.appendChild(pathsGroup);

    projectedStops.forEach((stop, index) => {
        const occupancy = getOccupancyForStop(stop.name, currentHour);
        const color = getOccupancyColor(occupancy);

        const g = document.createElementNS(svgNS, 'g');
        g.setAttribute('class', 'stop-node');
        g.setAttribute('data-stop-name', stop.name);
        g.setAttribute('data-occupancy', occupancy !== null ? occupancy.toFixed(1) : 'N/A');

        const outerCircle = document.createElementNS(svgNS, 'circle');
        outerCircle.setAttribute('cx', stop.x);
        outerCircle.setAttribute('cy', stop.y);
        outerCircle.setAttribute('r', NODE_RADIUS + 3);
        outerCircle.setAttribute('fill', 'white');
        outerCircle.setAttribute('filter', 'url(#nodeShadow)');
        g.appendChild(outerCircle);

        const circle = document.createElementNS(svgNS, 'circle');
        circle.setAttribute('cx', stop.x);
        circle.setAttribute('cy', stop.y);
        circle.setAttribute('r', NODE_RADIUS);
        circle.setAttribute('fill', color);
        circle.setAttribute('class', 'stop-circle');
        g.appendChild(circle);

        const innerCircle = document.createElementNS(svgNS, 'circle');
        innerCircle.setAttribute('cx', stop.x);
        innerCircle.setAttribute('cy', stop.y);
        innerCircle.setAttribute('r', 5);
        innerCircle.setAttribute('fill', 'white');
        innerCircle.setAttribute('opacity', '0.9');
        g.appendChild(innerCircle);

        const textBg = document.createElementNS(svgNS, 'rect');
        textBg.setAttribute('x', stop.x + NODE_RADIUS + 6);
        textBg.setAttribute('y', stop.y - 11);
        textBg.setAttribute('width', stop.name.length * 6.5 + 14);
        textBg.setAttribute('height', 22);
        textBg.setAttribute('fill', 'white');
        textBg.setAttribute('opacity', '0.95');
        textBg.setAttribute('rx', '6');
        textBg.setAttribute('filter', 'url(#nodeShadow)');
        g.appendChild(textBg);

        const text = document.createElementNS(svgNS, 'text');
        text.setAttribute('class', 'stop-label');
        text.setAttribute('x', stop.x + NODE_RADIUS + 13);
        text.setAttribute('y', stop.y + 1);
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
    title.setAttribute('y', 30);
    title.setAttribute('text-anchor', 'middle');
    title.setAttribute('class', 'map-title');
    title.textContent = `Маршрут ${currentRoute} в ${currentHour}:00`;
    svg.appendChild(title);

    const subtitle = document.createElementNS(svgNS, 'text');
    subtitle.setAttribute('x', SVG_WIDTH / 2);
    subtitle.setAttribute('y', 50);
    subtitle.setAttribute('text-anchor', 'middle');
    subtitle.setAttribute('class', 'map-subtitle');
    subtitle.textContent = `Кольцевой маршрут • ${projectedStops.length} остановок`;
    svg.appendChild(subtitle);
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
    tooltip.style.opacity = '0';
    setTimeout(() => tooltip.style.opacity = '1', 10);
    updateTooltipPosition(event);
}

function hideTooltip() {
    const tooltip = document.getElementById('tooltip');
    tooltip.style.opacity = '0';
    setTimeout(() => tooltip.style.display = 'none', 200);
}

function updateTooltipPosition(event) {
    const tooltip = document.getElementById('tooltip');
    tooltip.style.left = (event.pageX + 15) + 'px';
    tooltip.style.top = (event.pageY + 15) + 'px';
}

function getTextColorForOccupancy(occupancy) {
    if (occupancy < 50) return '#10b981';
    if (occupancy < 80) return '#f59e0b';
    if (occupancy < 100) return '#f97316';
    if (occupancy < 120) return '#ef4444';
    return '#dc2626';
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

function handleWeatherCheckboxChange() {
    useWeather = document.getElementById('useWeatherCheckbox').checked;
    updateWeatherBadge();
    if (currentRoute) {
        handleRouteChange();
    }
}

function updateWeatherBadge() {
    const weatherStatus = document.getElementById('weatherStatus');
    if (useWeather) {
        weatherStatus.innerHTML = '<span class="weather-badge active">+20% при дожде</span>';
    } else {
        weatherStatus.innerHTML = '<span class="weather-badge inactive">Отключено</span>';
    }
}

function handleViewModeChange() {
    viewMode = document.getElementById('viewModeCheckbox').checked ? 'table' : 'map';
    updateViewModeBadge();
    if (currentRoute && routeStops.length > 0) {
        if (viewMode === 'map') {
            showMapView();
        } else {
            showTableView();
        }
    }
}

function updateViewModeBadge() {
    const viewModeStatus = document.getElementById('viewModeStatus');
    if (viewMode === 'map') {
        viewModeStatus.innerHTML = '<span class="view-badge">Карта</span>';
    } else {
        viewModeStatus.innerHTML = '<span class="view-badge">Таблица</span>';
    }
}

function showMapView() {
    document.getElementById('heatmapSvg').style.display = 'block';
    document.getElementById('heatmapTable').style.display = 'none';
    document.querySelector('.slider-container').classList.remove('hidden');
    renderHeatmap();
}

function showTableView() {
    document.getElementById('heatmapSvg').style.display = 'none';
    document.getElementById('heatmapTable').style.display = 'block';
    document.querySelector('.slider-container').classList.add('hidden');
    renderTableHeatmap();
}

function renderTableHeatmap() {
    const container = document.getElementById('heatmapTable');
    const hours = Array.from({length: 13}, (_, i) => i + 6);

    let html = '<table class="heatmap-table">';
    html += '<thead><tr><th>Остановка</th>';
    hours.forEach(h => html += `<th>${h}:00</th>`);
    html += '</tr></thead><tbody>';

    routeStops.forEach(stop => {
        html += `<tr><td class="stop-name">${stop.name}</td>`;
        hours.forEach(hour => {
            var occupancy = getOccupancyForStop(stop.name, hour);
            var color = getOccupancyColor(occupancy);
            var value = occupancy !== null ? `${occupancy.toFixed(0)}%` : '—';
            html += `<td class="heatmap-cell" style="background-color: ${color}">${value}</td>`;
        });
        html += '</tr>';
    });

    html += '</tbody></table>';
    container.innerHTML = html;
}

async function downloadPdf() {
    if (!currentRoute) {
        ToastManager.error('Выберите маршрут для скачивания отчета');
        return;
    }

    var btn = document.getElementById('downloadPdfBtn');
    var originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = `
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 6v6l4 2"/>
        </svg>
        Генерация...
    `;

    try {
        var response = await fetch(`/api/reports/heatmap?route=${encodeURIComponent(currentRoute)}&useWeather=${useWeather}`);

        if (!response.ok) {
            if (response.status === 401) {
                throw new Error('Требуется авторизация');
            }
            throw new Error(`Ошибка сервера: ${response.status}`);
        }

        var blob = await response.blob();
        var url = window.URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = `heatmap_${currentRoute}_${new Date().toISOString().split('T')[0]}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        ToastManager.success('Отчет успешно скачан');
    } catch (error) {
        console.error('Error downloading PDF:', error);
        ToastManager.error(error.message || 'Не удалось скачать отчет');
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
}

function updateDownloadButton() {
    var btn = document.getElementById('downloadPdfBtn');
    btn.disabled = !currentRoute;
}

document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('routeSelect').addEventListener('change', handleRouteChange);
    document.getElementById('hourSlider').addEventListener('input', handleHourChange);
    document.getElementById('useWeatherCheckbox').addEventListener('change', handleWeatherCheckboxChange);
    document.getElementById('viewModeCheckbox').addEventListener('change', handleViewModeChange);
    document.getElementById('downloadPdfBtn').addEventListener('click', downloadPdf);

    await loadStops();
});
