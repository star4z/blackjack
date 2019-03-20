
fun ArrayList<Card>.getCard(game: Game): Card {
    return if (size > 0) {
        val i = (0 until size).random()
        val card = get(i)
        removeAt(i)
        card
    } else {
        game.initDeck()
        getCard(game)
    }
}

fun ArrayList<Card>.getTotal(): Int {
    var total = 0
    var aces = 0
    for (card in this) {
        total += if (card.pipValue > 10) {
            10
        } else {
            card.pipValue
        }
        if (card.pipValue == 1) {
            aces++
        }
    }

    //if adding the ace keeps the total under 21, ace = 11
    for (i in 1..aces) {
        if (total + 10 <= 21) {
            total += 10
        }
    }
    return total
}

fun ArrayList<Card>.containsDuplicates(): Boolean {
    var seenDupes = false
    val seenValues = HashSet<Int>()
    forEach {
        val value = if (it.pipValue > 10) 10 else it.pipValue //all 10-value cards are interchangeable
        if (seenValues.contains(value)) {
            seenDupes = true
        } else {
            seenValues.add(value)
        }
    }
    return seenDupes
}

//Can't override toString without extending class
fun ArrayList<Card>.niceToString(): String {
    var s = ""
    when (size) {
        1 -> s += "${component1()}"
        2 -> s += "${component1()} and ${component2()}"
        else -> forEachIndexed { index, card ->
            s += if (index == lastIndex) {
                "and $card"
            } else {
                "$card, "
            }
        }
    }
    return s
}