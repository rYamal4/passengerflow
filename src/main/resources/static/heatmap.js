let allStops = [];
let routeStops = [];
let predictions = [];
let currentRoute = '';
let currentHour = 12;

const SVG_WIDTH = 1000;
const SVG_HEIGHT = 600;
const MARGIN = { top: 60, right: 80, bottom: 60, left: 80 };
const NODE_RADIUS = 22;

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

    const svgNS = 'http://www.w3.org/2000/svg';
    const projectedStops = projectCoordinates(routeStops);

    const defs = document.createElementNS(svgNS, 'defs');

    const gradient = document.createElementNS(svgNS, 'linearGradient');
    gradient.setAttribute('id', 'lineGradient');
    gradient.setAttribute('gradientUnits', 'userSpaceOnUse');
    const stop1 = document.createElementNS(svgNS, 'stop');
    stop1.setAttribute('offset', '0%');
    stop1.setAttribute('style', 'stop-color:#667eea;stop-opacity:0.6');
    const stop2 = document.createElementNS(svgNS, 'stop');
    stop2.setAttribute('offset', '100%');
    stop2.setAttribute('style', 'stop-color:#764ba2;stop-opacity:0.6');
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
        arrow.setAttribute('fill', '#667eea');
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
        outerCircle.setAttribute('r', NODE_RADIUS + 4);
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
        innerCircle.setAttribute('r', 6);
        innerCircle.setAttribute('fill', 'white');
        innerCircle.setAttribute('opacity', '0.8');
        g.appendChild(innerCircle);

        const textBg = document.createElementNS(svgNS, 'rect');
        textBg.setAttribute('x', stop.x + NODE_RADIUS + 8);
        textBg.setAttribute('y', stop.y - 10);
        textBg.setAttribute('width', stop.name.length * 7 + 10);
        textBg.setAttribute('height', 20);
        textBg.setAttribute('fill', 'white');
        textBg.setAttribute('opacity', '0.9');
        textBg.setAttribute('rx', '4');
        textBg.setAttribute('filter', 'url(#nodeShadow)');
        g.appendChild(textBg);

        const text = document.createElementNS(svgNS, 'text');
        text.setAttribute('class', 'stop-label');
        text.setAttribute('x', stop.x + NODE_RADIUS + 13);
        text.setAttribute('y', stop.y);
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
