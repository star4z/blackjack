import kotlin.random.Random

var money = 0
val cards = ArrayList<Card>()
var random = Random(System.currentTimeMillis())

val deck = List(52) { i -> getCard(i) }

fun getCard(i: Int): Card{
    val a = i % 13
    val b = i / 13
    return Card(
        when (a) {
            0 -> "Ace"
            10 -> "Jack"
            11 -> "Queen"
            12 -> "King"
            else -> (a + 1).toString()
        },
        when (b) {
            0 -> "Spades"
            1 -> "Hearts"
            2 -> "Clubs"
            else -> "Diamonds"
        },
        if (a < 10) {
            a + 1
        } else {
            10
        }
    )
}

fun main() {
    println(deck)

    money = 1000
    val gameEnded = false

    initDeck(1)

    while (!gameEnded) {
        val amountToBet = readLine()?.toInt()

    }
}

fun initDeck(numOfDecks: Int) {
    cards.clear()
    repeat(numOfDecks) {
        cards.addAll(deck)
    }
}

fun dealCard(): Card {
    return cards.getCard()
}

fun ArrayList<Card>.getCard(): Card {
    val i = (0..size).random()
    val card = get(i)
    removeAt(i)
    return card
}

data class Card(val faceValue: String, val suite: String, val value: Int)