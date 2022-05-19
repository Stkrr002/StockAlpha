package com.plcoding.stockmarketapp.presentation.company_listings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plcoding.stockmarketapp.domain.repository.StockRepository
import com.plcoding.stockmarketapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompanyListingsViewModel @Inject constructor(
    private val repository: StockRepository
): ViewModel() {

     var state by mutableStateOf(CompanyListingState())

    private var searchJob: Job? = null
    init {
        getCompanyListing()
    }
     fun OnEvent(event: CompanyListingEvent){
         when(event){
             is CompanyListingEvent.Refresh -> {
                 getCompanyListing(fetchFromRemote = true)
             }
             is CompanyListingEvent.OnsearchQueryChange -> {
                 state = state.copy(searchQuery = event.query)
                 searchJob?.cancel()
                 searchJob = viewModelScope.launch {
                     delay(500L)
                     getCompanyListing()
                 }
             }
         }
     }
     fun getCompanyListing(
         query: String = state.searchQuery.lowercase(),
         fetchFromRemote: Boolean = false
     ){
         viewModelScope.launch {
             repository
                 .getCompanyListing(fetchFromRemote , query )
                 .collect { result ->
                     when(result) {
                         is Resource.Success -> {
                             result.data?.let { listing ->
                                 state = state.copy(
                                     companies = listing
                                 )
                             }
                         }
                         is Resource.Error -> Unit
                         is Resource.Loading -> {
                             state = state.copy(isLoading = result.isLoading)
                         }
                     }

                 }
         }

     }


}