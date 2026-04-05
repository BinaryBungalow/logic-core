package ai.binbun.delivery.core;

import ai.binbun.delivery.model.DeliveryJobStatus;
import ai.binbun.delivery.model.JsonDeliveryDeadLetterRepository;
import ai.binbun.delivery.model.JsonDeliveryJobRepository;

public final class DeliveryRuntimeStatusService {
    private final JsonDeliveryJobRepository jobs;
    private final JsonDeliveryDeadLetterRepository deadLetters;

    public DeliveryRuntimeStatusService(JsonDeliveryJobRepository jobs, JsonDeliveryDeadLetterRepository deadLetters) {
        this.jobs = jobs;
        this.deadLetters = deadLetters;
    }

    public DeliveryRuntimeStatus snapshot(boolean retryEnabled) {
        var all = jobs.list();
        int pending = (int) all.stream().filter(job -> job.status() == DeliveryJobStatus.PENDING).count();
        int sent = (int) all.stream().filter(job -> job.status() == DeliveryJobStatus.SENT).count();
        int failed = (int) all.stream().filter(job -> job.status() == DeliveryJobStatus.FAILED).count();
        return new DeliveryRuntimeStatus(all.size(), pending, sent, failed, deadLetters.list().size(), retryEnabled);
    }
}
