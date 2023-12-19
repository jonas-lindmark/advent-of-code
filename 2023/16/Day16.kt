package `16`

import java.io.File
import kotlin.system.measureTimeMillis

data class Pos(val x: Long, val y: Long)
data class Pass(val pos: Pos, val direction: Direction)
data class Bound(val width: Long, val height: Long)
enum class MirrorOrientation(val rep: Char) { LEFT('\\'), RIGHT('/') }
enum class SplitterOrientation(val rep: Char) { HORIZONTAL('-'), VERTICAL('|') }
enum class Direction { NORTH, EAST, SOUTH, WEST }

data class Board(
    val objects: Map<Pos, Object>,
    val bound: Bound,
    val energized: MutableSet<Pos> = mutableSetOf(),
    val visited: MutableSet<Pass> = mutableSetOf(),
)


sealed class Object {
    class Mirror(val orientation: MirrorOrientation) : Object()
    class Splitter(val splitterOrientation: SplitterOrientation) : Object()
}


fun main() {

    val objects = readBoard("2023/16/input.txt")
    val bounds = Bound(110, 110)
    //val objects = readBoard("2023/16/sample.txt")
    //val bounds = Bound(10, 10)

    val time = measureTimeMillis {
        part1(objects, bounds)
        part2(objects, bounds)
    }
    println("Done in $time ms")
}

fun part1(objects: Map<Pos, Object>, bounds: Bound) {

    val board = Board(objects, bounds)

    traverseBoard(board, Pos(0, 0), Direction.EAST)

    println()
    println("Part 1: found ${board.energized.size} energized tiles")
    printBoard(board)
}

fun part2(objects: Map<Pos, Object>, bounds: Bound) {

    val topAndBottom = (0..<bounds.width).flatMap { x ->
        listOf(
            Pass(Pos(x, 0), Direction.SOUTH),
            Pass(Pos(x, bounds.width - 1), Direction.NORTH)
        )
    }

    val leftAndRight = (0..<bounds.height).flatMap { y ->
        listOf(
            Pass(Pos(0, y), Direction.EAST),
            Pass(Pos(bounds.height - 1, y), Direction.WEST)
        )
    }

   val highestBoard = (topAndBottom + leftAndRight).map {
       val board = Board(objects, bounds)
       traverseBoard(board, it.pos, it.direction)
       Pair(it, board)
   }.maxByOrNull { it.second.energized.size }!!

    println()
    println("Part 2: found ${highestBoard.second.energized.size} energized tiles starting on ${highestBoard.first}")
    printBoard(highestBoard.second)
}


private fun printBoard(board: Board) {
    (0..<board.bound.height).forEach { y ->
        (0..<board.bound.width).forEach { x ->
            val pos = Pos(x, y)
            if (board.energized.contains(pos)) print("#") else print(".")
        }
        println()
    }
}

fun traverseBoard(board: Board, pos: Pos, from: Direction) {
    val pass = Pass(pos, from)
    if (board.visited.contains(pass) || board.bound.outside(pos)) {
        return
    }

    //println("$pos $from visited=${board.visited.size}")

    board.energized.add(pos)
    board.visited.add(pass)
    board.objects[pos]?.let { obj ->
        when (obj) {
            is Object.Mirror -> obj.bounce(from).let {
                traverseBoard(board, it.next(pos), it)
            }

            is Object.Splitter -> obj.split(from).forEach {
                traverseBoard(board, it.next(pos), it)
            }
        }
    } ?: traverseBoard(board, from.next(pos), from)
}

fun Bound.outside(pos: Pos) =
    pos.x < 0 || pos.x >= width || pos.y < 0 || pos.y >= height

fun Direction.next(pos: Pos): Pos = when (this) {
    Direction.NORTH -> Pos(pos.x, pos.y - 1)
    Direction.EAST -> Pos(pos.x + 1, pos.y)
    Direction.SOUTH -> Pos(pos.x, pos.y + 1)
    Direction.WEST -> Pos(pos.x - 1, pos.y)
}

fun Object.Mirror.bounce(from: Direction) = when (this.orientation) {
    MirrorOrientation.RIGHT -> when (from) {
        Direction.NORTH -> Direction.EAST
        Direction.EAST -> Direction.NORTH
        Direction.SOUTH -> Direction.WEST
        Direction.WEST -> Direction.SOUTH
    }

    MirrorOrientation.LEFT -> when (from) {
        Direction.NORTH -> Direction.WEST
        Direction.EAST -> Direction.SOUTH
        Direction.SOUTH -> Direction.EAST
        Direction.WEST -> Direction.NORTH
    }
}

fun Object.Splitter.split(from: Direction) = when (this.splitterOrientation) {
    SplitterOrientation.HORIZONTAL -> when (from) {
        Direction.NORTH, Direction.SOUTH -> listOf(Direction.EAST, Direction.WEST)
        Direction.EAST, Direction.WEST -> listOf(from)
    }

    SplitterOrientation.VERTICAL -> when (from) {
        Direction.NORTH, Direction.SOUTH -> listOf(from)
        Direction.EAST, Direction.WEST -> listOf(Direction.NORTH, Direction.SOUTH)
    }
}

fun Object.print() {
    when (this) {
        is Object.Mirror -> print(this.orientation.rep)
        is Object.Splitter -> print(this.splitterOrientation.rep)
    }
}


fun readBoard(file: String) = File(file).readLines()
    .flatMapIndexed { y, line ->
        line.mapIndexed { x, char ->
            val pos = Pos(x.toLong(), y.toLong())
            val obj = when (char) {
                '\\' -> Object.Mirror(MirrorOrientation.LEFT)
                '/' -> Object.Mirror(MirrorOrientation.RIGHT)
                '-' -> Object.Splitter(SplitterOrientation.HORIZONTAL)
                '|' -> Object.Splitter(SplitterOrientation.VERTICAL)
                '.' -> null
                else -> throw IllegalArgumentException("Unknown character $char")
            }
            obj?.let { Pair(pos, it) }
        }
    }
    .filterNotNull()
    .toMap()

