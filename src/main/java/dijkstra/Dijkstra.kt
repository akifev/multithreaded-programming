package dijkstra

import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance = 0
    val q = MultiPriorityQueue(workers, NODE_DISTANCE_COMPARATOR)
    q.add(start)
    val onFinish = Phaser(workers + 1)
    val activeNodes = AtomicInteger(1)
    repeat(workers) {
        thread {
            while (true) {
                val cur: Node = q.poll() ?: if (activeNodes.get() > 0) continue else break
                for (e in cur.outgoingEdges) {
                    while (true) {
                        val curDistance = e.to.distance
                        val newDistance = cur.distance + e.weight
                        if (newDistance >= curDistance) {
                            break
                        }
                        if (e.to.casDistance(curDistance, newDistance)) {
                            q.add(e.to)
                            activeNodes.getAndIncrement()
                            break
                        }
                    }
                }
                activeNodes.getAndDecrement()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

class MultiPriorityQueue(workers: Int, nodeDistanceComparator: Comparator<Node>) {
    private val list = arrayListOf<PriorityQueue<Node>>()
    private val size: Int = workers * 2;

    init {
        for (i in 0 until size) {
            list.add(PriorityQueue(nodeDistanceComparator));
        }
    }

    fun add(node: Node) {
        val index: Int = (0 until size).random()
        synchronized(list[index]) {
            list[index].add(node)
        }
    }

    fun poll(): Node? {
        for (i in 0..3) {
            var first: Int = (0 until size).random()
            var second: Int = (0 until size).random()
            if (first == second) {
                continue;
            } else {
                if (first > second) {
                    first = second.also { second = first }
                }
            }
            synchronized(list[first]) {
                synchronized(list[second]) {
                    val firstMin: Node? = list[first].peek()
                    val secondMin: Node? = list[second].peek()
                    if (firstMin != null && secondMin != null) {
                        if (firstMin.distance < secondMin.distance) {
                            return list[first].poll()
                        } else {
                            return list[second].poll()
                        }
                    }
                }
            }
        }
        for (index in 0 until size) {
            synchronized(list[index]) {
                if (list[index].size > 0) {
                    return list[index].poll()
                }
            }
        }
        return null
    }
}
