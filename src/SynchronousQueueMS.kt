import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    enum class Status { OK, RETRY }
    enum class NodeType { SENDER, RECEIVER }

    inner class Node<T>(
        val value: AtomicReference<T?> = AtomicReference(null),
        val next: AtomicReference<Node<T>?> = AtomicReference(null),
        val type: NodeType,
        var continuation: Continuation<Status>? = null
    ) {
        fun isSender() = this.type == NodeType.SENDER
        fun isReceiver() = this.type == NodeType.RECEIVER
    }

    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy: Node<E> = Node(type = NodeType.SENDER)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override suspend fun send(element: E) {
        val node: Node<E> = Node(value = AtomicReference(element), type = NodeType.SENDER)

        loop@ while (true) {
            val h: Node<E> = head.get()
            val t: Node<E> = tail.get()

            if (h == t || t.isSender()) {
                when (coroutineResult(node, t)) {
                    Status.RETRY -> continue@loop
                    Status.OK -> return
                }
            } else {
                val hNext: Node<E> = h.next.get() ?: continue
                if (t != tail.get() || h != head.get() || h == t) {
                    continue
                }

                if (hNext.continuation != null && head.compareAndSet(h, hNext)) {
                    hNext.value.compareAndSet(null, element)
                    hNext.continuation!!.resume(Status.OK)
                    return
                }
            }
        }
    }

    override suspend fun receive(): E {
        val node: Node<E> = Node(type = NodeType.RECEIVER)

        loop@ while (true) {
            val h: Node<E> = head.get()
            val t: Node<E> = tail.get()

            if (h == t || t.isReceiver()) {
                when (coroutineResult(node, t)) {
                    Status.RETRY -> continue@loop
                    Status.OK -> return node.value.get()!!
                }
            } else {
                val hNext: Node<E> = h.next.get() ?: continue
                if (t != tail.get() || h != head.get() || h == t) {
                    continue
                }

                val value = hNext.value.get() ?: continue
                if (hNext.continuation != null && head.compareAndSet(h, hNext)) {
                    hNext.value.compareAndSet(value, null)
                    hNext.continuation!!.resume(Status.OK)
                    return value
                }
            }
        }
    }

    suspend fun coroutineResult(node: Node<E>, t: Node<E>): Status {
        return suspendCoroutine<Status> sc@{
            node.continuation = it
            if (t.next.compareAndSet(null, node)) {
                tail.compareAndSet(t, node)
            } else {
                it.resume(Status.RETRY)
                return@sc
            }
        }
    }
}