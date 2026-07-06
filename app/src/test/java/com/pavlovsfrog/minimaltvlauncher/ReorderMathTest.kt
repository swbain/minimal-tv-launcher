package com.pavlovsfrog.minimaltvlauncher

import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderMathTest {

  private val cols = 4

  @Test
  fun `right moves the tile one slot later, shifting the neighbor back`() {
    assertEquals(
      listOf("a", "c", "b", "d", "e"),
      moveWithinVisible(listOf("a", "b", "c", "d", "e"), "b", MoveDirection.Right, cols),
    )
  }

  @Test
  fun `left moves the tile one slot earlier`() {
    assertEquals(
      listOf("b", "a", "c", "d"),
      moveWithinVisible(listOf("a", "b", "c", "d"), "b", MoveDirection.Left, cols),
    )
  }

  @Test
  fun `down moves the tile a full row, reflowing the slots between`() {
    // a is pulled out of slot 0 and inserted at slot 4; b,c,d,e each shift one toward slot 0.
    assertEquals(
      listOf("b", "c", "d", "e", "a", "f", "g", "h"),
      moveWithinVisible(listOf("a", "b", "c", "d", "e", "f", "g", "h"), "a", MoveDirection.Down, cols),
    )
  }

  @Test
  fun `up moves the tile a full row earlier`() {
    assertEquals(
      listOf("e", "a", "b", "c", "d"),
      moveWithinVisible(listOf("a", "b", "c", "d", "e"), "e", MoveDirection.Up, cols),
    )
  }

  @Test
  fun `left is blocked in the first column`() {
    val list = listOf("a", "b", "c", "d", "e")
    assertEquals(list, moveWithinVisible(list, "a", MoveDirection.Left, cols))
  }

  @Test
  fun `right is blocked in the last column`() {
    val list = listOf("a", "b", "c", "d", "e", "f", "g", "h")
    // d sits in column 3 (the last column) — no wrap to the next row.
    assertEquals(list, moveWithinVisible(list, "d", MoveDirection.Right, cols))
  }

  @Test
  fun `right is blocked on the last item of a partial row`() {
    val list = listOf("a", "b", "c", "d", "e")
    assertEquals(list, moveWithinVisible(list, "e", MoveDirection.Right, cols))
  }

  @Test
  fun `up is blocked on the top row`() {
    val list = listOf("a", "b", "c", "d", "e")
    assertEquals(list, moveWithinVisible(list, "c", MoveDirection.Up, cols))
  }

  @Test
  fun `down is blocked when there is no full row below`() {
    val list = listOf("a", "b", "c", "d", "e")
    // c would target slot 6, past the end — blocked, no wrap.
    assertEquals(list, moveWithinVisible(list, "c", MoveDirection.Down, cols))
  }

  @Test
  fun `single item never moves`() {
    val list = listOf("only")
    assertEquals(list, moveWithinVisible(list, "only", MoveDirection.Right, cols))
    assertEquals(list, moveWithinVisible(list, "only", MoveDirection.Down, cols))
  }

  @Test
  fun `absent package leaves the list unchanged`() {
    val list = listOf("a", "b", "c")
    assertEquals(list, moveWithinVisible(list, "z", MoveDirection.Right, cols))
  }

  @Test
  fun `threading a reorder keeps hidden apps pinned to their positions`() {
    // full order has two hidden apps (H1, H2) interleaved with visible a,b,c.
    val full = listOf("a", "H1", "b", "c", "H2")
    val newVisible = listOf("b", "a", "c") // a and b swapped in the visible sequence
    assertEquals(
      listOf("b", "H1", "a", "c", "H2"),
      threadVisibleIntoFull(full, newVisible),
    )
  }
}
