package stack;

import kotlinx.atomicfu.AtomicInt;
import kotlinx.atomicfu.AtomicIntArray;
import kotlinx.atomicfu.AtomicLong;
import kotlinx.atomicfu.AtomicRef;

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    private static class EliminationArray {
        final int capacity;
        final int empty;
        AtomicIntArray eliminationArray;

        EliminationArray(int size) {
            capacity = size;
            empty = Integer.MIN_VALUE;
            eliminationArray = new AtomicIntArray(capacity);
            for (int i = 0; i < capacity; i++) {
                eliminationArray.get(i).setValue(empty);
            }
        }

        int tryPush(int index, int value) {
            if (eliminationArray.get(index).compareAndSet(empty, value)) return index;
            if (index != 0 && eliminationArray.get(index - 1).compareAndSet(empty, value)) return index - 1;
            if (index != capacity - 1 && eliminationArray.get(index + 1).compareAndSet(empty, value)) return index + 1;
            return -1;
        }

        int tryExchange(int index) {
            int atom = eliminationArray.get(index).getValue();
            if (atom != empty && eliminationArray.get(index).compareAndSet(atom, empty)) return atom;
            if (index != 0) {
                atom = eliminationArray.get(index - 1).getValue();
                if (atom != empty && eliminationArray.get(index - 1).compareAndSet(atom, empty)) return atom;
            }
            if (index != capacity - 1) {
                atom = eliminationArray.get(index + 1).getValue();
                if (index != capacity - 1 && atom != empty && eliminationArray.get(index + 1).compareAndSet(atom, empty))
                    return atom;
            }
            return empty;
        }
    }

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);
    private EliminationArray eliminationArray = new EliminationArray(4);

    @Override
    public void push(int x) {
        int index = (int) (Math.random() * eliminationArray.capacity);
        int pushIndex = eliminationArray.tryPush(index, x);
        if (pushIndex != -1) {
            long end = System.currentTimeMillis() + 10;
            while (System.currentTimeMillis() < end) {
                if (eliminationArray.eliminationArray.get(pushIndex).compareAndSet(eliminationArray.empty, eliminationArray.empty))
                    return;
            }
//            eliminationArray.eliminationArray.get(pushIndex).setValue(eliminationArray.empty);
            if (!eliminationArray.eliminationArray.get(pushIndex).compareAndSet(x, eliminationArray.empty))
                return;
        }
        while (true) {
//            System.out.println("push");

            Node h = head.getValue();
            if (head.compareAndSet(h, new Node(x, h))) {
                break;
            }
        }
        //head.setValue(new Node(x, head.getValue()));
    }

    @Override
    public int pop() {
        int index = (int) (Math.random() * eliminationArray.capacity);
        int popResult = eliminationArray.tryExchange(index);
        if (popResult != eliminationArray.empty)
            return popResult;
        while (true) {
//            System.out.println("pop");
            Node h = head.getValue();
            if (h == null)
                return Integer.MIN_VALUE;
            if (head.compareAndSet(h, h.next.getValue())) {
                return h.x;
            }
        }
    }
}
