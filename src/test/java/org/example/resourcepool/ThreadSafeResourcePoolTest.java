package org.example.resourcepool;

import org.junit.jupiter.api.Test;

    import static org.junit.jupiter.api.Assertions.*;

class ThreadSafeResourcePoolTest {

    @Test
    void initializePool() {
        ThreadSafeResourcePool<String> pool = new ThreadSafeResourcePool<>(10, 5, () -> "resource");
        pool.initializePool();
        assertEquals(5, pool.getAvailableResourceCount());
        pool.acquire();
        pool.initializePool();
        //it should not be changed because the pool is already initialized
        assertEquals(4, pool.getAvailableResourceCount());
    }

    @Test
    void acquire() {
    }

    @Test
    void release() {
    }

    @Test
    void getAvailableResourceCount() {
    }

    @Test
    void getTotalResourceCount() {
    }

    @Test
    void getInUseResourceCount() {
    }

    @Test
    void getMaxPoolSize() {
    }
}