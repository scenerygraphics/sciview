package sc.iview.ui

import graphics.scenery.utils.LazyLogger
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.JLabel

class TaskManager(var update: ((Task?) -> Any)? = null) {
    val currentTasks = CopyOnWriteArrayList<Task>()
    val pie = ProgressPie()
    val label = JLabel()
    val logger by LazyLogger()

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