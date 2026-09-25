package com.movealarm.app.photo

import com.movealarm.app.data.PhotoOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PhotoSelectorTest {

    @Test fun `순서대로 모드는 차례로 돌고 끝나면 처음으로`() {
        val picks = (0 until 7).map { PhotoSelector.choose(PhotoOrder.SEQUENTIAL, 3, it, -1) }
        assertEquals(listOf(0, 1, 2, 0, 1, 2, 0), picks)
    }

    @Test fun `사진이 지워져 개수가 줄어도 범위 안의 사진을 고른다`() {
        assertEquals(1, PhotoSelector.choose(PhotoOrder.SEQUENTIAL, 2, 5, 4))
    }

    @Test fun `랜덤 모드에서 사진이 1장이면 항상 그 사진`() {
        repeat(20) { assertEquals(0, PhotoSelector.choose(PhotoOrder.RANDOM, 1, 0, 0, Random(it))) }
    }

    @Test fun `랜덤 모드는 직전 사진을 연속으로 고르지 않고 나머지는 모두 나온다`() {
        val random = Random(42)
        val seen = mutableSetOf<Int>()
        repeat(500) {
            val pick = PhotoSelector.choose(PhotoOrder.RANDOM, 4, 0, 2, random)
            assertNotEquals(2, pick)
            assertTrue(pick in 0 until 4)
            seen += pick
        }
        assertEquals(setOf(0, 1, 3), seen)
    }

    @Test fun `직전 기록이 없으면 모든 사진이 후보`() {
        val random = Random(7)
        val seen = (0 until 300).map { PhotoSelector.choose(PhotoOrder.RANDOM, 3, 0, -1, random) }.toSet()
        assertEquals(setOf(0, 1, 2), seen)
    }
}
