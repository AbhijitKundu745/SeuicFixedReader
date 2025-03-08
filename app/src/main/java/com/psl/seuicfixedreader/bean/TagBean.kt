package com.psl.seuicfixedreader.bean

data class TagBean( var batchId:String,
                    var epcId:String,
                    var rssi:Int,
                    var times:Int,
                    var antenna:String,
                    var additionalData:String,
                    var tagType:String,
                    var addedDateTime: String)
