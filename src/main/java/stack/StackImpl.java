package stack;

import kotlinx.atomicfu.AtomicRef;
import java.util.ArrayList;

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    private enum Status {EMPTY, WAITING, POPPED, PREPOPPED, PREPUSHED}

    private static class EliminationArray {
        final int capacity;
        final AtomicRef<Integer> empty;
        ArrayList<Integer> array;
        ArrayList<AtomicRef<Status>> statusArray;

        EliminationArray(int size) {
            capacity = size;
            empty = new AtomicRef<Integer>(Integer.MIN_VALUE);
            array = new ArrayList<Integer>();
            statusArray = new ArrayList<AtomicRef<Status>>();
            for (int i = 0; i < capacity; i++) {
                array.add(0);
                statusArray.add(new AtomicRef<Status>(Status.EMPTY));
            }
        }
    }

    private AtomicRef<Node> head = new AtomicRef<>(null);
    private EliminationArray eliminationArray = new EliminationArray(16);

    @Override
    public void push(int x) {
        int index = (int) (Math.random() * eliminationArray.capacity);
        int pushIndex = eliminationArray.empty.getValue();
        if (eliminationArray.statusArray.get(index).compareAndSet(Status.EMPTY, Status.PREPUSHED)) {
            eliminationArray.array.set(index, x);
            pushIndex = index;
        } else if (index != 0 && eliminationArray.statusArray.get(index - 1).compareAndSet(Status.EMPTY, Status.PREPUSHED)) {
            eliminationArray.array.set(index - 1, x);
            pushIndex = index - 1;
        } else if (index != eliminationArray.capacity - 1 && eliminationArray.statusArray.get(index + 1).compareAndSet(Status.EMPTY, Status.PREPUSHED)) {
            eliminationArray.array.set(index + 1, x);
            pushIndex = index + 1;
        }
        if (pushIndex != eliminationArray.empty.getValue()) {
            eliminationArray.statusArray.get(pushIndex).compareAndSet(Status.PREPUSHED, Status.WAITING);
            final long t = System.nanoTime() + 1;
            while (System.nanoTime() < t) {
                if (eliminationArray.statusArray.get(pushIndex).compareAndSet(Status.POPPED, Status.EMPTY))
                    return;
            }
            if (!eliminationArray.statusArray.get(pushIndex).compareAndSet(Status.WAITING, Status.PREPUSHED)) {
                eliminationArray.statusArray.get(pushIndex).compareAndSet(Status.POPPED, Status.EMPTY);
                return;
            }
        }
        while (true) {
            Node h = head.getValue();
            if ((pushIndex == eliminationArray.empty.getValue() && head.compareAndSet(h, new Node(x, h))) ||
                    (pushIndex != eliminationArray.empty.getValue() && head.compareAndSet(h, new Node(x, h)) && eliminationArray.statusArray.get(pushIndex).compareAndSet(Status.PREPUSHED, Status.EMPTY))) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        int index = (int) (Math.random() * eliminationArray.capacity);
        int popResult = eliminationArray.empty.getValue();
        int popIndex = eliminationArray.empty.getValue();
        if (eliminationArray.statusArray.get(index).compareAndSet(Status.WAITING, Status.PREPOPPED)) {
            popResult = eliminationArray.array.get(index);
            popIndex = index;
        } else if (index != 0 && eliminationArray.statusArray.get(index - 1).compareAndSet(Status.WAITING, Status.PREPOPPED)) {
            popResult = eliminationArray.array.get(index - 1);
            popIndex = index - 1;
        } else if (index != eliminationArray.capacity - 1 && eliminationArray.statusArray.get(index + 1).compareAndSet(Status.WAITING, Status.PREPOPPED)) {
            popResult = eliminationArray.array.get(index + 1);
            popIndex = index + 1;
        }
        if (popIndex != eliminationArray.empty.getValue()) {
            eliminationArray.statusArray.get(popIndex).compareAndSet(Status.PREPOPPED, Status.POPPED);
            return popResult;
        }
        while (true) {
            Node h = head.getValue();
            if (h == null)
                return Integer.MIN_VALUE;
            if (head.compareAndSet(h, h.next.getValue())) {
                return h.x;
            }
        }
    }
}