package io.github.ryamal4.passengerflow.service.report;

import io.github.ryamal4.passengerflow.dto.HeatmapReportDTO;

public interface IHeatmapReportService {
    byte[] generateHeatmapReport(String routeName, boolean useWeather);

    HeatmapReportDTO prepareReportData(String routeName, boolean useWeather);
}
