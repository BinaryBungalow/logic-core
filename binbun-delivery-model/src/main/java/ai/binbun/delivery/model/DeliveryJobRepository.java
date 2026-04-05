package ai.binbun.delivery.model;

import java.util.List;
import java.util.Optional;

public interface DeliveryJobRepository {
    void save(DeliveryJob job);
    Optional<DeliveryJob> findByIdempotencyKey(String connector, String idempotencyKey);
    List<DeliveryJob> list();
    List<DeliveryJob> listPending();
}
