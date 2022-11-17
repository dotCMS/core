package com.dotmarketing.image.filter;

import java.util.concurrent.Semaphore;

class ImageGeneratorSemaphore {

    private Semaphore semaphore;

    public ImageGeneratorSemaphore(int slotLimit) {
        semaphore = new Semaphore(slotLimit);
    }

    boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    void release() {
        semaphore.release();
    }

}

