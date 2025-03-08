package com.seuic.uhfandroid.util

import java.util.*
import kotlin.experimental.and

object ByteUtil {
    fun isOdd(num: Int): Int {
        return num and 0x1
    }

    fun hexToInt(inHex: String): Int {
        return inHex.toInt(16)
    }

    fun byteToInt(b: Byte): Int {
        return hexToInt(
            byte2Hex(
                b
            )
        )
    }

    //转换16进制字符串到10进制字符串
    fun hexToString(inHex: String?): String {
        return if (inHex == null || inHex.isEmpty()) "" else inHex.toLong(16).toString()
    }

    //转换10进制数字字符串到16进制字符串
    fun StringToHex(numberString: String): String {
        return hexLen(numberString.toInt())
    }

    //转换10进制数字字符串到16进制字符串
    fun StringToHexWord(numberString: String): String {
        val hex = hexLen(numberString.toInt())
        return if (hex.length == 2) {
            "00$hex"
        } else hex
    }

    //转换10进制数字到16进制字符串
    fun numberToHex(number: Any, length: Int): String {
        var hex = hexLen(number)
        while (hex.length < length) {
            hex = "00$hex"
        }
        return hex
    }

    fun hexToByte(inHex: String): Byte {
        return inHex.toInt(16).toByte()
    }

    fun byte2Hex(inByte: Byte?): String {
        return String.format("%02x", inByte).toUpperCase()
    }

    fun byteArrToHex(inBytArr: ByteArray?): String {
        val strBuilder = StringBuilder()
        if (inBytArr == null) return strBuilder.toString()

        val j = inBytArr.size
        for (i in 0 until j) {
            strBuilder.append(byte2Hex(inBytArr[i]))
        }
        return strBuilder.toString()
    }

    fun byteArrToHex(inBytArr: ByteArray?, offset: Int, byteCount: Int): String {
        val strBuilder = StringBuilder()
        if (inBytArr == null) return strBuilder.toString()
        if (inBytArr.size >= offset + byteCount) {
            for (i in offset until offset + byteCount) {
                strBuilder.append(byte2Hex(inBytArr[i]))
            }
        }
        return strBuilder.toString()
    }

    fun hexToByteArr(inHex: String?): ByteArray {
        var inHex = inHex ?: return byteArrayOf()
        var hexlen = inHex.length
        val result: ByteArray
        if (isOdd(hexlen) == 1) {
            hexlen++
            result = ByteArray(hexlen / 2)
            inHex = "0$inHex"
        } else {
            result = ByteArray(hexlen / 2)
        }
        var j = 0
        var i = 0
        while (i < hexlen) {
            result[j] = hexToByte(inHex.substring(i, i + 2))
            j++
            i += 2
        }
        return result
    }

    /**
     * get hex to ten Length
     */
    fun hexLen(len: Any): String {

        var re = when (len) {
            is Int -> len.toString(16).toUpperCase()
            is Long -> len.toString(16).toUpperCase()
            else -> ""
        }
        if (re.length % 2 != 0) {
            re = "0$re"
        }
        return re
    }

    /**
     * 截取byte数组   不改变原数组
     *
     * @param b      原数组
     * @param off    偏差值（索引）
     * @param length 长度
     * @return 截取后的数组
     */
    fun subByte(b: ByteArray?, off: Int, length: Int): ByteArray {
        return b?.copyOfRange(off, off + length) ?: byteArrayOf()
    }

    //sum check
    fun sumCheck(data: ByteArray): Byte {
        return data.sumBy { byte -> byteToInt(byte) }.toString(16).let {
            when (it.length) {
                1 -> return@let it.toInt(16).toByte()
                2 -> return@let it.toInt(16).toByte()
                else -> return@let it.substring(it.length - 2).toInt(16).toByte()
            }
        }
    }

    fun hexToDecimal(byte: Byte):String{
        return Integer.parseInt(byte2Hex(byte), 16).toString()
    }

    fun decimalToHex(ten: Int):String{
        return Integer.toHexString(ten)
    }

    //一个int值拆成两个高低位的字节
    fun chaiFenDataIntTo2Byte(data: Int): ByteArray {
        val byteArray = ByteArray(2)
        byteArray[0] = (data shr 8).toByte()
        byteArray[1] = data.toByte()

        return byteArray
    }

    //将两个高低位的字节换算成一个字节

    //复制赋值数组
    fun subBytes(src: ByteArray?, begin: Int, count: Int): ByteArray {
        val bs = ByteArray(count)
        System.arraycopy(src, begin, bs, 0, count)
        return bs
    }

    fun bytes_Hexstr(bArray: ByteArray): String {
        val sb = StringBuffer(bArray.size)
        for (i in bArray.indices) {
            val sTemp = Integer.toHexString(255 and bArray[i].toInt())
            if (sTemp.length < 2) {
                sb.append(0)
            }
            //b.append(sTemp.uppercase(Locale.getDefault()))
        }
        return sb.toString()
    }
}