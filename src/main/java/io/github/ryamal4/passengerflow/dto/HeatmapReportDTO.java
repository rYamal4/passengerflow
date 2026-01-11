package io.github.ryamal4.passengerflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapReportDTO {
    private String routeName;
    private LocalDate reportDate;
    private LocalDateTime generatedAt;
    private String dayOfWeekName;
    private boolean weatherEnabled;
    private List<String> stopNames;
    private List<Integer> hours;
    private Map<String, Map<Integer, Double>> heatmapData;
}
