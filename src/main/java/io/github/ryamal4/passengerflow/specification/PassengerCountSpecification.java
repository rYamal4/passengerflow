package io.github.ryamal4.passengerflow.specification;

import io.github.ryamal4.passengerflow.model.PassengerCount;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class PassengerCountSpecification {

    public static Specification<PassengerCount> hasBusId(Long busId) {
        return (root, query, cb) -> {
            if (busId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("bus").get("id"), busId);
        };
    }

    public static Specification<PassengerCount> hasStopId(Long stopId) {
        return (root, query, cb) -> {
            if (stopId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("stop").get("id"), stopId);
        };
    }

    public static Specification<PassengerCount> hasTimestampAfter(LocalDateTime startTime) {
        return (root, query, cb) -> {
            if (startTime == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("timestamp"), startTime);
        };
    }

    public static Specification<PassengerCount> hasTimestampBefore(LocalDateTime endTime) {
        return (root, query, cb) -> {
            if (endTime == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("timestamp"), endTime);
        };
    }

    public static Specification<PassengerCount> withFilters(Long busId, Long stopId,
                                                            LocalDateTime startTime, LocalDateTime endTime) {
        return hasBusId(busId)
                .and(hasStopId(stopId))
                .and(hasTimestampAfter(startTime))
                .and(hasTimestampBefore(endTime));
    }

    private PassengerCountSpecification() {
    }
}
