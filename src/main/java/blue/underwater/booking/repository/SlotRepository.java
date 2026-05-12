package blue.underwater.booking.repository;

import blue.underwater.booking.model.Slot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    boolean existsByDateTime(LocalDateTime dateTime);

    List<Slot> findByBookedFalseOrderByDateTimeAsc();

    List<Slot> findAllByOrderByDateTimeAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :id")
    Optional<Slot> findByIdForUpdate(UUID id);
}
