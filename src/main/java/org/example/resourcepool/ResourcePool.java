package org.example.resourcepool;

public interface ResourcePool<T> {
    void initializePool();

    T acquire();

    void release(T resource);

    int getAvailableResourceCount();

    int getTotalResourceCount();

    int getInUseResourceCount();

    int getMaxPoolSize();
}
