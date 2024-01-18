/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2024 sciview developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.iview.ui

import graphics.scenery.utils.lazyLogger
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JLabel

class TaskManager(var update: ((Task?) -> Any)? = null) {
    val currentTasks = CopyOnWriteArrayList<Task>()
    val pie = ProgressPie()
    val label = JLabel()
    val logger by lazyLogger()

    init {

        val timerTask = object: TimerTask() {
            override fun run() {
                currentTasks.removeIf { it.completion > 99.9999f }
                val current = currentTasks.lastOrNull()

                update?.invoke(current)
            }
        }
        Timer().scheduleAtFixedRate(timerTask, 0L, 200L)
    }

    fun addTask(task: Task) {
        currentTasks.add(task)
    }

    fun newTask(source: String, status: String = ""): Task {
        val task = Task(source, status, 0.0f)
        currentTasks.add(task)
        return task
    }

    fun removeTask(task: Task) {
        currentTasks.remove(task)
    }
}
