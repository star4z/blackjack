import kotlin.math.absoluteValue

var money = HashMap<Int, Int>()
val cards = ArrayList<Card>()

val deck = List(52) { i -> generateCard(i) }

val playersCards = HashMap<Int, ArrayList<Card>>()
val bets = HashMap<Int, Int>()

val dealerCards = ArrayList<Card>()
var dealerMoney = 0

fun generateCard(seed: Int): Card {
    val a = seed % 13
    val b = seed / 13
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
        a + 1
    )
}

const val getPlayerCount = "How many players? (Must be between 1 and 7)"
const val badPlayerCount = "Number of players must be between 1 and 7."

val getAiCount: (Int) -> String = { "How many players are AI? (Must be between 1 and $it)" }
val badAiCount: (Int) -> String = { "Number of AI players must be between 1 and $it" }

const val getNumOfDecks = "How many decks? (Must be between 1 and 8)"
const val badNumOfDecks = "Number of decks must be between 1 and 8."

val getHandsToPlay: (Int) -> String = { "How many hands will you play? (Must be at least 1 and no more than $it)" }
const val badHandsToPlay = "You must play at least 1 hand."

const val getAmount = "Enter an amount to bet (between \$5 and \$20):"
const val badAmount = "Entered amount was not a number between $5 and $20."

const val getOption = "What will you do?\n1. Stand\n2. Hit"
const val badOption = "Entry was not a valid option."

const val HIT = 2
const val STAND = 1

data class Card(val faceValue: String, val suite: String, val identifyingValue: Int) {
    override fun toString(): String {
        return "$faceValue of $suite"
    }
}

fun main() {

    val playerCount = getValidInput(getPlayerCount, badPlayerCount) { it in 1..7 }
    val aiCount = getValidInput(getAiCount(playerCount), badAiCount(playerCount)) { it in 1..playerCount }
    val numOfDecks = getValidInput(getNumOfDecks, badNumOfDecks) { it in 1..8 }
    val maxHands = (numOfDecks * 52) / (4 * (playerCount + 1))
    val handsToPlay = getValidInput(getHandsToPlay(maxHands), badHandsToPlay) { it in 1..maxHands }

    initDeck(numOfDecks)

    val lastRoundResults = HashMap<Int, Int>()
    var handNo = 1

    for (i in 1..playerCount) {
        playersCards[i] = ArrayList()
        money[i] = 1000
        bets[i] = 0
    }


    while (handNo <= handsToPlay) {
        println("Hands $handNo:")
        for (i in playersCards.keys) {
            println("Player $i, you have $${money[i]}.")

            if (lastRoundResults.containsKey(i) && lastRoundResults[i] == 3) {
                println("Player $i, since you pushed last round, your bet of $${bets[i]} remains.")
            } else {
                val amountToBet = if (i < (playerCount - aiCount)) {
                    getValidInput(getAmount, badAmount) { it in 5..20 }
                } else {
                    (5..20).random()
                }
                println("Player $i bet $$amountToBet.")
                money[i] = money[i]!!.minus(amountToBet)
                bets[i] = amountToBet
            }

        }

        for (i in playersCards.keys) {
            playersCards[i]!!.add(dealCard())
            playersCards[i]!!.add(dealCard())

            println("Player $i got ${playersCards[i]}.")
        }

        dealerCards.add(dealCard())
        dealerCards.add(dealCard())
        println("The dealer shows ${dealerCards[dealerCards.lastIndex]}.")

        for (i in playersCards.keys) {
            var option = 0
            while (option != STAND) {
                val playerCards = playersCards[i]
                val total = playerCards!!.getTotal()
                println("Player $i, your cards are $playerCards and you have $total points.")

                if (total > 21) {
                    println("You have gone bust.")
                    option = STAND
                } else if (playerCards.size == 2 && total == 21) {
                    println("You have Blackjack.")
                    option = STAND
                } else {
                    option = if (i < (playerCount - aiCount)) {
                        getValidInput(getOption, badOption) { it in 1..2 }
                    } else {
                        if (playerCards.getTotal() + 5 < 16) {
                            HIT
                        } else {
                            if (evalStand(playerCards) > 0.1){
                                HIT
                            } else {
                                STAND
                            }
                        }
                    }
                    if (option == HIT) {
                        println("Player $i hit.")
                        val card = dealCard()

                        println("You drew $card.")
                        playerCards.add(card)
                    } else if (option == STAND) {
                        println("Player $i stands.")
                    }
                }
            }
        }

        println("The dealer reveals ${dealerCards[dealerCards.lastIndex - 1]}")
        println("The dealer has a total of ${dealerCards.getTotal()} points.")

        while (dealerCards.getTotal() < 16) {
            dealerCards.add(dealCard())
            println("The dealer hit, and drew ${dealerCards[dealerCards.lastIndex]}.")
            println("The dealer's now has a total of ${dealerCards.getTotal()} points.")
        }

        val dealerTotal = dealerCards.getTotal()

        if (dealerTotal > 21) {
            println("The dealer went bust!")
        }
        playersCards.keys.forEach {
            lastRoundResults[it] = distributeWinnings(it, dealerTotal)
            playersCards[it]!!.clear()

        }

        handNo++
        dealerCards.clear()
        println()
    }

    println("The dealer ${if (dealerMoney >= 0) "made" else "lost"} $${dealerMoney.absoluteValue}.")

    for (player in playersCards.keys) {
        val playerMoney = money[player]!! - 1000
        println("Player $player ${if (playerMoney >= 0) "made" else "lost"} $${playerMoney.absoluteValue}.")
    }

}


