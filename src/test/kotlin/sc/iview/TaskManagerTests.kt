package sc.iview

import org.junit.Test
import sc.iview.ui.Task
import sc.iview.ui.TaskManager
import kotlin.test.assertTrue

class TaskManagerTests {

    @Test
    fun addTaskTest() {
        val taskManager = TaskManager()
        val task = Task("source", "testing", 1f)
        taskManager.addTask(task)
        assertTrue{taskManager.currentTasks.contains(task)}
    }

    @Test
    fun removeTask() {
        val taskManager = TaskManager()
        val task = Task("source", "testing", 1f)
        taskManager.addTask(task)
        taskManager.removeTask(task)
        assertTrue{!taskManager.currentTasks.contains(task)}
    }

}