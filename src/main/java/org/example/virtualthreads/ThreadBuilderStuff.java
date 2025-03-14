package org.example.virtualthreads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class ThreadBuilderStuff {
    private static final Logger logger = LoggerFactory.getLogger(ThreadBuilderStuff.class);
    private static final Bathroom bathroom = new Bathroom();
    private static final BathroomWithLock bathroomWithLock = new BathroomWithLock();
    public static void main(String[] args) {
        twoEmployeesInTheOfficeWithLock();
    }

    static void log(String message) {
        System.out.println(Thread.currentThread() + " | " + message);
        logger.info("{} | {}", Thread.currentThread().threadId(), message);
    }

    private static void sleep(Duration duration) throws InterruptedException {
        Thread.sleep(duration);
    }

    private static void stackOverFlowErrorExample() {
        for (int i = 0; i < 100_000; i++) {
            System.out.printf("%d: ", i);
            new Thread(() -> {
                try {
                    System.out.println("Starting: " + Thread.currentThread());
                    Thread.sleep(Duration.ofSeconds(300L));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private static Thread virtualThread(String name, Runnable runnable) {
        return Thread.ofVirtual().name(name).start(runnable);
    }

    static Thread bathTime() {
        return virtualThread("Bath Time", bathRunnable());
    }

    static Thread boilingWater() {
        return virtualThread("Boil some water", boilingWaterRunnable());
    }

    private static void concurrentMorningRoutine() throws InterruptedException {
        Thread bathTime = bathTime();
        Thread boilingWater = boilingWater();
        bathTime.join();
        boilingWater.join();
    }

    static void concurrentMorningRoutineUsingExecutorsWithName() {
        final ThreadFactory virtualThreadFactory = Thread.ofVirtual().name("routine-", 0).factory();
        try (ExecutorService executor = Executors.newThreadPerTaskExecutor(virtualThreadFactory)) {
            Future<?> bathTime = executor.submit(bathRunnable());
            Future<?> boilingWater = executor.submit(boilingWaterRunnable());
            bathTime.get();
            boilingWater.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Runnable bathRunnable() {
        return () -> {
            log("I'm going to take a bath");
            try {
                sleep(Duration.ofMillis(500L));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log("I'm done with a bath");
        };
    }

    private static Runnable boilingWaterRunnable() {
        return () -> {
            log("I'm going to boil some water");
            try {
                sleep(Duration.ofSeconds(1L));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log("I'm done with the water");
        };
    }

    static int numberOfCores() {
        return Runtime.getRuntime().availableProcessors();
    }
    private static void viewCarrierThreadPoolSize() {
        final ThreadFactory virtualThreadFactory = Thread.ofVirtual().name("routine-", 0).factory();
        try(ExecutorService executor = Executors.newThreadPerTaskExecutor(virtualThreadFactory)) {
            IntStream.range(0, numberOfCores()*10 + 1).forEach(i -> executor.submit(() -> {
                log("Hello, I'm a virtual thread number " + i);
                try {
                    sleep(Duration.ofSeconds(10L));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
        }
    }

    private static boolean alwaysTrue() {
        return true;
    }

    private static Thread workingHard() {
        return virtualThread("Working hard", () -> {
            log("I'm working hard");
            while(alwaysTrue()) {}
            try {
                sleep(Duration.ofMillis(100L));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log("I'm done working hard");
        });
    }

    private static Thread takeABreak() {
        return virtualThread(
                "Take a break",
                () -> {
                    log("I'm going to take a break");
                    try {
                        sleep(Duration.ofSeconds(1L));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log("I'm done with the break");
                });
    }

    private static void workingHardRoutine() {
        Thread workingHard = workingHard();
        Thread takeABreak = takeABreak();
        try {
            workingHard.join();
            takeABreak.join();
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static Thread workingConsciousness() {
        return virtualThread(
                "Working consciousness", () -> {
            log("I'm working hard");
            while (alwaysTrue()) {
                try {
                    sleep(Duration.ofMillis(100L));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            log("I'm done with working hard");
        });
    }

    static void workingConsciousnessRoutine() {
        var workingConsciousness = workingConsciousness();
        var takeABreak = takeABreak();
        try{
            workingConsciousness.join();
            takeABreak.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static class Bathroom {
        synchronized void useTheToilet() {
            log("I'm going to use the toilet");
            try {
                sleep(Duration.ofSeconds(1L));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log("I'm done with the toilet");
        }
    }

    static Thread goToTheToilet() {
        return virtualThread("Go to the toilet", () -> bathroom.useTheToilet());
    }

    static Thread goToTheToiletWithLock() {
        return virtualThread("Go to the toilet with lock", () -> bathroomWithLock.useTheToiletWithLock());
    }

    static void twoEmployeesInTheOffice() {
        Thread riccardo = goToTheToilet();
        Thread daniel = takeABreak();
        try {
            riccardo.join();
            daniel.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void twoEmployeesInTheOfficeWithLock() {
        Thread riccardo = goToTheToiletWithLock();
        Thread daniel = takeABreak();
        try {
            riccardo.join();
            daniel.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static class BathroomWithLock {
        private final Lock lock = new ReentrantLock();

        void useTheToiletWithLock() {
            try {
                if (lock.tryLock(10, TimeUnit.SECONDS)) {
                    try {
                        log("I'm going to use the toilet with lock");
                        sleep(Duration.ofSeconds(1L));
                        log("I'm done with the toilet with lock");
                    } finally {
                        lock.unlock();
                    }
                }
            } catch(InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
