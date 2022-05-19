package com.plcoding.stockmarketapp.presentation.company_listings

import com.plcoding.stockmarketapp.domain.model.CompanyListing

data class CompanyListingState(
    val companies: List<CompanyListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefereshing : Boolean = false,
    val searchQuery: String = ""
)
