/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3

import android.util.Log
import okhttp3.RealCall.AsyncCall
import okhttp3.internal.threadFactory
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Policy on when async requests are executed.
 *
 * Each dispatcher uses an [ExecutorService] to run calls internally. If you supply your own
 * executor, it should be able to run [the configured maximum][maxRequests] number of calls
 * concurrently.
 */
class Dispatcher constructor() {
    /**
     * The maximum number of requests to execute concurrently. Above this requests queue in memory,
     * waiting for the running calls to complete.
     *
     * If more than [maxRequests] requests are in flight when this is invoked, those requests will
     * remain in flight.
     */
    // TODO,sth unkown  :   synchronized修饰普通方法，Kotlin对应的是@Synchronized注解类。synchronized修饰静态方法，Kotlin对应的是@Synchronized注解类。
    // TODO,sth unkown  :   此处实际上是声明一个变量，以及附带的set方法。
    @get:Synchronized
    var maxRequests = 64
        set(maxRequests) {
            require(maxRequests >= 1) { "max < 1: $maxRequests" }
            synchronized(this) {
                field = maxRequests
            }
            promoteAndExecute()
        }

    /**
     * The maximum number of requests for each host to execute concurrently. This limits requests by
     * the URL's host name. Note that concurrent requests to a single IP address may still exceed this
     * limit: multiple hostnames may share an IP address or be routed through the same HTTP proxy.
     *
     * If more than [maxRequestsPerHost] requests are in flight when this is invoked, those requests
     * will remain in flight.
     *
     * WebSocket connections to hosts **do not** count against this limit.
     */
    @get:Synchronized
    var maxRequestsPerHost = 5
        set(maxRequestsPerHost) {
            require(maxRequestsPerHost >= 1) { "max < 1: $maxRequestsPerHost" }
            synchronized(this) {
                field = maxRequestsPerHost
            }
            promoteAndExecute()
        }

    /**
     * A callback to be invoked each time the dispatcher becomes idle (when the number of running
     * calls returns to zero).
     *
     * Note: The time at which a [call][Call] is considered idle is different depending on whether it
     * was run [asynchronously][Call.enqueue] or [synchronously][Call.execute]. Asynchronous calls
     * become idle after the [onResponse][Callback.onResponse] or [onFailure][Callback.onFailure]
     * callback has returned. Synchronous calls become idle once [execute()][Call.execute] returns.
     * This means that if you are doing synchronous calls the network layer will not truly be idle
     * until every returned [Response] has been closed.
     */
    @set:Synchronized
    @get:Synchronized
    var idleCallback: Runnable? = null

    private var executorServiceOrNull: ExecutorService? = null

    @get:Synchronized
    @get:JvmName("executorService")
    // TODO,sth unkown  :  JvmName,一句话就是对要调用的类的名字和方法的名字进行重命名。
    val executorService: ExecutorService
        get() {
            if (executorServiceOrNull == null) {
                // TODO,sth unkown  : 这样设计成不设上限的线程，以保证I/O任务中高阻塞低占用的过程，不会长时间卡在阻塞上。
                executorServiceOrNull = ThreadPoolExecutor(
                    0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
                    // TODO,sth unkown  :  SynchronousQueue,线程等待队列。同步队列，插入数据的线程和获取数据的线程，交替执行
                    // TODO,sth unkown  :  没有存储功能，因此put和take会一直阻塞，直到有另一个线程已经准备好参与到交付过程中。仅当有足够多的消费者，并且总是有一个消费者准备好获取交付的工作时，才适合使用同步队列。
                    // TODO,sth unkown  : SynchronousQueue每个插入操作必须等待另一个线程的移除操作，同样任何一个移除操作都等待另一个线程的插入操作。因此队列内部其实没有任何一个元素，或者说容量为0，严格说并不是一种容器，
                    //  由于队列没有容量，因此不能调用peek等操作，因此只有移除元素才有元素，显然这是一种快速传递元素的方式，也就是说在这种情况下元素总是以最快的方式从插入者(生产者)传递给移除者(消费者),
                    //  这在多任务队列中最快的处理任务方式。对于高频请求场景，无疑是最合适的。
                    SynchronousQueue(), threadFactory("OkHttp Dispatcher", false)
                )
            }
            return executorServiceOrNull!!
        }

    /** Ready async calls in the order they'll be run. */
    private val readyAsyncCalls = ArrayDeque<AsyncCall>()

    /** Running asynchronous calls. Includes canceled calls that haven't finished yet. */
    private val runningAsyncCalls = ArrayDeque<AsyncCall>()

