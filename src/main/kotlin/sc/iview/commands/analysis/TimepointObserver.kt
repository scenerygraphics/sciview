package sc.iview.commands.analysis

/**
 * Interface to allow subscription to timepoint updates, especially for updating sciview contents
 * after a user triggered a timepoint change via controller input.
 */
interface TimepointObserver {

    /**
     * Called when the timepoint was updated.
     */
    fun onTimePointChanged(timepoint: Int)
}