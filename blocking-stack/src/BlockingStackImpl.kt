import java.util.concurrent.atomic.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BlockingStackImpl<E> : BlockingStack<E> {

    // ==========================
    // Segment Queue Synchronizer
    // ==========================

    private val queueHead: AtomicReference<Receiver<E>>
    private val queueTail: AtomicReference<Receiver<E>>

    init {
        val queueDummy = Receiver<E>()
        queueHead = AtomicReference(queueDummy)
        queueTail = AtomicReference(queueDummy)
    }

    private suspend fun suspend(): E {
        return suspendCoroutine sc@{ cont ->
            val node = Receiver(cont)

            while (true) {
                val tail = queueTail.get()
                if (tail.next.compareAndSet(null, node)) {
                    queueTail.compareAndSet(tail, node)
                    break
                }
            }
        }
    }

    private fun resume(element: E) {
        while (true) {
            val head = queueHead.get()
            val tail = queueTail.get()
            if (head != queueHead.get() || tail != queueTail.get() || head == tail) {
                continue
            }

            val node = head.next.get() ?: continue
            if (node.continuation != null && queueHead.compareAndSet(head, node)) {
                node.continuation.resume(element)
                return
            }
        }
    }

    private class Receiver<E>(
        val continuation: Continuation<E>? = null,
        val next: AtomicReference<Receiver<E>?> = AtomicReference(null)
    )

    // ==============
    // Blocking Stack
    // ==============


    private val stackHead = AtomicReference<Node<E>?>()
    private val elements = AtomicInteger()

    override fun push(element: E) {
        val elements = this.elements.getAndIncrement()
        if (elements >= 0) {
            // push the element to the top of the stack
            while (true) {
                val head = stackHead.get()
                if (head == null) {
                    if (stackHead.compareAndSet(null, Node(element))) return
                } else {
                    if (head.element != SUSPENDED) {
                        if (stackHead.compareAndSet(head, Node(element, AtomicReference(head)))) return
                    } else {
                        val node = head.next.get()
                        if (stackHead.compareAndSet(head, node)) {
                            resume(element)
                            return
                        }
                    }
                }
            }
        } else {
            // resume the next waiting receiver
            resume(element)
        }
    }

    override suspend fun pop(): E {
        val elements = this.elements.getAndDecrement()
        if (elements > 0) {
            // remove the top element from the stack
            while (true) {
                val head = stackHead.get()

                if (head == null) {
                    if (stackHead.compareAndSet(null, Node(SUSPENDED))) return suspend()
                } else {
                    val node = head.next.get()
                    if (stackHead.compareAndSet(head, node)) return head.element as E
                }
            }
        } else {
            return suspend()
        }
    }
}

private class Node<E>(val element: Any? = null, val next: AtomicReference<Node<E>?> = AtomicReference(null))

private val SUSPENDED = Any()