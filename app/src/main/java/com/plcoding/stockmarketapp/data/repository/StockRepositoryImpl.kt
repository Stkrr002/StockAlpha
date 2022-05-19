package com.plcoding.stockmarketapp.data.repository

import com.plcoding.stockmarketapp.data.csv.CSVParser
import com.plcoding.stockmarketapp.data.csv.CompanyListingParser
import com.plcoding.stockmarketapp.data.local.StockDatabase
import com.plcoding.stockmarketapp.data.mappers.toCompanyInfo
import com.plcoding.stockmarketapp.data.mappers.toCompanyListing
import com.plcoding.stockmarketapp.data.mappers.toCompanyListingEntity
import com.plcoding.stockmarketapp.data.remote.StockApi
import com.plcoding.stockmarketapp.domain.model.CompanyInfo
import com.plcoding.stockmarketapp.domain.model.CompanyListing
import com.plcoding.stockmarketapp.domain.model.IntradayInfo
import com.plcoding.stockmarketapp.domain.repository.StockRepository
import com.plcoding.stockmarketapp.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val api: StockApi,
   private  val db: StockDatabase,
    private val companyListingParser: CSVParser<CompanyListing>,
    private val intradayInfoParser: CSVParser<IntradayInfo>
):StockRepository {

    private val dao= db.dao

    override suspend fun getCompanyListing(
        fetchFromRemote: Boolean,
        query: String
    ): Flow<Resource<List<CompanyListing>>> {
        return flow {
             emit(Resource.Loading(true))
            val localListing = dao.searchCompanyListing(query)
            emit(Resource.Success(
                data = localListing.map { it.toCompanyListing() }
            ))
            val isDbEmpty = localListing.isEmpty() && query.isBlank()
            val shouldJustLoadFromCache = !isDbEmpty && !fetchFromRemote
            if (shouldJustLoadFromCache) {
                emit(Resource.Loading(false))
                return@flow
            }
            val remoteListings = try {
                val response = api.getListing()
                companyListingParser.parse(response.byteStream())
            } catch (e: IOException) {
                e.printStackTrace()
                emit(Resource.Error("could not load data"))
                null
            } catch (e: HttpException) {
                emit(Resource.Error("could not load data"))
                null
            }
            remoteListings?.let { listing ->
                dao.clearCompanyListing()
                dao.insertCompanyListing(
                    listing.map { it.toCompanyListingEntity() }
                )
                emit(Resource.Success(
                    data = dao
                        .searchCompanyListing("")
                        .map { it.toCompanyListing() }
                ))
                emit( Resource.Loading(false))
            }
        }

    }

    override suspend fun getIntradayInfo(symbol: String): Resource<List<IntradayInfo>> {
        return try {
            val response = api.getIntradayInfo(symbol)
            val results = intradayInfoParser.parse(response.byteStream())
            Resource.Success(results)
        }catch (e:IOException){
            e.printStackTrace()
            Resource.Error(
                message = "couldn't load intraday info"
            )
        }catch (e: HttpException){
            e.printStackTrace()
            Resource.Error(
                message = "couldn't load intraday info"
            )
        }
    }

    override suspend fun getCompanyInfo(symbols: String): Resource<CompanyInfo> {
        return try {
            val results = api.getCompanyInfo(symbols)
            Resource.Success(results.toCompanyInfo())
        }catch (e:IOException){
            e.printStackTrace()
            Resource.Error(
                message = "couldn't load intraday info"
            )
        }catch (e: HttpException){
            e.printStackTrace()
            Resource.Error(
                message = "couldn't load intraday info"
            )
        }
    }
}



