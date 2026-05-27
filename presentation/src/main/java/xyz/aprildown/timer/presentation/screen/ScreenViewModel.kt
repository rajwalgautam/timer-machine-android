package xyz.aprildown.timer.presentation.screen

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.github.deweyreed.tools.arch.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.presentation.SimpleViewModel
import xyz.aprildown.timer.presentation.StreamMachineIntentProvider
import xyz.aprildown.timer.presentation.stream.MachineContract
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.TimerMachineListener
import xyz.aprildown.timer.presentation.stream.getStep
import javax.inject.Inject

@HiltViewModel
class ScreenViewModel @Inject constructor(
    private val streamMachineIntentProvider: StreamMachineIntentProvider
) : SimpleViewModel(), TimerMachineListener {

    val timerCurrentTime = MutableLiveData<Long>().apply { value = 0L }
    val timerStepInfo = MutableLiveData<String>()

    private val _step: MutableLiveData<StepEntity.Step?> = MutableLiveData()
    val step: LiveData<StepEntity.Step?> = _step.distinctUntilChanged()

    private val _stopEvent = MutableLiveData<Event<Unit>>()
    val stopEvent: LiveData<Event<Unit>> = _stopEvent
    private val _intentEvent = MutableLiveData<Event<Intent>>()
    val intentEvent: LiveData<Event<Intent>> = _intentEvent

    private var presenter: MachineContract.Presenter? = null

    private var timerId = TimerEntity.NULL_ID

    fun setTimerId(timerId: Int) {
        this.timerId = timerId
    }

    fun setPresenter(presenter: MachineContract.Presenter) {
        this.presenter = presenter
        this.presenter?.addListener(timerId, this)
        loadRunningTimers()
    }

    private fun loadRunningTimers() {
        presenter?.getTimerStateInfo(timerId)?.run {
            if (timerEntity.id == timerId && !state.isReset) {
                timerCurrentTime.value = time
                val currentStep = timerEntity.getStep(index)
                _step.value = currentStep
                timerStepInfo.value = currentStep?.label.toString()
            } else {
                _stopEvent.value = Event(Unit)
            }
        }
    }

    fun onAddOneMinute() {
        _intentEvent.value = Event(
            streamMachineIntentProvider.adjustTimeIntent(timerId, 60_000)
        )
    }

    fun onStop() {
        _intentEvent.value = Event(streamMachineIntentProvider.increIntent(timerId))
        _stopEvent.value = Event(Unit)
    }

    fun dropPresenter() {
        presenter?.removeListener(timerId, this)
        presenter = null
    }

    //
    // Timer Callbacks
    //

    override fun begin(timerId: Int) = Unit

    override fun started(timerId: Int, index: TimerIndex) = Unit

    override fun paused(timerId: Int) = Unit

    override fun updated(timerId: Int, time: Long) {
        timerCurrentTime.value = time
    }

    override fun finished(timerId: Int) {
        _stopEvent.value = Event(Unit)
    }

    override fun end(timerId: Int, forced: Boolean) = Unit
}
