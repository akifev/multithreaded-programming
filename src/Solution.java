import java.util.concurrent.atomic.*;

public class Solution implements Lock<Solution.Node> {
    private final Environment env;
    public Solution(Environment env) {
        this.env = env;
    }

    @Override
    public Node lock() {
        Node my = new Node(); // сделали узел
        while (my.locked.get()) env.park();
        return my; // вернули узел
    }

    @Override
    public void unlock(Node node) {
        node.locked.set(false);
        env.unpark(node.thread);
    }

    static class Node {
        final Thread thread = Thread.currentThread(); // запоминаем поток, которые создал узел
        final AtomicReference<Boolean> locked = new AtomicReference<>(true);
    }
}
