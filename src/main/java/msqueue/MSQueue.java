package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private Node head;
    private Node tail;

    private enum Status {CHANGING, VALID}

    private AtomicRef<Status> headStatus;
    private AtomicRef<Status> tailStatus;

    public MSQueue() {
        Node dummy = new Node(0);
        this.head = dummy;
        this.tail = dummy;
        this.headStatus = new AtomicRef<>(Status.VALID);
        this.tailStatus = new AtomicRef<>(Status.VALID);
    }

    @Override
    public void enqueue(int x) {
        while (true) {
            if (tailStatus.compareAndSet(Status.VALID, Status.CHANGING)) {
                Node newTail = new Node(x);
                tail.next = newTail;
                tail = newTail;
                tailStatus.compareAndSet(Status.CHANGING, Status.VALID);
                return;
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            if (headStatus.compareAndSet(Status.VALID, Status.CHANGING)) {
                while (true) {
                    if (tailStatus.compareAndSet(Status.VALID, Status.CHANGING)) {
                        int result = Integer.MIN_VALUE;
                        Node curHead = head;
                        if (curHead != tail) {
                            Node next = head.next;
                            head = next;
                            result = next.x;
                        }
                        tailStatus.compareAndSet(Status.CHANGING, Status.VALID);
                        headStatus.compareAndSet(Status.CHANGING, Status.VALID);
                        return result;
                    }
                }
            }
        }
    }

    @Override
    public int peek() {
        while (true) {
            if (headStatus.compareAndSet(Status.VALID, Status.CHANGING)) {
                while (true) {
                    if (tailStatus.compareAndSet(Status.VALID, Status.CHANGING)) {
                        Node curHead = head;
                        int result = Integer.MIN_VALUE;
                        if (curHead != tail) {
                            Node next = head.next;
                            result = next.x;
                        }
                        tailStatus.compareAndSet(Status.CHANGING, Status.VALID);
                        headStatus.compareAndSet(Status.CHANGING, Status.VALID);
                        return result;
                    }
                }
            }
        }
    }

    private class Node {
        final int x;
        Node next;

        Node(int x) {
            this.x = x;
        }
    }
}