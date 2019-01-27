package de.halfbit.interstate

import de.halfbit.interstate.GalleryRepository.Command
import de.halfbit.interstate.GalleryRepository.State
import de.halfbit.interstate.GalleryTemplateRepository.GalleryTemplate
import de.halfbit.interstate.dsl.statechart
import io.reactivex.Observable

interface GalleryTemplateRepository {
    interface GalleryTemplate

    val state: Observable<GalleryTemplate>
}

interface Gallery

interface GalleryRepository {

    sealed class State {
        data class NotReady(
            val hasPermissions: Boolean = false,
            val template: GalleryTemplate? = null
        ) : State()

        sealed class Ready : State() {
            data class Scanning(val template: GalleryTemplate, val gallery: Gallery?) : Ready()
            data class NoMedia(val template: GalleryTemplate, val gallery: Gallery) : Ready()
            data class Media(val template: GalleryTemplate, val gallery: Gallery) : Ready()
        }
    }

    sealed class Command {
        object Scan : Command()
    }
}

class DefaultGalleryRepository(
    private val galleryTemplateRepository: GalleryTemplateRepository
) : GalleryRepository {

    sealed class Action {
        sealed class Scanner : Action() {
            object Start : Scanner()
            object Stop : Scanner()
        }
    }

    private val statechart = statechart<State, Command> {

        state<State.NotReady> {
            on<Command.Scan> { checkPermissions() }
            on(galleryTemplateRepository.state) { refreshGalleryTemplate() }
        }

        state<State.Ready.NoMedia> {
            on<Command.Scan> { transition(currentState.toScanning()) }
            //on<Command.Scan> { scan() }
        }

        state<State.Ready.Media> {
            on<Command.Scan> { transition(currentState.toScanning()) }
            //on<Command.Scan> { scan() }
        }

        state<State.Ready.Scanning> {
            //on<StateEvent> { startStopScanner() }
            //entry { scan() }
            //exit { command.accept() }
        }

        //onSubscribe { State.NotReady }

        //on<Command.Scan> { scan() }
        //on(galleryTemplateRepository.currentState) { updateGallery() }
        //onSubscribed { post(Command.Scan) }
    }

    private fun EventReducer<GalleryTemplate, State.NotReady>.refreshGalleryTemplate(): Observable<Command> =
        event
            .map<State.NotReady> { currentState.copy(template = it) }
            .map { Command.Scan }

    private fun CommandReducer<Command.Scan, State.NotReady>.checkPermissions(): Observable<State> =
        TODO()

    private fun CommandReducer<Command.Scan, State.Ready.NoMedia>.scan(): Observable<State> =
        command
            .switchMap<State> {
                TODO("start scanner")
            }
            .map<State> {
                TODO("map results")
            }


    /*
    private fun EventReducer<GalleryTemplate, State>.updateGallery(): Observable<State> =
        event
            .filter { currentState is State.HasMedia }
            .switchMap<Command> {
                TODO("update template")
            }
            */

}

private fun State.Ready.NoMedia.toScanning() =
    State.Ready.Scanning(template, gallery)

private fun State.Ready.Media.toScanning() =
    State.Ready.Scanning(template, gallery)

private fun State.NotReady.isComplete(): Boolean =
    hasPermissions && template != null


