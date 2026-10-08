package org.example.resourcepool;

import org.junit.jupiter.api.Test;

    import static org.junit.jupiter.api.Assertions.*;

class SynchronizeResourcePoolTest {

    @Test
    void initializePool() {
        SynchronizeResourcePool<String> pool = new SynchronizeResourcePool<>(10, 5, () -> "resource");
        pool.initializePool();
        assertEquals(5, pool.getAvailableResourceCount());
        pool.acquire();
        assertThrows(IllegalStateException.class, pool::initializePool);
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