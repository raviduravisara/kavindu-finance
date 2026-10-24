package com.fintrack.kavindu.feature.freelance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.kavindu.core.data.repository.FreelanceRepository
import com.fintrack.kavindu.core.model.AppError
import com.fintrack.kavindu.core.model.FreelanceProject
import com.fintrack.kavindu.core.model.Milestone
import com.fintrack.kavindu.core.model.MilestoneStatus
import com.fintrack.kavindu.core.model.asAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProjectRow(
    val id: String,
    val clientName: String,
    val projectName: String,
    val pending: String?, // e.g. "LKR 85,000"; null when nothing is owed
    val overdueCount: Int,
    val receivedCount: Int,
    val milestoneCount: Int,
)

data class FreelanceProjectsUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val rows: List<ProjectRow> = emptyList(),
    val totalPending: String? = null,
    val totalOverdue: Int = 0,
    // Add project dialog
    val isAddDialogVisible: Boolean = false,
    val clientInput: String = "",
    val projectInput: String = "",
    val clientError: FreelanceFieldError? = null,
    val projectError: FreelanceFieldError? = null,
    val isSaving: Boolean = false,
    val saveError: AppError? = null,
)

/**
 * Freelance projects (S1): every client project with what it still owes and how many invoices are overdue.
 * Reads from Room only, so it works offline and updates live.
 */
@HiltViewModel
class FreelanceProjectsViewModel @Inject constructor(
    private val freelanceRepository: FreelanceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FreelanceProjectsUiState())
    val uiState: StateFlow<FreelanceProjectsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            freelanceRepository.observeProjectsWithMilestones()
                .catch { error -> _uiState.update { it.copy(isLoading = false, loadError = error.asAppError()) } }
                .collect { projects ->
                    val today = LocalDate.now()
                    val all = projects.flatMap { it.second }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            rows = projects.map { (project, milestones) -> project.toRow(milestones, today) },
                            totalPending = FreelanceRules.formatPending(FreelanceRules.pendingByCurrency(all)),
                            totalOverdue = all.count { m -> FreelanceRules.isOverdue(m, today) },
                        )
                    }
                }
        }
    }

    fun onRetryClicked() = load()

    fun onAddProjectClicked() = _uiState.update {
        it.copy(isAddDialogVisible = true, clientInput = "", projectInput = "", clientError = null, projectError = null, saveError = null)
    }

    fun onAddProjectDismissed() = _uiState.update { it.copy(isAddDialogVisible = false) }

    fun onClientChange(text: String) = _uiState.update { it.copy(clientInput = text, clientError = null) }

    fun onProjectChange(text: String) = _uiState.update { it.copy(projectInput = text, projectError = null) }

    fun onSaveProjectClicked() {
        val state = _uiState.value
        if (state.isSaving) return
        val check = FreelanceRules.validateProject(state.clientInput, state.projectInput)
        if (!check.isValid) {
            _uiState.update { it.copy(clientError = check.clientError, projectError = check.projectError) }
            return
        }
        val project = FreelanceProject(
            id = "", // blank id = new document
            clientName = check.clientName,
            projectName = check.projectName,
            createdAt = System.currentTimeMillis(),
        )
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val result = freelanceRepository.saveProject(project)
            _uiState.update {
                if (result.isSuccess) it.copy(isSaving = false, isAddDialogVisible = false)
                else it.copy(isSaving = false, saveError = result.exceptionOrNull()?.asAppError())
            }
        }
    }

    private fun FreelanceProject.toRow(milestones: List<Milestone>, today: LocalDate) = ProjectRow(
        id = id,
        clientName = clientName,
        projectName = projectName,
        pending = FreelanceRules.formatPending(FreelanceRules.pendingByCurrency(milestones)),
        overdueCount = milestones.count { FreelanceRules.isOverdue(it, today) },
        receivedCount = milestones.count { it.status == MilestoneStatus.RECEIVED },
        milestoneCount = milestones.count { it.status != MilestoneStatus.CANCELLED },
    )
}

/**
 * Every project with its milestones, as one live list. The repository gives milestones per project,
 * so this joins one Flow per project. Also used by the Income tab for the "pending · not counted" line.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun FreelanceRepository.observeProjectsWithMilestones(): Flow<List<Pair<FreelanceProject, List<Milestone>>>> =
    observeProjects().flatMapLatest { projects ->
        if (projects.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(projects.map { project -> observeMilestones(project.id).map { project to it } }) { it.toList() }
        }
    }
