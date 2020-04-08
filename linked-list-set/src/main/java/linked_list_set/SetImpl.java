package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {

    private class Removed extends Node {
        Removed(int x, Node next) {
            super(x, next);
        }
    }

    private class Node {
        int x;
        AtomicRef<Node> next;

        Node(int x, Node next) {
            this.x = x;
            this.next = new AtomicRef<>(next);
        }
    }

    private class Window {
        Node cur;
        Node next;
    }

    private final AtomicRef<Node> head = new AtomicRef<Node>(new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null)));

    private boolean isRemoved(Node node) {
        return node.next.getValue() instanceof Removed;
    }

    private Window findWindow(int x) {
        while (true) {
            Window w = new Window();
            w.cur = head.getValue();
            w.next = w.cur.next.getValue();
            while (w.next.x < x || isRemoved(w.next)) {
                if (isRemoved(w.cur)) break;
                if (isRemoved(w.next)) {
                    Node node = w.next.next.getValue().next.getValue();
                    if (!w.cur.next.compareAndSet(w.next, node)) {
                        w.next = w.cur.next.getValue();
                        continue;
                    }
                    w.next = node;
                } else {
                    w.cur = w.next;
                    w.next = w.cur.next.getValue();
                }
            }
            if (isRemoved(w.cur)) continue;
            return w;
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x == x) return false;
            if (w.cur.next.compareAndSet(w.next, new Node(x, w.next))) return true;
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x + 1);
            if (w.cur.x != x) return false;
            if (w.cur.next.compareAndSet(w.next, new Removed(Integer.MIN_VALUE, w.next))) return true;
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.x == x;
    }
}