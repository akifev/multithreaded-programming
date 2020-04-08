public class Solution implements AtomicCounter {

    private final Node head = new Node(0);
    private final ThreadLocal<Node> tail =
            ThreadLocal.withInitial(() -> head);

    private static class Node {
        final int value;
        final Consensus<Node> next = new Consensus<>();

        Node(int value) {
            this.value = value;
        }
    }

    public int getAndAdd(int x) {
        while (true) {
            final int oldValue = tail.get().value;
            final int newValue = oldValue + x;
            final Node newNode = new Node(newValue);
            tail.set(tail.get().next.decide(newNode));
            if (tail.get() == newNode) {
                return oldValue;
            }
        }
    }
}
