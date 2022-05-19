package com.plcoding.stockmarketapp.presentation.company_listings

sealed class CompanyListingEvent{
    object Refresh: CompanyListingEvent()
    data class OnsearchQueryChange(val query: String): CompanyListingEvent()
}
