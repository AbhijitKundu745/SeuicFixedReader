package com.psl.seuicfixedreader.APIHelpers

class dataModels {
    data class AuthRes(
        val PairedDeviceID: String,
        val Topic: List<TopicsList>
    )
    data class TopicsList(
        val Title: String,
        val TopicName: String
    )
}