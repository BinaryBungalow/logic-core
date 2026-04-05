package ai.binbun.gateway.recovery;

import ai.binbun.delivery.model.DeliveryJobRepository;

public final class DeliveryRecoveryExecutor implements RecoveryExecutor {
    private final DeliveryJobRepository deliveryJobRepository;

    public DeliveryRecoveryExecutor(DeliveryJobRepository deliveryJobRepository) {
        this.deliveryJobRepository = deliveryJobRepository;
    }

    @Override
    public String subsystemName() {
        return "delivery";
    }

    @Override
    public RecoveryCheckpoint execute() {
        if (deliveryJobRepository == null) {
            return new RecoveryCheckpoint("delivery", "SKIPPED", "delivery repository not available");
        }
        try {
            var pending = deliveryJobRepository.listPending();
            return new RecoveryCheckpoint("delivery", "RECOVERED", "found " + pending.size() + " pending delivery job(s) for replay");
        } catch (Exception e) {
            return new RecoveryCheckpoint("delivery", "FAILED", "delivery recovery error: " + e.getMessage());
        }
    }
}
