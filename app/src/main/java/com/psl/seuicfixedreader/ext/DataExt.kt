package com.seuic.uhfandroid.ext

import androidx.lifecycle.MutableLiveData
import com.psl.seuicfixedreader.bean.TagBean
import java.util.concurrent.atomic.AtomicInteger

fun String.getTagType(): String {
    if (this.length > 4) {
        when (substring(2, 4)) {
            "02" -> return typePallet
            "03" -> return typeBean
            "04" -> return typeTemporaryStorage
            "05" -> return typeLoadingArea
        }
    }
    return typeOther
}

const val typePallet: String = "PalletTag"
const val typeBean: String = "Bintag"
const val typeTemporaryStorage: String = "TS"
const val typeLoadingArea: String = "LA"
const val typeOther: String = "NA"

fun String.getCategoryID(): String {
    if (this.length > 4) {
        when (substring(2, 4)) {
            "02" -> return categoryPallet
            "03" -> return categoryBean
            "04" -> return categoryTemporaryStorage
            "05" -> return categoryLoadingArea
        }
    }
    return categoryOther
}
const val categoryPallet: String = "2"
const val categoryBean: String = "3"
const val categoryTemporaryStorage: String = "4"
const val categoryLoadingArea: String = "5"
const val categoryOther: String = "0"
var inventoryDatas = MutableLiveData<TagBean>()

var inventoryListDatas = MutableLiveData<MutableList<TagBean>>()

// 当前盘点天线
var currentAntennaArray = intArrayOf()

var connectResult = MutableLiveData<Int>()

// 清除标签列表
public var clearTagList = MutableLiveData<Boolean>()

// 当前选中的标签
var currentTag = ""

// 重置当前选中标签和标签列表的下标
var resetCurrentTag = MutableLiveData<Boolean>()

// 正在寻卡时不可以进行设置等操作
var isSearching = false

var totalCounts: AtomicInteger = AtomicInteger(0)