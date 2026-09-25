package com.movealarm.app.photo

import com.movealarm.app.data.PhotoOrder
import kotlin.random.Random

object PhotoSelector {

    /** 이번 알람에 보여줄 사진의 인덱스. 랜덤일 때는 직전에 보여준 사진을 연속으로 고르지 않는다. */
    fun choose(order: PhotoOrder, count: Int, sequentialNext: Int, lastShown: Int, random: Random = Random): Int {
        require(count > 0) { "count must be positive" }
        return when (order) {
            PhotoOrder.SEQUENTIAL -> Math.floorMod(sequentialNext, count)
            PhotoOrder.RANDOM -> when {
                count == 1 -> 0
                lastShown !in 0 until count -> random.nextInt(count)
                else -> random.nextInt(count - 1).let { if (it >= lastShown) it + 1 else it }
            }
        }
    }
}
