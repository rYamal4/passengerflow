package io.github.ryamal4.passengerflow.service.passenger;

import io.github.ryamal4.passengerflow.model.PassengerCount;
import io.github.ryamal4.passengerflow.repository.IPassengerCountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassengerCountServiceTest {

    @Mock
    private IPassengerCountRepository passengerCountRepository;

    @InjectMocks
    private PassengerCountService passengerCountService;

    @Test
    void testCreateCountSuccess() {
        PassengerCount inputCount = new PassengerCount();
        inputCount.setEntered(10);
        inputCount.setExited(5);

        PassengerCount savedCount = new PassengerCount();
        savedCount.setId(1L);
        savedCount.setEntered(10);
        savedCount.setExited(5);

        when(passengerCountRepository.save(inputCount)).thenReturn(savedCount);

        PassengerCount result = passengerCountService.createCount(inputCount);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEntered()).isEqualTo(10);
        assertThat(result.getExited()).isEqualTo(5);
    }
}