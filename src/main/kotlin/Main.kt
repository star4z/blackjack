import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

var money = HashMap<Int, Int>()
val cards = ArrayList<Card>()

val deck = List(52) { i -> getCard(i) }

val numberOfPlayers = 1
val playersCards = HashMap<Int, ArrayList<Card>>()

val dealerCards = ArrayList<Card>()

fun getCard(i: Int): Card {
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

val getAmount = "Enter an amount to bet (between \$5 and \$20):"
val badAmount = "Entered amount was not between $5 and $20."

val getOption = "What will you do?\n1. Stand\n2. Hit"
val badOption = "Entry was not a valid option."

data class Card(val faceValue: String, val suite: String, val value: Int)

fun main() {
    val gameEnded = false

    initDeck(1)

    for (i in 1..numberOfPlayers) {
        playersCards[i] = ArrayList()
        money[i] = 1000
    }

    while (!gameEnded) {
        var pot = 0
        for (i in playersCards.keys) {
            val amountToBet = getValidInput(getAmount, badAmount) { it in 5..20 }

            money[i] = money[i]!!.minus(amountToBet)
            pot += amountToBet

            playersCards[i]!!.add(dealCard())
            playersCards[i]!!.add(dealCard())

            dealerCards.add(dealCard())
            dealerCards.add(dealCard())

            var option = 2
            while (option == 2) {
                println("Your cards are ${playersCards[i]} and your total is ${playersCards[i]!!.getTotal()}")
                println("The dealer shows ${dealerCards[dealerCards.lastIndex]}")
                println("What will you do?\n1. Stand\n2. Hit")

                option = getValidInput(getOption, badOption) { it in 1..2 }

                if (option == 2) {
                    playersCards[i]!!.add(dealCard())
                }
                println("Your cards are ${playersCards[i]} and your total is ${playersCards[i]!!.getTotal()}")
                if (playersCards[i]!!.getTotal() > 21){
                    println("You have gone bust.")
                    option = 0
                }
            }
        }
        println("The dealer shows ${dealerCards[dealerCards.lastIndex - 1]}")
        println("The dealer has a total of ${dealerCards.getTotal()}")
        if (dealerCards.getTotal() <= 16){
            dealerCards.add(dealCard())
            println("The dealer hit, and drew ${dealerCards[dealerCards.lastIndex]}")
            println("The dealer's now has a total of ${dealerCards.getTotal()}")
        }

        val dealerTotal = dealerCards.getTotal()

        val winners = HashSet<Int>()
        if (dealerTotal > 21){
            for (i in 1..numberOfPlayers){
                if (playersCards[i]!!.getTotal() <= 21){
                    winners.add(i)
                }
            }
            println("The dealer went bust!")
        } else {
            for (i in 1..numberOfPlayers){
                val playerTotal = playersCards[i]!!.getTotal()
                if (playerTotal in (dealerTotal + 1)..21){
                    winners.add(i)
                }
            }
        }

        if (winners.isEmpty()){
            println()
        } else {

        }
    }
}

fun getValidInput(outputString: String, incorrectString: String, conditional: (Int) -> Boolean): Int {
    var enteredValidEntry = false
    var input = -1
    while (!enteredValidEntry) {
        println(outputString)
        input = readLine()?.toInt() ?: -1

        enteredValidEntry = conditional(input)

        if (!enteredValidEntry) {
            println(incorrectString)
        }
    }

    return input
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

fun ArrayList<Card>.getTotal(): Int {
    var total = 0
    for (card in this){
        total += card.value
    }
    return total
}