    /** Running synchronous calls. Includes canceled calls that haven't finished yet. */
    // TODO,sth unkown in kotlin :   不是很熟悉的数据结构 ArrayDeque
    private val runningSyncCalls = ArrayDeque<RealCall>()

    constructor(executorService: ExecutorService) : this() {
        this.executorServiceOrNull = executorService
    }

    internal fun enqueue(call: AsyncCall) {
        Log.e("test zjs", "Dispatcher enqueue")
        synchronized(this) {
            readyAsyncCalls.add(call)

            // Mutate the AsyncCall so that it shares the AtomicInteger of an existing running call to
            // the same host.
            if (!call.get().forWebSocket) {
                val existingCall = findExistingCallWithHost(call.host())
                if (existingCall != null) call.reuseCallsPerHostFrom(existingCall)
            }
        }
        promoteAndExecute()
    }

    private fun findExistingCallWithHost(host: String): AsyncCall? {
        for (existingCall in runningAsyncCalls) {
            if (existingCall.host() == host) return existingCall
        }
        for (existingCall in readyAsyncCalls) {
            if (existingCall.host() == host) return existingCall
        }
        return null
    }

    /**
     * Cancel all calls currently enqueued or executing. Includes calls executed both
     * [synchronously][Call.execute] and [asynchronously][Call.enqueue].
     */
    @Synchronized
    fun cancelAll() {
        for (call in readyAsyncCalls) {
            call.get().cancel()
        }
        for (call in runningAsyncCalls) {
            call.get().cancel()
        }
        for (call in runningSyncCalls) {
            call.cancel()
        }
    }

    /**
     * Promotes eligible calls from [readyAsyncCalls] to [runningAsyncCalls] and runs them on the
     * executor service. Must not be called with synchronization because executing calls can call
     * into user code.
     *
     * @return true if the dispatcher is currently running calls.
     */
    private fun  promoteAndExecute(): Boolean {
        Log.e("test zjs", "Dispatcher promoteAndExecute")
        assert(!Thread.holdsLock(this))

        val executableCalls = mutableListOf<AsyncCall>()
        val isRunning: Boolean
        synchronized(this) {
            val i = readyAsyncCalls.iterator()
            while (i.hasNext()) {
                val asyncCall = i.next()

                if (runningAsyncCalls.size >= this.maxRequests) break // Max capacity.
                if (asyncCall.callsPerHost().get() >= this.maxRequestsPerHost) continue // Host max capacity.

                i.remove()
                asyncCall.callsPerHost().incrementAndGet()
                executableCalls.add(asyncCall)
                runningAsyncCalls.add(asyncCall)
            }
            isRunning = runningCallsCount() > 0
        }

        for (i in 0 until executableCalls.size) {
            val asyncCall = executableCalls[i]
            asyncCall.executeOn(executorService)
        }

        return isRunning
    }

    /** Used by `Call#execute` to signal it is in-flight. */
    @Synchronized
    internal fun executed(call: RealCall) {
        runningSyncCalls.add(call)
    }

    /** Used by `AsyncCall#run` to signal completion. */
    // TODO,sth unkown  : 它会在相同模块内随处可见；
    internal fun finished(call: AsyncCall) {
        call.callsPerHost().decrementAndGet()
        finished(runningAsyncCalls, call)
    }

    /** Used by `Call#execute` to signal completion. */
    internal fun finished(call: RealCall) {
        finished(runningSyncCalls, call)
    }

    private fun <T> finished(calls: Deque<T>, call: T) {
        val idleCallback: Runnable?
        synchronized(this) {
            if (!calls.remove(call)) throw AssertionError("Call wasn't in-flight!")
            idleCallback = this.idleCallback
        }

        val isRunning = promoteAndExecute()

        if (!isRunning && idleCallback != null) {
            idleCallback.run()
        }
    }

    /** Returns a snapshot of the calls currently awaiting execution. */
    @Synchronized
    fun queuedCalls(): List<Call> {
        return Collections.unmodifiableList(readyAsyncCalls.map { it.get() })
    }

    /** Returns a snapshot of the calls currently being executed. */
    @Synchronized
    fun runningCalls(): List<Call> {
        return Collections.unmodifiableList(runningSyncCalls + runningAsyncCalls.map { it.get() })
    }

    @Synchronized
    fun queuedCallsCount(): Int = readyAsyncCalls.size

    @Synchronized
    fun runningCallsCount(): Int = runningAsyncCalls.size + runningSyncCalls.size

    @JvmName("-deprecated_executorService")
    @Deprecated(
        message = "moved to val",
        replaceWith = ReplaceWith(expression = "executorService"),
        level = DeprecationLevel.ERROR
    )
    fun executorService(): ExecutorService = executorService
}
