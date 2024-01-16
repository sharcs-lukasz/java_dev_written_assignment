package org.example.DeadlineEngine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimpleNotSynchronizedDeadlineEngineTest {

    @Test
    void simpleUsageTest() {
        SimpleNotSynchronizedDeadlineEngine engine = new SimpleNotSynchronizedDeadlineEngine();
        assertNotNull(engine);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(0, engine.size());
        engine.poll(1, null, 1);
        engine.poll(1, id -> System.out.println("this should not be triggered, id: " + id), 1);
        assertEquals(0, engine.size());

        long id1 = engine.schedule(60L);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(1, engine.size());
        engine.poll(59, id -> System.out.println("triggered schedule with id " + id), 1);
        assertEquals(1, engine.size());

        long id2 = engine.schedule(90L);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(2, engine.size());
        engine.poll(89, id -> System.out.println("triggered schedule with id " + id), 1);
        assertEquals(1, engine.size());

        long id3 = engine.schedule(110L);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(2, engine.size());

        engine.cancel(id2);
        assertEquals(1, engine.size());

        long id4 = engine.schedule(120L);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(2, engine.size());

        engine.poll(120, id -> System.out.println("triggered schedule with id " + id), 3);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(0, engine.size());

        long id5 = engine.schedule(150L);
        long id6 = engine.schedule(151L);
        long id7 = engine.schedule(152L);
        long id8 = engine.schedule(153L);
        long id9 = engine.schedule(154L);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(5, engine.size());

        engine.poll(155, id -> System.out.println("triggered schedule with id " + id), 3);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(2, engine.size());

        engine.poll(155, id -> System.out.println("triggered schedule with id " + id), 3);
        System.out.println("engine [" + engine.size() + "]: " + engine);
        assertEquals(0, engine.size());
    }
}