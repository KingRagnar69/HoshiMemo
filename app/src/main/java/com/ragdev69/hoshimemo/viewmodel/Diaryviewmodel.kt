package com.ragdev69.hoshimemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ragdev69.hoshimemo.data.local.DiaryEntity
import com.ragdev69.hoshimemo.data.local.HoshiDatabase
import com.ragdev69.hoshimemo.data.repository.DiaryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DiaryRepository(
        HoshiDatabase.getInstance(app).diaryDao()
    )

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Currently selected date
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Entry for selected date — auto-updates when date changes
    val currentEntry: StateFlow<DiaryEntity?> = _selectedDate
        .flatMapLatest { date -> repo.observeEntry(date.format(fmt)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dates that have entries (for calendar highlighting)
    private val _entryDates = MutableStateFlow<Set<String>>(emptySet())
    val entryDates: StateFlow<Set<String>> = _entryDates.asStateFlow()

    // Earliest date user wrote a diary (for calendar min bound)
    private val _earliestDate = MutableStateFlow<LocalDate?>(null)
    val earliestDate: StateFlow<LocalDate?> = _earliestDate.asStateFlow()

    init {
        loadMeta()
    }

    private fun loadMeta() {
        viewModelScope.launch {
            val keys = repo.getAllDateKeys()
            _entryDates.value = keys.toSet()

            val earliest = repo.getEarliestDateKey()
            _earliestDate.value = earliest?.let { LocalDate.parse(it, fmt) }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun saveEntry(diaryText: String, codeText: String) {
        viewModelScope.launch {
            val key = _selectedDate.value.format(fmt)
            repo.saveEntry(key, diaryText, codeText)
            loadMeta() // refresh entry dates + earliest
        }
    }
}