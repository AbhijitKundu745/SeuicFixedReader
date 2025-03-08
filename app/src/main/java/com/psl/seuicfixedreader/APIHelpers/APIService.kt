package com.psl.seuicfixedreader.APIHelpers

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface APIService {
    @POST(APIConstant.M_AUTH)
    fun getAuthorization(@Body request: AuthRequest): Call<APIResponse<dataModels.AuthRes>>
}