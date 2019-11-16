import java.util.concurrent.atomic.AtomicReference;

public class Solution implements Lock<Solution.Node> {
    private final Environment env;
    private final AtomicReference<Node> last;

    public Solution(Environment env) {
        this.env = env;
        last = new AtomicReference<>(null);
    }

    @Override
    public Node lock() {
        Node cur = new Node();
        Node prev = last.getAndSet(cur);
        if (prev != null) {
            prev.next.set(cur);
            while (cur.locked.get()) {
                env.park();
            }
        }

        return cur;
    }

    @Override
    public void unlock(Node node) {
        if (node.next.get() == null) {
            if (last.compareAndSet(node, null)) {
                return;
            }
            while (node.next.get() == null) {
                // Nothing to do here
            }
        }
        node.next.get().locked.set(false);
        env.unpark(node.next.get().thread);
    }

    static class Node {
        final Thread thread = Thread.currentThread();
        final AtomicReference<Boolean> locked = new AtomicReference<>(true);
        final AtomicReference<Node> next = new AtomicReference<>(null);
    }
}
