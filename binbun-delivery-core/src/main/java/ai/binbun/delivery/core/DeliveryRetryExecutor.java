package ai.binbun.delivery.core;

import ai.binbun.delivery.model.DeliveryJob;
import ai.binbun.delivery.model.DeliveryJobRepository;
import ai.binbun.delivery.model.DeliveryJobStatus;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DeliveryRetryExecutor implements AutoCloseable {
    private final DeliveryJobRepository jobRepository;
    private final ConnectorRegistry connectorRegistry;
    private final DeliveryRetryPolicy retryPolicy;
    private final DeliveryFailureHandler failureHandler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    public DeliveryRetryExecutor(DeliveryJobRepository jobRepository,
                                 ConnectorRegistry connectorRegistry,
                                 DeliveryRetryPolicy retryPolicy,
                                 DeliveryFailureHandler failureHandler) {
        this.jobRepository = jobRepository;
        this.connectorRegistry = connectorRegistry;
        this.retryPolicy = retryPolicy;
        this.failureHandler = failureHandler;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            workerThread = Thread.ofVirtual().name("delivery-retry-executor").start(this::runLoop);
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    private void runLoop() {
        while (running.get()) {
            try {
                processPendingJobs();
            } catch (Exception e) {
                // Log and continue
            }
            try {
                Thread.sleep(retryPolicy.baseDelayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processPendingJobs() {
        List<DeliveryJob> pending = jobRepository.listPending();
        for (DeliveryJob job : pending) {
            if (!running.get()) break;
            processJob(job);
        }
    }

    private void processJob(DeliveryJob job) {
        if (job.retryCount() >= retryPolicy.maxAttempts()) {
            jobRepository.save(new DeliveryJob(
                    job.connector(), job.idempotencyKey(), job.sessionId(),
                    job.destination(), job.payload(), DeliveryJobStatus.DEAD_LETTER,
                    job.retryCount(), "max retries exceeded", job.providerMessageId(),
                    job.createdAt(), Instant.now()
            ));
            return;
        }

        var connector = connectorRegistry.find(job.connector());
        if (connector.isEmpty()) {
            jobRepository.save(new DeliveryJob(
                    job.connector(), job.idempotencyKey(), job.sessionId(),
                    job.destination(), job.payload(), DeliveryJobStatus.FAILED,
                    job.retryCount(), "connector not found: " + job.connector(), job.providerMessageId(),
                    job.createdAt(), Instant.now()
            ));
            return;
        }

        try {
            var receipt = connector.get().send(new OutboundMessage(
                    job.idempotencyKey(), job.sessionId(), job.destination(),
                    job.payload(), java.util.Map.of(), null
            ));
            jobRepository.save(new DeliveryJob(
                    job.connector(), job.idempotencyKey(), job.sessionId(),
                    job.destination(), job.payload(), DeliveryJobStatus.SENT,
                    job.retryCount(), null, receipt.providerMessageId(),
                    job.createdAt(), Instant.now()
            ));
        } catch (Exception e) {
            int newRetryCount = job.retryCount() + 1;
            jobRepository.save(new DeliveryJob(
                    job.connector(), job.idempotencyKey(), job.sessionId(),
                    job.destination(), job.payload(), DeliveryJobStatus.FAILED,
                    newRetryCount, e.getMessage(), job.providerMessageId(),
                    job.createdAt(), Instant.now()
            ));
        }
    }
}
