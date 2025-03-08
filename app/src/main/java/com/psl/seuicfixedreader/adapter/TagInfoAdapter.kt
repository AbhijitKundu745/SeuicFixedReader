package com.psl.seuicfixedreader.adapter

import android.graphics.Color
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.psl.seuicfixedreader.R
import com.psl.seuicfixedreader.bean.TagBean
import com.seuic.uhfandroid.ext.totalCounts

class TagInfoAdapter(layoutResId: Int) : BaseQuickAdapter<TagBean, BaseViewHolder>(layoutResId) {

    var mPosition = -1

    override fun convert(holder: BaseViewHolder, item: TagBean) {
        holder.apply {
            val pos = adapterPosition + 1
            setText(R.id.tv_serial_number, pos.toString())
            setText(R.id.tv_epc, item.epcId)
            setText(R.id.tv_rssi, item.rssi.toString())
            setText(R.id.tv_times, totalCounts.incrementAndGet().toString())
            setText(R.id.tv_reader_antenna, item.antenna)
            //setText(R.id.tv_additional_data, item.additionalData)
            setText(R.id.tv_additional_data, item.tagType + " "+item.addedDateTime)
        }
    }

    fun setPosition(position: Int) {
        mPosition = position
    }
}