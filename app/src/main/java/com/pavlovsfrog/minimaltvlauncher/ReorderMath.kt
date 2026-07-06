package com.pavlovsfrog.minimaltvlauncher

/** Home grid width. Single source of truth shared by the grid layout and the reorder math. */
const val GRID_COLUMNS = 4

/** D-pad reorder directions in Move mode. */
enum class MoveDirection { Left, Right, Up, Down }

/**
 * Reorders the *visible* (favorites) grid by moving [moving] one step in [dir], as a list reflow:
 * the tile leaves its slot and is inserted at the target slot, every tile in between shifting one
 * position toward the vacated slot (not a pairwise swap). Edges do not wrap — a blocked move
 * (row edge, top/bottom row, or [moving] absent) returns the list unchanged.
 *
 * Left/Right move ±1; Up/Down move ±[columns] (one row).
 */
fun moveWithinVisible(
  visible: List<String>,
  moving: String,
  dir: MoveDirection,
  columns: Int,
): List<String> {
  val i = visible.indexOf(moving)
  if (i < 0) return visible
  val lastIndex = visible.lastIndex

  val blocked = when (dir) {
    MoveDirection.Left -> i % columns == 0
    MoveDirection.Right -> i % columns == columns - 1 || i == lastIndex
    MoveDirection.Up -> i < columns
    MoveDirection.Down -> i + columns > lastIndex
  }
  if (blocked) return visible

  val target = when (dir) {
    MoveDirection.Left -> i - 1
    MoveDirection.Right -> i + 1
    MoveDirection.Up -> i - columns
    MoveDirection.Down -> i + columns
  }

  return visible.toMutableList().apply { add(target, removeAt(i)) }
}

/**
 * Threads a reordered visible sequence back into the full package order while keeping every
 * non-visible (hidden) package pinned to its absolute position: walk [fullOrder], and wherever a
 * package is part of the visible set, emit the next package from [newVisible] instead.
 *
 * Precondition: the visible packages in [fullOrder] are exactly the set in [newVisible].
 */
fun threadVisibleIntoFull(fullOrder: List<String>, newVisible: List<String>): List<String> {
  val visibleSet = newVisible.toHashSet()
  val next = newVisible.iterator()
  return fullOrder.map { if (it in visibleSet && next.hasNext()) next.next() else it }
}
