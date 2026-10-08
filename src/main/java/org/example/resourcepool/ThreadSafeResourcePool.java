package org.example.resourcepool;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ThreadSafeResourcePool<T> implements ResourcePool<T> {

    private final int maxPoolSize;
    private final int initialPoolSize;
    private final ResourceFactory<T> resourceFactory;

    private final Queue<T> availableResources = new ConcurrentLinkedQueue<>();
    private final Set<T> inUseResources = ConcurrentHashMap.newKeySet();
    private final Set<T> registeredResources = ConcurrentHashMap.newKeySet();

    private enum InitializationState { UNINITIALIZED, INITIALIZING, INITIALIZED }

    private final AtomicReference<InitializationState> initializationState =
            new AtomicReference<>(InitializationState.UNINITIALIZED);

    // Capacity includes factory calls in progress; total counts only completed creations.
    private final AtomicInteger allocatedCount = new AtomicInteger(0);
    private final AtomicInteger totalResourceCount = new AtomicInteger(0);

    public ThreadSafeResourcePool(
            int maxPoolSize,
            int initialPoolSize,
            ResourceFactory<T> resourceFactory
    ) {
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException(
                    "maxPoolSize must be positive"
            );
        }

        if (initialPoolSize < 0 || initialPoolSize > maxPoolSize) {
            throw new IllegalArgumentException(
                    "Invalid initialPoolSize"
            );
        }

        this.maxPoolSize = maxPoolSize;
        this.initialPoolSize = initialPoolSize;
        this.resourceFactory = Objects.requireNonNull(resourceFactory, "resourceFactory");
    }

    /** A failed initialization rolls back pool state and may be retried. */
    @Override
    public void initializePool() {
        if (!initializationState.compareAndSet(
                InitializationState.UNINITIALIZED, InitializationState.INITIALIZING)) {
            throw new IllegalStateException(
                    "Pool is already initialized or initialization is in progress"
            );
        }
        boolean success = false;
        try {
            List<T> resources = new ArrayList<>(initialPoolSize);
            allocatedCount.set(initialPoolSize);
            for (int i = 0; i < initialPoolSize; i++) {
                resources.add(createReservedResource());
            }
            availableResources.addAll(resources);
            success = true;
        } finally {
            if (success) {
                initializationState.set(InitializationState.INITIALIZED);
            } else {
                availableResources.clear();
                registeredResources.clear();
                allocatedCount.set(0);
                totalResourceCount.set(0);
                initializationState.set(InitializationState.UNINITIALIZED);
            }
        }
    }

    /**
     * Requires completed initialization. Fails immediately when no resource or
     * creation slot is available, including while other factory calls are in progress.
     */
    @Override
    public T acquire() {
        if (initializationState.get() != InitializationState.INITIALIZED) {
            throw new IllegalStateException("Pool initialization has not completed");
        }
        T resource = availableResources.poll();

        if (resource != null) {
            inUseResources.add(resource);
            return resource;
        }
        while (true) {
            int allocated = allocatedCount.get();
            if (allocated >= maxPoolSize) {
                resource = availableResources.poll();
                if (resource != null) {
                    inUseResources.add(resource);
                    return resource;
                } else {
                    throw new ResourceIsNotAvailable("Resource is not available");
                }
            }
            if (allocatedCount.compareAndSet(allocated, allocated + 1)) {
                break;
            }
        }
        resource = createReservedResource();
        inUseResources.add(resource);
        return resource;
    }

    private T createReservedResource() {
        boolean success = false;
        try {
            T resource = Objects.requireNonNull(
                    resourceFactory.get(), "Resource factory returned null");
            if (!registeredResources.add(resource)) {
                throw new IllegalStateException("Resource factory returned an existing resource");
            }
            totalResourceCount.incrementAndGet();
            success = true;
            return resource;
        } finally {
            if (!success) {
                allocatedCount.decrementAndGet();
            }
        }
    }

    @Override
    public void release(T resource) {
        if (resource == null || !inUseResources.remove(resource)) {
            throw new IllegalArgumentException(
                    "Resource is null or not in use"
            );
        }

        availableResources.offer(resource);
    }

    /** Returns an approximate count during concurrent acquisition and release. */
    @Override
    public int getAvailableResourceCount() {
        return availableResources.size();
    }

    /** Counts successfully created resources, excluding factory calls in progress. */
    @Override
    public int getTotalResourceCount() {
        return totalResourceCount.get();
    }

    /** Returns an approximate count during concurrent acquisition and release. */
    @Override
    public int getInUseResourceCount() {
        return inUseResources.size();
    }

    @Override
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
}