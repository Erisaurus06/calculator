package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculationRepository(private val calculationDao: CalculationDao) {
    val allHistory: Flow<List<Calculation>> = calculationDao.getAllHistory()

    suspend fun insert(calculation: Calculation) {
        calculationDao.insertCalculation(calculation)
    }

    suspend fun delete(calculation: Calculation) {
        calculationDao.deleteCalculation(calculation.id)
    }

    suspend fun clearAll() {
        calculationDao.clearHistory()
    }
}
