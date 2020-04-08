package faaqueue;

import kotlinx.atomicfu.AtomicArray;
import kotlinx.atomicfu.AtomicInt;
import kotlinx.atomicfu.AtomicRef;

import static faaqueue.FAAQueue.Segment.SEGMENT_SIZE;

public class FAAQueue<T> implements Queue<T> {
    private static final Object DONE = new Object();

    private AtomicRef<Segment> head;
    private AtomicRef<Segment> tail;

    public FAAQueue() {
        Segment firstSegment = new Segment();
        head = new AtomicRef<>(firstSegment);
        tail = new AtomicRef<>(firstSegment);
    }

    @Override
    public void enqueue(T x) {
        while (true) {
            Segment last = this.tail.getValue();
            int enqIdx = last.enqIdx.getAndIncrement();
            if (enqIdx >= SEGMENT_SIZE) {
                Segment newTail = new Segment(x);
                if (last.next.compareAndSet(null, newTail)) {
                    this.tail.compareAndSet(last, newTail);
                    return;
                }
                this.tail.compareAndSet(last, last.next.getValue());
            } else {
                if (last.data.get(enqIdx).compareAndSet(null, x)) {
                    return;
                }
            }
        }
    }

    @Override
    public T dequeue() {
        while (true) {
            Segment first = this.head.getValue();
            Segment firstNext = first.next.getValue();
            if (first.isEmpty()) {
                if (firstNext == null) {
                    return null;
                }
                this.head.compareAndSet(first, firstNext);
                continue;
            }
            int deqIdx = first.deqIdx.getAndIncrement();
            if (deqIdx >= SEGMENT_SIZE) {
                continue;
            }
            Object res = first.data.get(deqIdx).getAndSet(DONE);
            if (res == null) {
                continue;
            }
            return (T) res;
        }
    }

    static class Segment {
        static final int SEGMENT_SIZE = 2;
        private final AtomicArray<Object> data = new AtomicArray<>(SEGMENT_SIZE);
        private AtomicRef<Segment> next = new AtomicRef<>(null);
        private AtomicInt enqIdx = new AtomicInt(0);
        private AtomicInt deqIdx = new AtomicInt(0);

        Segment() {
        }

        Segment(Object x) {
            this.enqIdx.setValue(1);
            this.data.get(0).setValue(x);
        }

        private boolean isEmpty() {
            return this.deqIdx.getValue() >= this.enqIdx.getValue() || this.deqIdx.getValue() >= SEGMENT_SIZE;
        }
    }
}