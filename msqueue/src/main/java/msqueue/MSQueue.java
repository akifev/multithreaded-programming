package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0);
        head = new AtomicRef<Node>(dummy);
        tail = new AtomicRef<Node>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x);
        while (true) {
            Node last = tail.getValue();
            if (last.next.compareAndSet(null, newTail)) {
                tail.compareAndSet(last, newTail);
                return;
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            Node first = head.getValue();
            Node last = tail.getValue();
            Node firstNext = first.next.getValue();
            if (last == first)
                if (firstNext == null) return Integer.MIN_VALUE;
                else
                    tail.compareAndSet(last, firstNext);
            else if (head.compareAndSet(first, firstNext)) return firstNext.x;
        }
    }

    @Override
    public int peek() {
        while (true) {
            Node first = head.getValue();
            Node last = tail.getValue();
            Node firstNext = first.next.getValue();
            if (last == first)
                if (firstNext == null) return Integer.MIN_VALUE;
                else
                    tail.compareAndSet(last, firstNext);
            else if (head.compareAndSet(first, first)) return firstNext.x;
        }
    }

    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x) {
            this.x = x;
            next = new AtomicRef<Node>(null);
        }
    }
}