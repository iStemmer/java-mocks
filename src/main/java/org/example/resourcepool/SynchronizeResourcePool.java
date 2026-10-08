package org.example.resourcepool;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class SynchronizeResourcePool<T> implements ResourcePool<T> {

    private final int maxPoolSize;
    private final int initialPoolSize;
    private final ResourceFactory<T> resourceFactory;

    private final Queue<T> availableResources = new LinkedList<>();
    private final Set<T> inUseResources = new HashSet<>();

    private boolean initialized;

    public SynchronizeResourcePool(
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
        this.resourceFactory = resourceFactory;
    }

    @Override
    public synchronized void initializePool() {
        if (initialized) {
            throw new IllegalStateException(
                    "Pool is already initialized"
            );
        }

        for (int i = 0; i < initialPoolSize; i++) {
            availableResources.offer(resourceFactory.get());
        }

        initialized = true;
    }

    @Override
    public synchronized T acquire() {
        T resource = availableResources.poll();

        if (resource != null) {
            inUseResources.add(resource);
            return resource;
        }

        if (inUseResources.size() >= maxPoolSize) {
            throw new ResourceIsNotAvailable("Resource is not available");
        }

        resource = resourceFactory.get();
        inUseResources.add(resource);

        return resource;
    }

    @Override
    public synchronized void release(T resource) {
        if (resource == null || !inUseResources.remove(resource)) {
            throw new IllegalArgumentException(
                    "Resource is null or not in use"
            );
        }

        availableResources.offer(resource);
    }

    @Override
    public synchronized int getAvailableResourceCount() {
        return availableResources.size();
    }

    @Override
    public synchronized int getTotalResourceCount() {
        return availableResources.size()
                + inUseResources.size();
    }

    @Override
    public synchronized int getInUseResourceCount() {
        return inUseResources.size();
    }

    @Override
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
}