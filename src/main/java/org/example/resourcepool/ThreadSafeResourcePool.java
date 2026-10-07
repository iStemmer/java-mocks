package org.example.resourcepool;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadSafeResourcePool<T> {
    private final int maxPoolSize;
    private final int initialPoolSize;
    private final ResourceFactory<T> resourceFactory;
    //I can work with 2 structures here
    //1 Queue of the available resources
    //2 Set of the inUse resources
    private final Queue<T> availableResources = new LinkedList<>();
    private final Set<T> inUseResources = new HashSet<>();
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    public ThreadSafeResourcePool(int maxPoolSize, int initialPoolSize, ResourceFactory<T> resourceFactory) {
        this.maxPoolSize = maxPoolSize;
        this.initialPoolSize = initialPoolSize;
        this.resourceFactory = resourceFactory;
    }

    public void initializePool() {
        if (!isInitialized.get()) {
            for (int i = 0; i < initialPoolSize; i++) {
                T resource = resourceFactory.get();
                availableResources.offer(resource);
            }
            isInitialized.set(true);
        } else {
            throw new IllegalStateException("Pool is already initialized");
        }
    }

    public T acquire() {
        if(availableResources.isEmpty() && inUseResources.size() < maxPoolSize) {
            T newResource = resourceFactory.get();
            inUseResources.add(newResource);
            return newResource;
        }
        T resource = availableResources.poll();
        if (resource != null) {
            inUseResources.add(resource);
        }
        return resource;
    }

    public void release(T resource) {
        if(resource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }
        if (inUseResources.remove(resource)) {
            availableResources.offer(resource);
        }
    }

    public int getAvailableResourceCount() {
        return availableResources.size();
    }

    public int getTotalResourceCount() {
        return availableResources.size() + inUseResources.size();
    }

    public int getInUseResourceCount() {
        return inUseResources.size();
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

}
