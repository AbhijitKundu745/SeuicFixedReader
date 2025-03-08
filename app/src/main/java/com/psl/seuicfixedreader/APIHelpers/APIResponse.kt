package com.psl.seuicfixedreader.APIHelpers

data class APIResponse<T>(
    val status: Boolean,
    val message: String,
    val data: T? = null
)