fun initDeck(numOfDecks: Int) {
    cards.clear()
    //Shuffle deck?
    repeat(numOfDecks) {
        cards.addAll(deck)
    }
}

fun dealCard(): Card {
    return cards.getCard()
}

fun evalStand(cards: ArrayList<Card>): Float {
    val hand = 5 + cards.getTotal()
    val sums = cards.map {
        var sum = 0
        cards.forEach {
            sum += it.identifyingValue + hand
        }
        sum
    }
    return sums.filter { it <= 21 }.size/sums.size.toFloat()
}

fun distributeWinnings(playerNumber: Int, dealerTotalValue: Int): Int {
    val playerCards = playersCards[playerNumber]
    val totalValue = playerCards!!.getTotal()
    val hands = playerCards.size - 2

    var moneyPaid = 0
    var returnValue = 0
    if (totalValue == 21 && hands == 0) {
        moneyPaid = (bets[playerNumber]!! * 2.5).toInt()
        println("Player $playerNumber had Blackjack and got a payout of $$moneyPaid.")
        returnValue = 1

    } else if ((totalValue in (dealerTotalValue + 1)..20) or (totalValue <= 21 && dealerTotalValue > 21)) {
        moneyPaid = bets[playerNumber]!! * 2
        println("Player $playerNumber had a total of $totalValue and got a payout of $$moneyPaid.")
        returnValue = 1

    } else if (totalValue < 21 && totalValue == dealerTotalValue) {
        println("Player $playerNumber had a push with a total of $totalValue.")
        returnValue = 3

    } else if (totalValue < dealerTotalValue) {
        println("Player $playerNumber lost with a score of $totalValue, which was less than the dealer's score.")

    } else {
        println("Player $playerNumber went bust, with a total of $totalValue.")

    }

    money[playerNumber] = money[playerNumber]!! + moneyPaid
    dealerMoney += -moneyPaid + bets[playerNumber]!!

    return returnValue
}

fun getValidInput(outputString: String, incorrectString: String, conditional: (Int) -> Boolean): Int {
    var enteredValidEntry = false
    var input = -1
    while (!enteredValidEntry) {
        println(outputString)
        val next = readLine()
        input = try {
            next?.toInt() ?: -1
        } catch (e: NumberFormatException) {
            -1
        }

        enteredValidEntry = conditional(input)

        if (!enteredValidEntry) {
            println(incorrectString)
        }
    }

    return input
}


fun ArrayList<Card>.getCard(): Card {
    val i = (0 until size).random()
    val card = get(i)
    removeAt(i)
    return card
}

fun ArrayList<Card>.getTotal(): Int {
    var total = 0
    var aces = 0
    for (card in this) {
        total += if (card.identifyingValue > 10) {
            10
        } else {
            card.identifyingValue
        }
        if (card.identifyingValue == 1) {
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
